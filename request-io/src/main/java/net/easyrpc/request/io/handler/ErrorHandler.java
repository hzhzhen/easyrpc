package net.easyrpc.request.io.handler;

public interface ErrorHandler {
    void onError(Throwable error);
}