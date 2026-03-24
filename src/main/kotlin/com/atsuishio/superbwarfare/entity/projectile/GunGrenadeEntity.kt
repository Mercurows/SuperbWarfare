package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModDamageTypes.causeProjectileHitDamage
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BellBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult

open class GunGrenadeEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    constructor(type: EntityType<out GunGrenadeEntity>, world: Level) : super(type, world) {
        this.noCulling = true
    }

    constructor(entity: Entity?, level: Level, damage: Float, explosionDamage: Float, explosionRadius: Float) : super(
        ModEntities.GUN_GRENADE.get(), entity, level
    ) {
        this.noCulling = true
        this.damageValue = damage
        this.explosionDamageValue = explosionDamage
        this.explosionRadiusValue = explosionRadius
    }

    override fun getDefaultItem(): Item {
        return ModItems.GRENADE_40MM.get()
    }

    override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)
        val entity = result.entity
        val owner = this.owner
        if (owner != null && owner.vehicle != null && entity == owner.vehicle) return

        entity.forceHurt(causeProjectileHitDamage(this.level().registryAccess(), this, owner), damageValue)

        if (entity is LivingEntity) {
            entity.invulnerableTime = 0
        }

        if (this.tickCount > 0) {
            if (this.level() is ServerLevel) {
                causeExplode(result.getLocation())
            }
        }

        this.discard()
    }

    public override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)
        val resultPos = result.blockPos
        val state = this.level().getBlockState(resultPos)
        val block = state.block

        if (block is BellBlock) {
            block.attemptToRing(this.level(), resultPos, result.direction)
        }
        if (this.level() is ServerLevel) {
            causeExplode(result.getLocation())
        }
        this.discard()
    }

    override fun tick() {
        super.tick()
        smallTrail()
    }

    override fun isFastMoving(): Boolean {
        return false
    }

    override fun getModel() = BedrockModelLoader.GUN_GRENADE_MODEL

    override fun getHiddenTicks() = 1
}
