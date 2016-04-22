package net.easyrpc.message.io.api;

import net.easyrpc.message.io.core.Transport;

public interface ConnectHandler {
    void onConnect(Transport transport);
}
