package com.atsuishio.superbwarfare.data;

import com.google.gson.JsonObject;

import java.io.Serializable;

public interface IDBasedData<T extends IDBasedData<T>> extends Serializable {
    String getId();

    void setId(String id);

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
