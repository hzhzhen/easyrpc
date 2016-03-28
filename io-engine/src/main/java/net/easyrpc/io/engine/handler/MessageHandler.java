package net.easyrpc.io.engine.handler;

import net.easyrpc.io.engine.Transport;

public interface MessageHandler<T extends Transport> {
    void call(T transport, byte[] bytes);
}
