package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class DroneRenderer(manager: EntityRendererProvider.Context) : BasicVehicleRenderer(manager) {
    override fun transformCustomModelPart(
        entity: VehicleEntity,
        instance: VehicleModelInstance,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks)
        if (entity.isWreck) return

        val propeller = instance.getBone("move_wingFL")
        val propeller2 = instance.getBone("move_wingFR")
        val propeller3 = instance.getBone("move_wingBL")
        val propeller4 = instance.getBone("move_wingBR")

        val rot = Mth.lerp(partialTicks, entity.propellerRotO, entity.propellerRot)

        propeller?.rotation?.rotationY(rot)
        propeller2?.rotation?.rotationY(rot)
        propeller3?.rotation?.rotationY(rot)
        propeller4?.rotation?.rotationY(rot)
    }
}
