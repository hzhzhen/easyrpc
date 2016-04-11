package net.easyrpc.message.io;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author chpengzh
 */
public interface Connectible extends Closeable {

    /***
     * connect to another node by socket channel
     *
     * @param host remote host name
     * @param port remote host port
     * @return this instance
     */
    Connectible connect(@NotNull String host, int port) throws IOException;

    /***
     * disconnect from another node if connected
     *
     * @param transport disconnect a transport
     */
    void disconnect(Transport transport);

    /***
     * open a listen port at localhost
     *
     * @param port listen port
     * @return this instance
     */
    Connectible listen(int port) throws IOException;

    /***
     * close a listen if it is open
     *
     * @param port listen port
     */
    void close(int port) throws IOException;
}
