package net.easyrpc.engine.io.model;

import net.easyrpc.engine.io.handler.ErrorHandler;
import org.jetbrains.annotations.NotNull;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class Transport implements Comparable<Transport> {

    public SocketChannel channel;
    public ErrorHandler errorHandler;
    private ReentrantLock lock = new ReentrantLock();
    private long id = 0L;

    public ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    public Transport(SocketChannel channel, ErrorHandler errorHandler) {
        this.channel = channel;
        this.errorHandler = errorHandler;
    }

    //发送一则消息
    public long send(String category, byte[] data) {
        lock.lock();
        try {
            id++;
            taskQueue.add(new Message(id, category, data));
            return id;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int compareTo(@NotNull Transport other) {
        if (other.hashCode() == hashCode()) return 0;
        return other.hashCode() < hashCode() ? -1 : 1;
    }
}