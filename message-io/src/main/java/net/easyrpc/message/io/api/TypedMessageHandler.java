package net.easyrpc.message.io.api;


import net.easyrpc.message.io.core.Transport;

public interface TypedMessageHandler<T> {
    void handle(Transport transport, T obj);
}