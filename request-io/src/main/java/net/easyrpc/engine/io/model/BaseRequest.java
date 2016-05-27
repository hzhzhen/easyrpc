package net.easyrpc.engine.io.model;

/***
 * 一个异步的Request请求
 */
public abstract class BaseRequest {

    public abstract int getTransportHash();

    public abstract String getTag();

    public abstract byte[] getData();

    public abstract void onResponse(byte[] data);

    public long timeout() {
        return 2000;
    }

    public void onTimeout(String message) {

    }

    public void onFail(int status, String message) {

    }

    public void onComplete() {

    }

}
