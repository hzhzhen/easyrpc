package net.easyrpc.message.io.model;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import net.easyrpc.message.io.api.UnTypedMessageHandler;
import net.easyrpc.message.io.core.Transport;
import org.jetbrains.annotations.NotNull;

/**
 * @author chpengzh
 */
public class EventActor extends AbstractActor {

    public static Props props(@NotNull Class<?> type, @NotNull UnTypedMessageHandler handler) {
        return Props.create(EventActor.class, () -> new EventActor(type, handler));
    }

    EventActor(Class<?> type, UnTypedMessageHandler handler) {
        receive(ReceiveBuilder.match(TransportEvent.class, event ->
                handler.handle(event.transport, event.obj)).build());
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
