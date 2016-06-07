package net.easyrpc.rpc.error;

public class RpcSerializeError extends RuntimeException {
    public RpcSerializeError(String message) {
        super(message);
    }
}
