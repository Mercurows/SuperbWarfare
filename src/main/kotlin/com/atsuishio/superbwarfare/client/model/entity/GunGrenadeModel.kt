package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.GunGrenadeEntity
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.GeoModel

class GunGrenadeModel : GeoModel<GunGrenadeEntity>() {
    override fun getAnimationResource(entity: GunGrenadeEntity) = loc("animations/cannon_shell.animation.json")

    override fun getModelResource(entity: GunGrenadeEntity) = loc("geo/cannon_shell.geo.json")

    override fun getTextureResource(entity: GunGrenadeEntity) = loc("textures/entity/cannon_shell.png")

    override fun setCustomAnimations(
        animatable: GunGrenadeEntity,
        instanceId: Long,
        animationState: AnimationState<GunGrenadeEntity>
    ) {
        val bone = animationProcessor.getBone("bone")
        bone.scaleX = 0.2f
        bone.scaleY = 0.2f
        bone.scaleZ = 0.2f
    }
}
