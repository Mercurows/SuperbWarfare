package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.FoodRainHandler
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3

open class FoodRainAtomBombEntity(type: EntityType<out FoodRainAtomBombEntity>, level: Level) :
    FastThrowableProjectile(type, level), BasicGeoProjectileEntity {
    private val animation = if (level.isClientSide) BasicProjectileAnimationInstance(
        this, loop = true, loopAnimation = "animation.projectile.flight"
    ) else null

    init {
        explosionDestroyValue = false
        lifeValue = 1200
    }

    override fun getDefaultItem() = ModItems.FOOD_RAIN_ATOM_BOMB.get()

    override fun getAnimationInstance() = animation

    override fun getCustomGravity() = 0.06f

    override fun canPassThroughFluid() = false

    override fun isFastMoving() = false

    override fun getDamage() = 0f

    override fun getExplosionDamage() = 0f

    override fun hasExplosionDestroy() = false

    override fun onHitBlock(result: BlockHitResult) = causeExplode(result.location)

    override fun afterHitBlock(result: BlockHitResult) = causeExplode(result.location)

    override fun onHitEntity(result: EntityHitResult) = causeExplode(result.location)

    override fun afterHitEntity(result: EntityHitResult) = causeExplode(result.location)

    override fun afterShieldBlock(result: EntityHitResult) = causeExplode(result.location)

    override fun causeExplode(vec3: Vec3, splitNormal: Vec3?) {
        val level = level() as? ServerLevel ?: return
        if (exploded) return
        exploded = true
        FoodRainHandler.detonate(level, vec3)
        discard()
    }
}
