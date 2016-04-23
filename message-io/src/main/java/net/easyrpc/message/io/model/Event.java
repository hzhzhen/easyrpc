package net.easyrpc.message.io.model;


import java.io.Serializable;

/**
 * @author chpengzh
 */
public class Event implements Serializable {

    public String tag;
    public Object obj;

    public Event() {
    }

    public Event(String tag, Object obj) {
        this.tag = tag;
        this.obj = obj;
    }
}
