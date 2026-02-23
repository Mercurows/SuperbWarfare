package com.atsuishio.superbwarfare.recipe.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem.Companion.createInstance
import com.atsuishio.superbwarfare.tools.TagDataParser
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.ForgeRegistries

class VehicleAssemblingResult {
    @JvmField
    @SerializedName("item")
    var itemString: String = ""

    @JvmField
    @SerializedName("entity")
    var entityTypeString: String = ""

    @JvmField
    @SerializedName("count")
    var count: Int = 1

    @JvmField
    @SerializedName("nbt")
    var nbt: JsonObject? = null

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
            val item = ForgeRegistries.ITEMS.getValue(ResourceLocation(itemString))
            if (item == null) {
                Mod.LOGGER.warn("invalid item: {}", itemString)
                this.result = ItemStack.EMPTY
            } else {
                if (nbt != null) {
                    val tag = TagDataParser.parse(nbt)
                    val tmp = CompoundTag()
                    if (tag.contains("ForgeCaps")) {
                        tmp.put("ForgeCaps", tag.get("ForgeCaps"))
                        tag.remove("ForgeCaps")
                    }

                    tmp.put("tag", tag)
                    tmp.putString("id", itemString)
                    tmp.putInt("Count", count)
                    this.result = ItemStack.of(tmp)
                } else {
                    this.result = ItemStack(item, count)
                }
            }
        } else {
            this.result = ItemStack.EMPTY
        }

        return this.result!!
    }
}
