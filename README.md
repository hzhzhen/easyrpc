IO-Engine is a basic C/S socket model with **no protocol**!

It provide a NIO model for message transport

Server: a server is a set of transports

how to create a server

```java
Engine.server()
        .onConnect(transport ->
                System.out.printf("session:%s connect to server\n", transport.getSid())
        )
        .onDisconnect(transport ->
                System.out.printf("session:%s disconnect\n", transport.getSid())
        )
        .onError((transport, error) ->
                error.printStackTrace()
        )
        .onMessage((transport, message) -> {
            //transport here is
            //message here is a byte array
            //just encode/decode it by your protocol!
            try {
                System.out.printf("server received message:%s", new String(message, "UTF-8"));
                transport.send("Thanks"); //just do response to this message
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        })
        .listen(8090);
```

Client: a client is a transport

how to create a client

```java
Client client = Engine.client()
        .onConnect(transport -> //transport here always mean client `this`
                System.out.printf("client connect at session:%s\n", transport.getSid())
        )
        .onDisconnect(transport ->
                System.out.printf("client disconnect at session:%s\n", transport.getSid())
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
```

then I wanna connect to server

```java
client.connect("localhost", 8090);
```

and I wanna send a message to server

```java
client.send("how are you");
```

or I wanna broadcast messages from server to client
```java
server.forEach(transport -> transport.send("hello everyone!"));
```