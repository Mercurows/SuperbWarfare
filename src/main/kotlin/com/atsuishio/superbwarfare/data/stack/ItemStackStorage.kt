package com.atsuishio.superbwarfare.data.stack

import com.atsuishio.superbwarfare.init.ModDataComponents
import com.atsuishio.superbwarfare.item.gun.GunItem
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

/**
 * 1.21.1 的 [GunStackStorage] 实现：数据是自注册的 `superbwarfare:item_tag` 组件。
 */
object ItemStackStorage : GunStackStorage {

    private fun component(stack: ItemStack): ModDataComponents.ItemTag? =
        stack.get(ModDataComponents.ITEM_TAG.get())

    private fun copyRoot(stack: ItemStack): CompoundTag? {
        val value = component(stack) ?: return legacyTag(stack)?.copy()
        val root = if (stack.item is GunItem && !value.belongsTo(stack)) value.written else value.tag
        return root.copy()
    }

    @Suppress("DEPRECATION")
    private fun legacyTag(stack: ItemStack): CompoundTag? =
        stack.get(DataComponents.CUSTOM_DATA)?.unsafe

    override fun copyStack(stack: ItemStack): ItemStack {
        val copy = stack.copy()
        if (!copy.isEmpty) copyRoot(stack)?.let { attach(copy, it) }
        return copy
    }

    override fun rootTag(stack: ItemStack): CompoundTag {
        component(stack)?.takeIf { it.belongsTo(stack) }?.let { return it.tag }
        // Vanilla copies, including inventory and equipment snapshots, share the component value.
        val created = copyRoot(stack) ?: CompoundTag()
        attach(stack, created)
        return created
    }

    override fun rootTagOrNull(stack: ItemStack): CompoundTag? =
        component(stack)?.takeIf { it.belongsTo(stack) }?.tag ?: copyRoot(stack)

    override fun hasData(stack: ItemStack): Boolean = component(stack) != null || legacyTag(stack) != null

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
        stack.set(ModDataComponents.ITEM_TAG.get(), ModDataComponents.ItemTag(tag, owner = stack))
    }
}
