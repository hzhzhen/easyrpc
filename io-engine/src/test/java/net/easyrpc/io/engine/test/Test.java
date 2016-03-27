package net.easyrpc.io.engine.test;

import net.easyrpc.io.engine.Engine;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author chpengzh
 */
public class Test {
    public static void main(String... args) throws IOException, InterruptedException {
        Engine.Server server = Engine.server()
                .onConnect(transport ->
                        System.out.printf("session:%s@server connect to server\n", transport.SID)
                )
                .onDisconnect(transport ->
                        System.out.printf("session:%s@server disconnect\n", transport.SID)
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

        Engine.Client client = Engine.client()
                .onConnect(transport -> //transport here always mean client `this`
                        System.out.printf("client connect at session:%s@client\n", transport.SID)
                )
                .onDisconnect(transport ->
                        System.out.printf("client disconnect at session:%s@client\n", transport.SID)
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
        client.send("how are you? Fine.\n");
    }
}
