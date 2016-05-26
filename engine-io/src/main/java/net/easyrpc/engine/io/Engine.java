package net.easyrpc.engine.io;

import net.easyrpc.engine.io.handler.ConnectHandler;
import net.easyrpc.engine.io.handler.ErrorHandler;
import net.easyrpc.engine.io.handler.MessageHandler;

import java.net.InetSocketAddress;

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
    Engine subscribe(String category, MessageHandler handler);

    /***
     * 发布一则消息
     *
     * @param hash     连接hashcode
     * @param category 消息分类
     * @param data     消息主体数据
     */
    void send(int hash, String category, byte[] data);

    /***
     * 关闭 Engine 实例
     */
    void terminate();

}
