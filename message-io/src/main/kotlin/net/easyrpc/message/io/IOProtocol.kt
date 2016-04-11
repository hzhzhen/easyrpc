package net.easyrpc.message.io

import com.alibaba.fastjson.JSON
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*


/**
 * @author chpengzh
 */
//use kotlin lib to simplify byte array to list convert
//to handle message io protocol
internal class IOProtocol(val channel: SocketChannel) {
    fun readMessage(): Event {
        val entityParts = ArrayList<Byte>()
        var size = 0;
        val buf = ByteBuffer.allocate(2048);
        do {
            var bytesRead = channel.read(buf)
            if (bytesRead == -1) throw RuntimeException("transport close")
            buf.flip()
            val part = buf.array().filter { b -> b != '\u0000'.toByte() }.toByteArray()
            size += part.size
            entityParts.addAll(part.toList())
            val messageEnd = buf.hasRemaining()
            buf.clear()
        } while (!messageEnd)
        val entity = String(entityParts.toByteArray())
        return JSON.parseObject(entity, Event::class.java)
    }

    fun sendMessage(event: Event) {
        channel.write(ByteBuffer.wrap(JSON.toJSONBytes(event)))
    }
}

