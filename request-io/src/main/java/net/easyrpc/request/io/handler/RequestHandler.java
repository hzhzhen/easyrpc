package net.easyrpc.request.io.handler;

public interface RequestHandler {
    byte[] onData(byte[] data);
}
