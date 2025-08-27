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
import java.util.List;

/**
 * {} -> [{}]
 */
public class ObjectToList<T> {
    public List<T> list;

    public ObjectToList(List<T> list) {
        this.list = list;
    }

    @SafeVarargs
    public ObjectToList(T... objects) {
        this.list = List.of(objects);
    }

    static class ListOrObjectAdapter<T> extends TypeAdapter<ObjectToList<T>> {

        /**
         * Type of T
         */
        private final Type type;
        private final Gson gson;

        public ListOrObjectAdapter(Type type, Gson gson) {
            this.gson = gson;
            this.type = ((ParameterizedType) type).getActualTypeArguments()[0];
        }

        @Override
        public void write(JsonWriter jsonWriter, ObjectToList<T> objectToList) throws IOException {
            if (objectToList == null || objectToList.list == null) {
                jsonWriter.beginArray().endArray();
                return;
            }

            if (objectToList.list.size() == 1) {
                gson.toJson(objectToList.list.get(0), type, jsonWriter);
            } else {
                gson.toJson(objectToList.list, TypeToken.getParameterized(List.class, type).getType(), jsonWriter);
            }
        }

        @Override
        public ObjectToList<T> read(JsonReader jsonReader) throws IOException {
            var token = jsonReader.peek();
            if (token != JsonToken.BEGIN_ARRAY) {
                // 单元素
                if (token == JsonToken.NULL) {
                    return new ObjectToList<>();
                }
                return new ObjectToList<>(gson.<T>fromJson(jsonReader, type));
            } else {
                // 数组
                var listType = TypeToken.getParameterized(List.class, type).getType();
                return new ObjectToList<>(gson.<List<T>>fromJson(jsonReader, listType));
            }
        }
    }

    static class AdapterFactory implements TypeAdapterFactory {

        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (ObjectToList.class.isAssignableFrom(type.getRawType())) {
                return (TypeAdapter<T>) new ListOrObjectAdapter<T>(type.getType(), gson);
            }
            return null;
        }
    }
}
