
package net.easyrpc.io.engine.handler;

import net.easyrpc.io.engine.Transport;

public interface Handler {
    void call(Transport transport);
}