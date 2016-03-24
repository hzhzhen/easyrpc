package net.easyrpc.engine.tcp;

/**
 * @author chpengzh
 */
public interface Engine {
    static Server server() {
        return new ServerImpl();
    }

    static Client client() {
        return new ClientImpl();
    }
}
