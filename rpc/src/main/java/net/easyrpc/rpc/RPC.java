package net.easyrpc.rpc;

import java.util.List;

public interface RPC {

    void publish(Object object);

    List<RpcResource> search(Class interfaceClass);

    <T> T get(int resourceId, Class<T> interfaceClass);

    <T> T getFirst(Class<T> interfaceClass);

    class RpcResource {
        public int id;

        public String describe;
    }

}
