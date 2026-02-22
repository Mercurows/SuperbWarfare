package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.GeoModel

class CannonShellEntityModel : GeoModel<CannonShellEntity>() {
    override fun getAnimationResource(entity: CannonShellEntity) = loc("animations/cannon_shell.animation.json")

    override fun getModelResource(entity: CannonShellEntity) = loc("geo/cannon_shell.geo.json")

    override fun getTextureResource(entity: CannonShellEntity) = loc("textures/entity/cannon_shell.png")

    override fun setCustomAnimations(
        animatable: CannonShellEntity,
        instanceId: Long,
        animationState: AnimationState<CannonShellEntity>
    ) {
        val bone = animationProcessor.getBone("bone")
        bone.isHidden = animatable.tickCount <= 1
    }
}
