package net.easyrpc.request.io;

import net.easyrpc.engine.io.Engine;
import net.easyrpc.request.io.error.FailError;
import net.easyrpc.request.io.error.TimeoutError;
import net.easyrpc.request.io.handler.Callback;
import net.easyrpc.request.io.handler.RequestHandler;
import net.easyrpc.request.io.protocol.RequestProtocol;

public abstract class Node {

    protected RequestProtocol protocol;

    public Node(RequestProtocol protocol) {
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
     * 异步执行请求
     *
     * @param hash     连接(transport)的hash
     * @param tag      请求元数据
     * @param data     数据内容
     * @param callback 执行响应
     */
    public abstract void request(int hash, String tag, byte[] data, Callback callback);

    /***
     * 同步执行请求
     *
     * @param hash 连接(transport)的hash
     * @param tag  请求元数据
     * @param data 数据内容
     * @return 请求数据结果
     * @throws TimeoutError 网络请求超时
     * @throws FailError    响应错误
     */
    public abstract byte[] request(int hash, String tag, byte[] data) throws TimeoutError, FailError;

    public static Node bind(Engine engine) {
        return new NodeImpl(engine);
    }
}
