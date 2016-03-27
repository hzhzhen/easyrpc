package net.easyrpc.engine.tcp

import net.easyrpc.engine.tcp.handler.ErrorListener
import net.easyrpc.engine.tcp.handler.Handler
import net.easyrpc.engine.tcp.handler.Listener
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal val POLL_PERIOD = 50L;

/**
 * @author chpengzh
 */
internal class ClientImpl : Client {

    val executor = Executors.newSingleThreadScheduledExecutor()
    val selector: Selector

    var mConnectHandler: Handler? = null
    var mDisconnectHandler: Handler? = null
    var mListener: Listener? = null
    var mErrorListener: ErrorListener? = null

    constructor() : super() {
        selector = Selector.open()
    }

    override fun onConnect(handler: Handler): Client {
        this.mConnectHandler = handler
        return this
    }

    override fun onDisconnect(handler: Handler): Client {
        this.mDisconnectHandler = handler
        return this
    }

    override fun onMessage(listener: Listener): Client {
        this.mListener = listener
        return this
    }

    override fun onError(listener: ErrorListener): Client? {
        this.mErrorListener = listener
        return this
    }

    override fun connect(host: String, port: Int) {
        this.channel?.connect(InetSocketAddress(host, port))
        mConnectHandler?.call(this)
        channel?.configureBlocking(false)?.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE)
        executor.scheduleWithFixedDelay({
            selector.select(50)
            selector.selectedKeys().forEach {
                try {
                    if (it.isReadable) {
                        val entity = readMessage(this.channel!!)
                        if (entity != null) mListener?.call(this@ClientImpl, entity)
                        else close()
                    } else if (it.isWritable) {
                        val next = this@ClientImpl.task.poll();
                        if (next != null) this@ClientImpl.channel?.write(ByteBuffer.wrap(next))
                    }
                } catch(e: Exception) {
                    mErrorListener?.call(this@ClientImpl, e)
                }
            }
        }, 0, POLL_PERIOD, TimeUnit.MILLISECONDS)
    }

    override fun close() {
        mConnectHandler?.call(this@ClientImpl)
        super.close()
        executor.shutdownNow()
    }
}

internal class ServerImpl : Server {

    val executor = Executors.newScheduledThreadPool(2)
    var serverSocketChannel: ServerSocketChannel
    val messageSelector: Selector

    var mConnectHandler: Handler? = null
    var mDisconnectHandler: Handler? = null
    var mListener: Listener? = null
    var mErrorListener: ErrorListener? = null

    constructor() : super() {
        messageSelector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open()
    }

    override fun onConnect(handler: Handler): Server {
        this.mConnectHandler = handler
        return this
    }

    override fun onDisconnect(handler: Handler): Server {
        this.mDisconnectHandler = handler
        return this
    }

    override fun onMessage(listener: Listener): Server {
        this.mListener = listener
        return this
    }

    override fun onError(listener: ErrorListener): Server {
        this.mErrorListener = listener
        return this
    }

    override fun close(transport: Transport) {
        transport.close()
        remove(transport)
    }

    override fun listen(port: Int) {
        serverSocketChannel.bind(InetSocketAddress(port))?.configureBlocking(false);
        //setup client channel
        executor.scheduleWithFixedDelay({
            val sc = serverSocketChannel.accept();
            if (sc != null) {
                val transport = Transport(sc)
                sc.configureBlocking(false).register(messageSelector,
                        SelectionKey.OP_READ or SelectionKey.OP_WRITE, transport)
                this@ServerImpl.add(transport)
                mConnectHandler?.call(transport)
            }
        }, 0, POLL_PERIOD, TimeUnit.MILLISECONDS);

        //do read and write task for authorized socket channels
        executor.scheduleWithFixedDelay({
            messageSelector.select(50)
            messageSelector.selectedKeys().forEach {
                val transport = it.attachment() as Transport
                try {
                    if (it.isReadable) {
                        val entity = readMessage(transport.channel!!);
                        if (entity != null) {
                            mListener?.call(transport, entity)
                        } else {
                            transport.channel?.close()
                            this@ServerImpl.remove(transport)
                            mDisconnectHandler?.call(transport)
                            return@forEach
                        }
                    } else if (it.isWritable) {
                        val next = transport.task.poll();
                        if (next != null) transport.channel?.write(ByteBuffer.wrap(next))
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