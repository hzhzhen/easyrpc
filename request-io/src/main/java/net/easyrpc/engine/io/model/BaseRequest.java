package net.easyrpc.engine.io.model;

/***
 * 一个异步的Request请求
 */
public abstract class BaseRequest {

    /***
     * 连接 hash 值, 在连接建立的时候可以获取
     */
    public abstract int getTransportHash();

    /***
     * 请求元信息
     */
    public abstract String getTag();

    /***
     * 请求数据内容
     */
    public abstract byte[] getData();

    /***
     * 请求响应回调 Important!
     */
    public abstract void onResponse(byte[] responseData);

    /***
     * 请求超时时间,请根据网络情况重载
     *
     * @return 超时时间, 单位ms
     */
    public long timeout() {
        return 2000;
    }

    /***
     * 请求开始执行
     */
    public void onStart(long serializeId) {

    }

    /***
     * 请求执行失败执行回调 Important!
     *
     * @param status  失败状态码
     * @param message 失败信息
     */
    public void onFail(int status, String message) {

    }

    /***
     * 请求响应超时执行回调 Important!
     *
     * @param message 超时信息
     */
    public void onTimeout(String message) {

    }

    /***
     * 请求结束执行回调
     */
    public void onComplete() {

    }

}
