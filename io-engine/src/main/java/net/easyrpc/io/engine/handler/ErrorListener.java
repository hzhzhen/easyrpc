package net.easyrpc.io.engine.handler;

import net.easyrpc.io.engine.Transport;

public interface ErrorListener<T extends Transport> {
    void call(T transport, Throwable error);
}
