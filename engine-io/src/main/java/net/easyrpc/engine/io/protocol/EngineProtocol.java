package net.easyrpc.engine.io.protocol;

import net.easyrpc.engine.io.model.Message;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/***
 * 异步编/解码协议:
 * Socket 连接为非阻塞,而消息的序列化和非序列化允许为阻塞方法
 * 所以在此采用异步的解析协议
 */
public interface EngineProtocol {

    void antiSerialize(@NotNull byte[] bytes, AntiSerializeCallback callback);

    void serialize(@NotNull Message message, SerializeCallBack callback);

    void close();

    interface AntiSerializeCallback {
        void onSerialize(Message events) throws IOException;
    }

    interface SerializeCallBack {
        void onAntiSerialize(byte[] bytes);
    }

}
