package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.data.DeserializeFromString;
import com.atsuishio.superbwarfare.data.JsonPropertyModifier;
import com.atsuishio.superbwarfare.data.PMC;
import com.atsuishio.superbwarfare.data.PropertyModifier1;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public class FireModeInfo implements DeserializeFromString, GunPropertyModifier, PropertyModifier1<GunData, DefaultGunData> {
    @SerializedName("Mode")
    public FireMode mode = FireMode.SEMI;

    @SerializedName("Name")
    public String name = "Semi";

    @SerializedName("Override")
    public JsonObject override = null;

    private final transient JsonPropertyModifier<GunData, DefaultGunData> jsonPropModifier = new JsonPropertyModifier<>();

    @Override
    public DefaultGunData computeProperties(@NotNull GunData gunData, DefaultGunData rawData) {
        jsonPropModifier.update(override);
        return jsonPropModifier.computeProperties(gunData, rawData);
    }

    @Override
    public void modifyProperty(@NotNull PMC<@NotNull GunData, @NotNull DefaultGunData> modifier) {
        // TODO Json PropertyModifier
    }

    public void init() {
    }

    @Override
    public void deserializeFromString(String str) {
        init();

        this.mode = FireMode.tryParse(str);
        this.name = str;
    }
}
