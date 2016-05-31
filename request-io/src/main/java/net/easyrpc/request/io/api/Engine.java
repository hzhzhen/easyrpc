package net.easyrpc.request.io.api;

import net.easyrpc.request.io.handler.ConnectHandler;
import net.easyrpc.request.io.handler.DataHandler;
import net.easyrpc.request.io.handler.ErrorHandler;

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
     * @param tcpHash 连接hash
     */
    void disconnect(int tcpHash);

    /***
     * 关闭一个本地监听端口
     *
     * @param address 本地端口
     */
    void close(InetSocketAddress address);

    /***
     * 处理事件类型
     *
     * @param metaData 请求元数据
     * @param handler  数据响应方法
     */
    void on(String metaData, DataHandler handler);

    /***
     * 添加一个发送事件
     *
     * @param tcpHash  连接hash
     * @param metaData 请求元数据
     * @param data     请求数据
     * @return 事件的序列id
     */
    long emit(int tcpHash, String metaData, byte[] data);

    /***
     * 关闭 Engine 实例
     */
    void terminate();
}
