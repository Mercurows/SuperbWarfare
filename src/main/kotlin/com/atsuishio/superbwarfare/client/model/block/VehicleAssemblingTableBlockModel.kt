package com.atsuishio.superbwarfare.client.model.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.entity.VehicleAssemblingTableBlockEntity
import software.bernie.geckolib.model.GeoModel

class VehicleAssemblingTableBlockModel : GeoModel<VehicleAssemblingTableBlockEntity>() {
    override fun getAnimationResource(animatable: VehicleAssemblingTableBlockEntity) = null

    override fun getModelResource(animatable: VehicleAssemblingTableBlockEntity) =
        loc("geo/vehicle_assembling_table.geo.json")

    override fun getTextureResource(animatable: VehicleAssemblingTableBlockEntity) =
        loc("textures/block/vehicle_assembling_table.png")
}
