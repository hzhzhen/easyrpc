package net.easyrpc.message.io.api;

import net.easyrpc.message.io.core.Transport;

public interface UnTypedMessageHandler {
    void handle(Transport transport, Object obj);
}