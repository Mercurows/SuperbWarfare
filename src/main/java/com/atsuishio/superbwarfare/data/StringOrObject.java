package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * "" -> {}
 */
public class StringOrObject<T> {
    public T value;

    public StringOrObject(T value) {
        this.value = value;
    }

    static class StringOrObjectAdapter<T> extends TypeAdapter<StringOrObject<T>> {

        private final Type type;
        private final Gson gson;

        public StringOrObjectAdapter(Type type, Gson gson) {
            this.gson = gson;
            this.type = ((ParameterizedType) type).getActualTypeArguments()[0];
        }

        @Override
        public void write(JsonWriter jsonWriter, StringOrObject<T> obj) {
            gson.toJson(obj.value, type, jsonWriter);
        }

        @Override
        @SuppressWarnings("unchecked")
        public StringOrObject<T> read(JsonReader jsonReader) throws IOException {
            var token = jsonReader.peek();
            if (token == JsonToken.NULL) return gson.fromJson("{}", type);

            if (token == JsonToken.BEGIN_OBJECT) {
                T t = gson.fromJson(jsonReader, type);
                return new StringOrObject<>(t);
            }

            if (token == JsonToken.BEGIN_ARRAY) {
                throw new IllegalArgumentException("excepted string or object, but got array");
            }

            var obj = gson.fromJson("{}", type);
            if (obj instanceof FromString fromString) {
                fromString.fromString(gson.fromJson(jsonReader, String.class));
            } else {
                Mod.LOGGER.warn("warning: type {} is not FromString", type);
            }

            return (StringOrObject<T>) new StringOrObject<>(obj);
        }
    }

    static class StringOrObjectAdapterFactory implements TypeAdapterFactory {

        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (StringOrObject.class.isAssignableFrom(type.getRawType()) && type.getType() instanceof ParameterizedType) {
                return (TypeAdapter<T>) new StringOrObjectAdapter<>(type.getType(), gson);
            }
            return null;
        }
    }
}
