package net.easyrpc.engine.io.handler;

import net.easyrpc.engine.io.Transport;

/***
 * 消息处理方法
 */
public interface MessageHandler {
    void handle(Transport transport, Long packageId, byte[] obj);
}