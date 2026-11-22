package com.atsuishio.superbwarfare.data.stack

import com.atsuishio.superbwarfare.data.gun.GunState
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack

/**
 * 物品上的枪械数据的存取入口
 */
interface GunStackStorage {

    /** Copies the stack with independent mutable gun data. */
    fun copyStack(stack: ItemStack): ItemStack = stack.copy()

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

    /** Binds a new sub-weapon stack to the host attachment's live [root] tag. */
    fun bindRoot(stack: ItemStack, root: CompoundTag)

    /**
     * 载体身份令牌
     */
    fun carrierToken(stack: ItemStack): Long

    /**
     * 上一次写回时那份**内容快照**；`null` = 这份实现不提供（那就退回"总是写"）。
     *
     * `GunData.persist` 用它判断"自那次写回之后有没有人改过这份 tag"。不能拿当前 tag 自己比：
     * 副武器的状态住在主武器 tag 的子 compound 里，主武器一个字段都没写、内容也会变。
     */
    fun lastWrittenTagOrNull(stack: ItemStack): CompoundTag? = null

    /** 清掉这份栈上的根 tag（`GunData` 把枪重置成空的时候用） */
    fun clearRoot(stack: ItemStack) {
        writeRoot(stack, CompoundTag())
    }

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

        /**
         * 把 [source] 折进 [target]，**两边都有的 compound 保持 [target] 那一侧的对象身份**。
         *
         * `GunData` 与它的子数据处理器都靠引用活着（`tag` / `gunDataTag` / `perkTag` / `attachmentTag`
         * 全是构造时捕获的 `val`），换掉对象 = 状态永久脱钩。
         *
         * @param removeMissing `true` = 键消失即"回到默认值"（`GunState.writeInto` 的口径）；
         *   `false` = 只做并集（普通 merge 语义）
         */
        @JvmStatic
        @JvmOverloads
        fun mergePreservingIdentity(target: CompoundTag, source: CompoundTag, removeMissing: Boolean = true) {
            if (target === source) return

            if (removeMissing) {
                for (key in target.allKeys.toList()) {
                    if (!source.contains(key)) target.remove(key)
                }
            }

            for (key in source.allKeys) {
                val incoming = source.get(key) ?: continue
                val existing = target.get(key)
                if (incoming is CompoundTag && existing is CompoundTag) {
                    mergePreservingIdentity(existing, incoming, removeMissing)
                } else {
                    target.put(key, incoming.copy())
                }
            }
        }
    }
}
