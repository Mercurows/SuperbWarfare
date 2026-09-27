package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.item.attachment.SubWeaponItem
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.tools.ActiveGun.dataOf
import com.atsuishio.superbwarfare.tools.ActiveGun.holster
import com.atsuishio.superbwarfare.tools.ActiveGun.stackOf
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * 「**当前操控的枪**」（active gun）的唯一解析入口 —— 四期的核心（§9.8.1）。
 *
 * ## 它解决什么问题
 *
 * 需求是「按下 G 之后在主武器与副武器之间切换，让当前操控的 gun 变成副武器，
 * 这样能对副武器进行原本的开火/换弹/瞄准等操作，而不需要单独判断 subweapon」。
 *
 * **不换物品**（评估过，三个硬障碍见 §9.8.1）：主手是双端权威槽、副武器栈是凭空造的合成栈、
 * 快捷栏 9 槽与"枪身上的配件"基数不同。所以这里只做一件事：
 * **把"我正在操作哪把枪"变成一个可查询的事实**，让全仓的读取点从 `player.mainHandItem`
 * 换成 [stackOf] / [dataOf]。
 *
 * ## 状态在哪
 *
 * `GunState.ActiveSlot`（空 = 主武器，否则是 [AttachmentType] 枚举名）+ `GunState.ActiveOwner`
 * （宿主枪 UUID）。两者都写在**宿主枪的枪械状态**里，因此随主武器 NBT 持久化、也随主武器同步到客户端 ——
 * 双端一致、不需要新存档字段、不需要新同步通道。
 *
 * ## 三条不变量（§9.8.10）
 *
 * 1. **只指向"主手那把枪身上确实装着的副武器"**：每次读取都重新校验（槽位还在、物品还是
 *    `SubWeaponItem`、`ActiveOwner` 等于宿主枪 UUID），不通过就当主武器用**并把状态清掉**（自愈）。
 * 2. **主手物品真的换了 → 自动收起**：落点是 `LivingEventHandler` 里现成的切枪检测。
 * 3. **服务端是唯一写入方**：客户端只发请求、只读确认，永不自己写（三期 §11.8.1-⑫ 的教训）。
 *
 * ## 读取点的分工（改代码时务必先看这张表）
 *
 * | 读到的东西 | 该用什么 |
 * |---|---|
 * | 「我正在操作的那把枪」 | **本类**（[stackOf] / [dataOf]） |
 * | 「物理上拿在手里的那件物品」 | 保持 `player.mainHandItem` 不动：全部 Mixin、`GunItem.inventoryTick`、`getAttributeModifiers`、改装界面/命令、交互与放置 |
 * | 「这件物品**拿在手上时**算不算枪」 | `GunItem.isHeldWeapon(stack)`（手持副武器物品本身时按普通物品处理，§8.3.1） |
 * | 「这个栈现在**能不能被当枪操作**」 | `GunItem.isOperable(stack)` —— 部署中的副武器栈走的是这一条 |
 */
object ActiveGun {

    /** 部署状态解析结果 */
    sealed interface Deployed {

        /** 部署中：[instance] 是那把副武器，[slot] 是它所在的槽位 */
        data class Active(val slot: AttachmentType, val instance: SubWeaponRuntime.Instance) : Deployed

        /**
         * 没有部署（`ActiveSlot` 为空）。
         *
         * @param staleState `true` = 状态里写着部署、但它已经失效（槽位被拆 / 换了枪 / 换了副武器）。
         *   调用方**不需要**处理它 —— 读取路径已经顺手把状态清掉了（自愈，见类注释的不变量 1）。
         */
        data class Holstered(val staleState: Boolean = false) : Deployed
    }

    /**
     * **物理上**的主手物品。
     *
     * 全仓只有"看真实手持物"的地方该用它：`LivingEventHandler` 的切枪检测、
     * `GunItem.inventoryTick`、全部 Mixin、改装界面/命令。
     */
    fun mainStack(player: Player): ItemStack = player.mainHandItem

    /**
     * 主手那把枪的 [GunData]（**不是**当前操控的枪）；主手不是枪时返回 `null`。
     *
     * 近战恒用它（§9.8.2：副武器没有近战），切枪检测也用它。
     */
    fun mainGun(player: Player): GunData? {
        val stack = player.mainHandItem
        if (!GunItem.isHeldWeapon(stack)) return null
        return GunData.from(stack)
    }

    /**
     * **操控手感**（瞄准 / 端枪 / 摇摆 / 气息 / 后坐衰减）该读的那把枪。
     *
     * 恒等于 [mainGun]，**副武器被切出来的期间也一样** —— 副武器是**挂在主武器身上的附件**
     * （§9.8.2），主/副切换换的只是"当前操控的枪"，手里那把枪从未离手。所以 `ZoomTime`、
     * `DrawTime`、`Weight` 这些描述"这把枪拿在手上什么手感"的数值，必须仍然来自主武器；
     * 否则挂上一支 GP-25（`Weight 1.5` / `DrawTime 1` / `ZoomTime 1`）之后，
     * 整把 AK 的瞄准、端枪和摇摆会全部变成榴弹筒的。
     *
     * | 读到的东西 | 该用什么 |
     * |---|---|
     * | 开火（`Spread` / `RecoilX` / `RecoilY` / `RPM` / 弹药 / 音效 / 开火动画） | [dataOf] —— 你**打**的是哪把枪 |
     * | 手感（`ZoomTime` / `DrawTime` / `Weight`） | **本函数** —— 你**拿**的是哪把枪 |
     *
     * 与 [mainGun] 是同一件事，拆成两个名字只为让读取点自解释。清单见 §9.8.13。
     */
    fun handlingData(player: Player): GunData? = mainGun(player)

    /** [handlingData] 的"任何生物"版本；非玩家没有副武器，就是它主手那把枪 */
    fun handlingData(entity: LivingEntity): GunData? {
        val main = entity.mainHandItem
        if (!GunItem.isHeldWeapon(main)) return null
        return GunData.from(main)
    }

    /**
     * **当前操控的枪**的合成栈。
     *
     * 主手不是枪 → [ItemStack.EMPTY]；部署了副武器 → 那把副武器的合成栈；否则 → 主手物品。
     */
    fun stackOf(player: Player): ItemStack {
        val main = player.mainHandItem
        if (!GunItem.isHeldWeapon(main)) return ItemStack.EMPTY
        return stackOf(GunData.from(main), player.level().isClientSide)
    }

    /**
     * 已知主手是这把枪时，直接拿当前操控的栈（省一次主手读取）。
     *
     * `client` 必须**显式传**：单人游戏里客户端与服务端共享静态缓存表，
     * 用错一侧的实例会让状态在两边互相覆盖（`SubWeaponRuntime` 的不变式 ③）。
     */
    fun stackOf(gun: GunData, client: Boolean): ItemStack =
        when (val deployed = resolve(gun, client)) {
            is Deployed.Active -> deployed.instance.stack
            is Deployed.Holstered -> gun.stack
        }

    /**
     * [stackOf] 的"任何生物"版本。
     *
     * 客户端的渲染/摇摆/后坐那一族回调拿到的常常是 `LivingEntity`（`ViewportEvent` /
     * `RenderHandEvent` 等），而部署状态只对玩家有意义。**非玩家一律退回它的主手物品**。
     */
    fun stackOf(entity: LivingEntity): ItemStack {
        val player = entity as? Player ?: return entity.mainHandItem
        return stackOf(player)
    }

    /** **当前操控的枪**的 [GunData]；主手不是枪时返回 `null` */
    fun dataOf(player: Player): GunData? {
        val stack = stackOf(player)
        if (stack.isEmpty) return null
        return GunData.from(stack)
    }

    /** [stackOf] 的 [GunData] 版本 */
    fun dataOf(gun: GunData, client: Boolean): GunData =
        when (val deployed = resolve(gun, client)) {
            is Deployed.Active -> deployed.instance.data
            is Deployed.Holstered -> gun
        }

    /** 这把枪上现在是不是有一把副武器被切了出来 */
    fun isDeployed(gun: GunData, client: Boolean): Boolean = resolve(gun, client) is Deployed.Active

    /** [dataOf] 出来的那份数据是不是副武器（渲染/动画侧的"这一发是谁打的"靠它） */
    fun isSubWeapon(data: GunData): Boolean = data.item is SubWeaponItem

    /**
     * 解析部署状态。
     *
     * **带自愈**：状态写着部署但已经失效时，在这里就把 `ActiveSlot`/`ActiveOwner` 清掉
     * （而不是每 tick 报错）。清状态只在**服务端**做 —— 客户端那份是从服务端同步过来的视图，
     * 自己清会被下一次同步覆盖回来，还会让两边短暂不一致。
     */
    fun resolve(gun: GunData, client: Boolean): Deployed {
        val slotName = gun.activeSlot.get()
        if (slotName.isEmpty()) return Deployed.Holstered()

        val slot = AttachmentType.entries.firstOrNull { it.name == slotName }
        if (slot == null) {
            invalidate(gun, client, "unknown slot '$slotName'")
            return Deployed.Holstered(staleState = true)
        }

        // ActiveOwner 是"这次部署属于哪把枪"的显式约束（§9.8.1）。
        // 主武器 UUID 与它不符 → 状态是从别的枪上搬过来的/被复制过，作废。
        val owner = gun.activeOwner.get()
        val self = gun.uuid?.toString()
        if (owner.isNotEmpty() && (self == null || !owner.equals(self, ignoreCase = true))) {
            invalidate(gun, client, "owner mismatch ($owner != $self)")
            return Deployed.Holstered(staleState = true)
        }

        val instance = SubWeaponRuntime.find(gun, slot, client)
        if (instance == null) {
            invalidate(gun, client, "slot ${slot.name} no longer holds a sub-weapon")
            return Deployed.Holstered(staleState = true)
        }

        return Deployed.Active(slot, instance)
    }

    /**
     * 把 [slot] 上的副武器切为当前操控的枪。**只在服务端调用**
     * （`SubWeaponDeployMessage` 的 handler）。
     */
    fun deploy(gun: GunData, slot: AttachmentType, instance: SubWeaponRuntime.Instance) {
        debug { "deploy ${slot.name} on ${gun.id}" }

        gun.activeSlot.set(slot.name)
        gun.activeOwner.set(gun.uuid?.toString() ?: "")
        gun.save()

        // 宿主枪在副武器顶替期间不参与开火/换弹，把它的在途状态清干净（不算"装填完成"）
        SubWeaponRuntime.interruptReload(gun)
        instance.wasReloading = false
    }

    /** 切回主武器。**只在服务端调用** */
    fun holster(gun: GunData) {
        if (gun.activeSlot.get().isEmpty()) return

        debug { "holster on ${gun.id}" }

        // 顺手把副武器那把枪的在途状态也清掉（它可能正装着弹）
        activeSlot(gun)?.let { slot ->
            SubWeaponRuntime.find(gun, slot, client = false)?.let { SubWeaponRuntime.interruptReload(it.data) }
        }

        gun.activeSlot.set("")
        gun.activeOwner.set("")
        gun.save()
    }

    /** 当前部署的槽位；没有或无法解析时返回 `null`（**不**清理状态，只读） */
    fun activeSlot(gun: GunData): AttachmentType? {
        val slotName = gun.activeSlot.get()
        if (slotName.isEmpty()) return null
        return AttachmentType.entries.firstOrNull { it.name == slotName }
    }

    /**
     * 主手物品变了（换枪/丢枪/被替换）时调用：把部署状态清掉。
     *
     * 与 [holster] 的区别是它**不做副武器侧的清理**（那把枪已经跟着旧宿主枪走了，
     * 我们手上未必还拿得到它的实例），只负责让新的这把枪从"主武器"开始。
     */
    fun onMainHandChanged(gun: GunData) {
        if (gun.activeSlot.get().isEmpty()) return
        debug { "auto-holster on main hand change (${gun.id})" }
        gun.activeSlot.set("")
        gun.activeOwner.set("")
        gun.save()
    }

    /** 解析失败时的自愈：清掉状态并记一条日志 */
    private fun invalidate(gun: GunData, client: Boolean, reason: String) {
        debug { "stale deploy state on ${gun.id}: $reason" }
        // 只有服务端是权威写入方（不变量 3）：客户端清了也会被下一次同步覆盖回来
        if (client) return

        gun.activeSlot.set("")
        gun.activeOwner.set("")
        gun.save()
    }

    private inline fun debug(message: () -> String) {
        if (DisplayConfig.MELEE_DEBUG_LOG.get()) {
            Mod.LOGGER.info("[ActiveGun] {}", message())
        }
    }
}
