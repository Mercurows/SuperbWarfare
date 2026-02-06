package com.atsuishio.superbwarfare.client.model.block

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.GeoModel

class BlueprintResearchTableBlockModel : GeoModel<BlueprintResearchTableBlockEntity>() {
    override fun getModelResource(animatable: BlueprintResearchTableBlockEntity?): ResourceLocation =
        Mod.loc("geo/blueprint_research_table.geo.json")

    override fun getTextureResource(animatable: BlueprintResearchTableBlockEntity?): ResourceLocation =
        Mod.loc("textures/block/blueprint_research_table.png")

    override fun getAnimationResource(animatable: BlueprintResearchTableBlockEntity?): ResourceLocation? = null
}