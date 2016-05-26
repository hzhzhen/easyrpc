package net.easyrpc.request.io

import com.alibaba.fastjson.JSON
import net.easyrpc.request.io.model.Request
import net.easyrpc.request.io.model.Response
import net.easyrpc.request.io.protocol.RequestProtocol


class JsonRequestProtocol : RequestProtocol {

    override fun antiSerializeRequest(bytes: ByteArray): Request? {
        try {
            return JSON.parseObject<Request>(bytes, Request::class.java)
        } catch (e: Exception) {
            return null
        }
    }

    override fun serializeRequest(request: Request): ByteArray = JSON.toJSONBytes(request)

    override fun antiSerializeResponse(bytes: ByteArray): Response? {
        try {
            return JSON.parseObject<Response>(bytes, Response::class.java)
        } catch (e: Exception) {
            return null
        }
    }

    override fun serializeResponse(response: Response): ByteArray = JSON.toJSONBytes(response);

}