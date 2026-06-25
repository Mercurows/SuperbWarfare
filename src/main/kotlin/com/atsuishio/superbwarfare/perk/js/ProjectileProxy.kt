package com.atsuishio.superbwarfare.perk.js

import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties
import com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity
import net.minecraft.world.entity.Entity

class ProjectileProxy(private val entity: Entity) {
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
    }

    fun isZoom(): Boolean = projectile?.isZoom() ?: false
}
