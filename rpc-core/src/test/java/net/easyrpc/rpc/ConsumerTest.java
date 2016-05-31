package net.easyrpc.rpc;

import net.easyrpc.rpc.core.ProxyInvoker;

import java.lang.reflect.Method;

public class ConsumerTest {

    static final TestImplement impl = new TestImplement();

    static ProxyInvoker consumer = new ProxyInvoker() {
        @Override
        public void onProxyInvoke(final Method method, final long taskId, final Object... args) throws Throwable {
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1500);//模拟迟缓的请求回调
                        try {
                            consumer.setResult(taskId, method.invoke(impl, args));
                        } catch (Exception e) {
                            consumer.setError(taskId, e);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    };

    public static void main(String... params) {
        TestInterface sample = consumer.get(TestInterface.class);
        System.out.println("2 / 1 = " + sample.divide(2, 1)); //正常执行, 结果为2
        System.out.println("1 / 0 = " + sample.divide(1, 0)); //抛出远程调用异常,信息内容为除0异常
    }

    public interface TestInterface {
        int divide(int a, int b);
    }

    public static class TestImplement implements TestInterface {

        @Override
        public int divide(int a, int b) {
            return a / b;
        }
    }

}
