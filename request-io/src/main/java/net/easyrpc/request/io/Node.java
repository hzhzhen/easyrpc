package net.easyrpc.request.io;

import net.easyrpc.engine.io.Engine;
import net.easyrpc.request.io.error.RemoteError;
import net.easyrpc.request.io.error.TimeoutError;
import net.easyrpc.request.io.handler.Callback;
import net.easyrpc.request.io.handler.RequestHandler;
import net.easyrpc.request.io.impl.NodeImpl;
import net.easyrpc.request.io.protocol.RequestProtocol;

public abstract class Node {

    protected RequestProtocol protocol;

    protected Node(RequestProtocol protocol) {
        this.protocol = protocol;
    }

    /***
     * 定义请求servlet handler
     *
     * @param tag     请求元数据
     * @param handler 数据响应方法
     * @return this
     */
    public abstract Node onRequest(String tag, RequestHandler handler);

    /***
     * 异步执行请求, 请求会在网络请求的执行线程池中执行, 需要提供执行回调方法
     *
     * @param hash     连接(transport)的hash
     * @param tag      请求元数据
     * @param data     数据内容
     * @param callback 执行响应
     */
    public abstract void request(int hash, String tag, byte[] data, Callback callback);

    /***
     * 同步执行请求, 请求会在当前线程中执行, 请求成功会返回结果, 失败或超时会抛出异常
     *
     * @param hash 连接(transport)的hash
     * @param tag  请求元数据
     * @param data 数据内容
     * @return 请求数据结果
     * @throws TimeoutError 网络请求超时
     * @throws RemoteError    响应错误
     */
    public abstract byte[] request(int hash, String tag, byte[] data) throws TimeoutError, RemoteError;

    /***
     * 绑定一个Engine
     *
     * @param engine 绑定实例
     * @return this
     */
    public static Node bind(Engine engine) {
        return new NodeImpl(engine);
    }
}
