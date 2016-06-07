package net.easyrpc.rpc.test;

import net.easyrpc.rpc.protocol.JsonSerializeProtocol;
import net.easyrpc.rpc.protocol.SerializeProtocol;
import net.easyrpc.rpc.service.api.DemoService;
import org.junit.Test;

public class JsonSerializeProtocolTest {

    public SerializeProtocol protocol = new JsonSerializeProtocol();

    @Test
    public void requestSerializerTest() throws NoSuchMethodException {
        final byte[] data = protocol.serializeRequest(1L,
                DemoService.class.getMethod("add", Integer.class, Integer.class), 1, 2);
        final SerializeProtocol.MethodRequest request = protocol.antiSerializeRequest(data);
        assert request.invokeId == 1L;
        assert request.method.getName().equals("add");
        assert (int) request.args[0] == 1;
        assert (int) request.args[1] == 2;
    }

    @Test
    public void responseSerializerTest() {
        final byte[] data1 = protocol.serializeResponseResult(2L, 56);
        final SerializeProtocol.ResultResponse response1 = protocol.antiSerializeResponse(data1);

        assert response1.invokeId == 2L;
        assert response1.error == null;
        assert (int) response1.result == 56;

        final byte[] data2 = protocol.serializeResponseError(3L, new NullPointerException("fuck"));
        final SerializeProtocol.ResultResponse response2 = protocol.antiSerializeResponse(data2);

        assert response2.invokeId == 3L;
        assert response2.error != null && response2.error.contains("fuck");
        assert response2.result == null;
    }

}
