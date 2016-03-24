package net.easyrpc.engine.tcp;

import net.easyrpc.engine.tcp.handler.ErrorListener;
import net.easyrpc.engine.tcp.handler.Handler;
import net.easyrpc.engine.tcp.handler.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author chpengzh
 */
public abstract class Client extends Transport implements Closeable {

    public Client() throws IOException {
        super(SocketChannel.open());
    }

    public abstract Client onConnect(@NotNull Handler handler);

    public abstract Client onDisconnect(@NotNull Handler handler);

    public abstract Client onMessage(@NotNull Listener listener);

    public abstract Client onError(@NotNull ErrorListener listener);

    public abstract void connect(@NotNull String host, int port) throws IOException;
}
