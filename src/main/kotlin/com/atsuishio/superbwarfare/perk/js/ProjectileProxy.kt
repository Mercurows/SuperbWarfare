package com.atsuishio.superbwarfare.perk.js

import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.entity.projectile.ReforgingLightningBallEntity
import com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity
import net.minecraft.world.entity.Entity

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

    fun getDamage(): Float = (entity as? ReforgingLightningBallEntity)?.currentHitDamage
        ?: taser?.damage ?: projectile?.getDamage() ?: 0f

    fun setDamage(damage: Number) {
        projectile?.setDamage(damage.toFloat())
    }
    fun getVolt(): Int = taser?.volt ?: (entity as? ReforgingLightningBallEntity)?.volt ?: 0
    fun getOwner(): EntityProxy = EntityProxy((entity as? net.minecraft.world.entity.projectile.Projectile)?.owner)

    fun isZoom(): Boolean = projectile?.isZoom() ?: false
}
