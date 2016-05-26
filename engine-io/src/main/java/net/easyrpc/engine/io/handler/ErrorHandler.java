package net.easyrpc.engine.io.handler;

public interface ErrorHandler {
    void onError(Throwable error);
}