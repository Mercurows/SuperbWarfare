package com.atsuishio.superbwarfare.client.model.block

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import software.bernie.geckolib.model.GeoModel

class BlueprintResearchTableBlockModel : GeoModel<BlueprintResearchTableBlockEntity>() {
    override fun getModelResource(animatable: BlueprintResearchTableBlockEntity) =
        Mod.loc("geo/blueprint_research_table.geo.json")

    override fun getTextureResource(animatable: BlueprintResearchTableBlockEntity) =
        Mod.loc("textures/block/blueprint_research_table.png")

    override fun getAnimationResource(animatable: BlueprintResearchTableBlockEntity) = null
}