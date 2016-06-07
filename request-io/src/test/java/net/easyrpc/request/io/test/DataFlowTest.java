package net.easyrpc.request.io.test;

import net.easyrpc.request.io.RequestIO;
import net.easyrpc.request.io.api.Client;
import net.easyrpc.request.io.api.Server;
import net.easyrpc.request.io.handler.ConnectHandler;
import net.easyrpc.request.io.handler.ErrorHandler;
import net.easyrpc.request.io.handler.RequestHandler;
import net.easyrpc.request.io.model.BaseRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 执行效率与数据流覆盖测试
 */
public class DataFlowTest {

    public static final int COUNTS = 100000; //总发送消息数目
    public static final long TIMEOUT = 8000; //超时时间(ms)

    public static long begin;
    public static Server node1;
    public static Client node2;
    public static final AtomicInteger tcpHash = new AtomicInteger(0);

    @BeforeClass
    public static void initiate() {
        node1 = RequestIO.server().listen(new InetSocketAddress(8090), new ConnectHandler() {
            @Override
            public void onEvent(final int tcpHash) {
                DataFlowTest.tcpHash.set(tcpHash);
            }
        }, new ErrorHandler() {
            @Override
            public void onError(Throwable error) {

            }
        });

        node2 = RequestIO.client().onRequest("handler", new RequestHandler() {
            @Override
            public byte[] onData(byte[] data) {
                return "start".getBytes();
            }
        }).connect(new InetSocketAddress("localhost", 8090),
                new ConnectHandler() {
                    @Override
                    public void onEvent(int hash) {

                    }
                },
                new ErrorHandler() {
                    @Override
                    public void onError(Throwable error) {

                    }
                });
    }

    @AfterClass
    public static void terminate() {
        node2.terminate();
        node1.terminate();
    }

    @Test
    public void doTest() throws InterruptedException {
        final AtomicLong start = new AtomicLong(0);    //onStart 覆盖计数
        final AtomicLong timeout = new AtomicLong(0);  //onTimeout 覆盖计数
        final AtomicLong success = new AtomicLong(0);  //onResponse 覆盖计数
        final AtomicLong fail = new AtomicLong(0);     //onFail 覆盖计数
        final AtomicLong complete = new AtomicLong(0); //onComplete 覆盖计数
        final AtomicLong branch = new AtomicLong(0);   //总分支覆盖计数

        final CountDownLatch latch = new CountDownLatch(COUNTS);

        begin = System.currentTimeMillis();
        for (int j = 0; j < COUNTS; j++) {
            node1.send(new BaseRequest() {
                @Override
                public int getTransportHash() {
                    return tcpHash.get();
                }

                @Override
                public String getTag() {
                    return "handler";
                }

                @Override
                public byte[] getData() {
                    return "nice job!".getBytes();
                }

                @Override
                public void onStart(long serializeId) {
                    start.incrementAndGet();
                }

                @Override
                public void onResponse(byte[] data) {
                    branch.incrementAndGet();
                    success.incrementAndGet();
                }

                @Override
                public void onFail(int status, String message) {
                    branch.incrementAndGet();
                    fail.incrementAndGet();
                }

                @Override
                public void onTimeout(String message) {
                    branch.incrementAndGet();
                    timeout.incrementAndGet();
                }

                @Override
                public void onComplete() {
                    complete.incrementAndGet();
                    latch.countDown();
                }

                @Override
                public long timeout() {
                    return TIMEOUT;
                }
            });
        }

        latch.await(TIMEOUT + 1000, TimeUnit.MILLISECONDS);

        long end = System.currentTimeMillis();

        System.out.println("start pass =>" + start.get());
        System.out.println("success pass =>" + success.get());
        System.out.println("timeout pass =>" + timeout.get());
        System.out.println("complete pass =>" + complete.get());
        System.out.println("branch pass =>" + branch.get());

        System.out.println("-------------------------");

        long result = success.get() * 1000 / (end - begin);
        System.out.println(success.get() + " request in " + (end - begin) + " ms");
        System.out.println("request tps: " + result);

        System.out.println("-------------------------");

        System.out.println("request timeout: " + timeout.get());
        System.out.println("request timeout percent: " + ((double) timeout.get()) * 100 / COUNTS + "%");

        assert branch.get() == success.get() + timeout.get() + fail.get() &&
                start.get() == complete.get() && complete.get() == branch.get();

    }

}