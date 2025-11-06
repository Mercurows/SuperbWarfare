package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

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
        public String toString() {
            return power + " " + motion + " " + equals;
        }
    }

    public static class LimitAdapter extends TypeAdapter<Limit> {

        private static final Pattern PATTERN = Pattern.compile("^(?<power>\\d+\\.\\d+|\\d+\\.|\\.\\d+|\\d+)\\s+(?<motion>\\d+\\.\\d+|\\d+\\.|\\.\\d+|\\d+)\\s+(?<equals>true|false)$", Pattern.CASE_INSENSITIVE);

        @Override
        public void write(JsonWriter out, Limit value) throws IOException {
            if (value == null) {
                value = new Limit(0, 0, false);
            }

            out.value(value.toString());
        }

        @Override
        public Limit read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            if (in.peek() != JsonToken.STRING) {
                throw new IllegalStateException("excepted PowerLimit to be String but was " + in.peek());
            }

            var str = in.nextString().trim();
            var matcher = PATTERN.matcher(str);

            if (!matcher.matches()) {
                Mod.LOGGER.error("invalid PowerLimit {}!", str);
                return null;
            }

            var power = matcher.group("power");
            if (power == null) {
                Mod.LOGGER.error("invalid value for PowerLimit {}!", str);
                return null;
            }

            var motion = matcher.group("motion");
            if (motion == null) {
                try {
                    return new Limit(Float.parseFloat(power), 0, false);
                } catch (NumberFormatException e) {
                    Mod.LOGGER.error("invalid value for PowerLimit {}!", str);
                    return new Limit(0, 0, false);
                }
            }

            var equals = matcher.group("equals");
            if (equals == null) {
                try {
                    return new Limit(Float.parseFloat(power), Float.parseFloat(motion), false);
                } catch (NumberFormatException e) {
                    Mod.LOGGER.error("invalid value for PowerLimit {}!", str);
                    return new Limit(0, 0, false);
                }
            }

            try {
                return new Limit(Float.parseFloat(power), Float.parseFloat(motion), Boolean.parseBoolean(equals));
            } catch (NumberFormatException e) {
                Mod.LOGGER.error("invalid value for PowerLimit {}!", str);
                return new Limit(0, 0, false);
            }
        }
    }
}
