package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.tools.localPlayer
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.GeoModel

class ProjectileEntityModel : GeoModel<ProjectileEntity>() {
    override fun getAnimationResource(entity: ProjectileEntity) = null

    override fun getModelResource(entity: ProjectileEntity) = loc("geo/projectile_entity.geo.json")

    override fun getTextureResource(entity: ProjectileEntity) = loc("textures/entity/empty.png")

    override fun setCustomAnimations(
        animatable: ProjectileEntity,
        instanceId: Long,
        animationState: AnimationState<ProjectileEntity>
    ) {
        val bone = animationProcessor.getBone("bone")
        val player = localPlayer ?: return
        bone.isHidden = animatable.position().distanceTo(player.position()) < 2 || animatable.tickCount < 1
    }
}
