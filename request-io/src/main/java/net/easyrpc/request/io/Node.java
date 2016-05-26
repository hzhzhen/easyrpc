package net.easyrpc.request.io;

import net.easyrpc.engine.io.Transport;
import net.easyrpc.request.io.error.FailError;
import net.easyrpc.request.io.error.TimeoutError;

public interface Node {

    Node onRequest(String tag, DataHandler data);

    byte[] request(Transport transport, String tag, byte[] data) throws TimeoutError, FailError;

    void request(Transport transport, DataHandler data, ErrorHandler error);

    interface DataHandler {
        void onData(byte[] data);
    }

    interface ErrorHandler {
        void onTimeout(TimeoutError error);

        void onFail(FailError error);
    }
}
