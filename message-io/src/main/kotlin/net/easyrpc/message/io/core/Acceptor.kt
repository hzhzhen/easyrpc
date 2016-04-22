package net.easyrpc.message.io.core

import net.easyrpc.message.io.api.ErrorHandler
import net.easyrpc.message.io.api.ConnectHandler
import java.nio.channels.ServerSocketChannel

data class Acceptor(val port: Int,
                    val ssc: ServerSocketChannel,
                    val accept: ConnectHandler,
                    val error: ErrorHandler)