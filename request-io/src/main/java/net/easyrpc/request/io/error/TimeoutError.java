package net.easyrpc.request.io.error;

public class TimeoutError extends Exception {
    public TimeoutError(int hash) {
        super("Request sending to transport@" + hash + " timeout, " +
                "please make sure if this transport is connectible!");
    }
}
