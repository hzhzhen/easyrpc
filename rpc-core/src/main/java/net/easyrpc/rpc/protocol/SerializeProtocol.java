package net.easyrpc.rpc.protocol;

import net.easyrpc.rpc.error.RpcSerializeError;

import java.lang.reflect.Method;

public interface SerializeProtocol {

    byte[] serializeRequest(long invokeId, Method method, Object... args);

    MethodRequest antiSerializeRequest(byte[] request) throws RpcSerializeError;

    byte[] serializeResponseError(long invokeId, Throwable throwable);

    byte[] serializeResponseResult(long invokeId, Object object);

    ResultResponse antiSerializeResponse(byte[] response) throws RpcSerializeError;

    class MethodRequest {
        public long invokeId;
        public Class service;
        public Method method;
        public Object[] args;
    }

    class ResultResponse {
        public long invokeId;
        public Object result;
        public String error;
    }

}
