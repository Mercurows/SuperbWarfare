package com.atsuishio.superbwarfare.recipe.vehicle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class VehicleAssemblingRecipeSerializer implements RecipeSerializer<VehicleAssemblingRecipe> {

    // TODO from JSON???

    public static final MapCodec<VehicleAssemblingRecipe> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    VehicleAssemblingIngredient.CODEC.listOf().fieldOf("ingredient").forGetter(VehicleAssemblingRecipe::getInputs),
                    Codec.STRING.fieldOf("category").forGetter(r -> r.getCategory().toString()),
                    VehicleAssemblingResult.CODEC.fieldOf("result").forGetter(VehicleAssemblingRecipe::getResult)
            ).apply(builder, VehicleAssemblingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleAssemblingRecipe> STREAM_CODEC = StreamCodec.composite(
            VehicleAssemblingIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), VehicleAssemblingRecipe::getInputs,
            ByteBufCodecs.STRING_UTF8, i -> i.getCategory().toString(),
            VehicleAssemblingResult.STREAM_CODEC, VehicleAssemblingRecipe::getResult,
            VehicleAssemblingRecipe::new
    );

    @Override
    public @NotNull MapCodec<VehicleAssemblingRecipe> codec() {
        return CODEC;
    }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, VehicleAssemblingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
