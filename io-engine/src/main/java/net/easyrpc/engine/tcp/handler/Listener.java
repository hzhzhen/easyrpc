package net.easyrpc.engine.tcp.handler;

import net.easyrpc.engine.tcp.Transport;

/**
 * @author chpengzh
 */
public interface Listener {
    void call(Transport transport, byte[] message);
}
