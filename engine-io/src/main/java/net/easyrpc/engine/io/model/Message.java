package net.easyrpc.engine.io.model;

import java.io.Serializable;

public class Message implements Serializable {

    public Long id;         //消息序列ID
    public String tag;      //消息tag
    public byte[] data;     //消息主体

    public Message() {
    }

    public Message(Long id, String tag, byte[] data) {
        this.id = id;
        this.tag = tag;
        this.data = data;
    }

}