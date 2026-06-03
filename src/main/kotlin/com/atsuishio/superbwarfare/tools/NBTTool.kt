package com.atsuishio.superbwarfare.tools

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.function.Consumer

object NBTTool {

    @JvmStatic
    fun getTag(stack: ItemStack): CompoundTag {
        val data = stack.get(DataComponents.CUSTOM_DATA)
        if (data != null) return data.copyTag()

        return CompoundTag()
    }

    /**
     * 警告：请勿使用该方法保存任何枪械NBT数据！请统一使用GunData.save()保存枪械数据
     */
    @JvmStatic
    fun saveTag(stack: ItemStack, tag: CompoundTag) {
        val data = stack.get(DataComponents.CUSTOM_DATA)
        val oldTag = if (data != null) data.copyTag() else CompoundTag()
        val newTag = oldTag.merge(tag)
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(newTag))
    }

    @JvmStatic
    fun withTag(item: DeferredHolder<Item, out Item>, count: Int, setter: Consumer<CompoundTag>): ItemStack {
        return withTag(ItemStack(item, count), setter)
    }

    @JvmStatic
    fun withTag(item: DeferredHolder<Item, out Item>, setter: Consumer<CompoundTag>): ItemStack {
        return withTag(item, 1, setter)
    }

    @JvmStatic
    fun withTag(stack: ItemStack, setter: Consumer<CompoundTag>): ItemStack {
        val tag = CompoundTag()
        setter.accept(tag)
        saveTag(stack, tag)
        return stack
    }
}