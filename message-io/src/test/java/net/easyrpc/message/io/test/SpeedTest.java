package net.easyrpc.message.io.test;

import net.easyrpc.message.io.MessageNode;
import net.easyrpc.message.io.Transport;

import java.io.IOException;

/**
 * @author chpengzh
 */
public class SpeedTest {
    volatile static int c = 1;
    static final int TIME = 20;

    public static void main(String... args) throws IOException, InterruptedException {
        try (
                MessageNode node1 = MessageNode.create()
                        .register("handler1", Status.class, (transport, object) -> {
                            c++;
                            transport.send("handler2", new Object() {
                                public int count = object.count + 1;
                                public String message = "message from node1";
                            });
                        }).listen(8090);

                MessageNode node2 = MessageNode.create()
                        .register("handler2", Status.class, (transport, object) -> {
                            c++;
                            transport.send("handler1", new Status() {
                                public int count = object.count + 1;
                                public String message = "message from node2";
                            });
                        })

        ) {
            Transport tcp = node2.connect("localhost", 8090);
            tcp.send("handler1", new Status() {
                public int count = c;
                public String message = "start flag";
            });
            Thread.sleep(TIME * 1000);
            System.out.printf("Speed test: %d message per second%n", c / TIME);
        }
    }
}
