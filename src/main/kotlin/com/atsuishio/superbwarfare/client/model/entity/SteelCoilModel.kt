package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.living.SteelCoilEntity
import net.minecraft.util.Mth
import software.bernie.geckolib.core.animation.AnimationState
import software.bernie.geckolib.model.GeoModel

class SteelCoilModel : GeoModel<SteelCoilEntity>() {
    override fun getModelResource(animatable: SteelCoilEntity) = loc("geo/steel_coil.geo.json")

    override fun getTextureResource(animatable: SteelCoilEntity) = loc("textures/entity/steel_coil.png")

    override fun getAnimationResource(animatable: SteelCoilEntity) = null

    override fun setCustomAnimations(
        animatable: SteelCoilEntity,
        instanceId: Long,
        animationState: AnimationState<SteelCoilEntity>
    ) {
        super.setCustomAnimations(animatable, instanceId, animationState)
        val main = animationProcessor.getBone("main")
        main.rotX = -animatable.getRotaion(animationState.partialTick) * Mth.DEG_TO_RAD
    }
}