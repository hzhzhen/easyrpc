package net.easyrpc.request.io.error;

public class FailError extends Exception {
    public FailError(int hash, String message) {
        super("Request sending to transport@" + hash +
                " fail for " + message);
    }

    public FailError(int hash, int status) {
        super("Request sending to transport@" + hash +
                " fail with status code " + status);
    }
}
