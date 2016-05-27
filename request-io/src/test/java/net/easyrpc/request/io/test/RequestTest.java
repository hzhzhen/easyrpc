package net.easyrpc.request.io.test;

import net.easyrpc.engine.io.impl.BaseEngine;
import net.easyrpc.request.io.Node;
import net.easyrpc.request.io.error.RemoteError;
import net.easyrpc.request.io.error.TimeoutError;
import net.easyrpc.request.io.handler.Callback;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;

public class RequestTest {

    private static int hash;

    public static void main(String... args) throws IOException, InterruptedException, RemoteError, TimeoutError {

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
            return null;
        });

        Node node = Node.bind(
                new BaseEngine().connect(new InetSocketAddress("localhost", 8090), (hash, transport) -> {
                    RequestTest.hash = hash;
                }, error -> {
                })
        );

        Thread.sleep(1000);

        node.request(hash, "some-request-", "what the fuck".getBytes(), new Callback() {
            @Override
            public void onData(byte[] data) {
                try {
                    System.out.println(new String(data, "utf-8"));
                    System.out.println("on received");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onTimeout(TimeoutError error) {
                System.out.println(error.toString());
            }

            @Override
            public void onFail(RemoteError error) {
                System.out.println(error.toString());
            }
        });

    }
}
