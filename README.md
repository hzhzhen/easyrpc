# EasyRPC

我房间衣柜上有一台很古老但配置不错的笔记本电脑，我还有一个最低配的阿里云实例。本着穷人都会有效利用现有资源的原则，我决定将我的阿里云实例改造成一台具有 8 G 内存，I5 4 核 8 线程的服务器主机。

这时候我需要一个依靠长连接实现的反向代理远程调用库，这样我就能在我的电脑上安心的配置mysql服务，编写service。与此同时在服务器上只需要通过简单的反向代理就能访问到我本地的service，效果拔群！

EasyRPC 便是这么诞生的~

EasyRPC 是一套基于 Java 的高性能服务器通信库与远程方法调用调用库

- 它提供了用于长连接通信的监听连接和建立连接方法

- 它提供了长连接环境下的`发送/监听`式发送规则

- 它提供了长连接环境下的`请求/响应`式发送规则

- 它提供了能实现反向代理的远程方法调用方法

## Request-io

> request-io 是一个异步的 java 长连接通信引擎, 他能被使用于服务器之间(或服务器与客户端之间)的长连接通信.
 
request-io 提供了两种长连接环境下的发信模式, 即`发送/监听`和`请求/响应`, 后者提供了一个的异步响应回调方法.
 
使用 request-io 监听一个 ip 地址

```java
node1 = RequestIO.server().listen(new InetSocketAddress(8090), null, null);
```

使用 request-io 通过 ip 地址连接到server

```java
node2 = RequestIO.client().connect(new InetSocketAddress("localhost", 8090), tcpHash -> {
    //建立成功执行回调, 可以为null
    //node 会为每个成功建立的连接分配一个 hash 值
}, error -> {
    //建立失败或连接断开执行回调, 可以为 null
});
```

处理消息, 和servlet规则类似

```java
node1.onRequest("handler", new RequestHandler() {
    @Override
    public byte[] onData(byte[] data) {
        return "start".getBytes();
    }
});
```

发送消息, 建议重载BaseRequest中的函数以自定义BaseRequest参数(如超时时间等)

```java
node2.send(new BaseRequest() {
    @Override
    public int getTransportHash() {
       return tcpHash;//连接 hash
    }
    
    @Override
    public String getTag() {
       return "handler";//请求元数据
    }
    
    @Override
    public byte[] getData() {
       return "some request".getBytes();//请求数据
    }
    
    @Override
    public void onResponse(byte[] data) {
       //异步执行的响应回调
    }
});
```

测试机器: 
cpu 2.7 GHz Intel Core i5 占用约 1%
memory 占用约 79 mb

对`请求/响应`方法进行的单元测试结果: 
`请求/响应`速度 约 9.5k tps, 该发送压力下执行的请求超时率 约 0.2% (减少发送速度能有效地降低发送超时的可能)

```
start pass =>100000
success pass =>99774
timeout pass =>226
complete pass =>100000
branch pass =>100000
-------------------------
99774 request in 10234 ms
request tps: 9749
-------------------------
request timeout: 226
request timeout percent: 0.226%
```

## rpc-socket

> rpc-socket 是可用于反向代理的远程方法调用库, 能完美的结合 spring-framework 利用 IOC 原理

定义了一个接口库repo1

```java
public interface DemoService {

    /***
     * @return a * b
     */
    Integer add(Integer a, Integer b);

    /***
     * @return a - b
     */
    Integer min(Integer a, Integer b);

    /***
     * @return (float) a / b
     */
    Float div(Integer a, Integer b);

    /***
     * @return a * b
     */
    Integer mul(Integer a, Integer b);

    /***
     * @return a ^ b
     */
    Integer pow(Integer a, Integer b);

}
```

定义了接口实现repo2
```java
@Service
public class DemoServiceImpl implements DemoService {

    @Override
    public Integer add(Integer a, Integer b) {
        return a + b;
    }

    @Override
    public Integer min(Integer a, Integer b) {
        return a - b;
    }

    @Override
    public Float div(Integer a, Integer b) {
        return (float) a / b;
    }

    @Override
    public Integer mul(Integer a, Integer b) {
        return a * b;
    }

    @Override
    public Integer pow(Integer a, Integer b) {
        return (int) Math.pow(a, b);
    }

}
```

在consumer端只需要引入repo1, 而provider端需要在spring context中注册 repo2 的内容, 如

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
    http://www.springframework.org/schema/beans/spring-beans.xsd
    http://www.springframework.org/schema/context
    http://www.springframework.org/schema/context/spring-context.xsd">
    
    <context:component-scan base-package="net.easyrpc.rpc.service"/>
    
</beans>
```

然后, 通过从 provider 向 consumer 建立连接

```java
consumer = new SocketConsumer().bind(new InetSocketAddress(8091));
new SocketProvider(context).connect(new InetSocketAddress("localhost", 8091));
```

连接成功后就能在consumer远程调用provider定义的函数实现了

```java
DemoService service = consumer.get(DemoService.class);
System.out.println("1 + 1 = " + service.add(1, 1));
```
