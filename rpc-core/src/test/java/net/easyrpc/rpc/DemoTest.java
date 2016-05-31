package net.easyrpc.rpc;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;

public class DemoTest {

    static final DemoInterface impl = new DemoImpl();

    public static void main(String... params) {
        final DemoInterface localInvoker = (DemoInterface) Enhancer.create(Object.class, new Class[]{DemoInterface.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return impl.getClass()
                                .getDeclaredMethod(method.getName(), method.getParameterTypes())
                                .invoke(impl, args);
                    }
                });
        System.out.println("5 + 2 = " + localInvoker.add(5, 2));
        System.out.println("9 + 3 = " + localInvoker.add(9, 3));
    }

    public interface DemoInterface {
        int add(int a, int b);
    }

    public static class DemoImpl implements DemoInterface {
        public int add(int a, int b) {
            return a + b;
        }
    }
}


