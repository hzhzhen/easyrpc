package net.easyrpc.message.io.core

import java.io.IOException
import java.nio.channels.ServerSocketChannel

data class Acceptor(val port: Int, val ssc: ServerSocketChannel,
                    val accept: (Transport) -> Unit, val error: (IOException) -> Unit)