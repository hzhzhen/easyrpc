package net.easyrpc.message.io.api;

import net.easyrpc.message.io.core.MessageNodeImpl;

/**
 * @author chpengzh
 */
public interface IO {
    static MessageNode node() {
        return new MessageNodeImpl();
    }
}
