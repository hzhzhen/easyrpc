package net.easyrpc.engine.io;

import net.easyrpc.engine.io.api.Client;
import net.easyrpc.engine.io.api.Engine;
import net.easyrpc.engine.io.api.Server;
import net.easyrpc.engine.io.impl.IOEngine;
import net.easyrpc.engine.io.impl.JsonEngineProtocol;
import net.easyrpc.engine.io.impl.JsonRequestProtocol;
import net.easyrpc.engine.io.protocol.EngineProtocol;
import net.easyrpc.engine.io.protocol.RequestProtocol;

/***
 * 发布类, 用于发布接口 Client, Server 和 Engine 的实现类
 */
public class Engines {

    public static Engine engine() {
        return new IOEngine(new JsonEngineProtocol(), new JsonRequestProtocol(), true);
    }

    public static Engine engine(EngineProtocol engineProtocol, RequestProtocol requestProtocol) {
        return new IOEngine(engineProtocol, requestProtocol, true);
    }

    public static Server server() {
        return new IOEngine(new JsonEngineProtocol(), new JsonRequestProtocol(), true);
    }

    public static Server server(EngineProtocol engineProtocol, RequestProtocol requestProtocol) {
        return new IOEngine(engineProtocol, requestProtocol, true);
    }

    public static Client client() {
        return new IOEngine(new JsonEngineProtocol(), new JsonRequestProtocol(), false);
    }

    public static Client client(EngineProtocol engineProtocol, RequestProtocol requestProtocol) {
        return new IOEngine(engineProtocol, requestProtocol, false);
    }

}
