package net.easyrpc.engine.tcp.test;

import net.easyrpc.engine.tcp.Client;
import net.easyrpc.engine.tcp.Engine;
import net.easyrpc.engine.tcp.Server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author chpengzh
 */
public class Test {
    public static void main(String... args) throws IOException, InterruptedException {
        Server server = Engine.server()
                .onConnect(transport ->
                        System.out.printf("session:%s connect to server\n", transport.SID)
                )
                .onDisconnect(transport ->
                        System.out.printf("session:%s disconnect\n", transport.SID)
                )
                .onError((transport, error) ->
                        error.printStackTrace()
                )
                .onMessage((transport, message) -> {
                    //transport here refer where message comes from
                    //message here is a byte array, for the sake of no protocol
                    //just encode/decode it by your protocol!
                    try {
                        System.out.printf("server received message:%s", new String(message, "UTF-8"));
                        transport.send("Thanks"); //just do response to this message
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                });
        server.listen(8090);

        Client client = Engine.client()
                .onConnect(transport -> //transport here always mean client `this`
                        System.out.printf("client connect at session:%s\n", transport.SID)
                )
                .onDisconnect(transport ->
                        System.out.printf("client disconnect at session:%s\n", transport.SID)
                )
                .onError((transport, error) ->
                        error.printStackTrace()
                )
                .onMessage((transport, message) -> {
                    try {
                        System.out.printf("client received message from server:%s ",
                                new String(message, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                });
        client.connect("localhost", 8090);
        client.send("how are you?");
        client.send("fine\n");
    }
}
