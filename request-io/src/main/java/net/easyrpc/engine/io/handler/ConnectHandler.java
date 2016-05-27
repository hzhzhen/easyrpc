package net.easyrpc.engine.io.handler;

import net.easyrpc.engine.io.model.Transport;

public interface ConnectHandler {
    void onEvent(int hash, Transport transport);
}
