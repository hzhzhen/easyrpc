package net.easyrpc.message.io.core

import akka.actor.ActorRef
import akka.actor.ActorSystem
import com.alibaba.fastjson.JSON
import net.easyrpc.message.io.annotation.Connect
import net.easyrpc.message.io.annotation.Listen
import net.easyrpc.message.io.annotation.OnEvent
import net.easyrpc.message.io.api.BaseConnector
import net.easyrpc.message.io.api.ConnectHandler
import net.easyrpc.message.io.api.ErrorHandler
import net.easyrpc.message.io.api.TypedMessageHandler
import net.easyrpc.message.io.error.IllegalTagException
import net.easyrpc.message.io.error.IllegalTypeException
import net.easyrpc.message.io.model.Event
import net.easyrpc.message.io.model.EventActor
import org.reflections.Reflections
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
    private val messagePolling: Long = 100 //time unit: μs
    private val acceptPollingUnit = TimeUnit.MILLISECONDS
    private val messagePollingUnit = TimeUnit.MICROSECONDS

    private val transports = ConcurrentSkipListSet<Transport>()
    private val servers = ConcurrentHashMap<Int, Acceptor>()

    private val system = ActorSystem.create()
    private val actors = ConcurrentHashMap<String, ActorRef>()

    private val mainService = Executors.newScheduledThreadPool(2)
    private val ioService = Executors.newSingleThreadExecutor()
    private val connService = Executors.newSingleThreadExecutor()
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
    fun setEventHandler(vararg pathArray: Array<String>) {
        pathArray.map { Reflections(it) }.map { it.getMethodsAnnotatedWith(OnEvent::class.java) }.forEach { set ->
            set.forEach { method ->
                //构造反射调用所需的实例, 注意为每个类保留一个公共的空构造方法
                try {
                    if (!instanceMap.containsKey(method.javaClass))
                        instanceMap.put(method.javaClass, method.javaClass.newInstance())
                    val flag = method.getAnnotation(OnEvent::class.java)
                    //检查是否存在该tag
                    if (actors[flag.value] != null)
                        throw IllegalTagException("node terminate with error\ntag:${flag.value} is already exists!!")

                    //注册消息行为
                    actors.put(flag.value, system.actorOf(EventActor.props(flag.type.java, { transport, any ->
                        //反射调用方法
                        method(instanceMap[method.javaClass], *Array<Any>(method.parameterCount, { index ->
                            when (method.parameterTypes[index]) {
                                Transport::class.java -> transport
                                flag.type -> JSON.parseObject(JSON.toJSONBytes(any), flag.type)
                                else -> throw IllegalArgumentException("can't fill argument at index:$index")
                            }
                        }))
                    }), flag.value))
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
    fun setConnector(vararg pathArray: Array<String>) {
        pathArray.map { Reflections(it) }.map { it.getTypesAnnotatedWith(Connect::class.java) }.forEach { set ->
            set.forEach { clazz ->
                try {
                    if (!ConnectHandler::class.java.isAssignableFrom(clazz))
                        throw IllegalTypeException("connector should extends BaseConnector")
                    if (!instanceMap.containsKey(clazz)) {
                        instanceMap.put(clazz, clazz.newInstance())
                    }
                    val instance = instanceMap[clazz]!! as BaseConnector
                    val flag = clazz.getAnnotation(Connect::class.java)!!
                    flag.value.forEach { host ->
                        val meta = host.split(':');
                        connect(meta[0], meta[1].toInt(), instance, instance)
                    }
                } catch(error: Exception) {
                    terminate()
                    throw error
                }
            }
        }
        pathArray.map { Reflections(it) }.map { it.getTypesAnnotatedWith(Listen::class.java) }.forEach { set ->
            set.forEach { clazz ->
                try {
                    if (!ConnectHandler::class.java.isAssignableFrom(clazz))
                        throw IllegalTypeException("connector should extends BaseConnector")
                    if (!instanceMap.containsKey(clazz)) {
                        instanceMap.put(clazz, clazz.newInstance())
                    }
                    val instance = instanceMap[clazz]!! as BaseConnector
                    val flag = clazz.getAnnotation(Listen::class.java)!!
                    flag.value.forEach { port ->
                        listen(port, instance, instance)
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
        actors.put(tag, system.actorOf(EventActor.props(type, { transport, any ->
            handler.handle(transport, JSON.parseObject(JSON.toJSONBytes(any), type))
        }), tag))
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
        transports.forEach({ this.disconnect(it) })
        servers.forEach({ port, acceptor -> shutdown(port) })
        system.terminate()
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
                        actors[event.tag]?.tell(event.bind(transport), ActorRef.noSender())
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