package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.data.stack.ItemStackStorage
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.function.Consumer

object NBTTool {

    @JvmStatic
    fun getTag(stack: ItemStack): CompoundTag = ItemStackStorage.rootTagOrNull(stack) ?: CompoundTag()

    /**
     * 警告：请勿使用该方法保存任何枪械NBT数据！请统一使用GunData.save()保存枪械数据
     */
    @JvmStatic
    fun saveTag(stack: ItemStack, tag: CompoundTag) {
        // 旧语义是 `oldTag.merge(tag)`（只增不删），所以先在外面合好再整体写回
        val merged = (ItemStackStorage.rootTagOrNull(stack) ?: CompoundTag()).copy()
        merged.merge(tag)
        ItemStackStorage.writeRoot(stack, merged)
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
