IO-Engine is a basic C/S socket model with **no protocol**!

It only provide a NIO model for message transport

### Server: 

> a server is a set of transports

- [x] how to create a server

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

### Client:

> a client is a transport

- [x] how to create a client

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

- [x] I wanna connect to server

```java
client.connect("localhost", 8090);
```

- [x] I wanna send a message to server

```java
client.send("how are you");
```

- [x] I wanna broadcast messages from server to client

```java
server.forEach(transport -> transport.send("hello everyone!"));
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