package net.easyrpc.rpc.error;

import java.lang.reflect.Method;

/***
 * 远程执行异常
 */
public class RpcRemoteError extends Exception {
    public RpcRemoteError(Method method, String message) {
        super("Remote method error for " +
                " [ method=" + method.getName() + ", error=" + message + " ]");
    }
}
