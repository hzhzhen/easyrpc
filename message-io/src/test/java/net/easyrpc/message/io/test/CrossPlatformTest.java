package net.easyrpc.message.io.test;

import net.easyrpc.message.io.MessageNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;

/**
 * @author chpengzh
 */
public class CrossPlatformTest {
    public static void main(String... args) throws IOException {
        MessageNode node1 = MessageNode.create().listen(8090);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                try {
                    String line = br.readLine();
                    node1.transports().forEach(transport ->
                            transport.send("broadcast", new Object() {
                                public String message = line;
                            }));
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
