package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth
import org.joml.Quaternionf

class UralRenderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : VehicleEntity, T : BasicGeoVehicleEntity {

    override fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        // wh_l1/r1/l2/r2/l3/r3 are hitbox-only cubes (read by the OBB config), not meant to be seen.
        for (name in HITBOX_ONLY_BONES) {
            model.getBone(name)?.visible = false
        }

        val rul = model.getBone("rul") ?: return

        rul.rotation.mul(Quaternionf().rotationZ(4 * Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot)))
    }

    companion object {
        private val HITBOX_ONLY_BONES = listOf("wh_l1", "wh_r1", "wh_l2", "wh_r2", "wh_l3", "wh_r3")
    }
}
