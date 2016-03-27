package net.easyrpc.io.engine;

import net.easyrpc.io.engine.handler.ErrorListener;
import net.easyrpc.io.engine.handler.Handler;
import net.easyrpc.io.engine.handler.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author chpengzh
 */
public interface Engine {
    static Server server() {
        return new ServerImpl();
    }

    static Client client() {
        return new ClientImpl();
    }

    abstract class Server extends ConcurrentSkipListSet<Transport> implements Closeable {

        public abstract Server onConnect(Handler handler);

        public abstract Server onDisconnect(Handler handler);

        public abstract Server onMessage(Listener listener);

        public abstract Server onError(ErrorListener listener);

        public abstract void close(Transport transport);

        public abstract void listen(int port);
    }

    abstract class Client extends Transport implements Closeable {

        public Client() throws IOException {
            super(SocketChannel.open());
        }

        public abstract Client onConnect(@NotNull Handler handler);

        public abstract Client onDisconnect(@NotNull Handler handler);

        public abstract Client onMessage(@NotNull Listener listener);

        public abstract Client onError(@NotNull ErrorListener listener);

        public abstract void connect(@NotNull String host, int port) throws IOException;
    }
}
