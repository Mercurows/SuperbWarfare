package com.atsuishio.superbwarfare.recipe.vehicle;

import com.atsuishio.superbwarfare.data.EnumCodec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class VehicleAssemblingRecipeSerializer implements RecipeSerializer<VehicleAssemblingRecipe> {

    public static final MapCodec<VehicleAssemblingRecipe> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    VehicleAssemblingIngredient.CODEC.listOf().fieldOf("Inputs").forGetter(VehicleAssemblingRecipe::getInputs),
                    EnumCodec.create(VehicleAssemblingRecipe.Category.class).fieldOf("Category").orElse(VehicleAssemblingRecipe.Category.LAND).forGetter(VehicleAssemblingRecipe::getCategory),
                    VehicleAssemblingResult.CODEC.fieldOf("Result").forGetter(VehicleAssemblingRecipe::getResult)
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
