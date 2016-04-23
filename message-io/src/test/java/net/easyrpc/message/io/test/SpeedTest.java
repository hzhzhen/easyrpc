package net.easyrpc.message.io.test;

import net.easyrpc.message.io.core.MessageNode;

import java.io.IOException;

/**
 * @author chpengzh
 */
public class SpeedTest {

    volatile static long c = 1;
    static final int TIME = 10;
    static long start;

    public static void main(String... args) throws IOException, InterruptedException {

        MessageNode node1 = new MessageNode().register("handler1", long.class, (transport, object) -> {
            c = object;
            transport.send("handler2", object + 1);
        }).listen(8090, transport -> {
        }, error -> {
        });

        MessageNode node2 = new MessageNode().register("handler2", long.class, (transport, object) -> {
            c = object;
            transport.send("handler1", object + 1);
        }).connect("localhost", 8090, transport -> {
            transport.send("handler1", 1L);
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
