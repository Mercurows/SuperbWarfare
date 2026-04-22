package com.atsuishio.superbwarfare.recipe.vehicle

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.crafting.RecipeSerializer

object VehicleAssemblingRecipeSerializer : RecipeSerializer<VehicleAssemblingRecipe> {
    override fun codec(): MapCodec<VehicleAssemblingRecipe> {
        return CODEC
    }

    override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, VehicleAssemblingRecipe> {
        return STREAM_CODEC
    }

    val CODEC: MapCodec<VehicleAssemblingRecipe> =
        RecordCodecBuilder.mapCodec { builder ->
            builder.group(
                Codec.STRING.listOf().fieldOf("inputs").flatXmap(
                    {
                        DataResult.success(
                            it.stream().map { s ->
                                val ingredient = VehicleAssemblingIngredient()
                                ingredient.deserializeFromString(s)
                                ingredient
                            }.toList()
                        )
                    },
                    {
                        DataResult.success(
                            it.stream().map { ingredient ->
                                if (ingredient.count > 1) {
                                    return@map "${ingredient.count} ${ingredient.ingredientString}"
                                } else {
                                    return@map ingredient.ingredientString
                                }
                            }.toList()
                        )
                    }
                ).forGetter { it.inputs },
                Codec.STRING.flatXmap(
                    { DataResult.success(VehicleAssemblingRecipe.Category.getCategory(it)) },
                    { DataResult.success(it.typeName) }
                ).fieldOf("category").orElse(VehicleAssemblingRecipe.Category.LAND).forGetter { it.category },
                VehicleAssemblingResult.CODEC.fieldOf("result").forGetter { it.result }
            ).apply(builder, ::VehicleAssemblingRecipe)
        }

    val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, VehicleAssemblingRecipe> =
        StreamCodec.composite(
            VehicleAssemblingIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()),
            { it.inputs },
            ByteBufCodecs.STRING_UTF8,
            { it.category.toString() },
            VehicleAssemblingResult.STREAM_CODEC,
            { it.result },
            ::VehicleAssemblingRecipe
        )
}
