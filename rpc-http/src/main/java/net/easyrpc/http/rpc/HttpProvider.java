package net.easyrpc.http.rpc;

import java.net.InetSocketAddress;

public abstract class HttpProvider {

    public static HttpProvider to(InetSocketAddress address) {
        return new HttpProviderImpl(address);
    }

    public abstract void provide(Object object);

}

class HttpProviderImpl extends HttpProvider {
    HttpProviderImpl(InetSocketAddress address) {

    }

    @Override
    public void provide(Object object) {

    }
}


