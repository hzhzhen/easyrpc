package net.easyrpc.message.io.api;

import net.easyrpc.message.io.model.Event;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface Protocol {

    void antiSerialize(@NotNull byte[] bytes, AntiSerializeCallback callback);

    void serialize(@NotNull Event event, SerializeCallBack callback);

    void close();

    interface AntiSerializeCallback {
        void onSerialize(Event events) throws IOException;
    }

    interface SerializeCallBack {
        void onAntiSerialize(byte[] bytes);
    }
}
