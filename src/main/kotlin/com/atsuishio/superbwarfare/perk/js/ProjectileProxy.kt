package com.atsuishio.superbwarfare.perk.js

import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity
import net.minecraft.world.entity.Entity

class ProjectileProxy(private val entity: Entity) {
    private val projectile: ProjectileEntity?
        get() = entity as? ProjectileEntity

    private val taser: TaserBulletEntity?
        get() = entity as? TaserBulletEntity

    fun setRGB(r: Number, g: Number, b: Number) {
        projectile?.setRGB(floatArrayOf(r.toFloat(), g.toFloat(), b.toFloat()))
    }

    fun beast() {
        projectile?.beast()
    }

    fun fireBullet(fireLevel: Number, dragonBreath: Boolean) {
        projectile?.fireBullet(fireLevel.toInt(), dragonBreath)
    }

    fun setPenetrating(penetrating: Boolean) {
        projectile?.isPenetrating = penetrating
    }

    fun setNoGravity(noGravity: Boolean) {
        entity.isNoGravity = noGravity
    }

    fun setWireLength(length: Number) {
        taser?.wireLength = length.toInt()
    }
}
