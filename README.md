# Message IO

Message IO is a message node system. 

> create a message node

```java
MessageNode node = MessageNode.create();
```

> bind to localhost server port

```java
node.listen(8090)
```

> create another message node to connect to this one

```java
Transport transport = MessageNode.create().connect("localhost",8090);
```

> register message node action

```java
node.register("handler", Status.class, (transport, object) -> {
    transport.send("handler_accept", new Status() {
        public int count = object.count + 1;
    }
});
```

> send a message through connected transport

```java
transport.send("handler", new Status(){
    public int count = 1;
})
```

message should be json serializable!

# Speed test

speed test using a simple json message 

```json
{"count":1,"message":"message content"}
```

once server node or client node received message, `count++`

the speed result of test sample is about 3000 message item per second!