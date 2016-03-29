package net.easyrpc.io.engine.handler;

import net.easyrpc.io.engine.Engine;

public interface ErrorListener<T extends Engine.Transport> {
    void call(T transport, Throwable error);
}
