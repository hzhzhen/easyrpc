package net.easyrpc.message.io.core

import com.alibaba.fastjson.JSON
import net.easyrpc.message.io.annotation.Connect
import net.easyrpc.message.io.annotation.Listen
import net.easyrpc.message.io.annotation.OnEvent
import net.easyrpc.message.io.api.BaseConnector
import net.easyrpc.message.io.api.ConnectHandler
import net.easyrpc.message.io.api.ErrorHandler
import net.easyrpc.message.io.api.TypedMessageHandler
import net.easyrpc.message.io.error.IllegalTagException
import net.easyrpc.message.io.model.Event
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author chpengzh
 */
open class MessageNode {

    private val acceptPolling: Long = 10   //time unit: ms
    private val messagePolling: Long = 50 //time unit: μs
    private val acceptPollingUnit = TimeUnit.MILLISECONDS
    private val messagePollingUnit = TimeUnit.MICROSECONDS

    private val transports = ConcurrentSkipListSet<Transport>()
    private val servers = ConcurrentHashMap<Int, Acceptor>()
    private val handlers = ConcurrentHashMap<String, (Transport, Any) -> Unit>()

    private val mainService = Executors.newScheduledThreadPool(2)
    private val ioService = Executors.newSingleThreadExecutor()
    private val connService = Executors.newSingleThreadExecutor()
    private val eventService = Executors.newFixedThreadPool(4)
    private val instanceMap = HashMap<Class<*>, Any>()

    private val selector = Selector.open()

    init {
        //Accept polling bus
        mainService.scheduleWithFixedDelay({
            servers.forEach({ port, acceptor ->
                try {
                    val channel = acceptor.ssc.accept()
                    if (channel != null) {
                        channel.configureBlocking(false)
                        val transport = Transport(channel = channel, error = acceptor.error)
                        channel.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE, transport)
                        transports.add(transport)
                        acceptor.accept.onConnect(transport)
                    }
                } catch (e: IOException) {
                    e.printStackTrace(System.out)
                }
            })
        }, 0, acceptPolling, acceptPollingUnit)

        //Message polling bus
        mainService.scheduleWithFixedDelay({
            try {
                selector.select(100)
                selector.selectedKeys().forEach {
                    val transport = it.attachment() as Transport
                    try {
                        if (it.isReadable) {
                            readMessage(transport)
                        } else if (it.isWritable) {
                            val event = transport.taskQueue.poll()
                            if (event != null) sendMessage(transport, event)
                        }
                    } catch (e: IOException) {
                        transport.error.onError(e)
                        disconnect(transport)//handle exception if disconnected
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace(System.out)
            }
        }, 0, messagePolling, messagePollingUnit)
    }

    /***
     * 扫描注册事件监视器
     */
    fun setEventHandler(vararg pathArray: String) {
        pathArray.map {
            Reflections(ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage(it))
                    .setScanners(MethodAnnotationsScanner()))
        }.map { it.getMethodsAnnotatedWith(OnEvent::class.java) }.forEach { set ->
            set.forEach { method ->
                //构造反射调用所需的实例, 注意为每个类保留一个公共的空构造方法
                try {
                    val clazz = method.declaringClass
                    if (!instanceMap.containsKey(clazz)) instanceMap.put(clazz, clazz.newInstance())
                    val flag = method.getAnnotation(OnEvent::class.java)
                    //检查是否存在该tag
                    if (handlers[flag.value] != null)
                        throw IllegalTagException("node terminate with error\ntag:${flag.value} is already exists!!")

                    //注册消息行为
                    handlers.put(flag.value, { transport, any ->
                        try {
                            val param = Array<Any>(method.parameterTypes.size, { index ->
                                if (method.parameterTypes[index] == flag.type.java) {
                                    System.out.println(flag.type.java.simpleName)
                                    return@Array JSON.parseObject(JSON.toJSONBytes(any), flag.type.java)
                                } else if (method.parameterTypes[index] == Transport::class.java) {
                                    return@Array transport
                                } else {
                                    throw IllegalArgumentException("can't fill argument with type " +
                                            "${method.parameterTypes[index]} at index:$index")
                                }
                            })
                            //反射调用方法
                            method.invoke(instanceMap[method.declaringClass], *param)
                        } catch(e: Exception) {
                            e.printStackTrace()
                        }
                    })
                } catch(error: Exception) {
                    //无论何种原因导致的失败都会抛出并中断注册
                    terminate()
                    throw error
                }
            }
        }
    }

    /***
     * 扫描连接器
     */
    fun setConnector(vararg pathArray: String) {
        pathArray.map { Reflections(it) }.map { it.getSubTypesOf(BaseConnector::class.java) }.forEach { set ->
            set.forEach { clazz ->
                try {
                    if (clazz.isAnnotationPresent(Connect::class.java)) {
                        if (!instanceMap.containsKey(clazz)) {
                            instanceMap.put(clazz, clazz.newInstance())
                        }
                        val instance = instanceMap[clazz]!! as BaseConnector

                        val flag = clazz.getAnnotation(Connect::class.java)!!
                        flag.value.forEach { host ->
                            val meta = host.split(':');
                            connect(meta[0], meta[1].toInt(), instance, instance)
                        }
                    }
                    if (clazz.isAnnotationPresent(Listen::class.java)) {
                        if (!instanceMap.containsKey(clazz)) {
                            instanceMap.put(clazz, clazz.newInstance())
                        }
                        val instance = instanceMap[clazz]!! as BaseConnector

                        val flag = clazz.getAnnotation(Listen::class.java)!!
                        flag.value.forEach { port ->
                            listen(port, instance, instance)
                        }
                    }
                } catch(error: Exception) {
                    terminate()
                    throw error
                }
            }
        }
    }

    /***
     * 获得当前建立的所有连接
     */
    fun transports(): ConcurrentSkipListSet<Transport> = transports

    /***
     * 定义消息行为
     */
    fun <T> register(tag: String, type: Class<T>, handler: TypedMessageHandler<T>): MessageNode {
        handlers.put(tag, { transport, any ->
            handler.handle(transport, JSON.parseObject(JSON.toJSONBytes(any), type))
        })
        return this
    }

    /***
     * 连接到另外的MessageNode
     */
    fun connect(host: String, port: Int,
                success: ConnectHandler = ConnectHandler { },
                fail: ErrorHandler = ErrorHandler { }): MessageNode {
        connService.submit {
            try {
                val channel = SocketChannel.open()
                channel.connect(InetSocketAddress(host, port))
                channel.configureBlocking(false)
                val transport = Transport(channel = channel, error = fail)
                channel.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE, transport)
                transports.add(transport)
                success.onConnect(transport)
            } catch(error: IOException) {
                fail.onError(error)
            }
        }
        return this
    }

    /***
     * 断开已经建立的连接
     */
    fun disconnect(transport: Transport) {
        if (transports.contains(transport)) {
            transports.remove(transport)
            transport.taskQueue.clear()
            transport.channel.close()
        }
    }

    /***
     * 监听本地端口
     */
    fun listen(port: Int, accept: ConnectHandler = ConnectHandler { },
               error: ErrorHandler = ErrorHandler { }): MessageNode {
        if (servers[port] != null) return this
        val ssc = ServerSocketChannel.open().bind(InetSocketAddress(port))
                .configureBlocking(false) as ServerSocketChannel
        servers.put(port, Acceptor(port = port, ssc = ssc, accept = accept, error = error))
        return this
    }


    /***
     * 关闭本地端口
     */
    fun shutdown(port: Int) {
        val acceptor = servers[port]
        if (acceptor != null) {
            acceptor.ssc.close()
            servers.remove(port)
        }
    }

    /***
     * 关闭MessageNode
     */
    fun terminate() {
        mainService.shutdownNow()
        ioService.shutdownNow()
        connService.shutdownNow()
        eventService.shutdownNow()
        transports.forEach({ this.disconnect(it) })
        servers.forEach({ port, acceptor -> shutdown(port) })
    }

    /***
     * IO 协议: 从 byte array 中读取周期中所有Event事件
     */
    private fun readMessage(transport: Transport) {
        val buffer = ByteBuffer.allocate(32 * 1024)
        val flag = transport.channel.read(buffer);
        if (flag == -1) throw IOException("session disconnected")
        if (buffer.position() == 0) return
        val bytes = buffer.array().copyOf(buffer.position())
        ioService.submit {
            BufferedReader(InputStreamReader(ByteArrayInputStream(bytes))).use {
                while (true) {
                    val line = it.readLine() ?: break
                    try {
                        val event = JSON.parseObject(line, Event::class.java)
                        eventService.submit {
                            handlers[event.tag]?.invoke(transport, event.obj)
                        }
                    } catch(ignore: Exception) {

                    }
                }
            }
        }
    }

    /***
     * IO 协议: 发送序列化一个event
     */
    private fun sendMessage(transport: Transport, event: Event) {
        transport.channel.write(ByteBuffer.wrap((JSON.toJSONString(event) + "\n").toByteArray()))
    }
}