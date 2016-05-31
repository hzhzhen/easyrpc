package net.easyrpc.request.io.api;

import net.easyrpc.request.io.handler.ConnectHandler;
import net.easyrpc.request.io.handler.ErrorHandler;
import net.easyrpc.request.io.handler.RequestHandler;
import net.easyrpc.request.io.model.BaseRequest;

import java.net.InetSocketAddress;

public interface Server {

    /***
     * 监听一个本地 socket 端口
     *
     * @param address  监听地址
     * @param onAccept 监听成功回调
     * @param onError  监听连接断开回调
     * @return this
     */
    Server listen(InetSocketAddress address, ConnectHandler onAccept, ErrorHandler onError);

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
     * 处理事件类型
     *
     * @param tag     请求元数据
     * @param handler 数据响应方法
     * @return this
     */
    Server onRequest(String tag, RequestHandler handler);

    /***
     * 异步发送一个事件
     *
     * @param request 异步请求任务
     */
    boolean send(BaseRequest request);

    /***
     * 关闭 Node 实例
     */
    void terminate();

}
