package net.easyrpc.io.engine.handler;

import net.easyrpc.io.engine.Transport;

/**
 * @author chpengzh
 */
public interface ErrorListener {
    void call(Transport transport, Throwable error);
}
