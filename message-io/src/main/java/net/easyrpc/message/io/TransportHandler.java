package net.easyrpc.message.io;

/***
 * transport model logic
 */
public interface TransportHandler {
    void handle(Transport transport);
}