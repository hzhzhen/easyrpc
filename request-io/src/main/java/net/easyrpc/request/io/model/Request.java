package net.easyrpc.request.io.model;

public class Request {

    public String tag;

    public byte[] data;

    public Request() {
    }

    public Request(String tag, byte[] data) {
        this.tag = tag;
        this.data = data;
    }
}
