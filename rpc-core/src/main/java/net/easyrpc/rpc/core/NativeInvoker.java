package net.easyrpc.rpc.core;

import net.easyrpc.rpc.protocol.JsonSerializeProtocol;
import net.easyrpc.rpc.protocol.SerializeProtocol;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

public class NativeInvoker {

    protected final SerializeProtocol protocol;
    protected final Logger log = Logger.getLogger(getClass().getSimpleName());
    protected ApplicationContext context;

    public NativeInvoker(ApplicationContext context) {
        this(context, new JsonSerializeProtocol());
    }

    public NativeInvoker(ApplicationContext context, SerializeProtocol protocol) {
        this.context = context;
        this.protocol = protocol;
    }

    public final byte[] doResponse(byte[] requestData) {
        SerializeProtocol.MethodRequest request = protocol.antiSerializeRequest(requestData);
        try {
            Object proxy = context.getBean(request.service);
            Object result = request.method.invoke(proxy, request.args);
            return protocol.serializeResponseResult(request.invokeId, result);
        } catch (Throwable e) {
            log.throwing(getClass().getSimpleName(), "doResponse", e);
            return protocol.serializeResponseError(request.invokeId, e);
        }
    }
}
