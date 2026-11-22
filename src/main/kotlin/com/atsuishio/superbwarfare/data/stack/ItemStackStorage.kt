package com.atsuishio.superbwarfare.data.stack

import com.atsuishio.superbwarfare.init.ModDataComponents
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

/**
 * 1.21.1 的 [GunStackStorage] 实现：数据是自注册的 `superbwarfare:item_tag` 组件。
 */
object ItemStackStorage : GunStackStorage {

    private fun component(stack: ItemStack): ModDataComponents.ItemTag? =
        stack.get(ModDataComponents.ITEM_TAG.get())

    /** 活引用：新组件优先；旧存档退回 `custom_data`（只读，不共用对象） */
    private fun liveTag(stack: ItemStack): CompoundTag? = component(stack)?.tag ?: legacyTag(stack)

    @Suppress("DEPRECATION")
    private fun legacyTag(stack: ItemStack): CompoundTag? =
        stack.get(DataComponents.CUSTOM_DATA)?.unsafe

    override fun copyStack(stack: ItemStack): ItemStack {
        val copy = stack.copy()
        // Vanilla stack copies share component values, including the mutable ItemTag.
        if (!copy.isEmpty) component(stack)?.let { attach(copy, it.tag.copy()) }
        return copy
    }

    override fun rootTag(stack: ItemStack): CompoundTag {
        component(stack)?.let { return it.tag }

        val created = CompoundTag()
        legacyTag(stack)?.let { GunStackStorage.mergePreservingIdentity(created, it) }
        attach(stack, created)
        return created
    }

    override fun rootTagOrNull(stack: ItemStack): CompoundTag? = liveTag(stack)

    override fun hasData(stack: ItemStack): Boolean = liveTag(stack) != null

    override fun writeRoot(stack: ItemStack, root: CompoundTag) {
        val live = rootTag(stack)
        if (live !== root) {
            GunStackStorage.mergePreservingIdentity(live, root)
        }
        attach(stack, live)
    }

    override fun bindRoot(stack: ItemStack, root: CompoundTag) = attach(stack, root)

    override fun carrierToken(stack: ItemStack): Long = GunStackStorage.tokenOf(rootTag(stack))

    override fun lastWrittenTagOrNull(stack: ItemStack): CompoundTag? = component(stack)?.written

    override fun clearRoot(stack: ItemStack) {
        stack.remove(ModDataComponents.ITEM_TAG.get())
        stack.remove(DataComponents.CUSTOM_DATA)
    }

    private fun attach(stack: ItemStack, tag: CompoundTag) {
        stack.set(ModDataComponents.ITEM_TAG.get(), ModDataComponents.ItemTag(tag))
    }
}
