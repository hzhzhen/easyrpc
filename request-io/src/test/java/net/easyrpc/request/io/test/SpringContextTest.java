package net.easyrpc.request.io.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class SpringContextTest {

    public static void main(String... args) throws InterruptedException {
        ApplicationContext context = new FileSystemXmlApplicationContext("classpath:request-io.xml");
        Node node = context.getBean(Node.class);

        Thread.sleep(1000);
        System.out.print("");
    }

}
