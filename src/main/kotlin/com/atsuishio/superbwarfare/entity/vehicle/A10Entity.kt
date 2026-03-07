package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import com.atsuishio.superbwarfare.init.ModParticleTypes
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class A10Entity(type: EntityType<A10Entity>, world: Level) : GeoVehicleEntity(type, world) {

    override fun baseTick() {
        super.baseTick()

        //测试粒子
//        if (level().isClientSide) {
//            val pos1 = Vec3(6.6, 1.66, -0.68)
//            val pos2 = Vec3(-6.6, 1.66, -0.68)
//
//            val worldPosition1 = transformPosition(
//                getVehicleTransform(1f),
//                pos1.x, pos1.y, pos1.z
//            )
//
//            val worldPosition2 = transformPosition(
//                getVehicleTransform(1f),
//                pos2.x, pos2.y, pos2.z
//            )
//
//            addRandomParticle(ParticleTypes.CLOUD, Vec3(worldPosition1.x, worldPosition1.y, worldPosition1.z), 0f, level(), 0f, 1)
//            addRandomParticle(ParticleTypes.CLOUD, Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z), 0f, level(), 0f, 1)
//        }

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
