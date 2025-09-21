package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.DataLoader;
import com.atsuishio.superbwarfare.data.DeserializeFromString;
import com.atsuishio.superbwarfare.data.Prop;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class FireModeInfo implements DeserializeFromString, GunPropertyModifier {
    @SerializedName("Mode")
    public FireMode mode = FireMode.SEMI;

    @SerializedName("Name")
    public String name = "Semi";

    @SerializedName("Override")
    public JsonObject override = null;

    private transient final Map<GunProp<?>, Prop.PropModifyContext<GunData, DefaultGunData, ?>> modifiers = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Map<GunProp<?>, Prop.PropModifyContext<GunData, DefaultGunData, ?>> getPropModifiers() {
        return this.modifiers;
    }

    private transient boolean init = false;

    @SuppressWarnings("unchecked")
    private void parseOverrideValues() {
        if (override != null && !init) {
            for (var element : override.entrySet()) {
                var key = element.getKey();
                var prop = GunProp.getByName(key);
                if (prop == null) {
                    Mod.LOGGER.warn("invalid override key: {}", key);
                    continue;
                }

                try {
                    var parsedValue = DataLoader.GSON.fromJson(element.getValue().toString(), prop.getFieldType());
                    this.setProperty((GunProp<Object>) prop, value -> parsedValue);
                } catch (Exception exception) {
                    Mod.LOGGER.error("invalid override value for key {}: {}", key, element.getValue());
                }
            }
            init = true;
        }
    }

    public void init() {
        parseOverrideValues();
    }

    @Override
    public void deserializeFromString(String str) {
        init();

        this.mode = FireMode.tryParse(str);
        this.name = str;
    }
}
