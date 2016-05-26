package net.easyrpc.engine.io.impl

import com.alibaba.fastjson.JSON
import net.easyrpc.engine.io.model.Message
import net.easyrpc.engine.io.protocol.EngineProtocol
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.concurrent.Executors

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