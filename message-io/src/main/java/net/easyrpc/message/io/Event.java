package net.easyrpc.message.io;

import java.io.Serializable;

/**
 * @author chpengzh
 */
public class Event implements Serializable {

    public String tag;
    public String json;

    public Event() {
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Object getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public Event(String tag, String json) {
        this.tag = tag;
        this.json = json;
    }

    public EventActor.TransportEvent bind(Transport transport) {
        return new EventActor.TransportEvent(tag, transport, json);
    }
}
