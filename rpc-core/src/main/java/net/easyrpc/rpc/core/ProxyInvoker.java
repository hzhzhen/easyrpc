package net.easyrpc.rpc.core;

import net.easyrpc.rpc.error.RpcRemoteError;
import net.easyrpc.rpc.error.RpcTimeoutError;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/***
 * 用于远程调用的核心类
 * 实现网络请求过程 onProxyInvoke 以完成远程调用
 */
public abstract class ProxyInvoker {

    private final AtomicLong taskId = new AtomicLong(0);

    protected final Logger logger;
    protected final ConcurrentHashMap<Long, RpcTask> tasks = new ConcurrentHashMap<>();

    /***
     * 调用此方法进行远程代理的执行
     *
     * @param method 方法
     * @param taskId 任务id
     * @param args   调用参数
     * @throws Throwable 抛出异常
     */
    protected abstract void onProxyInvoke(Method method, long taskId, Object... args) throws Throwable;

    /***
     * 核心调用方法, 使用了 cglib 的类生成计数以构造函数接口
     *
     * @param type 接口类型
     * @return 适用远程调用过程的接口实现
     */
    protected Object getObject(Class<?> type) {
        return Enhancer.create(Object.class, new Class[]{type}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                logger.info("Consumer: Invoke method@" + method.getName() + " =>");

                long id = taskId.incrementAndGet();

                RpcTask task = new RpcTask();
                tasks.put(id, task);
                long timeout = timeout(method);

                onProxyInvoke(method, id, args);//执行代理
                logger.info("Consumer: Invoker fetch result in " + timeout + " ms");
                task.latch.get().await(timeout, TimeUnit.MILLISECONDS);//阻塞并等待结果回调

                Object result = task.result.get();
                if (!(result instanceof Nothing)) {
                    logger.info("Consumer: Invoke success!");
                    return result;
                } else if (task.error.get() != null) {
                    throw new RpcRemoteError(method, task.error.get());
                } else {
                    throw new RpcTimeoutError(method, timeout);
                }
            }
        });
    }

    public ProxyInvoker() {
        this.logger = Logger.getLogger(getClass().getSimpleName());
    }

    public long timeout(Method method) {
        return 3000;
    }

    public void setResult(long taskId, Object result) {
        RpcTask rpcTask = tasks.remove(taskId);
        if (rpcTask != null) {
            rpcTask.result.set(result);
            rpcTask.latch.get().countDown();
        }
    }

    public void setError(long taskId, Exception error) {
        RpcTask rpcTask = tasks.remove(taskId);
        if (rpcTask != null) {
            rpcTask.error.set(error instanceof InvocationTargetException ?
                    ((InvocationTargetException) error).getTargetException().toString() : error.toString());
            rpcTask.latch.get().countDown();
        }
    }

    public <T> T get(Class<T> type) {
        return (T) getObject(type);
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
