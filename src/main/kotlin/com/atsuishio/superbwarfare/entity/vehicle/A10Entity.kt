package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModParticleTypes
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

open class A10Entity(type: EntityType<A10Entity>, world: Level) : VehicleEntity(type, world), BasicGeoVehicleEntity {
    val anim: VehicleAnimationInstance<A10Entity>? =
        if (world.isClientSide) VehicleAnimationInstance(this) else null
    override fun getAnimationInstance() = anim
    override fun getAnimation() = ANIM
    companion object {
        val ANIM = Mod.loc("animation/bedrock/vehicle/a_10a.animation.json")
    }

    override fun onEngine1Damaged(pos: Vec3) {
        if (level().isClientSide) {
            val random = 2 * (this.random.nextFloat() - 0.5f)
            addRandomParticle(ModParticleTypes.FIRE_STAR.get(), pos, 0f, level(), 0.25f, 5)
            addRandomParticle(ParticleTypes.LARGE_SMOKE, pos, 0.5f, level(), 0.001f, 1)
            addRandomParticle(
                CustomCloudOption(
                    1f,
                    0.25f,
                    0f,
                    (240 + 40 * random).toInt(),
                    2.5f + 0.5f * random,
                    -0.07f,
                    true,
                    true
                ), pos, 0.5f, level(), 1.5f, 1
            )
        }
    }

    override fun onEngine2Damaged(pos: Vec3) {
        if (level().isClientSide) {
            val random = 2 * (this.random.nextFloat() - 0.5f)
            addRandomParticle(ModParticleTypes.FIRE_STAR.get(), pos, 0f, level(), 0.25f, 5)
            addRandomParticle(ParticleTypes.LARGE_SMOKE, pos, 0.5f, level(), 0.001f, 1)
            addRandomParticle(
                CustomCloudOption(
                    1f,
                    0.25f,
                    0f,
                    (240 + 40 * random).toInt(),
                    2.5f + 0.5f * random,
                    -0.07f,
                    true,
                    true
                ), pos, 0.5f, level(), 1.5f, 1
            )
        }
    }
}
