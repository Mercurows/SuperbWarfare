package com.atsuishio.superbwarfare.data.stack

import com.atsuishio.superbwarfare.data.gun.GunState
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack

/**
 * 「物品上的枪械数据」的存取入口 —— **全仓唯一允许碰版本相关 API 的地方**。
 *
 * ## 为什么需要它
 *
 * 本项目有 **1.20.1 (Forge)** 与 **1.21.1 (NeoForge)** 两条分支，后者移除了 `ItemStack` 上的物品 NBT、
 * 改用 **DataComponent**（见 `localmod/README.md` 第五节的差异表）。而枪械数据层
 * （`GunData` / `GunState` / `Attachment` / `AmmoSlot` / `Perks`）**全部直接建立在 [CompoundTag] 上**
 * （`GunState` 走 `encodeToCompoundTag` / `decodeFromCompoundTag` 的 kotlinx NBT 格式）。
 *
 * 所以**不能**把 [CompoundTag] 换成一个跨版本的中立数据模型 —— 那等于重写整个枪械数据层。
 * `CompoundTag` 本身在 1.21.1 里仍然存在（NBT 没死，死的是 `ItemStack` 上的 NBT 槽位），
 * 于是正确做法是：**保留 `CompoundTag` 作为内存态，只把"它挂在物品上的哪儿、怎么读写"抽出来**。
 *
 * ## 两条实现
 *
 * | | 1.20.1 (Forge) | 1.21.1 (NeoForge) |
 * |---|---|---|
 * | 数据在哪 | `ItemStack` 的根 [CompoundTag] | 一个自注册的 `DataComponent`（内容仍是一份 `CompoundTag`） |
 * | 读取 | `stack.getOrCreateTag()` | `stack.get(COMPONENT)` |
 * | 写入 | 就是那份 tag 本身（活引用，改完即生效） | `stack.set(COMPONENT, tag)` |
 * | [carrierToken] | 根 tag 的 `System.identityHashCode` | 组件的 patch 版本号 / 递增序号 |
 *
 * ## ⚠ 唯一的硬约束：**活引用**
 *
 * 三期的 `SubWeaponRuntime` 踩过一整轮坑（§11.8.3），根因就是"`GunData` 在构造时把根 tag 与它的
 * 三个子 compound 全部**捕获成 `val`**"，一旦那份 tag 不再挂在栈上，所有写入都进了一个
 * 与主武器 NBT 无关的角落。所以：
 *
 * 1. **1.20.1 侧的 [rootTag] 返回的是活引用**——改它就是改栈；
 * 2. **1.21.1 侧不允许在改动中途重新 `get`**：必须在**同一个实例上原地改**，改完用 [Carrier.writeRoot]
 *    写回去。这也是为什么 [carrierToken] 不参与等值判断、只回答"还是不是我上次看到的那份载体"。
 */
interface GunStackStorage {

    /** 取（必要时创建）这份栈的根 compound —— 语义等同 1.20.1 的 `stack.getOrCreateTag()` */
    fun rootTag(stack: ItemStack): CompoundTag

    /** 只读：没有就是 `null`，**不产生副作用**（不要用它去"探测"再写入） */
    fun rootTagOrNull(stack: ItemStack): CompoundTag?

    /** 这份栈是否已经带着枪械数据（替代散落各处的 `stack.tag != null` / `hasTag()`） */
    fun hasData(stack: ItemStack): Boolean

    /**
     * 把 [root] 写回 [stack]。
     *
     * 1.20.1 侧是空操作（[rootTag] 本来就是活引用）；1.21.1 侧是 `stack.set(COMPONENT, root)`。
     */
    fun writeRoot(stack: ItemStack, root: CompoundTag)

    /**
     * 载体身份令牌 —— 用来回答"还是不是我上次看到的那份载体"。
     *
     * **只做同一性判断，不做等值判断**（等值比较用 [CompoundTag] 自己的 `==`）。
     * 1.20.1 侧取根 tag 的引用哈希；1.21.1 侧取组件的写入序号。
     */
    fun carrierToken(stack: ItemStack): Long

    /**
     * 一份栈当前的数据载体：**(根 tag 实例, 身份令牌)** 绑在一起。
     *
     * 需要"跨 tick 记住某份 tag"的地方（`SubWeaponRuntime.Instance` 就是唯一一处）应当持有
     * [Carrier] 而不是裸的 `CompoundTag` —— 否则换版本时会忘掉"令牌"这一半，退回到裸引用比较。
     */
    data class Carrier(val root: CompoundTag, val token: Long) {

        /** 同 [GunStackStorage.writeRoot]，但这个重载直接用载体自己那份 tag */
        fun writeBackTo(storage: GunStackStorage, stack: ItemStack) = storage.writeRoot(stack, root)
    }

    /** 取这份栈当前的载体；没有枪械数据时也会创建（`getOrCreate` 语义，与 1.20.1 的 `getOrCreateTag` 一致） */
    fun carrierOf(stack: ItemStack): Carrier = Carrier(rootTag(stack), carrierToken(stack))

    companion object {
        /**
         * 一个**已经拿在手里的** compound 的身份令牌。
         *
         * 用于"子 tag 而不是栈根 tag"的场景：`SubWeaponRuntime` 手里那份是宿主枪 NBT 里的
         * **附件子 compound**，它没有自己的 `ItemStack`，所以只能按对象身份取令牌。
         * 1.20.1 侧这个身份就是"活引用"本身；1.21.1 侧同样成立（子 compound 在同一个组件实例里）。
         */
        @JvmStatic
        fun tokenOf(tag: CompoundTag): Long = System.identityHashCode(tag).toLong()

        /**
         * 枪械状态子 tag 的键（`GunData` 的子 compound）。
         *
         * 与 `GunState.KEY_GUN_DATA` 同源 —— 这里再写一次是为了让本文件成为"根 tag 的布局"的
         * 唯一出口；改这个键要同时改 [GunState]。
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
