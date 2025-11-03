package com.atsuishio.superbwarfare.data;

import com.google.gson.JsonObject;

import java.io.Serializable;

public interface IDBasedData<T extends IDBasedData<T>> extends Serializable {
    String getId();

    default JsonObject toJson() {
        var cacheObj = DataLoader.JSON_OBJECT_CACHE.get(this);
        if (cacheObj != null) {
            return cacheObj;
        }

        var obj = DataLoader.GSON.toJsonTree(this).getAsJsonObject();
        DataLoader.JSON_OBJECT_CACHE.put(this, obj);
        return obj;
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
