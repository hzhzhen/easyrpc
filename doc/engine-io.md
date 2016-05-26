# Engine IO

Engine IO 是一个基于socket长连接的消息通信库, 它具有如下特点

- 基于异步轮询 能高效的管理同一节点上的多个主动连接和被动连接

- 连接透明 提供了钩子方法来访问每一个连接 (Transport) 的引用, 方便对每一个连接进行管理;

- 事件处理 每一个事件 (Event) 由唯一字符串 (Tag) 来标记, 对每个节点进行事件处理的编程, 能有效的实现一个复杂的消息处理和转发系统;

### 使用:

> 创建一个Engine实例

```java
Engine engine1 = new BaseEngine();
```

> 监听一个 ip host

```java
int hashcode;

engine1.listen(new InetAddress(8090), (tcpHash, transport) -> {
    hashcode = tcpHash;//每一个建立的连接都拥有一个int型的hash值
}, error -> {});
```

> Engine实例之间的连接

```java
List<Integer> tcps = new ArrayList();

Engine engine2 = new BaseEngine()
    .connect(new InetSocketAddress("localhost",8090), (tcpHash, transport) -> {
        tcp.add(tcpHash);
    }, error -> {});
```

> Engine 的事件订阅

```java
engine1.subscribe("SomeCategory", (transport, id, data) -> {
    System.out.println(new String(data,"utf-8"));
    transport.send("AlreadyReadIt", "Thanks for your message".getbytes()); 
});
```

> Engine 发送事件

```java
for (int i : tcps) {
    engine2.send(i,"SomeCategory", "Hi there!".getbytes());
}
```

> 核心接口 `Engine` 用于 Engine 节点之间的连接与消息订阅和发送, 如下为该接口定义

```java
public interface Engine {

    /***
     * 通过 socket 连接到另外一个Engine
     *
     * @param address   连接地址
     * @param onSuccess 连接成功回调
     * @param onError   连接失败或连接断开执行回调
     * @return this
     */
    Engine connect(InetSocketAddress address, ConnectHandler onSuccess, ErrorHandler onError);

    /***
     * 监听一个本地 socket 端口
     *
     * @param address  监听地址
     * @param onAccept 监听成功回调
     * @param onError  监听连接断开回调
     * @return this
     */
    Engine listen(InetSocketAddress address, ConnectHandler onAccept, ErrorHandler onError);

    /***
     * 断开一个连接
     *
     * @param hash 连接hash
     */
    void disconnect(int hash);

    /***
     * 关闭一个本地监听端口
     *
     * @param address 本地端口
     */
    void close(InetSocketAddress address);

    /***
     * 订阅一个消息
     *
     * @param category 消息分类
     * @param handler  订阅行为
     */
    Engine subscribe(String category, DataHandler handler);

    /***
     * 发布一则消息
     *
     * @param hash     连接hashcode
     * @param category 消息分类
     * @param data     消息主体数据
     */
    long send(int hash, String category, byte[] data);

    /***
     * 关闭 Engine 实例
     */
    void terminate();

}
```

### 效率测试

在明白了以上方法之后, 我们不妨来设计一个效率测试, 同时作为demo

```java
public class SpeedTest {

    volatile static long c = 1;
    static final int TIME = 30;
    static long start;

    public static void main(String... args) throws IOException, InterruptedException {
        Engine node1 = new BaseEngine().subscribe("handler1", (transport, id, data) -> {
            c++;
            //System.out.println("handler1:" + id);
            transport.send("handler2", "a".getBytes());
        }).listen(new InetSocketAddress(8090), (hash, transport) -> {
        }, error -> {
        });

        Engine node2 = new BaseEngine().subscribe("handler2", (transport, id, data) -> {
            c++;
            //System.out.println("handler2:" + id);
            transport.send("handler1", "a".getBytes());
        }).connect(new InetSocketAddress("localhost", 8090), (hash, transport) -> {
            transport.send("handler1", (1L + "").getBytes());
            start = System.currentTimeMillis();
        }, error -> {
        });
        Thread.sleep(TIME * 1000);
        long time = System.currentTimeMillis() - start;
        System.out.printf("Speed content: %d message per second%n", c * 1000 / time);

        node1.terminate();
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