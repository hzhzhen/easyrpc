package net.easyrpc.request.io.handler;

import net.easyrpc.request.io.error.RemoteError;
import net.easyrpc.request.io.error.TimeoutError;

public interface Callback {
    void onData(byte[] data);

    void onTimeout(TimeoutError error);

    void onFail(RemoteError error);
}