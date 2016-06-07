package net.easyrpc.rpc.socket.test;

import net.easyrpc.rpc.service.api.DemoService;
import net.easyrpc.rpc.socket.SocketConsumer;
import net.easyrpc.rpc.socket.SocketProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.InetSocketAddress;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:rpc-socket-test-context.xml")
public class WorkflowTest {

    @Autowired
    ApplicationContext context;
    final SocketConsumer consumer;

    public WorkflowTest() throws InterruptedException {
        consumer = new SocketConsumer().bind(new InetSocketAddress(8091));
    }

    @Test
    public void onWork() throws InterruptedException {
        assert new SocketProvider(context).publish(new InetSocketAddress("localhost", 8091));

        DemoService service = consumer.get(DemoService.class);

        int result1 = service.add(5, 6);
        assert result1 == 5 + 6;

        int result2 = service.min(5, 6);
        assert result2 == 5 - 6;

        float result3 = service.div(5, 6);
        assert result3 == (float) 5 / 6;

        int result4 = service.mul(5, 6);
        assert result4 == 5 * 6;

        int result5 = service.pow(5, 6);
        assert result5 == Math.pow(5, 6);
    }
}
