package net.easyrpc.engine.io.impl

import com.alibaba.fastjson.JSON
import net.easyrpc.engine.io.protocol.EngineProtocol
import net.easyrpc.engine.io.protocol.RequestProtocol
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
                        callback.onSerialize(JSON.parseObject(line, EngineProtocol.Message::class.java));
                    } catch(ignore: Exception) {
                        //ignore.printStackTrace()
                    }
                }
            }
        }
    }

    override fun serialize(message: EngineProtocol.Message, callback: EngineProtocol.SerializeCallBack) {
        service.submit({
            callback.onAntiSerialize((JSON.toJSONString(message) + "\n").toByteArray())
        });
    }

    override fun close() {
        service.shutdownNow()
    }
}

class JsonRequestProtocol : RequestProtocol {

    override fun antiSerializeRequest(bytes: ByteArray): RequestProtocol.Request? {
        try {
            return JSON.parseObject<RequestProtocol.Request>(bytes, RequestProtocol.Request::class.java)
        } catch (e: Exception) {
            return null
        }
    }

    override fun serializeRequest(request: RequestProtocol.Request): ByteArray = JSON.toJSONBytes(request)

    override fun antiSerializeResponse(bytes: ByteArray): RequestProtocol.Response? {
        try {
            return JSON.parseObject<RequestProtocol.Response>(bytes, RequestProtocol.Response::class.java)
        } catch (e: Exception) {
            return null
        }
    }

    override fun serializeResponse(response: RequestProtocol.Response): ByteArray = JSON.toJSONBytes(response);

}