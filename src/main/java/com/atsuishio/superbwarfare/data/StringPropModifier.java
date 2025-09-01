package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class StringPropModifier<DATA extends DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA> implements PropertyModifier<DATA, DEFAULT_DATA> {
    private final Map<Prop<DATA, DEFAULT_DATA, ?>, Prop.PropModifyContext<DATA,DEFAULT_DATA, ?>> modifiers = new HashMap<>();
    private Pair<String, JsonObject> propertyOverrideCache = new Pair<>("", null);
    private boolean isOverrideValid = true;

    private static final Gson GSON = DataLoader.GSON;

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Map<Prop<DATA, DEFAULT_DATA, ?>, Prop.PropModifyContext<DATA, DEFAULT_DATA, ?>> getPropModifiers() {
        return this.modifiers;
    }

    @SuppressWarnings("unchecked")
    public <T> void modifyPropertyByString(@Nullable String string, Prop<DATA, DEFAULT_DATA, T> prop) {
        if (string != null && !string.isEmpty()) {
            if (!propertyOverrideCache.getFirst().equals(string)) {
                modifiers.clear();

                try {
                    propertyOverrideCache = new Pair<>(string, GSON.fromJson(string, JsonObject.class));
                    isOverrideValid = true;
                } catch (Exception exception) {
                    Mod.LOGGER.error("invalid property override string {}", string);
                    propertyOverrideCache = new Pair<>(string, new JsonObject());
                    isOverrideValid = false;
                }
            }

            var propJson = propertyOverrideCache.getSecond();
            if (propJson != null && propJson.has(prop.name) && isOverrideValid && !modifiers.containsKey(prop)) {
                try {
                    var parsedValue = DataLoader.processValue(GSON.fromJson(propJson.get(prop.name).toString(), prop.getFieldType()));
                    setProperty(prop, (data, value) -> (T) parsedValue);
                } catch (Exception exception) {
                    Mod.LOGGER.error("invalid property override type for prop {}: {}", prop.name, propJson.get(prop.name).toString());
                    isOverrideValid = false;
                }
            }
        } else {
            modifiers.clear();
        }
    }
}
