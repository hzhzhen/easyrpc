package net.easyrpc.request.io;

import net.easyrpc.request.io.api.Client;
import net.easyrpc.request.io.api.Engine;
import net.easyrpc.request.io.api.Node;
import net.easyrpc.request.io.api.Server;
import net.easyrpc.request.io.impl.IONode;
import net.easyrpc.request.io.impl.JsonEngineProtocol;
import net.easyrpc.request.io.impl.JsonRequestProtocol;
import net.easyrpc.request.io.protocol.EngineProtocol;
import net.easyrpc.request.io.protocol.RequestProtocol;

/***
 * 发布类, 用于发布接口 Client, Server 和 Node 的实现类
 */
public class RequestIO {

    public static Engine engine() {
        return new IONode(new JsonEngineProtocol(), new JsonRequestProtocol(), true);
    }

    public static Engine engine(EngineProtocol engineProtocol) {
        return new IONode(engineProtocol, new JsonRequestProtocol(), true);
    }

    public static Server server() {
        return new IONode(new JsonEngineProtocol(), new JsonRequestProtocol(), true);
    }

    public static Server server(EngineProtocol engineProtocol, RequestProtocol requestProtocol) {
        return new IONode(engineProtocol, requestProtocol, true);
    }

    public static Client client() {
        return new IONode(new JsonEngineProtocol(), new JsonRequestProtocol(), false);
    }

    public static Client client(EngineProtocol engineProtocol, RequestProtocol requestProtocol) {
        return new IONode(engineProtocol, requestProtocol, false);
    }

    public static Node node() {
        return new IONode(new JsonEngineProtocol(), new JsonRequestProtocol(), true);
    }

    public static Node node(EngineProtocol engineProtocol, RequestProtocol requestProtocol) {
        return new IONode(engineProtocol, requestProtocol, true);
    }

}
