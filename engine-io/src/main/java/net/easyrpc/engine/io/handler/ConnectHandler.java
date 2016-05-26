package net.easyrpc.engine.io.handler;

import net.easyrpc.engine.io.Transport;

public interface ConnectHandler {
    void onEvent(int hash, Transport transport);
}
