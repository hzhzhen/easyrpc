package net.easyrpc.request.io.handler;

public interface DataHandler {
    void onData(int tcpHash, long serializeId, byte[] data);
}
