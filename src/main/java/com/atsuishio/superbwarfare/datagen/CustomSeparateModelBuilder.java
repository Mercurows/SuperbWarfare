package com.atsuishio.superbwarfare.datagen;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.generators.CustomLoaderBuilder;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.LinkedHashMap;
import java.util.Map;

public class CustomSeparateModelBuilder<T extends ModelBuilder<T>> extends CustomLoaderBuilder<T> {

    public static <T extends ModelBuilder<T>> CustomSeparateModelBuilder<T> begin(T parent, ExistingFileHelper existingFileHelper) {
        return new CustomSeparateModelBuilder<>(parent, existingFileHelper);
    }

    private String base;
    private final Map<String, String> childModels = new LinkedHashMap<>();
    private final Map<String, ResourceLocation> texture = new LinkedHashMap<>();

    protected CustomSeparateModelBuilder(T parent, ExistingFileHelper existingFileHelper) {
        super(new ResourceLocation("forge:separate_transforms"), parent, existingFileHelper);
    }

    public CustomSeparateModelBuilder<T> base(String location) {
        Preconditions.checkNotNull(location, "location must not be null");
        base = location;
        return this;
    }

    public CustomSeparateModelBuilder<T> perspective(ItemDisplayContext perspective, String location) {
        Preconditions.checkNotNull(perspective, "perspective must not be null");
        Preconditions.checkNotNull(location, "location must not be null");
        childModels.put(perspective.getSerializedName(), location);
        return this;
    }

    public CustomSeparateModelBuilder<T> texture(String name, ResourceLocation location) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(location, "location must not be null");
        texture.put(name, location);
        return this;
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        json = super.toJson(json);

        if (this.base != null) {
            var base = new JsonObject();
            base.addProperty("parent", this.base);
            json.add("base", base);
        }

        JsonObject parts = new JsonObject();
        for (Map.Entry<String, String> entry : childModels.entrySet()) {
            var part = new JsonObject();
            part.addProperty("parent", entry.getValue());
            parts.add(entry.getKey(), part);
        }
        json.add("perspectives", parts);

        JsonObject textures = new JsonObject();
        for (Map.Entry<String, ResourceLocation> entry : texture.entrySet()) {
            textures.addProperty(entry.getKey(), entry.getValue().toString());
        }
        json.add("textures", textures);

        return json;
    }
}
