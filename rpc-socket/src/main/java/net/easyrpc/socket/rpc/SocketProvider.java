package net.easyrpc.socket.rpc;

import net.easyrpc.request.io.RequestIO;
import net.easyrpc.request.io.api.Engine;
import net.easyrpc.request.io.handler.ConnectHandler;
import net.easyrpc.request.io.handler.DataHandler;
import net.easyrpc.request.io.handler.ErrorHandler;
import net.easyrpc.rpc.core.NativeInvoker;
import org.springframework.context.ApplicationContext;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/***
 * pro
 */
public class SocketProvider extends NativeInvoker {

    private final Engine engine;

    public SocketProvider(ApplicationContext context) {
        super(context);
        engine = RequestIO.engine();

        //对consumer发出的请求执行响应
        engine.on("/rpc-socket/request", new DataHandler() {
            @Override
            public void onData(int tcpHash, long serializeId, byte[] data) {
                engine.emit(tcpHash, "/rpc-socket/response", doResponse(data));
            }
        });
    }

    public boolean publish(InetSocketAddress address) throws InterruptedException {
        return publish(address, 3, TimeUnit.SECONDS);
    }

    /***
     * 发布: 连接到一个consumer并向其提供远程调用服务, 返回连接成功标志
     * 注意:
     * 1. 该方法包含网络请求过程, 为一个阻塞方法!
     * 2. 超时时间
     *
     * @param address     consumer ip地址
     * @param timeout     超时时间
     * @param timeoutUnit 时间单位
     * @return 发布成功标志
     * @throws InterruptedException
     */
    public boolean publish(InetSocketAddress address, long timeout, TimeUnit timeoutUnit) throws InterruptedException {
        log.info("发布资源 =>" + address.toString());
        final AtomicBoolean flag = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);
        engine.connect(address, new ConnectHandler() {
            @Override
            public void onEvent(int tcpHash) {
                log.info("发布成功! 连接hash值:" + tcpHash);
                flag.set(true);
                latch.countDown();
            }
        }, new ErrorHandler() {
            @Override
            public void onError(Throwable error) {
                log.info("发布失败! 错误信息:");
                log.throwing(getClass().getSimpleName(), "publish", error);
                latch.countDown();
            }
        });
        latch.await(timeout, timeoutUnit);
        return flag.get();
    }

    /***
     * 关闭provider
     */
    public void terminate() {
        engine.terminate();
    }
}