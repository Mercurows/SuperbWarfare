package com.atsuishio.superbwarfare.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public class ResourceLocationAdapter extends TypeAdapter<ResourceLocation> {

    @Override
    public void write(JsonWriter out, ResourceLocation value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.value(value.toString());
    }

    @Override
    public ResourceLocation read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        if (in.peek() != JsonToken.STRING) {
            throw new IllegalStateException("excepted ResourceLocation to be String but was " + in.peek());
        }

        return ResourceLocation.tryParse(in.nextString());
    }
}
