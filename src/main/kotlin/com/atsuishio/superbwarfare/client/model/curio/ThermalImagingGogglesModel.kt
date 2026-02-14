package com.atsuishio.superbwarfare.client.model.curio

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.google.common.collect.ImmutableList
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.world.entity.LivingEntity

@Suppress("unused", "UnusedVariable", "LocalVariableName")
class ThermalImagingGogglesModel(root: ModelPart) : HumanoidModel<LivingEntity>(root) {
    val bone: ModelPart = root.getChild("bone")

    override fun renderToBuffer(
        poseStack: PoseStack,
        vertexConsumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float
    ) {
        bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    }

    override fun headParts(): Iterable<ModelPart> {
        return ImmutableList.of(this.head)
    }

    override fun bodyParts(): Iterable<ModelPart> {
        return ImmutableList.of()
    }

    companion object {
        // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
        val LAYER_LOCATION: ModelLayerLocation = ModelLayerLocation(loc("thermal_imaging_goggles"), "main")
        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = createMesh(CubeDeformation(0.0f), 0.0f)
            val partdefinition = meshdefinition.root

            val bone = partdefinition.addOrReplaceChild(
                "bone",
                CubeListBuilder.create().texOffs(0, 8)
                    .addBox(-5.0f, -6.5f, -6.0f, 10.0f, 3.0f, 5.0f, CubeDeformation(0.0f))
                    .texOffs(24, 16).addBox(-5.0f, -7.0f, -7.0f, 10.0f, 1.0f, 4.0f, CubeDeformation(0.0f))
                    .texOffs(24, 21).addBox(-4.0f, -7.5f, -6.0f, 8.0f, 1.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(24, 24).addBox(-4.0f, -4.0f, -6.0f, 8.0f, 1.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(0, 16).addBox(-1.0f, -8.75f, -5.0f, 2.0f, 4.0f, 10.0f, CubeDeformation(0.0f))
                    .texOffs(0, 0).addBox(-5.0f, -6.0f, -1.0f, 10.0f, 2.0f, 6.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 24.0f, 0.0f)
            )

            val cube_r1 = bone.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create().texOffs(0, 30)
                    .addBox(-1.0f, -1.0f, -3.0f, 2.0f, 2.0f, 5.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-1.3587f, -5.25f, -7.9322f, 0.1872f, 0.1841f, 0.8027f)
            )

            val cube_r2 = bone.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create().texOffs(24, 27)
                    .addBox(-1.0f, -1.0f, -3.0f, 2.0f, 2.0f, 5.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(1.3587f, -5.25f, -7.9322f, 0.1872f, -0.1841f, -0.8027f)
            )

            val cube_r3 = bone.addOrReplaceChild(
                "cube_r3",
                CubeListBuilder.create().texOffs(26, 34)
                    .addBox(-1.0f, -1.0f, -2.0f, 2.0f, 2.0f, 4.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(4.1599f, -5.25f, -7.1816f, 0.4971f, -0.445f, -0.9001f)
            )

            val cube_r4 = bone.addOrReplaceChild(
                "cube_r4",
                CubeListBuilder.create().texOffs(14, 34)
                    .addBox(-1.0f, -1.0f, -2.0f, 2.0f, 2.0f, 4.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-4.1599f, -5.25f, -7.1816f, 0.4971f, 0.445f, 0.9001f)
            )

            val cube_r5 = bone.addOrReplaceChild(
                "cube_r5",
                CubeListBuilder.create().texOffs(32, 0)
                    .addBox(-5.0f, -2.0f, -2.0f, 5.0f, 3.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.3473f, -4.5f, -5.3622f, 0.0f, 0.2618f, 0.0f)
            )

            val cube_r6 = bone.addOrReplaceChild(
                "cube_r6",
                CubeListBuilder.create().texOffs(30, 8)
                    .addBox(0.0f, -2.0f, -2.0f, 5.0f, 3.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-0.3473f, -4.5f, -5.3622f, 0.0f, -0.2618f, 0.0f)
            )

            val cube_r7 = bone.addOrReplaceChild(
                "cube_r7",
                CubeListBuilder.create().texOffs(0, 37)
                    .addBox(4.25f, -1.5f, -1.5f, 1.0f, 2.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(14, 30).addBox(-5.25f, -1.5f, -1.5f, 1.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, -5.0f, -3.5f, 0.7854f, 0.0f, 0.0f)
            )

            val Head = partdefinition.addOrReplaceChild(
                "Head",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, CubeDeformation(0.0f))
                    .texOffs(32, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, CubeDeformation(0.5f)),
                PartPose.offset(0.0f, 24.0f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 64, 64)
        }
    }
}