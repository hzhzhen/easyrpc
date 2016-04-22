package net.easyrpc.message.io.api;

import java.io.IOException;

public interface ErrorHandler {
    void onError(IOException error);
}