IO-Engine is a basic C/S socket model with **no protocol**!

It only provide a NIO model for message transport

### Server: 

> a server is a set of transports

- [x] how to create a server

```java
Engine.server()
        .onConnect(transport ->
                log.i("session:%s@server connect to server\n", transport.SID)
        )
        .onDisconnect(transport ->
                log.i("session:%s@server disconnect\n", transport.SID)
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
        })
        .listen(8090);
```

### Client:

> a client is a transport

- [x] how to create a client

```java
Engine.Client client = Engine.client()
        .onConnect(transport -> //transport here always mean client `this`
                log.w("client connect at session:%s@client\n", transport.SID)
        )
        .onDisconnect(transport ->
                log.w("client disconnect at session:%s@client\n", transport.SID)
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
```

- [x] I wanna connect to server

```java
client.connect("localhost", 8090);
```

- [x] I wanna send a message to server

```java
client.send("how are you\n"::getBytes);
```

- [x] I wanna broadcast messages from server to client

```java
server.forEach(transport -> transport.send("hello everyone!\n"::getBytes));
```

### Stateful Transport

> a transport means a socket channel connection. 

Once client connect to a server, both client and server create their own transport to refer this connection.

So you can easily regard it as a socket wrapper.

Attach your data to make a transport stateful

For example

```java
class Authorization {
    public boolean isLogin = false;
}
```

then you can attach it to your transport, and get it in message handler

```java
client.attach(new Authorization())
    .onMessage((transport, message) -> {
        Authorization auth = transport.attachmentAs(Authorization.class);
        if (auth.isLogin)
            ...
        else if (...) //whatever, depends on your protocol
            auth.isLogin = true
        else
            ...
    })
```

just code it with your free wisdom!!