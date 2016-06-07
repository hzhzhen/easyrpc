package net.easyrpc.rpc.core;

import net.easyrpc.rpc.error.RpcRemoteError;
import net.easyrpc.rpc.error.RpcTimeoutError;
import net.easyrpc.rpc.protocol.JsonSerializeProtocol;
import net.easyrpc.rpc.protocol.SerializeProtocol;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/***
 * 用于远程调用的核心类
 * 实现网络请求过程 doProxyInvoke 以完成远程调用
 */
public abstract class ProxyInvoker {

    private final AtomicLong taskId = new AtomicLong(0);

    protected final SerializeProtocol protocol;
    protected final Logger log = Logger.getLogger(getClass().getSimpleName());
    protected final ConcurrentHashMap<Long, RpcTask> tasks = new ConcurrentHashMap<>();

    protected ProxyInvoker() {
        this(new JsonSerializeProtocol());
    }

    protected ProxyInvoker(SerializeProtocol protocol) {
        this.protocol = protocol;
    }

    /***
     * 调用此方法进行远程代理的执行
     *
     * @param requestData 待发送的请求数据
     * @throws Throwable 抛出异常
     */
    protected abstract void doProxyInvoke(byte[] requestData) throws Throwable;

    /***
     * 调用此方法执行远程代理的回调
     *
     * @param responseData 由 NativeInvoker 返回的 response对象
     */
    protected final void setProxyResult(byte[] responseData) {
        SerializeProtocol.ResultResponse response = protocol.antiSerializeResponse(responseData);
        RpcTask rpcTask = tasks.remove(response.invokeId);
        if (rpcTask != null) {
            if (response.result != null) {
                rpcTask.result.set(response.result);
            } else if (response.error != null) {
                rpcTask.error.set(response.error);
            }
            rpcTask.latch.get().countDown();
        }
    }

    /***
     * 核心调用方法, 使用了 cglib 的类生成计数以构造函数接口
     *
     * @param type 接口类型
     * @return 适用远程调用过程的接口实现
     */
    private Object getObject(final Class<?> type) {
        return Enhancer.create(Object.class, new Class[]{type}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                log.info("Consumer: Invoke method@" + method.getName() + " =>");

                long id = taskId.incrementAndGet();

                RpcTask task = new RpcTask();
                tasks.put(id, task);
                long timeout = timeout(method);

                doProxyInvoke(protocol.serializeRequest(id, method, args));//执行代理
                log.info("Consumer: Invoker fetch result in " + timeout + " ms");
                task.latch.get().await(timeout, TimeUnit.MILLISECONDS);//阻塞并等待结果回调

                Object result = task.result.get();
                if (!(result instanceof Nothing)) {
                    log.info("Consumer: Invoke success!");
                    return result;
                } else if (task.error.get() != null) {
                    throw new RpcRemoteError(method, task.error.get());
                } else {
                    throw new RpcTimeoutError(method, timeout);
                }
            }
        });
    }

    /***
     * 使用接口获得一个远程调用对象
     *
     * @param type 接口类型
     * @param <T>  类型泛型
     * @return 远程调用对象
     */
    public final <T> T get(Class<T> type) {
        return (T) getObject(type);
    }

    /***
     * 重载此方法以设置请求方法的超时时间
     *
     * @param method 调用方法
     * @return 超时时间(ms)
     */
    protected long timeout(Method method) {
        return 3000;
    }

    protected class RpcTask {
        public final AtomicReference<Object> result = new AtomicReference<>();
        public final AtomicReference<CountDownLatch> latch = new AtomicReference<>();
        public final AtomicReference<String> error = new AtomicReference<>();

        RpcTask() {
            result.set(new Nothing());
            latch.set(new CountDownLatch(1));
            error.set(null);
        }
    }

    protected class Nothing {
    }

}
