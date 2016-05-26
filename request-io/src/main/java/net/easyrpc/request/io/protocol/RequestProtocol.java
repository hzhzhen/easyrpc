package net.easyrpc.request.io.protocol;

import net.easyrpc.request.io.model.Request;
import net.easyrpc.request.io.model.Response;

public interface RequestProtocol {

    Request antiSerializeRequest(byte[] bytes);

    byte[] serializeRequest(Request request);

    Response antiSerializeResponse(byte[] bytes);

    byte[] serializeResponse(Response response);

}
