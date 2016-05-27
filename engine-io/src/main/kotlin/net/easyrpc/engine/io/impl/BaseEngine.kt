package net.easyrpc.engine.io.impl

import com.alibaba.fastjson.JSON
import net.easyrpc.engine.io.Engine
import net.easyrpc.engine.io.handler.ConnectHandler
import net.easyrpc.engine.io.handler.DataHandler
import net.easyrpc.engine.io.handler.ErrorHandler
import net.easyrpc.engine.io.model.Message
import net.easyrpc.engine.io.model.Transport
import net.easyrpc.engine.io.protocol.EngineProtocol
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author chpengzh
 */
open class BaseEngine : Engine {

    //编码协议,默认为Json编码
    protected var protocol: EngineProtocol = JsonEngineProtocol()
    protected val handlers = ConcurrentHashMap<String, DataHandler>()
    private val servers = ConcurrentHashMap<InetSocketAddress, Acceptor>()
    private val transports = ConcurrentHashMap<Int, Transport>()

    //IO事件处理Selector
    private val selector = Selector.open()

    //事件轮询线程
    private val main = Executors.newScheduledThreadPool(2)
    private val net = Executors.newSingleThreadExecutor() //连接为单线程池执行
    private val event = Executors.newFixedThreadPool(4)

    //事件轮询周期
    private val POLLING: Long = 50 //time unit: μs
    private val POLLING_UNIT = TimeUnit.MICROSECONDS

    constructor() {
        main.scheduleWithFixedDelay({
            try {
                selector.selectNow()
                selector.selectedKeys().forEach {
                    val transport = it.attachment() as Transport
                    try {
                        if (it.isReadable) {
                            readMessage(transport)
                        } else if (it.isWritable) {
                            do {
                                val event = transport.taskQueue.poll()
                                if (event != null) sendMessage(transport, event)
                                else break;
                            } while (true)
                        }
                    } catch (e: IOException) {
                        transport.errorHandler.onError(e)
                        disconnect(transport.hashCode())//handle exception if disconnected
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace(System.out)
            }
            servers.forEach({ port, acceptor ->
                try {
                    val channel = acceptor.ssc.accept()
                    if (channel != null) {
                        channel.configureBlocking(false)
                        val transport = Transport(channel, acceptor.error)
                        channel.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE, transport)
                        transports.put(transport.hashCode(), transport)
                        acceptor.accept.onEvent(transport.hashCode(), transport)
                    }
                } catch (e: IOException) {
                    e.printStackTrace(System.out)
                }
            })
        }, 0, POLLING, POLLING_UNIT)
    }

    constructor(protocol: EngineProtocol) : this() {
        this.protocol = protocol
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
                onSuccess.onEvent(transport.hashCode(), transport)
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
            servers[address] = Acceptor(address = address, ssc = ssc, accept = onAccept, error = onError)
        }
        return this
    }

    override fun disconnect(hash: Int) {
        net.submit {
            val transport = transports[hash];
            if (transport != null) {
                transport.taskQueue.clear()
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

    override fun subscribe(metaTag: String, handler: DataHandler): BaseEngine {
        handlers.put(metaTag, handler)
        return this
    }

    override fun send(hash: Int, category: String, data: ByteArray): Long {
        val transport = transports[hash] ?: return -1
        return transport.send(category, data)
    }

    override fun terminate() {
        event.shutdownNow()

        main.shutdownNow()
        protocol.close()

        transports.forEach({ this.disconnect(it.hashCode()) })
        servers.forEach({ address, acceptor -> close(address) })
        net.shutdown()
    }

    private fun readMessage(transport: Transport) {
        val buffer = ByteBuffer.allocate(32 * 1024)
        val flag = transport.channel.read(buffer);
        if (flag == -1) throw IOException("session disconnected")
        if (buffer.position() == 0) return
        val bytes = buffer.array().copyOf(buffer.position())
        protocol.antiSerialize(bytes, {
            event.submit { handlers[it.tag]?.handle(transport, it.id, it.data) }
        })
    }

    private fun sendMessage(transport: Transport, message: Message) {
        protocol.serialize(message, {
            transport.channel.write(ByteBuffer.wrap(it))
        })
    }

    private data class Acceptor(val address: InetSocketAddress, val ssc: ServerSocketChannel,
                                val accept: ConnectHandler, val error: ErrorHandler)

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
}