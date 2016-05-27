package net.easyrpc.engine.io.handler;

import net.easyrpc.engine.io.model.Transport;

/***
 * 消息处理方法
 */
public interface DataHandler {
    void handle(Transport transport, Long packageId, byte[] obj);
}