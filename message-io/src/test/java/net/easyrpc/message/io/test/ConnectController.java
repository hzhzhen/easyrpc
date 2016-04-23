package net.easyrpc.message.io.test;

import net.easyrpc.message.io.annotation.Connect;
import net.easyrpc.message.io.annotation.Listen;
import net.easyrpc.message.io.api.BaseConnector;
import net.easyrpc.message.io.core.Transport;

import java.io.IOException;

@Listen(8090)
@Connect("localhost:8090")
public class ConnectController implements BaseConnector {

    @Override
    public void onConnect(Transport transport) {

    }

    @Override
    public void onError(IOException error) {

    }

}
