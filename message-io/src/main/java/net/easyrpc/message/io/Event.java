package net.easyrpc.message.io;

import java.io.Serializable;

/**
 * @author chpengzh
 */
public class Event implements Serializable {

    public String tag;
    public Object obj;
    public Long requireId;

    public Event() {
    }

    public Event(String tag, Object obj, Long requireId) {
        this.tag = tag;
        this.obj = obj;
        this.requireId = requireId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Long getRequireId() {
        return requireId;
    }

    public void setRequireId(Long requireId) {
        this.requireId = requireId;
    }

    public EventActor.TransportEvent bind(Transport transport) {
        return new EventActor.TransportEvent(tag, transport, obj);
    }
}
