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

// TODO 解决RPM文本显示问题
// TODO 为多种开火模式添加提示
// TODO 添加切换快捷键
public class FireModeInfo implements DeserializeFromString, GunPropertyModifier {
    @SerializedName("Mode")
    public FireMode mode = FireMode.SEMI;

    @SerializedName("Override")
    public JsonObject override = new JsonObject();

    public static final FireModeInfo DEFAULT = new FireModeInfo();

    @Override
    public void deserializeFromString(String str) {
        this.mode = FireMode.fromValue(str);
    }

    private transient boolean init = false;

    @SuppressWarnings("unchecked")
    private void parseOverrideValues() {
        if (override != null) {
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
        }
    }

    public FireModeInfo init() {
        if (!init) {
            parseOverrideValues();
            init = true;
        }
        return this;
    }

    protected final transient Map<GunProp<?>, Prop.PropModifyContext<GunData, ?>> propertyModifiers = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Map<GunProp<?>, Prop.PropModifyContext<GunData, ?>> getPropModifiers() {
        return propertyModifiers;
    }
}
