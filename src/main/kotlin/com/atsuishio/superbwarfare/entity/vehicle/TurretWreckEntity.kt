package com.atsuishio.superbwarfare.entity.vehicle

import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.level.Level
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class TurretWreckEntity(type: EntityType<TurretWreckEntity>, world: Level) : Entity(type, world), GeoEntity {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)
    var wreckageType: String = "superbwarfare:lav_150"

    var roll = 0f
    var prevRoll = 0f

    override fun defineSynchedData() {
    }

    override fun baseTick() {
        super.baseTick()

        this.move(MoverType.SELF, this.deltaMovement)
        var f = 0.98f
        if (this.onGround()) {
            val pos = this.blockPosBelowThatAffectsMyMovement
            f = level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98f
        }

        this.deltaMovement = deltaMovement.multiply(f.toDouble(), 0.98, f.toDouble())
        if (this.onGround()) {
            this.deltaMovement = deltaMovement.multiply(1.0, -0.9, 1.0)
        }
    }

    override fun readAdditionalSaveData(pCompound: CompoundTag?) {
    }

    override fun addAdditionalSaveData(pCompound: CompoundTag?) {
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) {
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = this.cache

    fun getRoll(tickDelta: Float) = Mth.lerp(tickDelta, prevRoll, roll)

    fun getYaw(tickDelta: Float) = Mth.lerp(tickDelta, yRotO, yRot)

    fun getPitch(tickDelta: Float) = Mth.lerp(tickDelta, xRotO, xRot)
}
