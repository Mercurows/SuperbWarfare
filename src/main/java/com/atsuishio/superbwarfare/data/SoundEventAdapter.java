package com.atsuishio.superbwarfare.data;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.io.IOException;

public class SoundEventAdapter extends TypeAdapter<SoundEvent> {

    @Override
    public void write(JsonWriter out, SoundEvent value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.value(value.getLocation().toString());
    }

    @Override
    public SoundEvent read(JsonReader in) throws IOException {
        if (in.peek() != JsonToken.STRING) return null;

        var location = ResourceLocation.tryParse(in.nextString());
        if (location == null) return null;

        return BuiltInRegistries.SOUND_EVENT.get(location);
    }
}
