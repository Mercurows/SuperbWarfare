package com.atsuishio.superbwarfare.entity.vehicle.base

import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

abstract class GeckoArtilleryEntity(pEntityType: EntityType<*>, pLevel: Level) : ArtilleryEntity(pEntityType, pLevel),
    GeoEntity {

        // TODO 临时文件，换完就扔（恼）

    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)
    override fun getAnimatableInstanceCache() = this.cache

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) {}

}