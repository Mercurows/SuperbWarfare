package com.atsuishio.superbwarfare.data;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public interface IDBasedData<T extends IDBasedData<T>> extends Serializable {
    @NotNull String getId();

    void setId(@NotNull String id);

    default JsonObject toJson() {
        return DataLoader.JSON_OBJECT_CACHE.getUnchecked(this);
    }

    @SuppressWarnings("unchecked")
    default T fromJson(JsonObject json) {
        return (T) DataLoader.GSON.fromJson(json, getClass());
    }

    default T copy() {
        return fromJson(toJson());
    }

    default void limit() {
    }
}
