package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.ActiveGun.dataOf
import com.atsuishio.superbwarfare.data.gun.ActiveGun.stackOf
import com.atsuishio.superbwarfare.data.gun.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.item.attachment.SubWeaponItem
import com.atsuishio.superbwarfare.item.gun.GunItem
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * 当前操控的枪
 */
object ActiveGun {

    /** 部署状态解析结果 */
    sealed interface Deployed {

        /** 部署中：[instance] 是那把副武器，[slot] 是它所在的槽位 */
        data class Active(val slot: AttachmentType, val instance: SubWeaponRuntime.Instance) : Deployed

        /**
         * 没有部署（`ActiveSlot` 为空）
         */
        data class Holstered(val staleState: Boolean = false) : Deployed
    }

    @JvmStatic
    fun mainStack(player: Player): ItemStack = player.mainHandItem

    @JvmStatic
    fun mainGun(player: Player): GunData? {
        val stack = player.mainHandItem
        if (!GunItem.isHeldWeapon(stack)) return null
        return GunData.from(stack)
    }

    @JvmStatic
    fun handlingData(player: Player): GunData? = mainGun(player)

    @JvmStatic
    fun handlingData(entity: LivingEntity): GunData? {
        val main = entity.mainHandItem
        if (!GunItem.isHeldWeapon(main)) return null
        return GunData.from(main)
    }

    @JvmStatic
    fun stackOf(player: Player): ItemStack {
        val main = player.mainHandItem
        if (!GunItem.isHeldWeapon(main)) return ItemStack.EMPTY
        return stackOf(GunData.from(main), player.level().isClientSide)
    }

    @JvmStatic
    fun stackOf(gun: GunData, client: Boolean): ItemStack =
        when (val deployed = resolve(gun, client)) {
            is Deployed.Active -> deployed.instance.stack
            is Deployed.Holstered -> gun.stack
        }

    @JvmStatic
    fun stackOf(entity: LivingEntity): ItemStack {
        val player = entity as? Player ?: return entity.mainHandItem
        return stackOf(player)
    }

    /** **当前操控的枪**的 [GunData]；主手不是枪时返回 `null` */
    @JvmStatic
    fun dataOf(player: Player): GunData? {
        val stack = stackOf(player)
        if (stack.isEmpty) return null
        return GunData.from(stack)
    }

    /** [stackOf] 的 [GunData] 版本 */
    @JvmStatic
    fun dataOf(gun: GunData, client: Boolean): GunData =
        when (val deployed = resolve(gun, client)) {
            is Deployed.Active -> deployed.instance.data
            is Deployed.Holstered -> gun
        }

    /** 这把枪上现在是不是有一把副武器被切了出来 */
    @JvmStatic
    fun isDeployed(gun: GunData, client: Boolean): Boolean = resolve(gun, client) is Deployed.Active

    /** [dataOf] 出来的那份数据是不是副武器（渲染/动画侧的"这一发是谁打的"靠它） */
    @JvmStatic
    fun isSubWeapon(data: GunData): Boolean = data.item is SubWeaponItem

    /**
     * 解析部署状态
     */
    @JvmStatic
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
    @JvmStatic
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
    @JvmStatic
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
    @JvmStatic
    fun activeSlot(gun: GunData): AttachmentType? {
        val slotName = gun.activeSlot.get()
        if (slotName.isEmpty()) return null
        return AttachmentType.entries.firstOrNull { it.name == slotName }
    }

    /**
     * 主手物品变了（换枪/丢枪/被替换）时调用：把部署状态清掉
     */
    @JvmStatic
    fun onMainHandChanged(gun: GunData) {
        if (gun.activeSlot.get().isEmpty()) return
        debug { "auto-holster on main hand change (${gun.id})" }
        gun.activeSlot.set("")
        gun.activeOwner.set("")
        gun.save()
    }

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