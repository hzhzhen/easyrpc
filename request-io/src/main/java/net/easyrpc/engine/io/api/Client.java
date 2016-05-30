package net.easyrpc.engine.io.api;

import net.easyrpc.engine.io.handler.ConnectHandler;
import net.easyrpc.engine.io.handler.ErrorHandler;
import net.easyrpc.engine.io.handler.RequestHandler;
import net.easyrpc.engine.io.model.BaseRequest;

import java.net.InetSocketAddress;

public interface Client {

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
     * 断开一个连接
     *
     * @param hash 连接hash
     */
    void disconnect(int hash);

    /***
     * 处理事件类型
     *
     * @param tag     请求元数据
     * @param handler 数据响应方法
     * @return this
     */
    Engine onRequest(String tag, RequestHandler handler);

    /***
     * 异步发送一个事件
     *
     * @param request 异步请求任务
     */
    boolean send(BaseRequest request);

    /***
     * 关闭 Engine 实例
     */
    void terminate();

}
