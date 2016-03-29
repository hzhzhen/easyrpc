package net.easyrpc.io.engine

import net.easyrpc.io.engine.handler.ErrorListener
import net.easyrpc.io.engine.handler.Handler
import net.easyrpc.io.engine.handler.MessageHandler
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal val POLL_PERIOD = 50L;

internal class ClientImpl : Engine.Client<Engine.Transport> {

    val executor = Executors.newSingleThreadScheduledExecutor()
    val selector: Selector
    val transport: BaseTransport

    var mConnectHandler: Handler<Engine.Transport>? = null
    var mDisconnectHandler: Handler<Engine.Transport>? = null
    var mMessageHandler: MessageHandler<Engine.Transport>? = null
    var mErrorListener: ErrorListener<Engine.Transport>? = null

    constructor() {
        selector = Selector.open()
        transport = BaseTransport(SocketChannel.open())
    }

    override fun onConnect(handler: Handler<Engine.Transport>): Engine.Client<Engine.Transport> {
        this.mConnectHandler = handler
        return this
    }

    override fun onDisconnect(handler: Handler<Engine.Transport>): Engine.Client<Engine.Transport> {
        this.mDisconnectHandler = handler
        return this
    }

    override fun onBytes(handler: MessageHandler<Engine.Transport>): Engine.Client<Engine.Transport>? {
        this.mMessageHandler = handler
        return this
    }

    override fun onError(listener: ErrorListener<Engine.Transport>): Engine.Client<Engine.Transport> {
        this.mErrorListener = listener
        return this
    }

    override fun send(task: Engine.MessageTask) {
        this.transport.send(task)
    }

    override fun connect(host: String, port: Int) {
        transport.channel.connect(InetSocketAddress(host, port))
        mConnectHandler?.call(transport)
        transport.channel.configureBlocking(false)?.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE)
        executor.scheduleWithFixedDelay({
            selector.select(POLL_PERIOD)
            selector.selectedKeys().forEach {
                try {
                    if (it.isReadable) {
                        val entity = readMessage(transport.channel)
                        if (entity != null) mMessageHandler?.call(transport, entity)
                        else close()
                    } else if (it.isWritable) {
                        val next = transport.poll();
                        if (next != null) transport.channel.write(ByteBuffer.wrap(next.bytes))
                    }
                } catch(e: Exception) {
                    mErrorListener?.call(transport, e)
                }
            }
        }, 0, POLL_PERIOD, TimeUnit.MILLISECONDS)
    }

    override fun close() {
        mConnectHandler?.call(transport)
        transport.close()
        executor.shutdownNow()
    }
}

internal class ServerImpl : Engine.Server<Engine.Transport> {

    val executor = Executors.newScheduledThreadPool(2)
    var serverSocketChannel: ServerSocketChannel
    val messageSelector: Selector

    var mConnectHandler: Handler<Engine.Transport>? = null
    var mDisconnectHandler: Handler<Engine.Transport>? = null
    var mMessageHandler: MessageHandler<Engine.Transport>? = null
    var mErrorListener: ErrorListener<Engine.Transport>? = null

    constructor() : super() {
        messageSelector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open()
    }

    override fun onConnect(handler: Handler<Engine.Transport>): Engine.Server<Engine.Transport> {
        this.mConnectHandler = handler
        return this
    }

    override fun onDisconnect(handler: Handler<Engine.Transport>): Engine.Server<Engine.Transport> {
        this.mDisconnectHandler = handler
        return this
    }

    override fun onMessage(handler: MessageHandler<Engine.Transport>): Engine.Server<Engine.Transport>? {
        this.mMessageHandler = handler
        return this
    }

    override fun onError(listener: ErrorListener<Engine.Transport>): Engine.Server<Engine.Transport> {
        this.mErrorListener = listener
        return this
    }

    override fun close(transport: Engine.Transport) {
        transport.close()
        remove(transport)
    }

    override fun listen(port: Int) {
        serverSocketChannel.bind(InetSocketAddress(port))?.configureBlocking(false);
        //setup client channel
        executor.scheduleWithFixedDelay({
            val sc = serverSocketChannel.accept();
            if (sc != null) {
                val transport = BaseTransport(sc)
                sc.configureBlocking(false).register(messageSelector,
                        SelectionKey.OP_READ or SelectionKey.OP_WRITE, transport)
                this@ServerImpl.add(transport)
                mConnectHandler?.call(transport)
            }
        }, 0, POLL_PERIOD, TimeUnit.MILLISECONDS);

        //do read and write task for authorized socket channels
        executor.scheduleWithFixedDelay({
            messageSelector.select(POLL_PERIOD)
            messageSelector.selectedKeys().forEach {
                val transport = it.attachment() as BaseTransport
                try {
                    if (it.isReadable) {
                        val entity = readMessage(transport.channel);
                        if (entity != null) {
                            mMessageHandler?.call(transport, entity)
                        } else {
                            transport.channel.close()
                            this@ServerImpl.remove(transport)
                            mDisconnectHandler?.call(transport)
                            return@forEach
                        }
                    } else if (it.isWritable) {
                        val next = transport.poll();
                        if (next != null) transport.channel.write(ByteBuffer.wrap(next.bytes))
                    }
                } catch(e: Exception) {
                    mErrorListener?.call(transport, e)
                }
            }
        }, 0, POLL_PERIOD, TimeUnit.MILLISECONDS);
    }

    override fun close() {
        executor.shutdownNow()
        for (transport in this) transport.channel?.close()
        serverSocketChannel.close()
        clear()
    }
}

internal fun readMessage(channel: ReadableByteChannel): ByteArray? {
    val entityParts = ArrayList<Byte>()
    var size = 0;
    val buf = ByteBuffer.allocate(2048);
    do {
        var bytesRead = channel.read(buf)
        if (bytesRead == -1) return null
        buf.flip()
        val part = buf.array().filter { b -> b != '\u0000'.toByte() }.toByteArray()
        size += part.size
        entityParts.addAll(part.toList())
        val messageEnd = buf.hasRemaining()
        buf.clear()
    } while (!messageEnd)
    return entityParts.toByteArray()
}

internal class BaseTransport : ConcurrentLinkedQueue<Engine.MessageTask>, Engine.Transport {

    val sid: Long
    val sc: SocketChannel

    constructor(channel: SocketChannel) : super() {
        sid = System.currentTimeMillis()
        this.sc = channel
    }

    override fun send(messageTask: Engine.MessageTask) {
        add(messageTask)
    }

    override fun getSID(): Long = sid

    override fun getChannel(): SocketChannel = sc

    @Throws(IOException::class)
    override fun close() {
        if (sc.isOpen) sc.close()
    }

    override fun compareTo(other: Engine.Transport): Int {
        if (other.sid === sid) return 0
        return if (other.sid > this.sid) -1 else 1
    }
}
