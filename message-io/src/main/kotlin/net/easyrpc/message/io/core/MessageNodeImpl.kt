package net.easyrpc.message.io.core

import akka.actor.ActorRef
import akka.actor.ActorSystem
import com.alibaba.fastjson.JSON
import net.easyrpc.message.io.api.MessageNode
import net.easyrpc.message.io.api.TypedMessageHandler
import net.easyrpc.message.io.model.Event
import net.easyrpc.message.io.model.EventActor
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
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author chpengzh
 */
internal class MessageNodeImpl : MessageNode {

    private val ACCEPT_POLLING: Long = 10
    private val ACCEPT_POLLING_UNIT = TimeUnit.MILLISECONDS
    private val MESSAGE_POLLING: Long = 100
    private val MESSAGE_POLLING_UNIT = TimeUnit.MICROSECONDS

    private val transports = ConcurrentSkipListSet<Transport>()
    private val servers = ConcurrentHashMap<Int, Acceptor>()

    private val system = ActorSystem.create()
    private val actors = ConcurrentHashMap<String, ActorRef>()

    private val mainService = Executors.newScheduledThreadPool(2)
    private val ioService = Executors.newSingleThreadExecutor()
    private val connService = Executors.newSingleThreadExecutor()

    private val selector = Selector.open()

    constructor() {
        //Accept polling bus
        mainService.scheduleWithFixedDelay({
            servers.forEach({ port, acceptor ->
                try {
                    val channel = acceptor.ssc.accept()
                    if (channel != null) {
                        channel.configureBlocking(false)
                        val transport = Transport(channel = channel, error = acceptor.error)
                        channel.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE, transport)
                        transports.add(transport)
                        acceptor.accept(transport)
                    }
                } catch (e: IOException) {
                    e.printStackTrace(System.out)
                }
            })
        }, 0, ACCEPT_POLLING, ACCEPT_POLLING_UNIT)

        //Message polling bus
        mainService.scheduleWithFixedDelay({
            try {
                selector.select(100)
                selector.selectedKeys().forEach {
                    val transport = it.attachment() as Transport
                    try {
                        if (it.isReadable) {
                            readMessage(transport)
                        } else if (it.isWritable) {
                            val event = transport.taskQueue.poll()
                            if (event != null) sendMessage(transport, event)
                        }
                    } catch (e: IOException) {
                        transport.error(e)
                        disconnect(transport)//handle exception if disconnected
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace(System.out)
            }
        }, 0, MESSAGE_POLLING, MESSAGE_POLLING_UNIT)
    }

    override fun transports(): ConcurrentSkipListSet<Transport> = transports

    override fun <T> register(tag: String, type: Class<T>, handler: TypedMessageHandler<T>): MessageNode {
        actors.put(tag, system.actorOf(EventActor.props(type, handler), tag))
        return this
    }

    override fun connect(host: String, port: Int, success: (Transport) -> Unit,
                         fail: (IOException) -> Unit): MessageNode {
        connService.submit {
            try {
                val channel = SocketChannel.open()
                channel.connect(InetSocketAddress(host, port))
                channel.configureBlocking(false)
                val transport = Transport(channel = channel, error = fail)
                channel.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE, transport)
                transports.add(transport)
                success(transport)
            } catch(error: IOException) {
                fail(error)
            }
        }
        return this
    }

    override fun disconnect(transport: Transport) {
        if (transports.contains(transport)) {
            transports.remove(transport)
            transport.taskQueue.clear()
            transport.channel.close()
        }
    }

    override fun listen(port: Int, accept: (Transport) -> Unit, fail: (IOException) -> Unit): MessageNode {
        if (servers[port] != null) return this
        val ssc = ServerSocketChannel.open().bind(InetSocketAddress(port))
                .configureBlocking(false) as ServerSocketChannel
        servers.put(port, Acceptor(port = port, ssc = ssc, accept = accept, error = fail))
        return this
    }

    override fun shutdown(port: Int) {
        val acceptor = servers[port]
        if (acceptor != null) {
            acceptor.ssc.close()
            servers.remove(port)
        }
    }

    override fun close() {
        mainService.shutdownNow()
        ioService.shutdownNow()
        connService.shutdownNow()
        transports.forEach({ this.disconnect(it) })
        servers.forEach({ port, acceptor -> shutdown(port) })
        system.terminate()
    }

    //handle object serialization
    fun readMessage(transport: Transport) {
        val buffer = ByteBuffer.allocate(32 * 1024)
        val flag = transport.channel.read(buffer);
        if (flag == -1) throw IOException("session disconnected")
        if (buffer.position() == 0) return
        val bytes = buffer.array().copyOf(buffer.position())
        ioService.submit {
            BufferedReader(InputStreamReader(ByteArrayInputStream(bytes))).use {
                while (true) {
                    val line = it.readLine() ?: break
                    try {
                        val event = JSON.parseObject(line, Event::class.java)
                        actors[event.tag]?.tell(event.bind(transport), ActorRef.noSender())
                    } catch(ignore: Exception) {

                    }
                }
            }
        }
    }

    fun sendMessage(transport: Transport, event: Event) {
        transport.channel.write(ByteBuffer.wrap((JSON.toJSONString(event) + "\n").toByteArray()))
    }
}