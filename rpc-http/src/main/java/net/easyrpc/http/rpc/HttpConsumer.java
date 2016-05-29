package net.easyrpc.http.rpc;

public abstract class HttpConsumer {

    public static HttpConsumer from(String url) {
        return new HttpConsumerImpl(url);
    }

    public abstract <T> T get(Class<T> type);

}

class HttpConsumerImpl extends HttpConsumer {

    HttpConsumerImpl(String url) {

    }

    @Override
    public <T> T get(Class<T> type) {
        return null;
    }

}