package net.easyrpc.message.io.test;

import net.easyrpc.message.io.annotation.OnEvent;

public class EventController {

    @OnEvent("message")
    public void print(String message) {
        System.out.println(message);
    }

    @OnEvent(value = "content", type = Test.class)
    public void test(Test test) {
        System.out.println(test.content);
    }

    public static class Test {
        public String content;
    }

}
