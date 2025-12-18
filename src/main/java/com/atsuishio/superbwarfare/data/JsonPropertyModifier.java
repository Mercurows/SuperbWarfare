package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.JsonObject;

// TODO 取代StringPropModifier
public class JsonPropertyModifier<DATA extends DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA> implements PropertyModifier<DATA, DEFAULT_DATA> {
    private JsonObject obj;
    private String str;

    public void update(JsonObject object) {
        this.obj = object;
        this.str = null;
    }

    public void update(String string) {
        if (string == null || string.isEmpty() || string.equals(this.str)) return;
        this.str = string;

        try {
            update(DataLoader.GSON.fromJson(string, JsonObject.class));
        } catch (Exception exception) {
            Mod.LOGGER.error("Failed to parse string prop modifier: {}", string, exception);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public DEFAULT_DATA computeProperties(DATA data, DEFAULT_DATA rawData) {
        if (obj == null || obj.size() == 0) return rawData;

        var dataJson = DataLoader.GSON.toJsonTree(rawData).getAsJsonObject();
        for (var entry : obj.entrySet()) {
            dataJson.add(entry.getKey(), entry.getValue());
        }

        return (DEFAULT_DATA) DataLoader.GSON.fromJson(dataJson, rawData.getClass());
    }
}
