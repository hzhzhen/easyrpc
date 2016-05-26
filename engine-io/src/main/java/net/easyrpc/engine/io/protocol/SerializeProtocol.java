package net.easyrpc.engine.io.protocol;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;

/***
 * 异步编/解码协议:
 * Socket 连接为非阻塞,而消息的序列化和非序列化允许为阻塞方法
 * 所以在此采用异步的解析协议
 */
public interface SerializeProtocol {

    void antiSerialize(@NotNull byte[] bytes, AntiSerializeCallback callback);

    void serialize(@NotNull Message message, SerializeCallBack callback);

    void close();

    interface AntiSerializeCallback {
        void onSerialize(Message events) throws IOException;
    }

    interface SerializeCallBack {
        void onAntiSerialize(byte[] bytes);
    }

    class Message implements Serializable {

        public Long id;         //消息序列ID
        public String tag;      //消息tag
        public byte[] data;     //消息主体

        public Message() {
        }

        public Message(Long id, String tag, byte[] data) {
            this.id = id;
            this.tag = tag;
            this.data = data;
        }
    }
}
