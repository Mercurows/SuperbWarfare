package com.atsuishio.superbwarfare.recipe.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem.Companion.createInstance
import com.atsuishio.superbwarfare.tools.TagDataParser
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack

class VehicleAssemblingResult {
    @SerializedName("item")
    @JvmField
    var itemString: String = ""

    @SerializedName("entity")
    @JvmField
    var entityTypeString: String = ""

    @SerializedName("count")
    @JvmField
    var count: Int = 1

    @SerializedName("nbt")
    @JvmField
    var nbt: JsonObject? = null

    constructor()

    constructor(itemString: String, entityTypeString: String, count: Int) {
        this.itemString = itemString
        this.entityTypeString = entityTypeString
        this.count = count
    }

    @Transient
    @get:JvmName("result")
    var result: ItemStack? = null

    fun getResult(): ItemStack {
        if (this.result != null) return this.result!!

        if (!entityTypeString.isEmpty()) {
            val type = EntityType.byString(entityTypeString).orElse(null)
            if (type == null) {
                Mod.LOGGER.warn("invalid entity type: {}", entityTypeString)
                this.result = ItemStack.EMPTY
            } else {
                this.result = createInstance(type).copyWithCount(count)
            }
        } else if (!itemString.isEmpty()) {
            val location = ResourceLocation.parse(itemString)
            val item = BuiltInRegistries.ITEM.get(location)
            if (nbt != null) {
                val tag = TagDataParser.parse(nbt)
                tag.putString("id", location.toString())
                tag.putInt("count", 1)
                ItemStack.parse(RegistryAccess.EMPTY, tag).ifPresent { this.result = it }
            } else {
                this.result = ItemStack(item, count)
            }
        } else {
            this.result = ItemStack.EMPTY
        }

        return this.result!!
    }

    companion object {
        val CODEC: Codec<VehicleAssemblingResult> =
            RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<VehicleAssemblingResult> ->
                builder.group(
                    Codec.STRING.optionalFieldOf(
                        "item",
                        BuiltInRegistries.ITEM.getKey(ModItems.CONTAINER.value()).toString()
                    ).forGetter { it.itemString },
                    Codec.STRING.optionalFieldOf("entity", "").forGetter { it.entityTypeString },
                    Codec.INT.optionalFieldOf("count", 1).forGetter { it.count }
                ).apply(builder, ::VehicleAssemblingResult)
            }.codec()

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, VehicleAssemblingResult> =
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, { it.itemString },
                ByteBufCodecs.STRING_UTF8, { it.entityTypeString },
                ByteBufCodecs.VAR_INT, { it.count },
                ::VehicleAssemblingResult
            )
    }
}
