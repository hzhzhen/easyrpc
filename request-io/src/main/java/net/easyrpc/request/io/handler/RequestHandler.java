package net.easyrpc.request.io.handler;

public interface RequestHandler {
    void onData(byte[] data);
}
