package net.easyrpc.engine.io.test;

import net.easyrpc.engine.io.api.Engine;
import net.easyrpc.engine.io.Engines;
import net.easyrpc.engine.io.handler.ConnectHandler;
import net.easyrpc.engine.io.handler.ErrorHandler;
import net.easyrpc.engine.io.handler.RequestHandler;
import net.easyrpc.engine.io.model.BaseRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * 数据请求响应测试
 */
public class ResponseTest {

    public static Engine node1;
    public static Engine node2;
    public static final AtomicInteger tcpHash = new AtomicInteger(0);
    public static CountDownLatch latch1 = new CountDownLatch(1);
    public static CountDownLatch latch2 = new CountDownLatch(1);

    @BeforeClass
    public static void initiate() {
        node1 = Engines.server().listen(new InetSocketAddress(8090), new ConnectHandler() {
            @Override
            public void onEvent(final int tcpHash) {
                ResponseTest.tcpHash.set(tcpHash);
                latch1.countDown();
            }
        }, new ErrorHandler() {
            @Override
            public void onError(Throwable error) {

            }
        });

        node2 = Engines.client().connect(new InetSocketAddress("localhost", 8090), new ConnectHandler() {
            @Override
            public void onEvent(int hash) {

            }
        }, new ErrorHandler() {
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
    public void test() throws InterruptedException {
        latch1.await(2, TimeUnit.SECONDS);
        System.out.println(tcpHash.get());
        assert tcpHash.get() != 0;
        node2.onRequest("hello", new RequestHandler() {
            @Override
            public byte[] onData(byte[] data) {
                try {
                    System.out.println("message from node1 <= " + new String(data, "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return "Fine,thank you.".getBytes();
            }
        });
        node1.send(new BaseRequest() {
            @Override
            public int getTransportHash() {
                return tcpHash.get();
            }

            @Override
            public String getTag() {
                return "hello";
            }

            @Override
            public byte[] getData() {
                return "How are you?".getBytes();
            }

            @Override
            public void onResponse(byte[] responseData) {
                try {
                    System.out.println("message from node2 <= " + new String(responseData, "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                latch2.countDown();
            }
        });
        latch2.await(2, TimeUnit.SECONDS);
        assert latch1.getCount() == 0 && latch2.getCount() == 0;
    }

}
