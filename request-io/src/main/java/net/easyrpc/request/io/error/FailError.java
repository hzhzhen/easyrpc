package net.easyrpc.request.io.error;

public class FailError extends Exception {
    public FailError(String message) {
        super(message);
    }
}
