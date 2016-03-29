package net.easyrpc.io.engine.handler;


import net.easyrpc.io.engine.Engine;

public interface MessageHandler<T extends Engine.Transport> {
    void call(T transport, byte[] bytes);
}
