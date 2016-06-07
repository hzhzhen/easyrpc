package net.easyrpc.rpc.test;

import net.easyrpc.rpc.core.NativeInvoker;
import net.easyrpc.rpc.core.ProxyInvoker;
import net.easyrpc.rpc.protocol.JsonSerializeProtocol;
import net.easyrpc.rpc.service.api.DemoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:test-context.xml")
public class WorkFlowTest {

    public static NativeInvoker nativeInvoker;
    public static ProxyInvoker proxyInvoker;

    @Autowired
    public DemoService service;
    @Autowired
    public ApplicationContext context;

    @Test
    public void onWork() {
        nativeInvoker = new NativeInvoker(context);
        proxyInvoker = new ProxyInvoker(new JsonSerializeProtocol()) {
            @Override
            protected void doProxyInvoke(byte[] requestData) throws Throwable {
                setProxyResult(nativeInvoker.doResponse(requestData));
            }
        };

        DemoService proxyService = proxyInvoker.get(DemoService.class);


        int result1 = proxyService.add(5, 6);
        assert result1 == service.add(5, 6) && result1 == 5 + 6;

        int result2 = proxyService.min(5, 6);
        assert result2 == service.min(5, 6) && result2 == 5 - 6;

        float result3 = proxyService.div(5, 6);
        assert result3 == service.div(5, 6) && result3 == (float) 5 / 6;

        int result4 = proxyService.mul(5, 6);
        assert result4 == service.mul(5, 6) && result4 == 5 * 6;

        int result5 = proxyService.pow(5, 6);
        assert result5 == service.pow(5, 6) && result5 == Math.pow(5, 6);
    }
}
