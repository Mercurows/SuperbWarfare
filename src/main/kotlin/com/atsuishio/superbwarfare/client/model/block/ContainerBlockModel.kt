package com.atsuishio.superbwarfare.client.model.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.entity.ContainerBlockEntity
import software.bernie.geckolib.model.GeoModel

class ContainerBlockModel : GeoModel<ContainerBlockEntity>() {
    override fun getAnimationResource(animatable: ContainerBlockEntity) = loc("animations/container.animation.json")

    override fun getModelResource(animatable: ContainerBlockEntity) = loc("geo/container.geo.json")

    override fun getTextureResource(animatable: ContainerBlockEntity) = loc("textures/block/container.png")
}
