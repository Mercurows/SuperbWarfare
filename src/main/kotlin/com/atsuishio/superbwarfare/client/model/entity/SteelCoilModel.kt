package com.atsuishio.superbwarfare.client.model.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.SteelCoilEntity
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.GeoModel

class SteelCoilModel : GeoModel<SteelCoilEntity>() {
    override fun getModelResource(animatable: SteelCoilEntity?): ResourceLocation = loc("geo/steel_coil.geo.json")

    override fun getTextureResource(animatable: SteelCoilEntity?): ResourceLocation =
        loc("textures/entity/steel_coil.png")

    override fun getAnimationResource(animatable: SteelCoilEntity?): ResourceLocation? = null
}