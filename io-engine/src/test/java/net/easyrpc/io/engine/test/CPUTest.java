package net.easyrpc.io.engine.test;

import net.easyrpc.io.engine.Engine;

import java.io.IOException;

/**
 * @author chpengzh
 */
public class CPUTest {

    public static void main(String... args) throws IOException, InterruptedException {
        Engine.server().listen(8090);
    }
}
