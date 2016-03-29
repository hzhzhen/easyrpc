package net.easyrpc.io.engine.test;

import net.easyrpc.io.engine.Engine;
import org.quickapi.core.Data;

import java.io.IOException;

/**
 * @author chpengzh
 */
public class SpeedTest {
    static long count = 0;
    static long start;
    static long end;
    static long sampleCount = 100 * 1000L;
    static byte[] sample = Data.randomLetters(16).getBytes();
    static long max = sample.length * sampleCount;

    public static void main(String... args) throws IOException, InterruptedException {
        Engine.server().onMessage((transport, bytes) -> {
            count += bytes.length;
            if (count == max) {
                end = System.currentTimeMillis();
                System.out.printf("send cast %d ms\n", end - start);
                System.out.printf("messages were sent in %d per second\n", 1000 * sampleCount / (end - start));
            }
        }).listen(8090);
        Engine.Client client = Engine.client();
        client.connect("localhost", 8090);
        start = System.currentTimeMillis();
        for (Integer i = 0; i < sampleCount; i++) client.send(() -> sample);
    }
}
