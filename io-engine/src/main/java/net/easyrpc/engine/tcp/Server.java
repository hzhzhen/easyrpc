package net.easyrpc.engine.tcp;

import net.easyrpc.engine.tcp.handler.ErrorListener;
import net.easyrpc.engine.tcp.handler.Handler;
import net.easyrpc.engine.tcp.handler.Listener;

import java.io.Closeable;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author chpengzh
 */
public abstract class Server extends ConcurrentSkipListSet<Transport> implements Closeable {

    public abstract Server onConnect(Handler handler);

    public abstract Server onDisconnect(Handler handler);

    public abstract Server onMessage(Listener listener);

    public abstract Server onError(ErrorListener listener);

    public abstract void close(Transport transport);

    public abstract void listen(int port);
}
