package com.atsuishio.superbwarfare.data;

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
public class StringToObject<T extends DeserializeFromString> {
    public T value;

    public StringToObject(T value) {
        this.value = value;
    }

    static class StringOrObjectAdapter<T extends DeserializeFromString> extends TypeAdapter<StringToObject<T>> {

        /**
         * Type of T
         */
        private final Type type;
        private final Gson gson;

        public StringOrObjectAdapter(Type type, Gson gson) {
            this.gson = gson;
            this.type = ((ParameterizedType) type).getActualTypeArguments()[0];
        }

        @Override
        public void write(JsonWriter jsonWriter, StringToObject<T> obj) {
            gson.toJson(obj.value, type, jsonWriter);
        }

        @Override
        public StringToObject<T> read(JsonReader jsonReader) throws IOException {
            var token = jsonReader.peek();
            if (token == JsonToken.NULL) return gson.fromJson("{}", type);

            if (token == JsonToken.BEGIN_OBJECT || token == JsonToken.BEGIN_ARRAY) {
                return new StringToObject<>(gson.fromJson(jsonReader, type));
            }

            var obj = gson.<T>fromJson("{}", type);
            obj.deserializeFromString(gson.fromJson(jsonReader, String.class));

            return new StringToObject<>(obj);
        }
    }

    static class AdapterFactory implements TypeAdapterFactory {

        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (StringToObject.class.isAssignableFrom(type.getRawType()) && type.getType() instanceof ParameterizedType) {
                return (TypeAdapter<T>) new StringOrObjectAdapter<>(type.getType(), gson);
            }
            return null;
        }
    }
}
