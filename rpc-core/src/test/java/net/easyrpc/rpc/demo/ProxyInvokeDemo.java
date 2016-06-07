package net.easyrpc.rpc.demo;

import net.easyrpc.rpc.service.api.DemoService;
import net.easyrpc.rpc.service.impl.DemoServiceImpl;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;

public class ProxyInvokeDemo {

    static final DemoService impl = new DemoServiceImpl();

    public static void main(String... params) {
        final DemoService localInvoker = (DemoService) Enhancer
                .create(Object.class, new Class[]{DemoService.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return impl.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes())
                                .invoke(impl, args);
                    }
                });
        System.out.println("5 + 2 = " + localInvoker.add(5, 2));
        System.out.println("9 + 3 = " + localInvoker.add(9, 3));
    }
}


