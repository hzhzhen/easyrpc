package net.easyrpc.message.io.model;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.alibaba.fastjson.JSON;
import net.easyrpc.message.io.api.TypedMessageHandler;
import net.easyrpc.message.io.core.Transport;

/**
 * @author chpengzh
 */
public class EventActor<T> extends AbstractActor {

    public static <T> Props props(Class<T> type, TypedMessageHandler<T> handler) {
        return Props.create(EventActor.class, () -> new EventActor<>(type, handler));
    }

    EventActor(Class<T> type, TypedMessageHandler<T> handler) {
        receive(ReceiveBuilder
                .match(TransportEvent.class, event ->
                        handler.handle(event.transport, JSON.parseObject(JSON.toJSONBytes(event.obj), type)))
                .build());
    }

    public static class TransportEvent {

        public String tag;
        public Transport transport;
        public Object obj;

        public TransportEvent(String tag, Transport transport, Object obj) {
            this.tag = tag;
            this.transport = transport;
            this.obj = obj;
        }

    }
}
