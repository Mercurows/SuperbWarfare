package com.atsuishio.superbwarfare.data.stack

import com.atsuishio.superbwarfare.data.stack.ItemStackStorage.carrierToken
import com.atsuishio.superbwarfare.data.stack.ItemStackStorage.rootTag
import com.atsuishio.superbwarfare.data.stack.ItemStackStorage.writeRoot
import com.atsuishio.superbwarfare.tools.tag
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

/**
 * 1.20.1 (Forge) 的 [GunStackStorage] 实现：**数据就是 `ItemStack` 的根 [CompoundTag]**。
 *
 * 这一份实现是"活引用"语义的原点：
 * - [rootTag] 返回的就是 `stack.getOrCreateTag()` 那个对象，**改它就是改栈**；
 * - [writeRoot] 因此是**空操作**（除非栈上挂的不是同一份 —— 那种情况下补一刀，
 *   与三期 `SubWeaponRuntime.assemble` 里那句 `if (stack.tag !== liveTag) stack.tag = liveTag` 等价）；
 * - [carrierToken] 用根 tag 的引用哈希：它回答的是"这份 tag 还是不是挂在栈上的那一份"。
 *
 * ## 1.21.1 分支要写什么
 *
 * `ItemStack` 在那边没有物品 NBT 了，实现应当是：自注册一个承载 [CompoundTag] 的 `DataComponent`，
 * [rootTag] 走 `stack.get(COMPONENT)`、[writeRoot] 走 `stack.set(COMPONENT, root)`、
 * [carrierToken] 返回组件的写入序号。**业务逻辑（`SubWeaponRuntime` / `GunData` / 附件与弹药子 tag）
 * 一行都不用改** —— 这正是把差异收进这个接口的目的。
 *
 * ⚠ 那条分支开工前必须先验证一件事：`GunData` 会把根 tag 与三个子 compound 捕获成 `val`，
 * 所以"装配时拿到的那份 tag"与"后续写回时用的那份"必须是**同一个实例**。
 * 1.20.1 靠活引用天然成立；1.21.1 侧要在组件读写路径上做等价的事，否则三期 §11.8.3 的
 * 三个症状（装填走完没装上 / 按住 G 音效一直响 / 按 G 完全没反应）会原样复现。
 */
object ItemStackStorage : GunStackStorage {

    override fun rootTag(stack: ItemStack): CompoundTag = stack.tag ?: CompoundTag()

    override fun rootTagOrNull(stack: ItemStack): CompoundTag? =
        if (stack.tag == null) null else stack.tag

    override fun hasData(stack: ItemStack): Boolean = stack.tag != null

    override fun writeRoot(stack: ItemStack, root: CompoundTag) {
        // 活引用：正常情况下什么都不用做。`ItemStack(item, count, tag)` 不保证原样持有传进去的
        // tag（三期实测过），所以这里补一刀，成本是一次引用比较。
        if (stack.tag !== root) {
            stack.tag = root
        }
    }

    override fun carrierToken(stack: ItemStack): Long =
        GunStackStorage.tokenOf(rootTag(stack))
}
