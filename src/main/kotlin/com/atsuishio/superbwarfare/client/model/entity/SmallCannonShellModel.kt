package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.GeoModel

class SmallCannonShellModel : GeoModel<SmallCannonShellEntity>() {
    override fun getAnimationResource(entity: SmallCannonShellEntity) = null

    override fun getModelResource(entity: SmallCannonShellEntity) = loc("geo/small_cannon_shell.geo.json")

    override fun getTextureResource(entity: SmallCannonShellEntity) = loc("textures/entity/small_cannon_shell.png")

    override fun setCustomAnimations(
        animatable: SmallCannonShellEntity,
        instanceId: Long,
        animationState: AnimationState<SmallCannonShellEntity>
    ) {
        val bone = animationProcessor.getBone("bone")
        bone.scaleY = (1 + 2 * animatable.deltaMovement.length()).toFloat()
    }
}
