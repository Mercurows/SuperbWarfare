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
public class ListOrObject<T> {
    List<T> list;

    public ListOrObject(List<T> list) {
        this.list = list;
    }

    static class ListOrObjectAdapter<T> extends TypeAdapter<ListOrObject<T>> {

        private final Type elementType;
        private final Gson gson;

        public ListOrObjectAdapter(Type type, Gson gson) {
            this.gson = gson;
            this.elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
        }

        @Override
        public void write(JsonWriter jsonWriter, ListOrObject<T> listOrObject) {
            gson.toJson(listOrObject.list, TypeToken.getParameterized(List.class, elementType).getType(), jsonWriter);
        }

        @Override
        @SuppressWarnings("unchecked")
        public ListOrObject<T> read(JsonReader jsonReader) throws IOException {
            var token = jsonReader.peek();
            if (token != JsonToken.BEGIN_ARRAY) {
                if (token == JsonToken.NULL) {
                    return new ListOrObject<>(List.of());
                }
                return (ListOrObject<T>) new ListOrObject<>(List.of((Object) gson.fromJson(jsonReader, elementType)));
            } else {
                var listType = TypeToken.getParameterized(List.class, elementType).getType();
                return new ListOrObject<>(gson.fromJson(jsonReader, listType));
            }
        }
    }

    static class ListOrObjectFactory implements TypeAdapterFactory {

        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (ListOrObject.class.isAssignableFrom(type.getRawType())) {
                return (TypeAdapter<T>) new ListOrObjectAdapter<T>(type.getType(), gson);
            }
            return null;
        }
    }
}
