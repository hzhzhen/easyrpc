package net.easyrpc.message.io;

import org.jetbrains.annotations.NotNull;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Transport implements Comparable<Transport> {

    public SocketChannel channel;
    public Object attachment;
    public ConcurrentLinkedQueue<Event> taskQueue = new ConcurrentLinkedQueue<>();

    Transport(SocketChannel channel) {
        this.channel = channel;
    }

    public void send(String tag, Object object) {
        taskQueue.add(new Event(tag, object));
    }

    Transport attach(Object attachment) {
        this.attachment = attachment;
        return this;
    }

    @Override
    public int compareTo(@NotNull Transport o) {
        if (o.hashCode() == hashCode()) return 0;
        return (o.hashCode() > hashCode()) ? 1 : -1;
    }
}
