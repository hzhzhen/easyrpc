package net.easyrpc.engine.io.test;

import net.easyrpc.engine.io.BaseEngine;
import net.easyrpc.engine.io.Engine;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author chpengzh
 */
public class SpeedTest {

    volatile static long c = 1;
    static final int TIME = 30;
    static long start;

    public static void main(String... args) throws IOException, InterruptedException {
        Engine node1 = new BaseEngine().subscribe("handler1", (transport, id, data) -> {
            c++;
            //System.out.println("handler1:" + id);
            transport.send("handler2", "a".getBytes());
        }).listen(new InetSocketAddress(8090), (hash, transport) -> {
        }, error -> {
        });

        Engine node2 = new BaseEngine().subscribe("handler2", (transport, id, data) -> {
            c++;
            //System.out.println("handler2:" + id);
            transport.send("handler1", "a".getBytes());
        }).connect(new InetSocketAddress("localhost", 8090), (hash, transport) -> {
            transport.send("handler1", (1L + "").getBytes());
            start = System.currentTimeMillis();
        }, error -> {
        });
        Thread.sleep(TIME * 1000);
        long time = System.currentTimeMillis() - start;
        System.out.printf("Speed content: %d message per second%n", c * 1000 / time);

        node1.terminate();
        node2.terminate();
    }
}
