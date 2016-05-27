package net.easyrpc.request.io.impl

import com.alibaba.fastjson.JSON
import net.easyrpc.engine.io.Engine
import net.easyrpc.request.io.Node
import net.easyrpc.request.io.error.RemoteError
import net.easyrpc.request.io.error.TimeoutError
import net.easyrpc.request.io.handler.Callback
import net.easyrpc.request.io.handler.RequestHandler
import net.easyrpc.request.io.model.Request
import net.easyrpc.request.io.model.Response
import net.easyrpc.request.io.protocol.RequestProtocol
import net.easyrpc.request.io.protocol.Status
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class NodeImpl(val engine: Engine) : Node(JsonRequestProtocol()) {

    private val reqTasks = ConcurrentHashMap<String, Task>()
    private val handlers = ConcurrentHashMap<String, RequestHandler>()
    private val service = Executors.newCachedThreadPool()

    init {
        engine.subscribe("/request-io/request", { transport, id, packageData ->

            val request = protocol.antiSerializeRequest(packageData) ?: return@subscribe

            val response: Response;

            val handler = handlers[request.tag];
            if (handler == null) {
                response = Response(id, Status.NOT_FOUND.code, Status.NOT_FOUND.message.toByteArray())
            } else {
                try {
                    response = Response(id, Status.OK.code,
                            handler.onData(request.data) ?: ByteArray(0, { i -> '\u0000'.toByte() }))
                } catch (e: Exception) {
                    response = Response(id, Status.REMOTE_ERROR.code, e.toString().toByteArray())
                }
            }

            transport.send("/request-io/response", protocol.serializeResponse(response.meta(id)))

        }).subscribe("/request-io/response", { transport, id, packageData ->

            val response = protocol.antiSerializeResponse(packageData) ?: return@subscribe
            val task = reqTasks["${transport.hashCode()}-$id"] ?: return@subscribe

            task.callback(response)
            reqTasks.remove("${transport.hashCode()}-$id")

        })
    }

    override fun onRequest(tag: String, handler: RequestHandler): Node {
        handlers.put(tag, handler)
        return this
    }

    @Throws(RemoteError::class, TimeoutError::class)
    override fun request(hash: Int, tag: String, data: ByteArray): ByteArray {
        val id = engine.send(hash, "/request-io/request", protocol.serializeRequest(Request(tag, data)));
        if (id == -1L) throw TimeoutError(hash);
        val newTask = Task(tcpHash = hash, requestId = id)
        reqTasks[newTask.hash()] = newTask
        return newTask.execute().data;
    }

    override fun request(hash: Int, tag: String, data: ByteArray, callback: Callback) {
        service.submit {
            try {
                callback.onData(request(hash, tag, data))
            } catch(e: RemoteError) {
                callback.onFail(e)
            } catch(e: TimeoutError) {
                callback.onTimeout(e)
            }
        }
    }

    private class Task(val tcpHash: Int, val requestId: Long,
                       val timeout: Long = 3L, val timeUnit: TimeUnit = TimeUnit.SECONDS) {

        private val latch = CountDownLatch(1)
        private val response = Array<Response?>(1, { i -> null });

        fun hash(): String = "$tcpHash-$requestId"

        @Throws(RemoteError::class, TimeoutError::class)
        fun execute(): Response {
            latch.await(timeout, timeUnit)
            try {
                val result = this.response[0] ?: throw TimeoutError(tcpHash)
                if (result.status == Status.OK.code) return result
                else throw RemoteError(tcpHash, result.status)
            } catch(error: InterruptedException) {
                throw RemoteError(tcpHash, error.toString())
            }
        }

        fun callback(response: Response) {
            this.response[0] = response
            latch.countDown()
        }
    }

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
}



