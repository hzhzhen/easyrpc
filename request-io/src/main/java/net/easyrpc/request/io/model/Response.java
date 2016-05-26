package net.easyrpc.request.io.model;

public class Response {

    public long requestId;

    public int status;

    public byte[] data;

    public Response() {
    }

    public Response meta(long requestId) {
        this.requestId = requestId;
        return this;
    }

    public Response(long requestId, int status, byte[] data) {
        this.requestId = requestId;
        this.status = status;
        this.data = data;
    }
}
