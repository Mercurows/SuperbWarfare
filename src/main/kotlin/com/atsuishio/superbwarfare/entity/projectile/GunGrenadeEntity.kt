package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.EntityHitResult

open class GunGrenadeEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    constructor(type: EntityType<out GunGrenadeEntity>, world: Level) : super(type, world)

    constructor(entity: Entity?, level: Level, damage: Float, explosionDamage: Float, explosionRadius: Float) : super(
        ModEntities.GUN_GRENADE.get(), entity, level
    ) {
        this.damageValue = damage
        this.explosionDamageValue = explosionDamage
        this.explosionRadiusValue = explosionRadius
    }

    override fun getDefaultItem(): Item {
        return ModItems.GRENADE_40MM.get()
    }

    override fun afterHitEntity(result: EntityHitResult) {
        if (this.tickCount > 0) {
            super.afterHitEntity(result)
        }
        this.discard()
    }

    override fun tick() {
        super.tick()
        shellTrail()
    }

    override fun isFastMoving(): Boolean {
        return false
    }

    override fun getHiddenTicks() = 1
}
