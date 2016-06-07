package net.easyrpc.socket.rpc;

import net.easyrpc.request.io.RequestIO;
import net.easyrpc.request.io.api.Engine;
import net.easyrpc.request.io.handler.ConnectHandler;
import net.easyrpc.request.io.handler.DataHandler;
import net.easyrpc.request.io.handler.ErrorHandler;
import net.easyrpc.rpc.core.ProxyInvoker;
import net.easyrpc.rpc.error.RpcTimeoutError;
import net.easyrpc.rpc.protocol.JsonSerializeProtocol;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

/***
 * consumer action:
 * - get: get a rpc service to be invoked
 * - bind: listen a local IP port
 * - terminate: shutdown
 */
public class SocketConsumer extends ProxyInvoker {

    private final Engine engine;

    private final static int DISCONNECT = -1;
    private final AtomicReference<Integer> transport = new AtomicReference<>(DISCONNECT);

    @Override
    protected void doProxyInvoke(byte[] requestData) throws Throwable {
        int tcpHash = transport.get();
        if (tcpHash == DISCONNECT) throw new RpcTimeoutError("No provider connected!");
        engine.emit(tcpHash, "/rpc-socket/request", requestData);
    }

    public SocketConsumer() {
        super();
        engine = RequestIO.engine();
        engine.on("/rpc-socket/response", new DataHandler() {
            @Override
            public void onData(int tcpHash, long serializeId, byte[] data) {
                setProxyResult(data);
            }
        });
    }

    /***
     * 绑定某ip地址, 监听改地址的provider发布
     *
     * @param address ip地址
     * @return this
     */
    public SocketConsumer bind(InetSocketAddress address) {
        engine.listen(address, new ConnectHandler() {
            @Override
            public void onEvent(int tcpHash) {
                transport.set(tcpHash);
            }
        }, new ErrorHandler() {
            @Override
            public void onError(Throwable error) {
                transport.set(DISCONNECT);
            }
        });
        return this;
    }

    public void terminate() {
        engine.terminate();
    }

}