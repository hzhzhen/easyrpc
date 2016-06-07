package net.easyrpc.request.io.impl

import com.alibaba.fastjson.JSON
import net.easyrpc.request.io.api.Client
import net.easyrpc.request.io.api.Engine
import net.easyrpc.request.io.api.Node
import net.easyrpc.request.io.api.Server
import net.easyrpc.request.io.handler.ConnectHandler
import net.easyrpc.request.io.handler.DataHandler
import net.easyrpc.request.io.handler.ErrorHandler
import net.easyrpc.request.io.handler.RequestHandler
import net.easyrpc.request.io.model.BaseRequest
import net.easyrpc.request.io.protocol.EngineProtocol
import net.easyrpc.request.io.protocol.EngineProtocol.Message
import net.easyrpc.request.io.protocol.RequestProtocol
import net.easyrpc.request.io.protocol.RequestProtocol.Response
import net.easyrpc.request.io.protocol.Status
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
import java.util.concurrent.atomic.AtomicLong

/***
 * 一套标准实现, 多套接口用于发布
 */
internal class IONode(val engineProtocol: EngineProtocol = JsonEngineProtocol(),
                      val requestProtocol: RequestProtocol = JsonRequestProtocol(),
                      val serverMode: Boolean = true) : Node, Client, Server, Engine {

    //handler
    private val eventHandlers = ConcurrentHashMap<String, (Int, Long, ByteArray) -> Unit>()
    private val requestHandlers = ConcurrentHashMap<String, RequestHandler>()

    //server & transport
    private val servers = ConcurrentHashMap<InetSocketAddress, Acceptor>()
    private val transports = ConcurrentHashMap<Int, Transport>()

    //task container
    private val requestContainer = RequestContainer()
    private val serializeId = AtomicLong(0)

    //IO事件处理Selector
    private val selector = Selector.open()

    //work executor service
    private val main = Executors.newScheduledThreadPool(2)//事件轮询线程
    private val eventService = Executors.newFixedThreadPool(4)//事件执行线程

    //main event polling time unit
    private val POLLING: Long = 1 //time unit: μs
    private val POLLING_UNIT = TimeUnit.MILLISECONDS

    private val HEARTBEAT: Long = 50
    private val HEARTBEAT_UNIT = TimeUnit.MILLISECONDS

    init {
        subscribeRequest()//初始化Request-IO
        main.scheduleWithFixedDelay({//事件轮询
            socketPolling()//连接数据轮询
            if (serverMode) serverSocketPolling()//请求连接轮询
        }, 0, POLLING, POLLING_UNIT)
        main.scheduleWithFixedDelay({
            requestContainer.poll()//请求任务状态更新
        }, 0, HEARTBEAT, HEARTBEAT_UNIT)
    }

    @Synchronized
    override fun connect(address: InetSocketAddress, onSuccess: ConnectHandler?, onError: ErrorHandler?): IONode {
        try {
            val channel = SocketChannel.open()
            channel.connect(address)
            channel.configureBlocking(false)
            val transport = Transport(channel, onError ?: ErrorHandler { })
            channel.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE, transport)
            transports.put(transport.hashCode(), transport)
            onSuccess?.onEvent(transport.hashCode())
        } catch(error: IOException) {
            onError?.onError(error)
        }
        return this
    }

    @Synchronized
    override fun listen(address: InetSocketAddress, onAccept: ConnectHandler?, onError: ErrorHandler?): IONode {
        if (servers[address] != null) return this
        val ssc = ServerSocketChannel.open().bind(address).configureBlocking(false) as ServerSocketChannel
        servers[address] = Acceptor(
                ssc = ssc, accept = onAccept ?: ConnectHandler { }, error = onError ?: ErrorHandler { }
        )
        return this
    }

    @Synchronized
    override fun disconnect(hash: Int) {
        val transport = transports[hash];
        if (transport != null) {
            transport.channel.close()
            transports.remove(hash)
        }
    }

    @Synchronized
    override fun close(address: InetSocketAddress) {
        val acceptor = servers[address]
        if (acceptor != null) {
            acceptor.ssc.close()
            servers.remove(address)
        }
    }

    override fun onRequest(tag: String, handler: RequestHandler): IONode {
        requestHandlers.put(tag, handler)
        return this
    }

    override fun send(request: BaseRequest): Boolean {
        val transport = transports[request.transportHash] ?: return false
        val id = appendQueue(transport, "/request-io/request",
                requestProtocol.serializeRequest(RequestProtocol.Request(request.tag, request.data)))
        if (id == -1L) return false
        requestContainer.add(id, request);
        return true
    }

    override fun on(metaData: String, handler: DataHandler) {
        eventHandlers.put(metaData, { tcpHash, id, packageData -> handler.onData(tcpHash, id, packageData) })
    }

    override fun emit(tcpHash: Int, metaData: String, data: ByteArray): Long {
        val transport = transports[tcpHash] ?: return -1;
        return appendQueue(transport, metaData, data);
    }

    override fun terminate() {
        eventService.shutdownNow()

        main.shutdownNow()
        engineProtocol.close()

        transports.forEach({ this.disconnect(it.hashCode()) })
        servers.forEach({ address, acceptor -> close(address) })
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
                    response = Response(id, Status.OK.code, handler.onData(request.data) ?:
                            ByteArray(0, { i -> '\u0000'.toByte() }))
                } catch (e: Exception) {
                    response = Response(id, Status.REMOTE_ERROR.code, e.toString().toByteArray())
                }
            }
            appendQueue(transports[tcpHash]!!, "/request-io/response",
                    requestProtocol.serializeResponse(response.meta(id)))
        })
        eventHandlers.put("/request-io/response", { tcpHash, id, packageData ->

            val response = requestProtocol.antiSerializeResponse(packageData) ?: return@put

            val request = requestContainer.remove(response.requestId) ?: return@put

            try {
                if (response.status == Status.OK.code)
                    request.onResponse(response.data)
                else
                    request.onFail(response.status, "Request sending to transport@" +
                            "$tcpHash fail for ${String(response.data)}")
            } finally {
                request.onComplete()
            }
        })
    }

    //监听连接数据
    private fun socketPolling() {
        try {
            selector.selectNow()
            selector.selectedKeys().forEach {
                val transport = it.attachment() as Transport
                try {
                    if (it.isReadable) {
                        readMessage(transport)
                    } else if (it.isWritable) {
                        do {
                            val event = transport.taskQueue.poll() ?: break
                            sendMessage(transport, event)
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
    private fun serverSocketPolling() {
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
        val buffer = ByteBuffer.allocate(32 * 1024)
        val flag = transport.channel.read(buffer);
        if (flag == -1) throw IOException("session disconnected")
        if (buffer.position() == 0) return
        val bytes = buffer.array().copyOf(buffer.position())
        engineProtocol.antiSerialize(bytes, {
            eventService.submit { eventHandlers[it.tag]?.invoke(transport.hashCode(), it.id, it.data) }
        })
    }

    //写消息
    private fun sendMessage(transport: Transport, message: Message) {
        engineProtocol.serialize(message, {
            transport.channel.write(ByteBuffer.wrap(it))
        })
    }

    //向消息队列中添加消息
    private fun appendQueue(transport: Transport, metaData: String, data: ByteArray): Long {
        val id = serializeId.incrementAndGet()
        if (transport.taskQueue.add(Message(id, metaData, data))) return id
        return -1
    }

    inner class Acceptor(val ssc: ServerSocketChannel, val accept: ConnectHandler, val error: ErrorHandler)

    inner class Transport(var channel: SocketChannel, var errorHandler: ErrorHandler) {
        val taskQueue: ConcurrentLinkedQueue<Message> = ConcurrentLinkedQueue()
    }

    private inner class RequestContainer {

        val tasks = ConcurrentHashMap<Long, Task> ()

        /***
         * 在主事件轮询线程中更新 container
         */
        fun poll() {
            val now = System.currentTimeMillis();
            synchronized(this, {
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
            })
        }

        /***
         * 向容器中添加添加一个 request 任务
         */
        @Synchronized
        fun add(id: Long, request: BaseRequest) {
            tasks.put(id, Task(request = request, timeout = request.timeout()))
            eventService.execute { request.onStart(id) }
        }

        /***
         * 从容器中取出一个 request task
         */
        @Synchronized
        fun remove(id: Long): BaseRequest? {
            val task = tasks[id]
            if (task == null) {
                return null
            } else {
                tasks.remove(id)
                return task.request;
            }
        }

        inner class Task(val request: BaseRequest, val timeout: Long, var timestamp: Long = System.currentTimeMillis())
    }


}

class JsonEngineProtocol : EngineProtocol {

    private val service = Executors.newSingleThreadExecutor()

    override fun antiSerialize(bytes: ByteArray, callback: EngineProtocol.AntiSerializeCallback) {
        service.submit {
            BufferedReader(InputStreamReader(ByteArrayInputStream(bytes))).use {
                while (true) {
                    val line = it.readLine() ?: break
                    try {
                        callback.onSerialize(JSON.parseObject(line, EngineProtocol.Message::class.java));
                    } catch(ignore: Exception) {
                        //ignore.printStackTrace()
                    }
                }
            }
        }
    }

    override fun serialize(message: EngineProtocol.Message, callback: EngineProtocol.SerializeCallBack) {
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

    override fun antiSerializeResponse(bytes: ByteArray): RequestProtocol.Response? {
        try {
            return JSON.parseObject<RequestProtocol.Response>(bytes, RequestProtocol.Response::class.java)
        } catch (e: Exception) {
            return null
        }
    }

    override fun serializeResponse(response: RequestProtocol.Response): ByteArray = JSON.toJSONBytes(response);

}