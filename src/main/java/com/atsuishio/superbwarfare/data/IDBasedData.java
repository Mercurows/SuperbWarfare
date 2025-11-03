package com.atsuishio.superbwarfare.data;

import com.google.gson.JsonObject;

public interface IDBasedData<T extends IDBasedData<T>> {
    String getId();

    default JsonObject toJson() {
        return DataLoader.GSON.toJsonTree(this).getAsJsonObject();
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
