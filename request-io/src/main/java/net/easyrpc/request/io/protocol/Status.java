package net.easyrpc.request.io.protocol;

public enum Status {

    OK(1, "almost ok!"),

    NOT_FOUND(2, "no such tag in remote service"),

    REMOTE_ERROR(3, "exception occur from remote service");

    public final int code;
    public final String message;

    Status(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
