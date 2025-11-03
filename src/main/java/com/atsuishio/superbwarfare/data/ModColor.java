package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

// RGB Color
public class ModColor {
    private int color = 0xFFFFFFFF;

    public ModColor() {
    }

    public ModColor(int color) {
        this.color = 0xFF000000 | color;
    }

    public int get() {
        return 0xFF000000 | this.color;
    }

    static class ModColorAdapter extends TypeAdapter<ModColor> {

        @Override
        public void write(JsonWriter out, ModColor value) throws IOException {
            if (value == null) {
                value = new ModColor();
            }
            
            out.value(value.color);
        }

        private static final Pattern COLOR_PATTERN = Pattern.compile("^(#|0x)?(?<color>[a-f0-9]{6,})$");

        @Override
        public ModColor read(JsonReader in) throws IOException {
            var p = in.peek();
            var obj = new ModColor();

            if (p == JsonToken.STRING) {
                var str = in.nextString().trim().toLowerCase(Locale.ROOT);
                var matcher = COLOR_PATTERN.matcher(str);

                if (matcher.matches()) {
                    var colorStr = matcher.group("color");
                    obj.color = 0xFF000000 | Integer.parseInt(colorStr.substring(colorStr.length() - 6), 16);
                } else {
                    Mod.LOGGER.warn("invalid color string: {}", str);
                }
            } else if (p == JsonToken.NUMBER) {
                obj.color = 0xFF000000 | in.nextInt();
            } else {
                Mod.LOGGER.warn("invalid color token: {}", p);
            }

            return obj;
        }
    }
}
