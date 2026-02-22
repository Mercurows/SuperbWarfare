package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.Ptkm1rEntity
import net.minecraft.util.Mth
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.GeoModel

class Ptkm1rModel : GeoModel<Ptkm1rEntity>() {
    override fun setCustomAnimations(
        animatable: Ptkm1rEntity,
        instanceId: Long,
        animationState: AnimationState<Ptkm1rEntity>
    ) {
        super.setCustomAnimations(animatable, instanceId, animationState)

        val body = animationProcessor.getBone("body")
        if (body != null) {
            body.rotY = -animatable.yRot * Mth.DEG_TO_RAD
        }

        val zhu = animationProcessor.getBone("zhu2")
        if (zhu != null) {
            zhu.rotX = 0.5f * Mth.lerp(
                animationState.partialTick,
                animatable.xRotO,
                animatable.xRot
            ) * Mth.DEG_TO_RAD
        }
    }

    override fun getAnimationResource(entity: Ptkm1rEntity) = loc("animations/ptkm_1r.animation.json")

    override fun getModelResource(entity: Ptkm1rEntity) = loc("geo/ptkm_1r.geo.json")

    override fun getTextureResource(entity: Ptkm1rEntity) = loc("textures/entity/ptkm_1r.png")
}
