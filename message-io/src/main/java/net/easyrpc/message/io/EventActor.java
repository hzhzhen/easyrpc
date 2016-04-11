package net.easyrpc.message.io;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.alibaba.fastjson.JSON;

/**
 * @author chpengzh
 */
public class EventActor<T> extends AbstractActor {

    public static <T> Props props(Class<T> type, MessageHandler<T> handler) {
        return Props.create(EventActor.class, () -> new EventActor<>(type, handler));
    }

    EventActor(Class<T> type, MessageHandler<T> handler) {
        receive(ReceiveBuilder
                .match(TransportEvent.class, event ->
                        handler.handle(event.transport,
                                JSON.parseObject(JSON.toJSONBytes(event.object), type)))
                .build());
    }

    public static class TransportEvent {

        public String tag;
        public Transport transport;
        public Object object;

        public TransportEvent(String tag, Transport transport, Object object) {
            this.tag = tag;
            this.transport = transport;
            this.object = object;
        }

    }
}
