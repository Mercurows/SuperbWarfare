package com.atsuishio.superbwarfare.data.stack

import com.atsuishio.superbwarfare.init.ModDataComponents
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

/**
 * 1.21.1 的 [GunStackStorage] 实现：数据是自注册的 `superbwarfare:item_tag` 组件。
 *
 * 两条互相拉扯的要求，这里同时满足：
 *
 * 1. **读出来的必须是活引用** —— `GunData` 把根 tag 与三个子 compound 捕获成 `val`，副武器
 *    （`SubWeaponRuntime`）还要求"装配时那份"与"之后写回的那份"是同一个对象；
 * 2. **内容变了必须让 MC 看得见** —— 槽位同步靠 `ItemStack.matches` 比对组件值，而
 *    `ItemStack.copy()` 只共享组件值，所以就地改 tag 之后比对永远相等，服务端一个字节都不会
 *    发给客户端（弹药不减 / 装假火 / 副武器切不动，都是这一条）。
 *
 * 办法：写入时**折内容**（保住对象身份）并**换一个新的组件值实例**
 * （[ModDataComponents.ItemTag]，身份判等），让"变了"这件事看得见。
 *
 * 旧存档与配方结果把数据写在 `minecraft:custom_data`，这里只做**只读兜底**：
 * 第一次写入时把内容复制进新组件，旧组件原样留着。
 */
object ItemStackStorage : GunStackStorage {

    private fun component(stack: ItemStack): ModDataComponents.ItemTag? =
        stack.get(ModDataComponents.ITEM_TAG.get())

    /** 活引用：新组件优先；旧存档退回 `custom_data`（只读，不共用对象） */
    private fun liveTag(stack: ItemStack): CompoundTag? = component(stack)?.tag ?: legacyTag(stack)

    @Suppress("DEPRECATION")
    private fun legacyTag(stack: ItemStack): CompoundTag? =
        stack.get(DataComponents.CUSTOM_DATA)?.unsafe

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
