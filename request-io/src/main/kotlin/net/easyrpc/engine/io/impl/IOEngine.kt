package net.easyrpc.engine.io.impl

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
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class IOEngine : Engine {

    //编码协议,默认为Json编码
    protected var engineProtocol: EngineProtocol = JsonEngineProtocol()
    protected var requestProtocol: RequestProtocol = JsonRequestProtocol()

    //handler
    protected val eventHandlers = ConcurrentHashMap<String, (Int, Long, ByteArray) -> Unit>()
    protected val requestHandlers = ConcurrentHashMap<String, RequestHandler>()

    //server & transport
    private val servers = ConcurrentHashMap<InetSocketAddress, Acceptor>()
    private val transports = ConcurrentHashMap<Int, Transport>()

    //task container
    private val messageQueue = ConcurrentLinkedQueue<MessageTask>()
    private val requestContainer = RequestContainer()

    //IO事件处理Selector
    private val selector = Selector.open()

    //work executor service
    private val main = Executors.newSingleThreadScheduledExecutor()     //事件轮询线程
    //TODO : 使用协程来替换事件执行线程池
    //issue: https://github.com/chpengzh/easyrpc/issues/1
    private val eventService = Executors.newFixedThreadPool(10)         //事件执行线程

    //main event polling time unit
    private val POLLING: Long = 50 //time unit: μs
    private val POLLING_UNIT = TimeUnit.MICROSECONDS

    constructor() {
        subscribeRequest()//初始化Request-IO
        main.scheduleWithFixedDelay({//事件轮询
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

    @Synchronized
    override fun connect(address: InetSocketAddress, onSuccess: ConnectHandler, onError: ErrorHandler): Engine {
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
        return this
    }

    @Synchronized
    override fun listen(address: InetSocketAddress, onAccept: ConnectHandler, onError: ErrorHandler): Engine {
        if (servers[address] != null) return this
        val ssc = ServerSocketChannel.open().bind(address).configureBlocking(false) as ServerSocketChannel
        servers[address] = Acceptor(ssc = ssc, accept = onAccept, error = onError)
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
    protected fun appendQueue(transport: Transport, tag: String, bytes: ByteArray): Long {
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

    /***
     * TODO: 使用自定义行为的线程池进行更新并比较二者效率
     * issue: https://github.com/chpengzh/easyrpc/issues/3
     */
    private inner class RequestContainer {

        val tasks = HashMap<String, Task> ()

        /***
         * 在主事件轮询线程中更新 container
         */
        fun update() {
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
        fun add(tcpHash: Int, id: Long, request: BaseRequest) {
            tasks.put("$tcpHash-$id", Task(request = request, timeout = request.timeout()))
        }

        /***
         * 从容器中取出一个 request task
         */
        @Synchronized
        fun remove(tcpHash: Int, id: Long): BaseRequest? {
            val task = tasks["$tcpHash-$id"]
            if (task == null) {
                return null
            } else {
                tasks.remove("$tcpHash-$id")
                return task.request;
            }
        }

        inner class Task(val request: BaseRequest, val timeout: Long, var timestamp: Long = System.currentTimeMillis())
    }

    inner class Transport(var channel: SocketChannel, var errorHandler: ErrorHandler)
}