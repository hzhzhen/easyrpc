package net.easyrpc.message.io;

/**
 * @author chpengzh
 */
public class Event {
    public String tag;
    public Object object;

    public Event() {
    }

    public Event(String tag, Object object) {
        this.tag = tag;
        this.object = object;
    }

    public EventActor.TransportEvent bind(Transport transport) {
        return new EventActor.TransportEvent(tag, transport, object);
    }
}
