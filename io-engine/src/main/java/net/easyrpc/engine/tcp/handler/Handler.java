
package net.easyrpc.engine.tcp.handler;

import net.easyrpc.engine.tcp.Transport;

public interface Handler {
    void call(Transport transport);
}