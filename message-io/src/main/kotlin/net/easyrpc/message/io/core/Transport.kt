package net.easyrpc.message.io.core

import net.easyrpc.message.io.api.ErrorHandler
import net.easyrpc.message.io.model.Event
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author chpengzh
 */
class Transport(var channel: SocketChannel,
                private var attachment: Any? = null,
                val error: ErrorHandler,
                val taskQueue: ConcurrentLinkedQueue<Event> = ConcurrentLinkedQueue()) :
        Comparable<Transport> {

    override fun compareTo(other: Transport): Int = when {
        other.hashCode() == hashCode() -> 0
        other.hashCode() < hashCode() -> -1
        else -> 1
    }

    fun send(tag: String, obj: Any) {
        taskQueue.add(Event(tag, obj))
    }

    fun attach(attachment: Any): Transport {
        this.attachment = attachment
        return this
    }

    fun attachment(): Any? {
        return attachment
    }

}