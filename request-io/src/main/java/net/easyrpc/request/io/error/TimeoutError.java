package net.easyrpc.request.io.error;

public class TimeoutError extends Exception {
    public TimeoutError(String message) {
        super(message);
    }
}
