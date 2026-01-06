package com.atsuishio.superbwarfare.data.gun.value

import com.atsuishio.superbwarfare.data.gun.value.base.TagValue
import com.atsuishio.superbwarfare.tools.sameWith
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

class ItemStackValue(
    private val tag: CompoundTag,
    private val name: String,
    defaultValue: ItemStack = ItemStack.EMPTY
) : TagValue<ItemStack> {

    override val defaultValue: ItemStack = defaultValue.copy()

    private var cache: ItemStack

    init {
        this.cache = defaultValue.copy()
    }

    override fun get(): ItemStack {
        if (!this.cache.isEmpty) {
            return this.cache
        }

        if (tag.contains(name)) {
            return ItemStack.of(tag.getCompound(name))
        }
        return defaultValue
    }

    override fun set(value: ItemStack) {
        if (value sameWith defaultValue) {
            tag.remove(name)
        } else {
            tag.put(name, value.save(CompoundTag()))
        }

        this.cache = value.copy()
    }
}
