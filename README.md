# Message IO

Message IO 是一个基于socket长连接的消息通信库, 它具有如下特点

- 基于异步轮询 能高效的管理同一节点上的多个主动连接和被动连接

- 连接透明 提供了钩子方法来访问每一个连接 (Transport) 的引用, 方便对每一个连接进行管理;

- 事件处理 每一个事件 (Event) 由唯一字符串 (Tag) 来标记, 对每个节点进行事件处理的编程, 能有效的实现一个复杂的消息处理和转发系统;

### 使用:

> 创建一个消息节点 (MessageNode) 

```java
MessageNode node = MessageNode.create();
```

> 消息节点监听 (listen) 本地端口

```java
node.listen(8090, transport -> {}, error -> {});//需提供处理当"接收到一个连接"和"检查到连接断开"的方法
```

> 同其他消息节点的连接 (connect)

```java
node.connect("localhost", 8090, transport -> {}, error->{});//同理需要提供"连接建立成功"和"连接未成功或断开"的方法
```

> 定义消息的接收行为

```java
node.register("handler", Status.class, (transport, object) -> {
    transport.send("handler_accept", new Status() {
        public int count = object.count + 1;
    }
});
```

> 发送一个消息

```java
//消息可以是一个简单的数据结构(String, long, byte... 等)
transport.send("message","bingo!");

//消息也可以为一个能被序列化为json的对象
transport.send("handler", new Object(){
    public int count = 1;
});
```

> 获取到一个消息节点下的所有连接

```java
Set<Transport> transports = node.transports();
```

### spring-context 下的使用与配置

> 定义一个连接与连接行为

```java
@Listen(8090)
@Connect("localhost:8090")
public class ConnectController implements BaseConnector {

    @Override
    public void onConnect(Transport transport) {

    }

    @Override
    public void onError(IOException error) {

    }

}
```

> 定义事件处理

```java
public class EventController {

    @OnEvent("message")
    public void print(String message) {
        System.out.println(message);
    }

    @OnEvent(value = "content", type = Test.class)
    public void test(Test test) {
        System.out.println(test.content);
    }

    public static class Test {
        public String content;
    }
       
}
```

> spring-context 中的配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
    http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean class="net.easyrpc.message.io.core.MessageNode">
        <property name="eventHandler" value="net.easyrpc.message.io.test"/>
        <property name="connector" value="net.easyrpc.message.io.test"/>
    </bean>

</beans>
```

其中eventHandler对应事件处理方法所在的package, 参数可以为一个array
而connector对应连接类(BaseConnector)所在的的package, 参数可以为一个array

### 效率测试

在明白了以上方法之后, 我们不妨来设计一个效率测试, 同时作为demo

```java
public class SpeedTest {

    volatile static long c = 1;
    static final int TIME = 10;
    static long start;

    public static void main(String... args) throws IOException, InterruptedException {
        //node1 会监听 8090 端口
        //当handler1接收到一个数字时会把这个数字+1, 并发送到node2节点下的handler2
        MessageNode node1 = new MessageNode().register("handler1", long.class, (transport, object) -> {
            c = object;
            transport.send("handler2", object + 1);
        }).listen(8090, transport -> {
        }, error -> {
        });

        //node2 会连接到 localhost:8090
        //当handler2接收到一个数字时会把这个数字+1, 并发送到node1节点下的handler1
        MessageNode node2 = new MessageNode().register("handler2", long.class, (transport, object) -> {
            c = object;
            transport.send("handler1", object + 1);
        }).connect("localhost", 8090, transport -> {
            transport.send("handler1", 1L);
            start = System.currentTimeMillis();
        }, error -> {
        });

        Thread.sleep(TIME * 1000); //持续一段时间
        long time = System.currentTimeMillis() - start;
        System.out.printf("Speed content: %d message per second%n", c * 1000 / time);//计算每秒的发信速度
       
        node1.terminate(); //关闭节点
        node2.terminate();
    }
}
```

抱着超级不严谨的态度我这里只能提供在我自己电脑上的运行结果, 结果自测,对,大约每秒5500条

```shell
Speed content: 5494 message per secon
```

对CPU为10%不满意的可以通过修改在`MessageNode.kt`中定义的轮询周期来调节占用, 这个修改并不会增加内存占用

```java
private val acceptPolling: Long = 10   //time unit: ms
private val messagePolling: Long = 50  //time unit: μs
```