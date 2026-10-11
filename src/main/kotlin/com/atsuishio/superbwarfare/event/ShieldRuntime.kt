package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.ShieldEmitter
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.tools.SoundTool
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.capabilities.ForgeCapabilities
import kotlin.math.ceil
import kotlin.math.max

/**
 * 枪盾的运行时状态：耐久、回充与防护锥判定
 */
object ShieldRuntime {
    private const val CHARGE = "Charge"
    private const val LAST_HIT = "LastHit"
    private const val BROKEN_AT = "BrokenAt"
    private const val RECOVERED = "Recovered"
    private const val TICKS_PER_SECOND = 20.0

    /** 一件装着枪盾的全部状态，[tag] 是槽位 compound 的活引用 */
    class Instance(
        val emitter: ShieldEmitter,
        private val tag: CompoundTag,
    ) {
        val slot: AttachmentType get() = emitter.slot.type
        val info get() = emitter.info

        /** 当前耐久 */
        var charge: Double
            get() = if (tag.contains(CHARGE)) tag.getDouble(CHARGE) else info.durability
            set(value) {
                tag.putDouble(CHARGE, value.coerceIn(0.0, info.durability))
            }

        /** 上次吸收到伤害时的世界时间（`level.gameTime`，不是实体的 `tickCount`） */
        var lastHit: Long
            get() = tag.getLong(LAST_HIT)
            set(value) {
                tag.putLong(LAST_HIT, value)
            }

        /** 破碎时的世界时间，0 = 未破碎 */
        var brokenAt: Long
            get() = tag.getLong(BROKEN_AT)
            set(value) {
                tag.putLong(BROKEN_AT, value)
            }

        /** 本轮恢复是否已经播过 [ShieldInfo.rechargeSound] */
        var recovered: Boolean
            get() = tag.getBoolean(RECOVERED)
            set(value) {
                tag.putBoolean(RECOVERED, value)
            }

        val broken: Boolean get() = brokenAt != 0L

        fun maxCharge(): Double = info.durability

        fun sync(): Double {
            val max = maxCharge()
            if (charge <= max) return charge
            charge = max
            return max
        }
    }

    @JvmStatic
    fun of(gun: GunData?): List<Instance> {
        if (gun == null) return emptyList()

        return AttachmentDefinition.findShields(gun).mapNotNull { emitter ->
            val tag = gun.attachment.getTagIfCompound(emitter.slot.type) ?: return@mapNotNull null
            Instance(emitter, tag)
        }
    }

    @JvmStatic
    fun ofMainHand(entity: LivingEntity): List<Instance> {
        val stack = entity.mainHandItem
        if (stack.isEmpty) return emptyList()
        return of(GunData.from(stack))
    }

    @JvmStatic
    fun coverPoint(entity: LivingEntity, travel: Vec3, from: Vec3): Vec3 {
        val lenSqr = travel.lengthSqr()
        if (lenSqr < 1.0E-6) return from

        val eye = entity.eyePosition
        val t = from.subtract(eye).dot(travel) / lenSqr
        return from.add(travel.scale(t.coerceIn(0.0, 1.0)))
    }

    @JvmStatic
    fun inCover(entity: LivingEntity, shield: Instance, travel: Vec3, from: Vec3): Boolean {
        val axis = entity.getViewVector(1f)
        if (axis.lengthSqr() < 1.0E-6) return false

        val to = coverPoint(entity, travel, from).subtract(entity.eyePosition)
        if (to.lengthSqr() < 1.0E-6) return false

        return axis.normalize().dot(to.normalize()) >= shield.info.coverCos()
    }

    @JvmStatic
    fun travelAt(entity: Entity): Vec3 = entity.position().subtract(entity.xo, entity.yo, entity.zo)

    @JvmStatic
    fun originAt(entity: Entity): Vec3 = Vec3(entity.xo, entity.yo, entity.zo)

    @JvmStatic
    fun travelOf(source: DamageSource): Vec3 =
        source.directEntity?.let { travelAt(it) } ?: Vec3.ZERO

    @JvmStatic
    fun originOf(source: DamageSource): Vec3? {
        val projectile = source.directEntity
        if (projectile != null) {
            val travel = travelAt(projectile)
            if (travel.lengthSqr() > 1.0E-6) return originAt(projectile)
        }

        return source.entity?.position()
    }

    @JvmStatic
    fun tick(holder: LivingEntity, gun: GunData) {
        val stack = gun.stack
        if (stack.isEmpty) return
        val level = holder.level()
        if (level.isClientSide) return

        val now = level.gameTime
        for (shield in of(gun)) {
            val max = shield.maxCharge()
            if (shield.sync() >= max) continue

            val elapsed = (now - shield.lastHit).coerceAtLeast(0L)
            if (elapsed < shield.info.rechargeDelay.toLong()) continue

            if (shield.broken) {
                val locked = (now - shield.brokenAt).coerceAtLeast(0L)
                if (locked < shield.info.brokenLockout.toLong()) continue
                shield.brokenAt = 0L
            }

            // `RechargeRate` 是每秒恢复的耐久，按 20 tick/秒折算到这一 tick
            val rate = shield.info.rechargeRate.coerceAtLeast(0.0) / TICKS_PER_SECOND
            val amount = rate.coerceAtMost(max - shield.charge)
            if (amount <= 0.0) continue
            if (!consume(stack, amount * shield.info.energyPerCharge.coerceAtLeast(0))) continue

            // 开始恢复：本轮第一次真的涨耐久时播给自己听
            if (!shield.recovered) {
                shield.recovered = true
                shield.info.rechargeSound?.let { SoundTool.playLocalSound(holder as? Player, it) }
            }

            shield.charge += amount
            if (shield.charge >= max) shield.charge = max
        }
    }

    private fun consume(stack: ItemStack, energy: Double): Boolean {
        if (energy <= 0.0) return true

        val need = max(1, ceil(energy).toInt())
        val stored = stack.getCapability(ForgeCapabilities.ENERGY)
        if (!stored.isPresent) return false

        return stored.map { it.extractEnergy(need, false) >= need }.orElseGet { false }
    }
}
