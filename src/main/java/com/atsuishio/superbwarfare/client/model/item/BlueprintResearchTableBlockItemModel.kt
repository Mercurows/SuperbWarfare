package com.atsuishio.superbwarfare.client.model.item

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.item.BlueprintResearchTableBlockItem
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.GeoModel

class BlueprintResearchTableBlockItemModel : GeoModel<BlueprintResearchTableBlockItem>() {
    override fun getModelResource(animatable: BlueprintResearchTableBlockItem?): ResourceLocation? =
        Mod.loc("geo/blueprint_research_table.geo.json")

    override fun getTextureResource(animatable: BlueprintResearchTableBlockItem?): ResourceLocation =
        Mod.loc("textures/block/blueprint_research_table.png")

    override fun getAnimationResource(animatable: BlueprintResearchTableBlockItem?): ResourceLocation? = null
}