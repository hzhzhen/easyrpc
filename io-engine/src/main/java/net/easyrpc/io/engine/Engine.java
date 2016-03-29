package net.easyrpc.io.engine;

import net.easyrpc.io.engine.handler.ErrorListener;
import net.easyrpc.io.engine.handler.Handler;
import net.easyrpc.io.engine.handler.MessageHandler;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListSet;

public interface Engine {
    static Server<Transport> server() {
        return new ServerImpl();
    }

    static Client<Transport> client() {
        return new ClientImpl();
    }

    abstract class Server<T extends Transport> extends ConcurrentSkipListSet<T> implements Closeable {

        public abstract Server<T> onConnect(@NotNull Handler<T> handler);

        public abstract Server<T> onDisconnect(@NotNull Handler<T> handler);

        public abstract Server<T> onMessage(@NotNull MessageHandler<T> messageHandler);

        public abstract Server<T> onError(@NotNull ErrorListener<T> listener);

        public abstract void close(T transport);

        public abstract void listen(int port);
    }

    abstract class Client<T extends Transport> implements Closeable {

        public abstract Client<T> onConnect(@NotNull Handler<T> handler);

        public abstract Client<T> onDisconnect(@NotNull Handler<T> handler);

        public abstract Client<T> onBytes(@NotNull MessageHandler<T> messageHandler);

        public abstract Client<T> onError(@NotNull ErrorListener<T> listener);

        public abstract void send(@NotNull Transport.MessageTask task);

        public abstract void connect(@NotNull String host, int port) throws IOException;
    }
}
