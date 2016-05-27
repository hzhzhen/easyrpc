package net.easyrpc.engine.io.test;

import net.easyrpc.engine.io.Engine;
import net.easyrpc.engine.io.impl.EngineNode;
import net.easyrpc.engine.io.model.BaseRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author chpengzh
 */
public class SpeedTest {

    public static final int COUNTS = 10000;
    public static CountDownLatch latch;
    public static ExecutorService executorService;
    public static long start;

    public static Engine node1;
    public static Engine node2;

    public static int fail = 0;
    public static int timeout = 0;

    public static void main(String... args) throws IOException, InterruptedException {

        fail = 0;
        timeout = 0;
        latch = new CountDownLatch(COUNTS);
        executorService = Executors.newFixedThreadPool(10);

        node1 = new EngineNode().listen(new InetSocketAddress(8090), (hash, transport) -> {
            start = System.currentTimeMillis();
            for (int i = 0; i < COUNTS; i++) {
                node1.request(executorService, new BaseRequest() {
                    @Override
                    public int getTransportHash() {
                        return hash;
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
                    public void onResponse(byte[] data) {

                    }

                    @Override
                    public void onFail(int status, String message) {
                        fail++;
                    }

                    @Override
                    public void onTimeout(String message) {
                        timeout++;
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });
            }
        }, error -> {

        });

        node2 = new EngineNode().onRequest("handler", origin -> "ok!".getBytes())
                .connect(new InetSocketAddress("localhost", 8090), (hash, transport) -> {
                }, error -> {
                });

        latch.await();
        long end = System.currentTimeMillis();
        long result = (long) (COUNTS - fail - timeout) * 1000 / (end - start);
        System.out.println(COUNTS + " request in " + (end - start) + " ms");
        System.out.println("request tps: " + result);
        System.out.println("request fail: " + ((float) fail) * 100 / COUNTS + "%");
        System.out.println("request timeout: " + ((float) timeout) * 100 / COUNTS + "%");

        Thread.sleep(2000);
        node2.terminate();
        node1.terminate();
    }
}


