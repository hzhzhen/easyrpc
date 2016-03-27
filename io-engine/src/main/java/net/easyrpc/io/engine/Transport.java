package net.easyrpc.io.engine;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A transport is a wrapper object of socket channel
 */
public class Transport implements Closeable, Comparable<Transport> {

    private Object attachment;

    public SocketChannel channel;
    public long SID;
    public ConcurrentLinkedQueue<byte[]> task = new ConcurrentLinkedQueue<>();

    public Transport(@NotNull SocketChannel channel) {
        this.channel = channel;
        this.SID = System.currentTimeMillis();
    }

    public void send(String text) {
        task.add(text.getBytes());
    }

    public void send(byte[] bytes) {
        task.add(bytes);
    }

    public Transport attach(@NotNull Object object) {
        this.attachment = object;
        return this;
    }

    public Object attachment(){
        return attachment;
    }

    public <T> T attachmentAs(Class<T> type) {
        return (T) attachment;
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
}
