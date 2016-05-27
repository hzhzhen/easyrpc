package net.easyrpc.engine.io.handler;

public interface RequestHandler {
    byte[] onData(byte[] data);
}
