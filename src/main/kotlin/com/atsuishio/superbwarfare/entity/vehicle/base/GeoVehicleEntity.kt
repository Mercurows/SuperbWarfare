package com.atsuishio.superbwarfare.entity.vehicle.base

import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

abstract class GeoVehicleEntity(pEntityType: EntityType<*>, pLevel: Level) : VehicleEntity(pEntityType, pLevel),
    GeoEntity {

    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)
    override fun getAnimatableInstanceCache() = this.cache

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) {}

}