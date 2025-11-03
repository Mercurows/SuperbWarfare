package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;

public class StringOrVec3 {
    public final String string;
    public final Vec3 vec3;

    public StringOrVec3(String string) {
        this.string = string;
        this.vec3 = null;
    }

    public StringOrVec3() {
        this(Vec3.ZERO);
    }

    public StringOrVec3(Vec3 vec3) {
        this.vec3 = vec3;
        this.string = null;
    }

    public boolean isString() {
        return string != null;
    }

    public boolean isVec3() {
        return vec3 != null;
    }

    static class StringOrVec3Adapter extends TypeAdapter<StringOrVec3> {

        @Override
        public void write(JsonWriter out, StringOrVec3 value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            if (value.string != null) {
                out.value(value.string);
            } else {
                out.beginArray();
                assert value.vec3 != null;
                out.value(value.vec3.x);
                out.value(value.vec3.y);
                out.value(value.vec3.z);
                out.endArray();
            }
        }

        @Override
        public StringOrVec3 read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                Mod.LOGGER.warn("null StringOrVec3 value!");
                return new StringOrVec3();
            }

            if (in.peek() == JsonToken.STRING) {
                return new StringOrVec3(in.nextString());
            }

            if (in.peek() == JsonToken.BEGIN_ARRAY) {
                in.beginArray();
                var x = in.nextDouble();
                var y = in.nextDouble();
                var z = in.nextDouble();
                in.endArray();
                return new StringOrVec3(new Vec3(x, y, z));
            }

            Mod.LOGGER.warn("invalid StringOrVec3 value!");
            return new StringOrVec3();
        }
    }
}
