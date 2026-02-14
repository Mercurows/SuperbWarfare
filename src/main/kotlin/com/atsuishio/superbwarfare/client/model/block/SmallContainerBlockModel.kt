package com.atsuishio.superbwarfare.client.model.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.entity.SmallContainerBlockEntity
import software.bernie.geckolib.model.GeoModel

class SmallContainerBlockModel : GeoModel<SmallContainerBlockEntity>() {
    override fun getAnimationResource(animatable: SmallContainerBlockEntity) =
        loc("animations/small_container.animation.json")

    override fun getModelResource(animatable: SmallContainerBlockEntity) = loc("geo/small_container.geo.json")

    override fun getTextureResource(animatable: SmallContainerBlockEntity) =
        if (animatable.lootTableSeed != 0L && animatable.lootTableSeed % 205 == 0L) {
            loc("textures/block/small_container_sui.png")
        } else {
            loc("textures/block/small_container.png")
        }
}
