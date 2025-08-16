package com.atsuishio.superbwarfare.recipe.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.DeserializeFromString;
import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.regex.Pattern;

public class VehicleAssemblingIngredient implements DeserializeFromString {
    @SerializedName("ingredient")
    String ingredientString = "";
    @SerializedName("count")
    int count = 1;

    public static final Codec<VehicleAssemblingIngredient> CODEC = RecordCodecBuilder.<VehicleAssemblingIngredient>mapCodec(builder ->
            builder.group(
                    Codec.STRING.fieldOf("ingredient").forGetter(i -> i.ingredientString),
                    Codec.INT.fieldOf("count").forGetter(i -> i.count)
            ).apply(builder, VehicleAssemblingIngredient::new)).codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleAssemblingIngredient> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, i -> i.ingredientString,
            ByteBufCodecs.VAR_INT, i -> i.count,
            VehicleAssemblingIngredient::new
    );

    public VehicleAssemblingIngredient() {
    }

    public VehicleAssemblingIngredient(String ingredientString, int count) {
        this.ingredientString = ingredientString;
        this.count = count;
    }

    public transient Ingredient ingredientObject;

    private static final Pattern INGREDIENT_PATTERN = Pattern.compile("^(?<count>(\\d+)?)\\s*(x\\s*)?(?<prefix>#?)(?<id>\\w+:\\S+)$");

    public Ingredient getIngredient() {
        if (ingredientObject == null) {
            deserializeFromString(ingredientString);
        }
        return ingredientObject;
    }

    public int getCount() {
        return count;
    }

    @Override
    public void deserializeFromString(String str) {
        var matcher = INGREDIENT_PATTERN.matcher(str);
        if (!matcher.matches()) {
            Mod.LOGGER.warn("invalid vehicle assembling ingredient: {}", str);
            ingredientObject = Ingredient.EMPTY;
            return;
        }

        var countString = matcher.group("count");
        if (!countString.isEmpty()) {
            count = Math.max(1, Integer.parseInt(countString));
        }

        var id = matcher.group("id");
        if (matcher.group("prefix").equals("#")) {
            ingredientObject = Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(id)));
        } else {
            ingredientObject = Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
        }
    }
}
