package com.atsuishio.superbwarfare.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;

public class Vec3Adapter extends TypeAdapter<Vec3> {

    @Override
    public void write(JsonWriter out, Vec3 value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.beginArray();
        out.value(value.x);
        out.value(value.y);
        out.value(value.z);
        out.endArray();
    }

    @Override
    public Vec3 read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        if (in.peek() != JsonToken.BEGIN_ARRAY) {
            throw new IllegalStateException("invalid Vec3 value");
        }

        in.beginArray();
        var x = in.nextDouble();
        var y = in.nextDouble();
        var z = in.nextDouble();
        in.endArray();
        return new Vec3(x, y, z);
    }
}
