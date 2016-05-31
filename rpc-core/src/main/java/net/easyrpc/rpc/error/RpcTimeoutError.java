package net.easyrpc.rpc.error;

import java.lang.reflect.Method;

/***
 * 调用超时异常
 */
public class RpcTimeoutError extends Exception {
    public RpcTimeoutError(Method method, long timeout) {
        super("Fetch result from remote fail for timeout:" +
                " [ method=" + method.getName() + ", timeout=" + timeout + " ]");
    }
}
