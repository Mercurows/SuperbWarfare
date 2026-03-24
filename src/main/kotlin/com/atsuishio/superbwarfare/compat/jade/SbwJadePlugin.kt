package com.atsuishio.superbwarfare.compat.jade

import com.atsuishio.superbwarfare.block.ContainerBlock
import com.atsuishio.superbwarfare.block.VehicleDeployerBlock
import com.atsuishio.superbwarfare.block.entity.VehicleDeployerBlockEntity
import com.atsuishio.superbwarfare.compat.jade.providers.*
import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.atsuishio.superbwarfare.entity.projectile.C4Entity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.IWailaCommonRegistration
import snownee.jade.api.IWailaPlugin
import snownee.jade.api.WailaPlugin

@WailaPlugin
class SbwJadePlugin : IWailaPlugin {
    override fun register(registration: IWailaCommonRegistration) {
        registration.registerBlockDataProvider(VehicleDeployerProvider, VehicleDeployerBlockEntity::class.java)
    }

    override fun registerClient(registration: IWailaClientRegistration) {
        registration.registerEntityComponent(VehicleHealthProvider, VehicleEntity::class.java)
        registration.registerEntityComponent(C4InfoProvider, C4Entity::class.java)
        registration.registerEntityComponent(DPSGeneratorProvider, DPSGeneratorEntity::class.java)
        registration.registerBlockComponent(ContainerEntityProvider, ContainerBlock::class.java)
        registration.registerBlockComponent(VehicleDeployerProvider, VehicleDeployerBlock::class.java)
    }
}
