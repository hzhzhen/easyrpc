package net.easyrpc.engine.io.impl

import com.alibaba.fastjson.JSON
import net.easyrpc.engine.io.Engine
import net.easyrpc.engine.io.handler.ConnectHandler
import net.easyrpc.engine.io.handler.ErrorHandler
import net.easyrpc.engine.io.handler.RequestHandler
import net.easyrpc.engine.io.model.BaseRequest
import net.easyrpc.engine.io.protocol.EngineProtocol
import net.easyrpc.engine.io.protocol.EngineProtocol.Message
import net.easyrpc.engine.io.protocol.RequestProtocol
import net.easyrpc.engine.io.protocol.RequestProtocol.Response
import net.easyrpc.engine.io.protocol.Status
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * @author chpengzh
 */
open class IOEngine : Engine {

    //编码协议,默认为Json编码
    protected var engineProtocol: EngineProtocol = JsonEngineProtocol()
    protected var requestProtocol: RequestProtocol = JsonRequestProtocol()

    protected val eventHandlers = ConcurrentHashMap<String, (Int, Long, ByteArray) -> Unit>()
    protected val requestHandlers = ConcurrentHashMap<String, RequestHandler>()

    private val servers = ConcurrentHashMap<InetSocketAddress, Acceptor>()
    private val transports = ConcurrentHashMap<Int, Transport>()
    private val messageQueue = ConcurrentLinkedQueue<MessageTask>()

    private val requestContainer = RequestContainer()

    //IO事件处理Selector
    private val selector = Selector.open()

    private val main = Executors.newSingleThreadScheduledExecutor()     //事件轮询线程
    private val net = Executors.newSingleThreadExecutor()               //连接为单线程池执行
    private val eventService = Executors.newFixedThreadPool(10)         //事件执行线程

    //事件轮询周期
    private val POLLING: Long = 50 //time unit: μs
    private val POLLING_UNIT = TimeUnit.MICROSECONDS

    constructor() {
        subscribeRequest()//初始化Request-IO
        main.scheduleWithFixedDelay({//高频主事件轮询
            ioPolling()//连接数据轮询
            acceptPolling()//请求连接轮询
            requestContainer.update()//请求任务状态更新
        }, 0, POLLING, POLLING_UNIT)
    }

    constructor(protocol: EngineProtocol, requestProtocol: RequestProtocol) : this() {
        this.engineProtocol.close()
        this.engineProtocol = protocol
        this.requestProtocol = requestProtocol
    }

    override fun connect(address: InetSocketAddress, onSuccess: ConnectHandler, onError: ErrorHandler): Engine {
        net.submit {
            try {
                val channel = SocketChannel.open()
                channel.connect(address)
                channel.configureBlocking(false)
                val transport = Transport(channel, onError)
                channel.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE, transport)
                transports.put(transport.hashCode(), transport)
                onSuccess.onEvent(transport.hashCode())
            } catch(error: IOException) {
                onError.onError(error)
            }
        }
        return this
    }

    override fun listen(address: InetSocketAddress, onAccept: ConnectHandler, onError: ErrorHandler): Engine {
        net.submit {
            if (servers[address] != null) return@submit
            val ssc = ServerSocketChannel.open().bind(address).configureBlocking(false) as ServerSocketChannel
            servers[address] = Acceptor(ssc = ssc, accept = onAccept, error = onError)
        }
        return this
    }

    override fun disconnect(hash: Int) {
        net.submit {
            val transport = transports[hash];
            if (transport != null) {
                transport.channel.close()
                transports.remove(hash)
            }
        }
    }

    override fun close(address: InetSocketAddress) {
        net.submit {
            val acceptor = servers[address]
            if (acceptor != null) {
                acceptor.ssc.close()
                servers.remove(address)
            }
        }
    }

    override fun onRequest(tag: String, handler: RequestHandler): Engine {
        requestHandlers.put(tag, handler)
        return this
    }

    override fun send(request: BaseRequest) {
        val transport = transports[request.transportHash]
        if (transport == null) {
            eventService.submit {
                try {
                    request.onTimeout("Transport@${request.transportHash} is not exists!")
                } finally {
                    request.onComplete()
                }
            }
        } else {
            val id = appendQueue(transport, "/request-io/request",
                    requestProtocol.serializeRequest(RequestProtocol.Request(request.tag, request.data)))
            requestContainer.add(request.transportHash, id, request);
        }
    }

    override fun terminate() {
        eventService.shutdownNow()

        main.shutdownNow()
        engineProtocol.close()

        transports.forEach({ this.disconnect(it.hashCode()) })
        servers.forEach({ address, acceptor -> close(address) })
        net.shutdown()
    }

    //send/response 的消息订阅
    private fun subscribeRequest() {
        eventHandlers.put("/request-io/request", { tcpHash, id, packageData ->
            val request = requestProtocol.antiSerializeRequest(packageData) ?: return@put

            val response: Response;

            val handler = requestHandlers[request.tag]
            if (handler == null) {
                response = Response(id, Status.NOT_FOUND.code, Status.NOT_FOUND.message.toByteArray())
            } else {
                try {
                    response = Response(id, Status.OK.code,
                            handler.onData(request.data) ?: ByteArray(0, { i -> '\u0000'.toByte() }))
                } catch (e: Exception) {
                    response = Response(id, Status.REMOTE_ERROR.code, e.toString().toByteArray())
                }
            }
            appendQueue(transports[tcpHash]!!, "/request-io/response",
                    requestProtocol.serializeResponse(response.meta(id)))
        })
        eventHandlers.put("/request-io/response", { tcpHash, id, packageData ->
            val response = requestProtocol.antiSerializeResponse(packageData) ?: return@put
            val request = requestContainer.remove(tcpHash, response.requestId) ?: return@put
            try {
                when {
                    response.status == Status.OK.code -> request.onResponse(response.data)

                    else -> request.onFail(response.status, "Request sending to transport@" +
                            "$tcpHash fail for ${String(response.data)}")
                }
            } finally {
                request.onComplete()
            }
        })
    }

    //监听连接数据
    private fun ioPolling() {
        try {
            selector.selectNow()
            selector.selectedKeys().forEach {
                val transport = it.attachment() as Transport
                try {
                    if (it.isReadable) {
                        readMessage(transport)
                    } else if (it.isWritable) {
                        do {
                            val event = messageQueue.poll()
                            if (event != null) {
                                val tcp = transports[event.tcpHash]
                                val msg = event.message
                                if (tcp != null && msg != null) sendMessage(tcp, msg)
                            } else break;
                        } while (true)
                    }
                } catch (e: IOException) {
                    transport.errorHandler.onError(e)
                    disconnect(transport.hashCode())//handle exception if disconnected
                }
            }
        } catch (ignore: IOException) {
        }
    }

    //监听建立连接
    private fun acceptPolling() {
        servers.forEach({ port, acceptor ->
            try {
                val channel = acceptor.ssc.accept()
                if (channel != null) {
                    channel.configureBlocking(false)
                    val transport = Transport(channel, acceptor.error)
                    channel.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE, transport)
                    transports.put(transport.hashCode(), transport)
                    acceptor.accept.onEvent(transport.hashCode())
                }
            } catch (ignore: IOException) {
            }
        })
    }

    //读消息
    private fun readMessage(transport: Transport) {
        transport.channel.blockingLock().apply {
            val buffer = ByteBuffer.allocate(32 * 1024)
            val flag = transport.channel.read(buffer);
            if (flag == -1) throw IOException("session disconnected")
            if (buffer.position() == 0) return
            val bytes = buffer.array().copyOf(buffer.position())
            engineProtocol.antiSerialize(bytes, {
                eventService.submit { eventHandlers[it.tag]?.invoke(transport.hashCode(), it.id, it.data) }
            })
        }
    }

    //写消息
    private fun sendMessage(transport: Transport, message: Message) {
        transport.channel.blockingLock().apply {
            engineProtocol.serialize(message, {
                transport.channel.write(ByteBuffer.wrap(it))
            })
        }
    }

    //向消息队列中添加消息
    private fun appendQueue(transport: Transport, tag: String, bytes: ByteArray): Long {
        val task = MessageTask(transport.hashCode())
        task.obtain(Message(task.hashCode().toLong(), tag, bytes))
        messageQueue.add(task)
        return task.hashCode().toLong()
    }

    private inner class Acceptor(val ssc: ServerSocketChannel, val accept: ConnectHandler, val error: ErrorHandler)

    private inner class MessageTask(val tcpHash: Int) {
        var message: Message? = null;
        fun obtain(msg: Message) {
            message = msg;
        }
    }

    private inner class RequestContainer {

        val tasks = ConcurrentHashMap<String, Task> ()
        val updateLock: ReentrantLock = ReentrantLock()

        fun update() {
            updateLock.apply {
                val now = System.currentTimeMillis();
                tasks.entries.removeAll {
                    if (now > it.value.timestamp + it.value.timeout) {
                        eventService.execute {
                            try {
                                it.value.request.onTimeout("Transport@${it.value.request.transportHash}" +
                                        " is request timeout, please make sure if it is connectible!")
                            } finally {
                                it.value.request.onComplete()
                            }
                        }
                        return@removeAll true
                    } else {
                        return@removeAll false
                    }
                }
            }
        }

        fun add(tcpHash: Int, id: Long, request: BaseRequest) {
            updateLock.apply {
                tasks.put("$tcpHash-$id", Task(request = request, timeout = request.timeout()))
            }
        }

        fun remove(tcpHash: Int, id: Long): BaseRequest? {
            updateLock.lock()
            try {
                val task = tasks["$tcpHash-$id"]
                if (task == null) {
                    return null
                } else {
                    tasks.remove("$tcpHash-$id")
                    return task.request;
                }
            } finally {
                updateLock.unlock()
            }
        }

        inner class Task(val request: BaseRequest, val timeout: Long, var timestamp: Long = System.currentTimeMillis())
    }

    inner class Transport(var channel: SocketChannel, var errorHandler: ErrorHandler)
}

class JsonEngineProtocol : EngineProtocol {

    private val service = Executors.newSingleThreadExecutor()

    override fun antiSerialize(bytes: ByteArray, callback: EngineProtocol.AntiSerializeCallback) {
        service.submit {
            BufferedReader(InputStreamReader(ByteArrayInputStream(bytes))).use {
                while (true) {
                    val line = it.readLine() ?: break
                    try {
                        callback.onSerialize(JSON.parseObject(line, Message::class.java));
                    } catch(ignore: Exception) {
                        //ignore.printStackTrace()
                    }
                }
            }
        }
    }

    override fun serialize(message: Message, callback: EngineProtocol.SerializeCallBack) {
        service.submit({
            callback.onAntiSerialize((JSON.toJSONString(message) + "\n").toByteArray())
        });
    }

    override fun close() {
        service.shutdownNow()
    }
}

class JsonRequestProtocol : RequestProtocol {

    override fun antiSerializeRequest(bytes: ByteArray): RequestProtocol.Request? {
        try {
            return JSON.parseObject<RequestProtocol.Request>(bytes, RequestProtocol.Request::class.java)
        } catch (e: Exception) {
            return null
        }
    }

    override fun serializeRequest(request: RequestProtocol.Request): ByteArray = JSON.toJSONBytes(request)

    override fun antiSerializeResponse(bytes: ByteArray): Response? {
        try {
            return JSON.parseObject<Response>(bytes, Response::class.java)
        } catch (e: Exception) {
            return null
        }
    }

    override fun serializeResponse(response: Response): ByteArray = JSON.toJSONBytes(response);

}