package com.atsuishio.superbwarfare.perk.js

import com.atsuishio.superbwarfare.entity.projectile.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraftforge.registries.ForgeRegistries

class ProjectileProxy(private val entity: Entity) {
    fun setHoming(range: Number, turnDegrees: Number, coneDegrees: Number) {
        (entity as? ProjectileEntity)?.setHoming(range.toDouble(), turnDegrees.toDouble(), coneDegrees.toDouble())
    }

    fun canEmitLightning(): Boolean = entity is TaserBulletEntity || entity is ReforgingLightningBallEntity

    private val projectile: IBulletProperties?
        get() = entity as? IBulletProperties

    private val taser: TaserBulletEntity?
        get() = entity as? TaserBulletEntity

    fun setRGB(r: Number, g: Number, b: Number) {
        projectile?.setRGB(floatArrayOf(r.toFloat(), g.toFloat(), b.toFloat()))
    }

    fun addEffect(effectId: String, duration: Int, amplifier: Int): Boolean {
        val projectile = projectile ?: return false
        if (duration <= 0) return false
        val id = ResourceLocation.tryParse(effectId) ?: return false
        val effect = ForgeRegistries.MOB_EFFECTS.getValue(id) ?: return false
        projectile.setEffects(listOf(MobEffectInstance(effect, duration, amplifier.coerceAtLeast(0))))
        return true
    }

    fun beast() {
        projectile?.setBeast(true)
    }

    fun forceKnockback() {
        projectile?.setForceKnockback(true)
    }

    fun fireBullet(fireLevel: Number, dragonBreath: Boolean) {
        projectile?.setFireLevel(fireLevel.toInt())
        projectile?.setDragonBreath(dragonBreath)
    }

    fun setPenetrating(penetrating: Boolean) {
        projectile?.setPenetrating(penetrating)
    }

    fun isPenetrating(): Boolean = projectile?.isPenetrating() ?: false

    fun setNoGravity(noGravity: Boolean) {
        entity.isNoGravity = noGravity
    }

    fun setWireLength(length: Number) {
        taser?.wireLength = length.toInt()
    }

    fun setVolt(volt: Number) {
        taser?.volt = volt.toInt()
        (entity as? ReforgingLightningBallEntity)?.volt = volt.toInt()
    }

    fun getDamage(): Float = (entity as? PerkAwareProjectile)?.currentHitDamage
        ?: taser?.damage ?: projectile?.getDamage() ?: 0f

    fun setDamage(damage: Number) {
        projectile?.setDamage(damage.toFloat())
    }

    fun getAllyHealRatio(): Float = projectile?.getAllyHealRatio() ?: 0f

    fun setAllyHealRatio(ratio: Number) {
        projectile?.setAllyHealRatio(ratio.toFloat())
    }

    fun getVolt(): Int = taser?.volt ?: (entity as? ReforgingLightningBallEntity)?.volt ?: 0
    fun getOwner(): EntityProxy = EntityProxy((entity as? net.minecraft.world.entity.projectile.Projectile)?.owner)

    fun isZoom(): Boolean = projectile?.isZoom() ?: false
}
