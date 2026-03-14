package com.atsuishio.superbwarfare.datagen.builder

import com.google.common.base.Preconditions
import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemDisplayContext
import net.minecraftforge.client.model.generators.CustomLoaderBuilder
import net.minecraftforge.client.model.generators.ModelBuilder
import net.minecraftforge.common.data.ExistingFileHelper

open class CustomSeparateModelBuilder<T : ModelBuilder<T>> protected constructor(
    parent: T,
    existingFileHelper: ExistingFileHelper
) : CustomLoaderBuilder<T>(
    ResourceLocation("forge:separate_transforms"), parent, existingFileHelper
) {
    private var base: String? = null
    private val childModels: MutableMap<String, String> = linkedMapOf()
    private val texture: MutableMap<String, ResourceLocation> = linkedMapOf()

    fun base(location: String): CustomSeparateModelBuilder<T> {
        Preconditions.checkNotNull<String>(location, "location must not be null")
        base = location
        return this
    }

    fun perspective(perspective: ItemDisplayContext, location: String): CustomSeparateModelBuilder<T> {
        Preconditions.checkNotNull<ItemDisplayContext>(perspective, "perspective must not be null")
        Preconditions.checkNotNull<String>(location, "location must not be null")
        childModels[perspective.serializedName] = location
        return this
    }

    fun texture(name: String, location: ResourceLocation): CustomSeparateModelBuilder<T> {
        Preconditions.checkNotNull<String>(name, "name must not be null")
        Preconditions.checkNotNull<ResourceLocation>(location, "location must not be null")
        texture[name] = location
        return this
    }

    override fun toJson(json: JsonObject): JsonObject {
        var json = json
        json = super.toJson(json)

        if (this.base != null) {
            val base = JsonObject()
            base.addProperty("parent", this.base)
            json.add("base", base)
        }

        val parts = JsonObject()
        for (entry in childModels.entries) {
            val part = JsonObject()
            part.addProperty("parent", entry.value)
            parts.add(entry.key, part)
        }
        json.add("perspectives", parts)

        val textures = JsonObject()
        for (entry in texture.entries) {
            textures.addProperty(entry.key, entry.value.toString())
        }
        json.add("textures", textures)

        return json
    }

    companion object {
        fun <T : ModelBuilder<T>> begin(
            parent: T,
            existingFileHelper: ExistingFileHelper
        ): CustomSeparateModelBuilder<T> {
            return CustomSeparateModelBuilder(parent, existingFileHelper)
        }
    }
}
