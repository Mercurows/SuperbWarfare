package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent

/**
 * 枪盾的伤害拦截
 */
@EventBusSubscriber
object ShieldHandler {

    private val soundCooldown = HashMap<Int, Long>()
    private const val SOUND_INTERVAL = 4L

    data class AbsorbResult(val absorbed: Float, val broken: Boolean) {
        val blocked: Boolean get() = absorbed > 0f
    }

    @JvmStatic
    fun isBlockableSource(source: DamageSource): Boolean {
        if (source.`is`(DamageTypeTags.IS_EXPLOSION)) return false
        if (source.directEntity is Projectile) return true
        if (DamageTypeTool.isGunDamage(source)) return true
        return false
    }

    @JvmStatic
    fun hasActiveShield(victim: LivingEntity): Boolean {
        val gun = mainHandGun(victim) ?: return false
        if (!gun.zooming.get()) return false
        return ShieldRuntime.of(gun).any { !it.broken && it.charge > 0.0 }
    }

    @JvmStatic
    fun willCover(victim: LivingEntity, travel: Vec3, from: Vec3): Boolean {
        val gun = mainHandGun(victim) ?: return false
        if (!gun.zooming.get()) return false
        return ShieldRuntime.of(gun).any {
            !it.broken && it.charge > 0.0 && ShieldRuntime.inCover(victim, it, travel, from)
        }
    }

    private fun mainHandGun(victim: LivingEntity): GunData? {
        val stack = victim.mainHandItem
        if (stack.isEmpty) return null
        return GunData.from(stack)
    }

    @SubscribeEvent
    fun onEntityHurt(event: LivingIncomingDamageEvent) {
        val victim = event.entity ?: return
        if (victim.level().isClientSide) return
        if (event.source.entity == victim) return
        if (!isBlockableSource(event.source)) return

        val gun = mainHandGun(victim) ?: return
        if (!gun.zooming.get()) return

        val travel = ShieldRuntime.travelOf(event.source)
        val from = ShieldRuntime.originOf(event.source) ?: return
        val result = tryAbsorb(victim, gun, event.source, travel, from, event.amount) ?: return
        if (!result.blocked) return

        val amount = event.amount - result.absorbed
        if (amount <= 0) {
            event.isCanceled = true
            return
        }

        event.amount = amount.coerceAtLeast(0f)
    }

    @JvmStatic
    fun tryAbsorb(
        victim: LivingEntity,
        gun: GunData,
        source: DamageSource,
        travel: Vec3,
        from: Vec3,
        damage: Float,
    ): AbsorbResult? {
        if (damage <= 0f) return null

        val now = victim.tickCount.toLong()
        for (shield in ShieldRuntime.of(gun)) {
            if (shield.broken || shield.sync() <= 0.0) continue
            if (!ShieldRuntime.inCover(victim, shield, travel, from)) continue

            // 穿甲类伤害额外乘 (1 + 穿甲倍率)，非穿甲类就是伤害本身
            val rate = if (DamageTypeTool.isArmorPiercingDamage(source)) {
                gun.get(GunProp.BYPASSES_ARMOR).coerceAtLeast(0.0)
            } else {
                0.0
            }

            val cost = damage * (1.0 + rate)
            val broken = cost >= shield.charge
            shield.charge -= cost
            shield.lastHit = now
            // 挨了一下就是新一轮恢复，下次开始恢复时要重新播一次开始恢复的音效
            shield.recovered = false

            if (broken) {
                shield.charge = 0.0
                shield.brokenAt = now
                shield.info.breakSound?.let { playSound(victim, it, now, true) }
            } else {
                shield.info.blockSound?.let { playSound(victim, it, now, false) }
            }

            // 破盾那一发按 BreakOverflow 决定放过多少，默认 0 = 全额穿透
            val absorbed = if (broken) {
                damage * (shield.info.breakOverflow.coerceIn(0.0, 100.0) / 100.0)
            } else {
                damage
            }.toFloat()

            return AbsorbResult(absorbed, broken)
        }

        return null
    }

    private fun playSound(victim: LivingEntity, sound: SoundEvent, now: Long, force: Boolean) {
        if (!force) {
            val next = soundCooldown[victim.id] ?: 0L
            if (now < next) return
            soundCooldown[victim.id] = now + SOUND_INTERVAL
        }

        victim.level().playSound(
            null,
            victim.x,
            victim.y + victim.bbHeight * 0.5,
            victim.z,
            sound,
            SoundSource.PLAYERS,
            1f,
            1f
        )
    }
}
