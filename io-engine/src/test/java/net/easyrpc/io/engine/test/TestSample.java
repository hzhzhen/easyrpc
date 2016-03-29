package net.easyrpc.io.engine.test;

import net.easyrpc.io.engine.Engine;
import org.quickapi.core.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author chpengzh
 */
public class TestSample {

    static Logger log = Logger.defaultInstance();

    public static void main(String... args) throws IOException, InterruptedException {
        Engine.Server<Engine.Transport> server = Engine.server()
                .onConnect(transport ->
                        log.i("session:%s@server connect to server\n", transport.getSID())
                )
                .onDisconnect(transport ->
                        log.i("session:%s@server disconnect\n", transport.getSID())
                )
                .onError((transport, error) ->
                        error.printStackTrace()
                )
                .onMessage((transport, bytes) -> {
                    //transport here refer where message comes from
                    //message here is a byte array, for the sake of no protocol
                    //just encode/decode it by your protocol!
                    try {
                        log.i("server received message:%s", new String(bytes, "UTF-8"));
                        transport.send("Thanks"::getBytes); //just do response to this message
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                });
        server.listen(8090);

        Engine.Client client = Engine.client()
                .onConnect(transport -> //transport here always mean client `this`
                        log.w("client connect at session:%s@client\n", transport.getSID())
                )
                .onDisconnect(transport ->
                        log.w("client disconnect at session:%s@client\n", transport.getSID())
                )
                .onError((transport, error) ->
                        error.printStackTrace()
                )
                .onBytes((transport, bytes) -> {
                    try {
                        log.w("client received bytes from server:%s", new String(bytes, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                });
        client.connect("localhost", 8090);
        client.send("how are you?"::getBytes);
        client.send("Fine.\n"::getBytes);

        Thread.sleep(10000);
        server.forEach(transport -> transport.send("broadcast"::getBytes));
    }
}
