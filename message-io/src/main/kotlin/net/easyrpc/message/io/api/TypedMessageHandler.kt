package net.easyrpc.message.io.api

import net.easyrpc.message.io.core.Transport

/**
 * @author chpengzh
 */
interface TypedMessageHandler<T> {
    fun handle(transport: Transport, obj: T)
}