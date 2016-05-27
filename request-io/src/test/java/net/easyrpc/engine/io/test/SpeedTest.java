package net.easyrpc.engine.io.test;

import net.easyrpc.engine.io.Engine;
import net.easyrpc.engine.io.handler.ConnectHandler;
import net.easyrpc.engine.io.handler.ErrorHandler;
import net.easyrpc.engine.io.impl.IOEngine;
import net.easyrpc.engine.io.model.BaseRequest;
import net.easyrpc.engine.io.model.Transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * @author chpengzh
 */
public class SpeedTest {

    public static final int COUNTS = 10000;
    public static CountDownLatch batchLatch;
    public static CountDownLatch totalLatch;
    public static CountDownLatch lossLatch;
    public static long start;

    public static Engine node1;
    public static Engine node2;

    public static void main(String... args) throws IOException, InterruptedException {

        totalLatch = new CountDownLatch(COUNTS);
        batchLatch = new CountDownLatch(COUNTS);
        lossLatch = new CountDownLatch(COUNTS);

        node1 = new IOEngine().listen(new InetSocketAddress(8090), new ConnectHandler() {
            @Override
            public void onEvent(int hash, Transport transport) {
                start = System.currentTimeMillis();
                for (int i = 0; i < COUNTS; i++) {
                    node1.send(new BaseRequest() {
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
                            batchLatch.countDown();
                        }

                        @Override
                        public void onFail(int status, String message) {
                            batchLatch.countDown();
                        }

                        @Override
                        public void onTimeout(String message) {
                            batchLatch.countDown();
                            lossLatch.countDown();
                        }

                        @Override
                        public void onComplete() {
                            totalLatch.countDown();
                        }
                    });
                }
            }
        }, new ErrorHandler() {
            @Override
            public void onError(Throwable error) {

            }
        });

        node2 = new IOEngine().onRequest("handler", origin -> "ok!".getBytes())
                .connect(new InetSocketAddress("localhost", 8090), new ConnectHandler() {
                    @Override
                    public void onEvent(int hash, Transport transport) {

                    }
                }, new ErrorHandler() {
                    @Override
                    public void onError(Throwable error) {

                    }
                });

        totalLatch.await();
        System.out.println("total latch pass =>");
        batchLatch.await();
        System.out.println("batch latch pass =>");

        long end = System.currentTimeMillis();

        long result = lossLatch.getCount() * 1000 / (end - start);
        System.out.println(COUNTS + " request in " + (end - start) + " ms");
        System.out.println("request tps: " + result);
        System.out.println("-------------------------");
        System.out.println("request timeout: " + (COUNTS - lossLatch.getCount()));

        System.out.println("-------------------------");
        System.out.println("request lose: " + ((double) (COUNTS - lossLatch.getCount())) * 100 / COUNTS + "%");

        node2.terminate();
        node1.terminate();
    }
}


