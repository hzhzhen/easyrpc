package net.easyrpc.message.io.test;

import net.easyrpc.message.io.api.IO;
import net.easyrpc.message.io.api.MessageNode;

import java.io.IOException;

/**
 * @author chpengzh
 */
public class SpeedTest {
    volatile static long c = 1;
    static final int TIME = 10;
    static long start;

    public static void main(String... args) throws IOException, InterruptedException {
        try (
                MessageNode node1 = IO.node()
                        .register("handler1", Status.class, (transport, object) -> {
                            c = object.count;
                            transport.send("handler2", new Object() {
                                public long count = object.count + 1;
                                public String message = "message from node1";
                            });
                        })
                        .listen(8090, transport -> null, error -> null);
                MessageNode node2 = IO.node()
                        .register("handler2", Status.class, (transport, object) -> {
                            c = object.count;
                            transport.send("handler1", new Status() {
                                public long count = object.count + 1;
                                public String message = "message from node2";
                            });
                        })
        ) {
            node2.connect("localhost", 8090, transport -> {
                transport.send("handler1", new Status() {
                    public long count = c;
                    public String message = "start flag";
                });
                start = System.currentTimeMillis();
                return null;
            }, error -> null);
            Thread.sleep(TIME * 1000);
            long time = System.currentTimeMillis() - start;
            System.out.printf("Speed test: %d message per second%n", c * 1000 / time);
        }
    }
}
