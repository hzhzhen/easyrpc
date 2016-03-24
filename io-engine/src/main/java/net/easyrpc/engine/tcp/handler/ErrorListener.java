package net.easyrpc.engine.tcp.handler;

import net.easyrpc.engine.tcp.Transport;

/**
 * @author chpengzh
 */
public interface ErrorListener {
    void call(Transport transport, Throwable error);
}
