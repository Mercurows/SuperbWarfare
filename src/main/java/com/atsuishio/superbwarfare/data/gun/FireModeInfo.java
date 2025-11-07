package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.data.DeserializeFromString;
import com.atsuishio.superbwarfare.data.JsonPropertyModifier;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class FireModeInfo implements DeserializeFromString, GunPropertyModifier {
    @SerializedName("Mode")
    public FireMode mode = FireMode.SEMI;

    @SerializedName("Name")
    public String name = "Semi";

    @SerializedName("Override")
    public JsonObject override = null;

    private final transient JsonPropertyModifier<GunData, DefaultGunData> jsonPropModifier = new JsonPropertyModifier<>();

    @Override
    public DefaultGunData computeProperties(GunData gunData, DefaultGunData rawData) {
        jsonPropModifier.update(override);
        return jsonPropModifier.computeProperties(gunData, rawData);
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
