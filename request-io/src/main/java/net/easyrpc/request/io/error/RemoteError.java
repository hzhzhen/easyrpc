package net.easyrpc.request.io.error;

public class RemoteError extends Exception {
    public RemoteError(int hash, String message) {
        super("Request sending to transport@" + hash +
                " fail for " + message);
    }

    public RemoteError(int hash, int status) {
        super("Request sending to transport@" + hash +
                " fail with status code " + status);
    }
}
