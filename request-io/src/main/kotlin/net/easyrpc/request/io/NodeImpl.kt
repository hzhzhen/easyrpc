package net.easyrpc.request.io

import net.easyrpc.engine.io.Engine
import net.easyrpc.request.io.error.FailError
import net.easyrpc.request.io.error.TimeoutError
import net.easyrpc.request.io.handler.Callback
import net.easyrpc.request.io.handler.RequestHandler
import net.easyrpc.request.io.model.Request
import net.easyrpc.request.io.model.Response
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
                    response = Response(id, Status.OK.code, handler.onData(request.data))
                } catch (e: Exception) {
                    response = Response(id, Status.REMOTE_ERROR.code, e.toString().toByteArray())
                }
            }

            transport.send("/request-io/response", protocol.serializeResponse(response.meta(id)))

        }).subscribe("/request-io/response", { transport, id, packageData ->

            val response = protocol.antiSerializeResponse(packageData) ?: return@subscribe
            val task = reqTasks["${transport.hashCode()}-$id"] ?: return@subscribe
            task.callback(response)

        })
    }

    override fun onRequest(tag: String, handler: RequestHandler): Node {
        handlers.put(tag, handler)
        return this
    }

    @Throws(FailError::class, TimeoutError::class)
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
            } catch(e: FailError) {
                callback.onFail(e)
            } catch(e: TimeoutError) {
                callback.onTimeout(e)
            }
        }
    }

    private class Task(val tcpHash: Int, val requestId: Long) {

        private val latch = CountDownLatch(1)
        private val response = Array<Response?>(1, { i -> null });

        fun hash(): String = "$tcpHash-$requestId"

        @Throws(FailError::class, TimeoutError::class)
        fun execute(): Response {
            latch.await(3, TimeUnit.SECONDS)
            try {
                val result = this.response[0] ?: throw TimeoutError(tcpHash)
                if (result.status != 200) throw FailError(tcpHash, result.status)
                return result
            } catch(error: InterruptedException) {
                throw FailError(tcpHash, error.message)
            }
        }

        fun callback(response: Response) {
            this.response[0] = response
            latch.countDown()
        }
    }

}

