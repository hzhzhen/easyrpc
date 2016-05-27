package net.easyrpc.engine.io;

import net.easyrpc.engine.io.handler.ConnectHandler;
import net.easyrpc.engine.io.handler.DataHandler;
import net.easyrpc.engine.io.handler.ErrorHandler;
import net.easyrpc.engine.io.handler.RequestHandler;
import net.easyrpc.engine.io.model.BaseRequest;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

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
     * 定义请求servlet handler
     *
     * @param tag     请求元数据
     * @param handler 数据响应方法
     * @return this
     */
    Engine onRequest(String tag, RequestHandler handler);

    /***
     * 异步执行请求, 请求会在网络请求的执行线程池中执行, 需要提供执行回调方法
     *
     * @param executor 执行线程
     * @param request  异步请求任务
     */
    void request(ExecutorService executor, BaseRequest request);

    /***
     * 关闭 Engine 实例
     */
    void terminate();

}
