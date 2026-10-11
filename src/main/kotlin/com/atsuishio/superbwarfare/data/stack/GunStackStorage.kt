package com.atsuishio.superbwarfare.data.stack

import com.atsuishio.superbwarfare.data.gun.GunState
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack

/**
 * 物品上的枪械数据的存取入口
 */
interface GunStackStorage {

    /** 取（必要时创建）这份栈的根 compound */
    fun rootTag(stack: ItemStack): CompoundTag

    /** 只读：没有就是 `null`，**不产生副作用**（不要用它去"探测"再写入） */
    fun rootTagOrNull(stack: ItemStack): CompoundTag?

    /** 这份栈是否已经带着枪械数据（替代散落各处的 `stack.tag != null` / `hasTag()`） */
    fun hasData(stack: ItemStack): Boolean

    /**
     * 把 [root] 写回 [stack]
     */
    fun writeRoot(stack: ItemStack, root: CompoundTag)

    /**
     * 载体身份令牌
     */
    fun carrierToken(stack: ItemStack): Long

    /**
     * 一份栈当前的数据载体：**(根 tag 实例, 身份令牌)** 绑在一起
     */
    data class Carrier(val root: CompoundTag, val token: Long) {

        /** 同 [GunStackStorage.writeRoot]，但这个重载直接用载体自己那份 tag */
        fun writeBackTo(storage: GunStackStorage, stack: ItemStack) = storage.writeRoot(stack, root)
    }

    /** 取这份栈当前的载体；没有枪械数据时也会创建 */
    fun carrierOf(stack: ItemStack): Carrier = Carrier(rootTag(stack), carrierToken(stack))

    companion object {
        /**
         * 一个**已经拿在手里的** compound 的身份令牌
         */
        @JvmStatic
        fun tokenOf(tag: CompoundTag): Long = System.identityHashCode(tag).toLong()

        /**
         * 枪械状态子 tag 的键
         */
        const val KEY_GUN_DATA: String = GunState.KEY_GUN_DATA

        /** 直接取枪械状态子 tag；没有就创建一个（**写回由调用方负责**，见 [GunStackStorage.writeRoot]） */
        fun gunStateTag(root: CompoundTag): CompoundTag =
            if (root.contains(KEY_GUN_DATA, Tag.TAG_COMPOUND.toInt())) {
                root.getCompound(KEY_GUN_DATA)
            } else {
                CompoundTag().also { root.put(KEY_GUN_DATA, it) }
            }

        /** 只读版的枪械状态子 tag；没有就是 `null` */
        fun gunStateTagOrNull(root: CompoundTag): CompoundTag? =
            if (root.contains(KEY_GUN_DATA, Tag.TAG_COMPOUND.toInt())) root.getCompound(KEY_GUN_DATA) else null
    }
}
