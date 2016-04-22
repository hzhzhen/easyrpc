package net.easyrpc.message.io.test;

import net.easyrpc.message.io.core.MessageNode;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:message-io.xml"})
public class SpringContextTest {

    @Autowired
    MessageNode node;

}
