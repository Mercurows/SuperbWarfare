package com.atsuishio.superbwarfare.recipe.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.DeserializeFromString
import com.google.gson.annotations.SerializedName
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.crafting.Ingredient
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.max

class VehicleAssemblingIngredient : DeserializeFromString {
    @SerializedName("ingredient")
    var ingredientString: String = ""

    @SerializedName("count")
    @JvmField
    var count: Int = 1

    constructor()

    constructor(ingredientString: String, count: Int) {
        this.ingredientString = ingredientString
        this.count = count
    }

    @Transient
    var ingredientObject: Ingredient? = null

    val ingredient: Ingredient
        get() {
            if (ingredientObject == null) {
                deserializeFromString(ingredientString)
            }
            return ingredientObject!!
        }

    override fun deserializeFromString(str: String) {
        this.ingredientString = str
        val matcher: Matcher = INGREDIENT_PATTERN.matcher(str)
        if (!matcher.matches()) {
            Mod.LOGGER.warn("invalid vehicle assembling ingredient: {}", str)
            ingredientObject = Ingredient.EMPTY
            return
        }

        val countString = matcher.group("count")
        if (!countString.isEmpty()) {
            count = max(1, countString.toInt())
        }

        val id = matcher.group("id")
        ingredientObject = if (matcher.group("prefix") == "#") {
            Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(id)))
        } else {
            Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)))
        }
    }

    companion object {
        val CODEC: Codec<VehicleAssemblingIngredient> =
            RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<VehicleAssemblingIngredient> ->
                builder.group(
                    Codec.STRING.fieldOf("ingredient").forGetter { it.ingredientString },
                    Codec.INT.fieldOf("count").forGetter { it.count }
                ).apply(
                    builder,
                    ::VehicleAssemblingIngredient
                )
            }.codec()

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, VehicleAssemblingIngredient> =
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, { it.ingredientString },
                ByteBufCodecs.VAR_INT, { it.count },
                ::VehicleAssemblingIngredient
            )

        private val INGREDIENT_PATTERN: Pattern =
            Pattern.compile("^(?<count>(\\d+)?)\\s*(x\\s*)?(?<prefix>#?)(?<id>\\w+:\\S+)$")
    }
}
