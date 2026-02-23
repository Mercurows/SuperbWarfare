package com.atsuishio.superbwarfare.recipe.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.DeserializeFromString
import com.google.gson.annotations.SerializedName
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.crafting.Ingredient
import net.minecraftforge.registries.ForgeRegistries
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.max

class VehicleAssemblingIngredient : DeserializeFromString {
    @SerializedName("ingredient")
    var ingredientString: String = ""

    @JvmField
    @SerializedName("count")
    var count: Int = 1

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
            Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation(id)))
        } else {
            Ingredient.of(ForgeRegistries.ITEMS.getValue(ResourceLocation(id)))
        }
    }

    companion object {
        private val INGREDIENT_PATTERN: Pattern =
            Pattern.compile("^(?<count>(\\d+)?)\\s*(x\\s*)?(?<prefix>#?)(?<id>\\w+:\\S+)$")
    }
}
