package net.easyrpc.request.io.test;

import net.easyrpc.engine.io.impl.BaseEngine;
import net.easyrpc.request.io.Node;
import net.easyrpc.request.io.error.FailError;
import net.easyrpc.request.io.error.TimeoutError;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;

public class RequestTest {

    private static int hash;

    public static void main(String... args) throws IOException, InterruptedException, FailError, TimeoutError {

        Node.bind(
                new BaseEngine().listen(new InetSocketAddress(8090), (hash, transport) -> {
                }, error -> {
                })
        ).onRequest("some-request", data -> {
            try {
                System.out.println(new String(data, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return "I'v received your message".getBytes();
        });

        Node node = Node.bind(
                new BaseEngine().connect(new InetSocketAddress("localhost", 8090), (hash, transport) -> {
                    RequestTest.hash = hash;
                }, error -> {
                })
        );

        Thread.sleep(1000);
        System.out.println(new String(node.request(hash, "some-request", "what the fuck".getBytes()), "utf-8"));

    }
}
