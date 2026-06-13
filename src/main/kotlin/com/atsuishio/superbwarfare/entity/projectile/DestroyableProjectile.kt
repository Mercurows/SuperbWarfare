package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier.Companion.createDefaultModifier
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.level.Level

abstract class DestroyableProjectile : FastThrowableProjectile {
    constructor(pEntityType: EntityType<out ThrowableItemProjectile>, pLevel: Level) : super(pEntityType, pLevel)

    constructor(pEntityType: EntityType<out ThrowableItemProjectile>, pShooter: Entity?, pLevel: Level) : super(pEntityType, pLevel) {
        this.owner = pShooter
        if (pShooter != null) {
            this.setPos(pShooter.x, pShooter.eyeY - 0.1, pShooter.z)
        }
    }

    var health by HEALTH

    override fun isPickable() = !this.isRemoved

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val entity = source.directEntity
        if (entity is DestroyableProjectile && this.javaClass == entity.javaClass) {
            if (this.owner == entity.owner) return false
        }

        this.health -= DAMAGE_MODIFIER.compute(this, source, amount)
        return super.hurt(source, amount)
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(HEALTH, this.maxHealth)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("Health")) {
            health = compound.getFloat("Health")
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putFloat("Health", health)
    }

    open val maxHealth get() = 30F

    companion object {
        @JvmField
        val HEALTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(DestroyableProjectile::class.java, EntityDataSerializers.FLOAT)

        private val DAMAGE_MODIFIER = createDefaultModifier()
    }

    override fun tick() {
        super.tick()

        if (health <= 0) {
            if (!level().isClientSide) {
                causeExplode(position())
            }
            this.discard()
        }
    }
}
