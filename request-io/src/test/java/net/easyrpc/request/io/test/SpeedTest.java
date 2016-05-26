package net.easyrpc.request.io.test;

import net.easyrpc.engine.io.impl.BaseEngine;
import net.easyrpc.request.io.Node;

import java.io.IOException;
import java.net.InetSocketAddress;

public class SpeedTest {

    public static void main(String... args) throws IOException, InterruptedException {
        Node.bind(
                new BaseEngine().listen(new InetSocketAddress(8090), (hash, transport) -> {

                }, error -> {

                })
        ).onRequest("handler1", data -> "I'v received your message".getBytes());

    }
}
