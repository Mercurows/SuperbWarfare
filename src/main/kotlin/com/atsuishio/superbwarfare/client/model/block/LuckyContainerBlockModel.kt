package com.atsuishio.superbwarfare.client.model.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.entity.LuckyContainerBlockEntity
import software.bernie.geckolib.model.GeoModel

class LuckyContainerBlockModel : GeoModel<LuckyContainerBlockEntity>() {
    override fun getAnimationResource(animatable: LuckyContainerBlockEntity) =
        loc("animations/container.animation.json")

    override fun getModelResource(animatable: LuckyContainerBlockEntity) = loc("geo/container.geo.json")

    override fun getTextureResource(animatable: LuckyContainerBlockEntity) = loc("textures/block/lucky_container.png")
}
