package net.easyrpc.engine.io.test;

import net.easyrpc.engine.io.Engine;
import net.easyrpc.engine.io.handler.ConnectHandler;
import net.easyrpc.engine.io.handler.ErrorHandler;
import net.easyrpc.engine.io.handler.RequestHandler;
import net.easyrpc.engine.io.impl.IOEngine;
import net.easyrpc.engine.io.model.BaseRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author chpengzh
 */
public class SpeedTest {

    public static final int COUNTS = 100000;
    public static final long TIMEOUT = 5000;

    public static final AtomicLong start = new AtomicLong(0);
    public static final AtomicLong batch = new AtomicLong(0);
    public static final AtomicLong total = new AtomicLong(0);
    public static final AtomicLong loss = new AtomicLong(0);
    public static final AtomicLong success = new AtomicLong(0);

    public static long begin;
    public static Engine node1;
    public static Engine node2;

    public static void main(String... args) throws IOException, InterruptedException {

        node1 = new IOEngine().listen(new InetSocketAddress(8090), new ConnectHandler() {
            @Override
            public void onEvent(final int tcpHash) {
                begin = System.currentTimeMillis();
                for (int j = 0; j < COUNTS; j++) {
                    node1.send(new BaseRequest() {
                        @Override
                        public int getTransportHash() {
                            return tcpHash;
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
                            batch.incrementAndGet();
                            success.incrementAndGet();
                        }

                        @Override
                        public void onFail(int status, String message) {
                            batch.incrementAndGet();
                        }

                        @Override
                        public void onTimeout(String message) {
                            batch.incrementAndGet();
                            loss.incrementAndGet();
                        }

                        @Override
                        public void onComplete() {
                            total.incrementAndGet();
                        }

                        @Override
                        public long timeout() {
                            return TIMEOUT;
                        }
                    });
                }

            }
        }, new ErrorHandler() {
            @Override
            public void onError(Throwable error) {

            }
        });

        node2 = new IOEngine().onRequest("handler", new RequestHandler() {
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

        Thread.sleep(TIMEOUT+5000);
        System.out.println("start pass =>" + start.get());
        System.out.println("success pass =>" + success.get());
        System.out.println("loss pass =>" + loss.get());
        System.out.println("total pass =>" + total.get());
        System.out.println("batch pass =>" + batch.get());

        System.out.println("-------------------------");

        long end = System.currentTimeMillis();
        long result = total.get() * 1000 / (end - begin);
        System.out.println(total.get() + " request in " + (end - begin) + " ms");
        System.out.println("request tps: " + result);

        System.out.println("-------------------------");

        System.out.println("request timeout: " + loss.get());
        System.out.println("request lose: " + ((double) loss.get()) * 100 / COUNTS + "%");

        node2.terminate();
        node1.terminate();
    }

}