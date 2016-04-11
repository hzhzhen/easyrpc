package net.easyrpc.message.io

import akka.actor.ActorRef
import akka.actor.ActorSystem
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.Executors

/**
 * @author chpengzh
 */
internal class MessageNodeImpl : MessageNode {

    private val transports = ConcurrentSkipListSet<Transport>()
    private val servers = ConcurrentHashMap<Int, ServerSocketChannel>()
    private val system = ActorSystem.create()
    private val actors = ConcurrentHashMap<String, ActorRef>()
    private val mainService = Executors.newScheduledThreadPool(2)
    private var mConnectHandler: TransportHandler? = null
    private var mDisconnectHandler: TransportHandler? = null
    private val selector = Selector.open()

    constructor() {
        //Accept polling bus
        mainService.scheduleWithFixedDelay({
            servers.forEach({ port, ssc ->
                try {
                    val channel = ssc.accept()
                    if (channel != null) {
                        channel.configureBlocking(false)
                        val transport = Transport(channel)
                        channel.register(selector,
                                SelectionKey.OP_READ or SelectionKey.OP_WRITE, transport)
                        transports.add(transport)
                        mConnectHandler?.handle(transport)
                    }
                } catch (e: IOException) {
                    e.printStackTrace(System.out)
                }
            })
        }, 0, MessageNode.ACCEPT_POLLING, MessageNode.ACCEPT_POLLING_UNIT)

        //Message polling bus
        mainService.scheduleWithFixedDelay({
            try {
                selector.select(100)
                selector.selectedKeys().forEach {
                    val transport = it.attachment() as Transport
                    try {
                        val polling = IOProtocol(transport.channel)
                        if (it.isReadable) {
                            val event = polling.readMessage() //throw runtime exception if disconnected
                            actors[event.tag]?.tell(event.bind(transport), ActorRef.noSender())
                        } else if (it.isWritable) {
                            val event = transport.taskQueue.poll()
                            if (event != null) {
                                polling.sendMessage(event)
                            }
                        }
                    } catch (e: Exception) {
                        //handle exception if disconnected
                        e.printStackTrace(System.out)
                        mDisconnectHandler?.handle(transport)
                        disconnect(transport)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace(System.out)
            }
        }, 0, MessageNode.MESSAGE_POLLING, MessageNode.MESSAGE_POLLING_UNIT)
    }

    override fun transports(): ConcurrentSkipListSet<Transport> = transports.clone()

    override fun <T> register(tag: String, type: Class<T>, handler: MessageHandler<T>): MessageNode {
        actors.put(tag, system.actorOf(EventActor.props(type, handler), tag))
        return this
    }

    @Throws(IOException::class)
    override fun connect(host: String, port: Int): MessageNode {
        val channel = SocketChannel.open()
        channel.connect(InetSocketAddress(host, port))
        channel.configureBlocking(false)
        val transport = Transport(channel)
        channel.register(selector,
                SelectionKey.OP_READ or SelectionKey.OP_WRITE, transport)
        transports.add(transport)
        return this
    }

    override fun disconnect(transport: Transport) {
        if (transports.contains(transport)) {
            transports.remove(transport)
            try {
                transport.channel.close()
            } catch (e: IOException) {
                e.printStackTrace(System.out)
            }

        }
    }

    @Throws(IOException::class)
    override fun listen(port: Int): MessageNode {
        if (servers[port] != null) return this
        val ssc = ServerSocketChannel.open()
                .bind(InetSocketAddress("localhost", port))
                .configureBlocking(false) as ServerSocketChannel
        servers.put(port, ssc)
        return this
    }

    @Throws(IOException::class)
    override fun close(port: Int) {
        val ssc = servers[port]
        if (ssc != null) {
            ssc.close()
            servers.remove(port)
        }
    }

    override fun setConnectHandler(handler: TransportHandler): MessageNode {
        mConnectHandler = handler
        return this
    }

    override fun setDisconnectHandler(handler: TransportHandler): MessageNode {
        this.mDisconnectHandler = handler
        return this
    }

    @Throws(IOException::class)
    override fun close() {
        mainService.shutdownNow()
        transports.forEach({ this.disconnect(it) })
        servers.forEach({ port, ssc ->
            try {
                ssc.close()
            } catch (e: IOException) {
                e.printStackTrace(System.out)
            }
        })
        system.terminate()
    }
}