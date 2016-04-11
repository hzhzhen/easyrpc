package net.easyrpc.message.io.test;

import net.easyrpc.message.io.MessageNode;

import java.io.IOException;

/**
 * @author chpengzh
 */
public class ServerNode {

    public static void main(String... args) throws IOException, InterruptedException {
        MessageNode.create()
                .register("handler1", Status.class, (transport, object) -> {
                    System.out.println(object.count);
                    Thread.sleep(1000);
                    transport.send("handler2", new Status() {
                        public int count = object.count + 1;
                    });
                })
                .listen(8090);
    }
}
