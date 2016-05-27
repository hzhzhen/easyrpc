package net.easyrpc.engine.io.protocol;

import java.io.IOException;
import java.io.Serializable;

/***
 * 异步编/解码协议:
 * Socket 连接为非阻塞,而消息的序列化和非序列化允许为阻塞方法
 * 所以在此采用异步的解析协议
 */
public interface EngineProtocol {

    void antiSerialize(byte[] bytes, AntiSerializeCallback callback);

    void serialize(Message message, SerializeCallBack callback);

    /***
     * 因为是异步解析, 在主进程关闭时需要提供解析停止的接口
     */
    void close();

    /***
     * 反序列化回调
     */
    interface AntiSerializeCallback {
        void onSerialize(Message events) throws IOException;
    }

    /***
     * 序列化回调
     */
    interface SerializeCallBack {
        void onAntiSerialize(byte[] bytes);
    }

    /***
     * 消息模型
     */
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
