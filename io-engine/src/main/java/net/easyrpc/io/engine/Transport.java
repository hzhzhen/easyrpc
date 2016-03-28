package net.easyrpc.io.engine;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Transport extends ConcurrentLinkedQueue<Transport.MessageTask>
        implements Closeable, Comparable<Transport> {

    public SocketChannel channel;
    public long SID;

    public Transport(@NotNull SocketChannel channel) {
        this.channel = channel;
        this.SID = System.currentTimeMillis();
    }

    public void send(@NotNull MessageTask messageTask) {
        add(messageTask);
    }

    @Override
    public void close() throws IOException {
        if (channel != null && channel.isOpen()) channel.close();
    }

    @Override
    public int compareTo(@NotNull Transport other) {
        if (other.SID == SID) return 0;
        return (other.SID > this.SID) ? -1 : 1;
    }

    public interface MessageTask {
        byte[] getBytes();
    }
}
