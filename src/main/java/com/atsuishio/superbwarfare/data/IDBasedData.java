package com.atsuishio.superbwarfare.data;

import kotlinx.serialization.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * 数据包默认数据（gun / vehicle / attachment ...）的公共接口。
 *
 * <p>{@link #toJson()} / {@link #fromJson} / {@link #copy()} 全部走 kotlinx.serialization，
 * 实现见 {@link DataCodec}：本接口是 Java 接口且存在 Java 实现类，Kotlin 接口默认方法无法被它们继承。
 */
public interface IDBasedData<T extends IDBasedData<T>> extends Serializable {
    @NotNull String getId();

    void setId(@NotNull String id);

    default JsonObject toJson() {
        return DataCodec.toJsonObject(this);
    }

    @SuppressWarnings("unchecked")
    default T fromJson(JsonObject json) {
        return (T) DataCodec.fromJsonObject(this, json);
    }

    default T copy() {
        return fromJson(toJson());
    }

    default void limit() {
    }
}
