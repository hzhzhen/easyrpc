package net.easyrpc.message.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

/***
 * MessageNode(core)
 * <li>A message node can define its behaviours using register method</li>
 * <li>A message node can bind some ports of localhost using listen method</li>
 * <li>A message node can connect it to some nodes using connect method</li>
 * <li>A message node is a keep-alive transport container</li>
 * <li>Message nodes are closeable</li>
 */
public interface MessageNode extends Connectible, Closeable {

    long ACCEPT_POLLING = 10;
    TimeUnit ACCEPT_POLLING_UNIT = TimeUnit.MILLISECONDS;

    long MESSAGE_POLLING = 100;
    TimeUnit MESSAGE_POLLING_UNIT = TimeUnit.MICROSECONDS;

    /***
     * get all transports
     *
     * @return connected transport
     */
    ConcurrentSkipListSet<Transport> transports();

    /***
     * register a message model
     *
     * @param tag     model tag
     * @param handler message model logic
     * @return this instance
     */
    <T> MessageNode register(String tag, Class<T> type, MessageHandler<T> handler);

    /***
     * add event model when transport connect
     *
     * @param handler transport model
     * @return this instance
     */
    MessageNode setConnectHandler(TransportHandler handler);

    /***
     * add event model when transport disconnect
     *
     * @param handler transport model
     * @return this instance
     */
    MessageNode setDisconnectHandler(TransportHandler handler);

    /***
     * open a listen port at localhost
     *
     * @param port listen port
     * @return this instance
     */
    MessageNode listen(int port) throws IOException;

    /***
     * static: create a new message actor instance
     *
     * @return message actor instance
     */
    static MessageNode create() {
        return new MessageNodeImpl();
    }
}