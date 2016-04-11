package net.easyrpc.message.io;

/**
 * basic message model logic
 */
public interface MessageHandler<T> {
    void handle(Transport transport, T object) throws Exception;
}
