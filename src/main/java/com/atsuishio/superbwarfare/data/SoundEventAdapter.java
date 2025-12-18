package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.io.IOException;
import java.util.regex.Pattern;

public class SoundEventAdapter extends TypeAdapter<SoundEvent> {

    @Override
    public void write(JsonWriter out, SoundEvent value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.value(value.getLocation().toString());
    }

    private static final Pattern PATTERN = Pattern.compile("^(?<location>\\S+)( (?<range>-?\\d*(\\.(?=\\d))?\\d*))?$", Pattern.CASE_INSENSITIVE);

    @Override
    public SoundEvent read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        if (in.peek() != JsonToken.STRING) {
            throw new IllegalStateException("excepted SoundEvent to be String but was " + in.peek());
        }

        var str = in.nextString().trim();
        var matcher = PATTERN.matcher(str);

        if (!matcher.matches()) {
            Mod.LOGGER.error("invalid SoundEvent {}!", str);
            return null;
        }

        var locationGroup = matcher.group("location");
        if (!locationGroup.contains(":")) {
            locationGroup = "minecraft:" + locationGroup;
        }

        var location = ResourceLocation.tryParse(locationGroup);
        if (location == null) {
            Mod.LOGGER.error("invalid resource location for SoundEvent {}!", str);
            return null;
        }

        var rangeGroup = matcher.group("range");
        if (rangeGroup != null) {
            float range;
            try {
                range = Float.parseFloat(rangeGroup);
            } catch (Exception exception) {
                Mod.LOGGER.error("invalid range for SoundEvent {}!", str);
                return null;
            }
            return SoundEvent.createFixedRangeEvent(location, range);
        }

        return SoundEvent.createVariableRangeEvent(location);
    }
}
