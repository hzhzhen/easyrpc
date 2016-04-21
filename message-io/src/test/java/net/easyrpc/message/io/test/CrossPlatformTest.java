package net.easyrpc.message.io.test;

import net.easyrpc.message.io.api.IO;
import net.easyrpc.message.io.api.MessageNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;

/**
 * @author chpengzh
 */
public class CrossPlatformTest {
    public static void main(String... args) throws IOException {
        MessageNode node1 = IO.node()
                .register("message_receive", Message.class, (tsp, object) ->
                        System.out.println(object.message)
                )
                .listen(8090, transport -> null, error -> null);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                try {
                    String line = br.readLine();
                    node1.transports().forEach(transport -> transport.send("message", new Object() {
                        public String message = line;
                    }));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static class Message {
        public String message;
    }
}
