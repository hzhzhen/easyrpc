package net.easyrpc.io.engine.handler;

import net.easyrpc.io.engine.Transport;

/**
 * @author chpengzh
 */
public interface Listener {
    void call(Transport transport, byte[] message);
}
