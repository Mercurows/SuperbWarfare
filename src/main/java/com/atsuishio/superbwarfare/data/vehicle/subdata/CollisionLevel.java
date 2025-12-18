package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class CollisionLevel {

    /**
     * 碰撞等级，范围是0~4
     * 0 - 无法撞坏方块
     * 1 - 允许撞坏软方块
     * 2 - 允许撞坏普通方块
     * 3 - 允许撞坏硬方块
     * 4 - 允许野兽撞击模式
     */
    @SerializedName("Level")
    public int level = 2;

    @SerializedName("PowerLimits")
    public List<Limit> powerLimits = List.of();

    public record Limit(float power, float motion, boolean equals) {

        @Override
        public @NotNull String toString() {
            return "[" + power + ", " + motion + ", " + equals + "]";
        }
    }

    public static class LimitAdapter extends TypeAdapter<Limit> {

        @Override
        public void write(JsonWriter out, Limit value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            out.beginArray();
            out.value(value.power);
            out.value(value.motion);
            out.value(value.equals);
            out.endArray();
        }

        @Override
        public Limit read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            in.beginArray();
            var obj = new CollisionLevel.Limit((float) in.nextDouble(), (float) in.nextDouble(), in.nextBoolean());
            in.endArray();

            return obj;
        }
    }
}
