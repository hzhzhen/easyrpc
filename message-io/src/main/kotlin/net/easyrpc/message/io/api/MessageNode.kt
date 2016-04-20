package net.easyrpc.message.io.api

import net.easyrpc.message.io.core.Transport
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.ConcurrentSkipListSet

interface MessageNode : Closeable {

    /***
     * 获得当前建立的所有连接
     */
    fun transports(): ConcurrentSkipListSet<Transport>

    /***
     * 定义消息行为
     */
    fun <T> register(tag: String, type: Class<T>, handler: TypedMessageHandler<T>): MessageNode

    /***
     * 连接到另外的MessageNode
     */
    fun connect(host: String, port: Int, success: (Transport) -> Unit = {},
                fail: (IOException) -> Unit = {}): MessageNode

    /***
     * 断开已经建立的连接
     */
    fun disconnect(transport: Transport)

    /***
     * 监听本地端口
     */
    fun listen(port: Int, accept: (Transport) -> Unit = {},
               fail: (IOException) -> Unit = {}): MessageNode

    /***
     * 关闭本地端口
     */
    fun shutdown(port: Int)

}