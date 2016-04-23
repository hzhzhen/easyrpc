package net.easyrpc.message.io.test;

import net.easyrpc.message.io.core.MessageNode;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class SpringContextTest {

    public static void main(String... args) {
        ApplicationContext context = new FileSystemXmlApplicationContext("classpath:message-io.xml");
        MessageNode node = context.getBean(MessageNode.class);
    }

}
