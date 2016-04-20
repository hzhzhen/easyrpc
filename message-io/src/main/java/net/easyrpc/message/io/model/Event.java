package net.easyrpc.message.io.model;


import net.easyrpc.message.io.core.Transport;

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

    public EventActor.TransportEvent bind(Transport transport) {
        return new EventActor.TransportEvent(tag, transport, obj);
    }
}
