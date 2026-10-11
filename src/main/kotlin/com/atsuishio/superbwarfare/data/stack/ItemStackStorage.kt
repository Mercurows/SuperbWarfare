package com.atsuishio.superbwarfare.data.stack

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

/**
 * 1.20.1 (Forge) 的 [GunStackStorage] 实现：**数据就是 `ItemStack` 的根 [CompoundTag]**。
 */
object ItemStackStorage : GunStackStorage {

    override fun rootTag(stack: ItemStack): CompoundTag = stack.orCreateTag

    override fun rootTagOrNull(stack: ItemStack): CompoundTag? =
        if (stack.tag == null) null else stack.tag

    override fun hasData(stack: ItemStack): Boolean = stack.tag != null

    override fun writeRoot(stack: ItemStack, root: CompoundTag) {
        if (stack.tag !== root) {
            stack.tag = root
        }
    }

    override fun carrierToken(stack: ItemStack): Long =
        GunStackStorage.tokenOf(rootTag(stack))
}
