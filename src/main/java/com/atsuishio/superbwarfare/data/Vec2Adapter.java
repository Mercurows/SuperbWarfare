package com.atsuishio.superbwarfare.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.world.phys.Vec2;

import java.io.IOException;

public class Vec2Adapter extends TypeAdapter<Vec2> {

    @Override
    public void write(JsonWriter out, Vec2 value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.beginArray();
        out.value(value.x);
        out.value(value.y);
        out.endArray();
    }

    @Override
    public Vec2 read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        if (in.peek() != JsonToken.BEGIN_ARRAY) {
            throw new IllegalStateException("invalid Vec2 value");
        }

        in.beginArray();
        var x = in.nextDouble();
        var y = in.nextDouble();
        in.endArray();
        return new Vec2((float) x, (float) y);
    }
}
