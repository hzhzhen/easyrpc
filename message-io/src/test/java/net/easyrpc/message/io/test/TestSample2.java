package net.easyrpc.message.io.test;

import net.easyrpc.message.io.MessageNode;

import java.io.IOException;

/**
 * @author chpengzh
 */
public class TestSample2 {
    public static void main(String... args) throws IOException, InterruptedException {
        MessageNode node2 = MessageNode.create()
                .register("handler2", Status.class, (transport, object) -> {
                    System.out.println(object.count);
                    Thread.sleep(1000);
                    transport.send("handler1", new Object() {
                        public int count = object.count + 1;
                    });
                })
                .connect("localhost", 8090);
        node2.transports().forEach(transport ->
                transport.send("handler1", new Object() {
                    public int count = 1;
                }));
    }
}
