package net.easyrpc.message.io.core

import com.alibaba.fastjson.JSON
import net.easyrpc.message.io.api.Protocol
import net.easyrpc.message.io.model.Event
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.concurrent.Executors

/**
 * 数据的反序列化方法通常是阻塞的,所以采用异步的方式统一序列化和反序列化
 */
class JsonProtocol : Protocol {

    private val service = Executors.newSingleThreadExecutor()

    override fun antiSerialize(bytes: ByteArray, callback: Protocol.AntiSerializeCallback) {
        service.submit {
            BufferedReader(InputStreamReader(ByteArrayInputStream(bytes))).use {
                while (true) {
                    val line = it.readLine() ?: break
                    try {
                        callback.onSerialize(JSON.parseObject(line, Event::class.java));
                    } catch(ignore: Exception) {
                    }
                }

            }
        }
    }

    override fun serialize(event: Event, callback: Protocol.SerializeCallBack) {
        service.submit({
            callback.onAntiSerialize((JSON.toJSONString(event) + "\n").toByteArray())
        });
    }

    override fun close() {
        service.shutdownNow()
    }

}