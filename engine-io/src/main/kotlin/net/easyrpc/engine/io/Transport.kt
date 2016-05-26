package net.easyrpc.engine.io

import net.easyrpc.engine.io.handler.ErrorHandler
import net.easyrpc.engine.io.protocol.SerializeProtocol.Message
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock

class Transport(internal var channel: SocketChannel, internal val error: ErrorHandler) : Comparable<Transport> {

    private val lock = ReentrantLock()
    private var id: Long = 0

    internal val taskQueue: ConcurrentLinkedQueue<Message> = ConcurrentLinkedQueue()

    override fun compareTo(other: Transport): Int = when {
        other.hashCode() == hashCode() -> 0
        other.hashCode() < hashCode() -> -1
        else -> 1
    }

    //发送一则消息
    fun send(category: String, data: ByteArray): Long {
        try {
            lock.lock()
            id++;
            taskQueue.add(Message(id, category, data))
            return id;
        } finally {
            lock.unlock()
        }
    }
}