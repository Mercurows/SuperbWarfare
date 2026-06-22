package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.unaryMinus
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.*

class SodayoPickUpTowEntity(type: EntityType<SodayoPickUpTowEntity>, world: Level) : VehicleEntity(type, world) {
    override fun baseTick() {
        super.baseTick()
        if (decoyInputDown) {
            horn()
        }
    }

    override fun vehicleShoot(living: LivingEntity?, uuid: UUID?, targetPos: Vec3?) {
        super.vehicleShoot(living, uuid, targetPos)

        val barrelVector = getBarrelVector(1f)
        val pos = getShootPos(living, 1f).add(barrelVector.scale(-0.5))
        val ab = AABB(pos, pos).inflate(0.75).move(barrelVector.scale(-2.0)).expandTowards(barrelVector.scale(-5.0))

        // 尾焰伤害
        for (entity in level().getEntities(EntityTypeTest.forClass(Entity::class.java), ab) {
            target -> target !== this && target !== getFirstPassenger() && target.vehicle == null }
        ) {
            entity.hurt(
                    ModDamageTypes.causeBurnDamage(entity.level().registryAccess(), living),
                    30 - 2 * entity.distanceTo(this)
            )
            val force = 4 - 0.7 * entity.distanceTo(this)
            entity.push(-force * barrelVector.x, -force * barrelVector.y, -force * barrelVector.z)
        }

        // 粒子效果
        val level = this.level()
        if (level is ServerLevel) {
            ParticleTool.spawnMediumCannonMuzzleParticles(-barrelVector, pos, level, this)
            ParticleTool.spawnMediumCannonMuzzleParticles(barrelVector, pos, level, this)
        }
    }
}
