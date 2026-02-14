package com.atsuishio.superbwarfare.client.model.curio

import com.atsuishio.superbwarfare.Mod.Companion.loc
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
class ParachuteModel(root: ModelPart) : HumanoidModel<LivingEntity>(root) {
    private val parachute: ModelPart = root.getChild("parachute")

    override fun setupAnim(
        entity: LivingEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
    }

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
        parachute.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    }

    companion object {
        // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
        val LAYER_LOCATION: ModelLayerLocation = ModelLayerLocation(loc("parachute"), "main")

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = createMesh(CubeDeformation(0.0f), 0.0f)
            val partdefinition = meshdefinition.root

            val parachute =
                partdefinition.addOrReplaceChild("parachute", CubeListBuilder.create(), PartPose.offset(0f, -22f, 0f))

            val sheng = parachute.addOrReplaceChild("sheng", CubeListBuilder.create(), PartPose.offset(0f, 0f, 0f))

            val cube_r1 = sheng.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create().texOffs(280, 330).addBox(-1f, -20.5f, 0f, 1f, 33f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-11.2256f, -11.9688f, -0.5f, 0f, 0f, -0.3491f)
            )

            val cube_r2 = sheng.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create().texOffs(344, 452).addBox(0f, -113f, -1f, 1f, 113f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-18.8841f, -30.357f, 1f, -0.2784f, 0.0038f, -0.597f)
            )

            val cube_r3 = sheng.addOrReplaceChild(
                "cube_r3",
                CubeListBuilder.create().texOffs(140, 270).addBox(-1f, -19.5f, 0f, 1f, 40f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-30.5703f, -47.2295f, -0.5f, 0f, 0f, -0.6545f)
            )

            val cube_r4 = sheng.addOrReplaceChild(
                "cube_r4",
                CubeListBuilder.create().texOffs(360, 452)
                    .addBox(-1f, -41.899f, -14.0343f, 1f, 75f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-63.2025f, -89.7566f, 0f, 0.3927f, 0f, -0.6545f)
            )

            val cube_r5 = sheng.addOrReplaceChild(
                "cube_r5",
                CubeListBuilder.create().texOffs(364, 452)
                    .addBox(-1f, -41.899f, 13.0343f, 1f, 75f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-63.2025f, -89.7566f, 0f, -0.3927f, 0f, -0.6545f)
            )

            val cube_r6 = sheng.addOrReplaceChild(
                "cube_r6",
                CubeListBuilder.create().texOffs(352, 452)
                    .addBox(0f, -41.899f, 13.0343f, 1f, 75f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(63.2025f, -89.7566f, 0f, -0.3927f, 0f, 0.6545f)
            )

            val cube_r7 = sheng.addOrReplaceChild(
                "cube_r7",
                CubeListBuilder.create().texOffs(356, 452)
                    .addBox(0f, -41.899f, -14.0343f, 1f, 75f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(63.2025f, -89.7566f, 0f, 0.3927f, 0f, 0.6545f)
            )

            val cube_r8 = sheng.addOrReplaceChild(
                "cube_r8",
                CubeListBuilder.create().texOffs(276, 330).addBox(0f, -20.5f, 0f, 1f, 33f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(11.2256f, -11.9688f, -0.5f, 0f, 0f, 0.3491f)
            )

            val cube_r9 = sheng.addOrReplaceChild(
                "cube_r9",
                CubeListBuilder.create().texOffs(144, 204).addBox(0f, -19.5f, 0f, 1f, 40f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(30.5703f, -47.2295f, -0.5f, 0f, 0f, 0.6545f)
            )

            val cube_r10 = sheng.addOrReplaceChild(
                "cube_r10",
                CubeListBuilder.create().texOffs(292, 256).addBox(3f, 15.5f, -2f, 45f, 2f, 3f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-25.5f, -47.2295f, 0.5f, 0f, 0f, 0f)
            )

            val cube_r11 = sheng.addOrReplaceChild(
                "cube_r11",
                CubeListBuilder.create().texOffs(336, 452).addBox(-1f, -113f, 0f, 1f, 113f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(18.8841f, -30.357f, -1f, 0.2784f, 0.0038f, 0.597f)
            )

            val cube_r12 = sheng.addOrReplaceChild(
                "cube_r12",
                CubeListBuilder.create().texOffs(340, 452).addBox(-1f, -113f, -1f, 1f, 113f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(18.8841f, -30.357f, 1f, -0.2784f, -0.0038f, 0.597f)
            )

            val cube_r13 = sheng.addOrReplaceChild(
                "cube_r13",
                CubeListBuilder.create().texOffs(328, 452).addBox(0f, -117f, -1f, 1f, 117f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-18.8841f, -30.357f, 1f, -0.2744f, 0.048f, -0.4417f)
            )

            val cube_r14 = sheng.addOrReplaceChild(
                "cube_r14",
                CubeListBuilder.create().texOffs(332, 452).addBox(-1f, -117f, -1f, 1f, 117f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(18.8841f, -30.357f, 1f, -0.2744f, -0.048f, 0.4417f)
            )

            val cube_r15 = sheng.addOrReplaceChild(
                "cube_r15",
                CubeListBuilder.create().texOffs(324, 452).addBox(-1f, -117f, 0f, 1f, 117f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(18.8841f, -30.357f, -1f, 0.2744f, 0.048f, 0.4417f)
            )

            val cube_r16 = sheng.addOrReplaceChild(
                "cube_r16",
                CubeListBuilder.create().texOffs(312, 452).addBox(0f, -120f, -1f, 1f, 120f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-18.8841f, -30.357f, 1f, -0.2754f, 0.0896f, -0.2784f)
            )

            val cube_r17 = sheng.addOrReplaceChild(
                "cube_r17",
                CubeListBuilder.create().texOffs(316, 452).addBox(-1f, -120f, -1f, 1f, 120f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(18.8841f, -30.357f, 1f, -0.2754f, -0.0896f, 0.2784f)
            )

            val cube_r18 = sheng.addOrReplaceChild(
                "cube_r18",
                CubeListBuilder.create().texOffs(308, 452).addBox(-1f, -120f, 0f, 1f, 120f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(18.8841f, -30.357f, -1f, 0.2754f, 0.0896f, 0.2784f)
            )

            val cube_r19 = sheng.addOrReplaceChild(
                "cube_r19",
                CubeListBuilder.create().texOffs(296, 452).addBox(0f, -124f, -1f, 1f, 124f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-18.8841f, -30.357f, 1f, -0.2754f, 0.0896f, -0.1126f)
            )

            val cube_r20 = sheng.addOrReplaceChild(
                "cube_r20",
                CubeListBuilder.create().texOffs(300, 452).addBox(-1f, -124f, -1f, 1f, 124f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(18.8841f, -30.357f, 1f, -0.2754f, -0.0896f, 0.1126f)
            )

            val cube_r21 = sheng.addOrReplaceChild(
                "cube_r21",
                CubeListBuilder.create().texOffs(292, 452).addBox(-1f, -124f, 0f, 1f, 124f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(18.8841f, -30.357f, -1f, 0.2754f, 0.0896f, 0.1126f)
            )

            val cube_r22 = sheng.addOrReplaceChild(
                "cube_r22",
                CubeListBuilder.create().texOffs(280, 452).addBox(0f, -127f, -1f, 1f, 127f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-18.8841f, -30.357f, 1f, -0.2754f, 0.0896f, 0.0402f)
            )

            val cube_r23 = sheng.addOrReplaceChild(
                "cube_r23",
                CubeListBuilder.create().texOffs(284, 452).addBox(-1f, -127f, -1f, 1f, 127f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(18.8841f, -30.357f, 1f, -0.2754f, -0.0896f, -0.0402f)
            )

            val cube_r24 = sheng.addOrReplaceChild(
                "cube_r24",
                CubeListBuilder.create().texOffs(276, 452).addBox(-1f, -127f, 0f, 1f, 127f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(18.8841f, -30.357f, -1f, 0.2754f, 0.0896f, -0.0402f)
            )

            val cube_r25 = sheng.addOrReplaceChild(
                "cube_r25",
                CubeListBuilder.create().texOffs(272, 392).addBox(0f, -127f, 0f, 1f, 127f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-18.8841f, -30.357f, -1f, 0.2754f, -0.0896f, 0.0402f)
            )

            val cube_r26 = sheng.addOrReplaceChild(
                "cube_r26",
                CubeListBuilder.create().texOffs(288, 452).addBox(0f, -124f, 0f, 1f, 124f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-18.8841f, -30.357f, -1f, 0.2754f, -0.0896f, -0.1126f)
            )

            val cube_r27 = sheng.addOrReplaceChild(
                "cube_r27",
                CubeListBuilder.create().texOffs(304, 452).addBox(0f, -120f, 0f, 1f, 120f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-18.8841f, -30.357f, -1f, 0.2754f, -0.0896f, -0.2784f)
            )

            val cube_r28 = sheng.addOrReplaceChild(
                "cube_r28",
                CubeListBuilder.create().texOffs(320, 452).addBox(0f, -117f, 0f, 1f, 117f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-18.8841f, -30.357f, -1f, 0.2744f, -0.048f, -0.4417f)
            )

            val cube_r29 = sheng.addOrReplaceChild(
                "cube_r29",
                CubeListBuilder.create().texOffs(348, 452).addBox(0f, -113f, 0f, 1f, 113f, 1f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-18.8841f, -30.357f, -1f, 0.2784f, -0.0038f, -0.597f)
            )

            val san = parachute.addOrReplaceChild("san", CubeListBuilder.create(), PartPose.offset(0f, 0f, 0f))

            val bone2 = san.addOrReplaceChild(
                "bone2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4f, -3.673f, -33f, 8f, 2f, 66f, CubeDeformation(0f)),
                PartPose.offset(0f, -157.327f, 0f)
            )

            val cube_r30 = bone2.addOrReplaceChild(
                "cube_r30",
                CubeListBuilder.create().texOffs(464, 240).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0.4052f, 33.6069f, -1.1781f, 0f, 0f)
            )

            val cube_r31 = bone2.addOrReplaceChild(
                "cube_r31",
                CubeListBuilder.create().texOffs(224, 464).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0.4052f, -33.6069f, 1.1781f, 0f, 0f)
            )

            val cube_r32 = bone2.addOrReplaceChild(
                "cube_r32",
                CubeListBuilder.create().texOffs(0, 136).addBox(-4f, -2.5391f, -33f, 8f, 2f, 66f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-7.2065f, -0.1851f, 0f, 0f, 0f, -0.2618f)
            )

            val cube_r33 = bone2.addOrReplaceChild(
                "cube_r33",
                CubeListBuilder.create().texOffs(64, 468)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(7.2065f, -0.1851f, 0f, -1.1781f, 0f, 0.2618f)
            )

            val cube_r34 = bone2.addOrReplaceChild(
                "cube_r34",
                CubeListBuilder.create().texOffs(96, 468)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-7.2065f, -0.1851f, 0f, -1.1781f, 0f, -0.2618f)
            )

            val cube_r35 = bone2.addOrReplaceChild(
                "cube_r35",
                CubeListBuilder.create().texOffs(32, 468)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-7.2065f, -0.1851f, 0f, 1.1781f, 0f, -0.2618f)
            )

            val cube_r36 = bone2.addOrReplaceChild(
                "cube_r36",
                CubeListBuilder.create().texOffs(0, 468)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(7.2065f, -0.1851f, 0f, 1.1781f, 0f, 0.2618f)
            )

            val cube_r37 = bone2.addOrReplaceChild(
                "cube_r37",
                CubeListBuilder.create().texOffs(0, 68).addBox(-4f, -2.5391f, -33f, 8f, 2f, 66f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(7.2065f, -0.1851f, 0f, 0f, 0f, 0.2618f)
            )

            val bone3 = san.addOrReplaceChild(
                "bone3",
                CubeListBuilder.create().texOffs(148, 0)
                    .addBox(16.4413f, -5.4614f, -32f, 8f, 2f, 64f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, -157.327f, 0f, 0f, 0f, 0.1745f)
            )

            val cube_r38 = bone3.addOrReplaceChild(
                "cube_r38",
                CubeListBuilder.create().texOffs(0, 478).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(20.4413f, -1.3832f, 32.6069f, -1.1781f, 0f, 0f)
            )

            val cube_r39 = bone3.addOrReplaceChild(
                "cube_r39",
                CubeListBuilder.create().texOffs(224, 474).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(20.4413f, -1.3832f, -32.6069f, 1.1781f, 0f, 0f)
            )

            val cube_r40 = bone3.addOrReplaceChild(
                "cube_r40",
                CubeListBuilder.create().texOffs(148, 132)
                    .addBox(-4f, -2.5391f, -32f, 8f, 2f, 64f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(13.2347f, -1.9735f, 0f, 0f, 0f, -0.2618f)
            )

            val cube_r41 = bone3.addOrReplaceChild(
                "cube_r41",
                CubeListBuilder.create().texOffs(192, 474)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(27.6478f, -1.9735f, -1f, -1.1781f, 0f, 0.2618f)
            )

            val cube_r42 = bone3.addOrReplaceChild(
                "cube_r42",
                CubeListBuilder.create().texOffs(160, 474)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(13.2347f, -1.9735f, -1f, -1.1781f, 0f, -0.2618f)
            )

            val cube_r43 = bone3.addOrReplaceChild(
                "cube_r43",
                CubeListBuilder.create().texOffs(128, 474)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(13.2347f, -1.9735f, 1f, 1.1781f, 0f, -0.2618f)
            )

            val cube_r44 = bone3.addOrReplaceChild(
                "cube_r44",
                CubeListBuilder.create().texOffs(368, 472)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(27.6478f, -1.9735f, 1f, 1.1781f, 0f, 0.2618f)
            )

            val cube_r45 = bone3.addOrReplaceChild(
                "cube_r45",
                CubeListBuilder.create().texOffs(148, 66).addBox(-4f, -2.5391f, -32f, 8f, 2f, 64f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(27.6478f, -1.9735f, 0f, 0f, 0f, 0.2618f)
            )

            val bone4 = san.addOrReplaceChild(
                "bone4",
                CubeListBuilder.create().texOffs(148, 198).addBox(-4f, -3.673f, -32f, 8f, 2f, 64f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-20.4413f, -155.5386f, 0f, 0f, 0f, -0.1745f)
            )

            val cube_r46 = bone4.addOrReplaceChild(
                "cube_r46",
                CubeListBuilder.create().texOffs(368, 482).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0.4052f, 32.6069f, -1.1781f, 0f, 0f)
            )

            val cube_r47 = bone4.addOrReplaceChild(
                "cube_r47",
                CubeListBuilder.create().texOffs(96, 478).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0.4052f, -32.6069f, 1.1781f, 0f, 0f)
            )

            val cube_r48 = bone4.addOrReplaceChild(
                "cube_r48",
                CubeListBuilder.create().texOffs(144, 264)
                    .addBox(-4f, -2.5391f, -32f, 8f, 2f, 64f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(7.2065f, -0.1851f, 0f, 0f, 0f, 0.2618f)
            )

            val cube_r49 = bone4.addOrReplaceChild(
                "cube_r49",
                CubeListBuilder.create().texOffs(64, 478)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-7.2065f, -0.1851f, -1f, -1.1781f, 0f, -0.2618f)
            )

            val cube_r50 = bone4.addOrReplaceChild(
                "cube_r50",
                CubeListBuilder.create().texOffs(192, 464)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(7.2065f, -0.1851f, -1f, -1.1781f, 0f, 0.2618f)
            )

            val cube_r51 = bone4.addOrReplaceChild(
                "cube_r51",
                CubeListBuilder.create().texOffs(160, 464)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(7.2065f, -0.1851f, 1f, 1.1781f, 0f, 0.2618f)
            )

            val cube_r52 = bone4.addOrReplaceChild(
                "cube_r52",
                CubeListBuilder.create().texOffs(32, 478)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-7.2065f, -0.1851f, 1f, 1.1781f, 0f, -0.2618f)
            )

            val cube_r53 = bone4.addOrReplaceChild(
                "cube_r53",
                CubeListBuilder.create().texOffs(0, 204).addBox(-4f, -2.5391f, -32f, 8f, 2f, 64f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-7.2065f, -0.1851f, 0f, 0f, 0f, -0.2618f)
            )

            val bone5 = san.addOrReplaceChild(
                "bone5",
                CubeListBuilder.create().texOffs(0, 270)
                    .addBox(4.6892f, 5.1413f, -31f, 8f, 2f, 62f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-51.4413f, -155.5386f, 0f, 0f, 0f, -0.3491f)
            )

            val cube_r54 = bone5.addOrReplaceChild(
                "cube_r54",
                CubeListBuilder.create().texOffs(224, 484).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(8.6892f, 9.2195f, 31.6069f, -1.1781f, 0f, 0f)
            )

            val cube_r55 = bone5.addOrReplaceChild(
                "cube_r55",
                CubeListBuilder.create().texOffs(192, 484).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(8.6892f, 9.2195f, -31.6069f, 1.1781f, 0f, 0f)
            )

            val cube_r56 = bone5.addOrReplaceChild(
                "cube_r56",
                CubeListBuilder.create().texOffs(292, 0).addBox(-4f, -2.5391f, -31f, 8f, 2f, 62f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(15.8957f, 8.6291f, 0f, 0f, 0f, 0.2618f)
            )

            val cube_r57 = bone5.addOrReplaceChild(
                "cube_r57",
                CubeListBuilder.create().texOffs(160, 484)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(1.4827f, 8.6291f, -2f, -1.1781f, 0f, -0.2618f)
            )

            val cube_r58 = bone5.addOrReplaceChild(
                "cube_r58",
                CubeListBuilder.create().texOffs(64, 458)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(15.8957f, 8.6291f, -2f, -1.1781f, 0f, 0.2618f)
            )

            val cube_r59 = bone5.addOrReplaceChild(
                "cube_r59",
                CubeListBuilder.create().texOffs(32, 458)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(15.8957f, 8.6291f, 2f, 1.1781f, 0f, 0.2618f)
            )

            val cube_r60 = bone5.addOrReplaceChild(
                "cube_r60",
                CubeListBuilder.create().texOffs(128, 484)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(1.4827f, 8.6291f, 2f, 1.1781f, 0f, -0.2618f)
            )

            val cube_r61 = bone5.addOrReplaceChild(
                "cube_r61",
                CubeListBuilder.create().texOffs(288, 264)
                    .addBox(-4f, -2.5391f, -31f, 8f, 2f, 62f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(1.4827f, 8.6291f, 0f, 0f, 0f, -0.2618f)
            )

            val bone6 = san.addOrReplaceChild(
                "bone6",
                CubeListBuilder.create().texOffs(292, 64)
                    .addBox(-12.6892f, 5.1413f, -31f, 8f, 2f, 62f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(51.4413f, -155.5386f, 0f, 0f, 0f, 0.3491f)
            )

            val cube_r62 = bone6.addOrReplaceChild(
                "cube_r62",
                CubeListBuilder.create().texOffs(96, 488).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-8.6892f, 9.2195f, 31.6069f, -1.1781f, 0f, 0f)
            )

            val cube_r63 = bone6.addOrReplaceChild(
                "cube_r63",
                CubeListBuilder.create().texOffs(64, 488).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-8.6892f, 9.2195f, -31.6069f, 1.1781f, 0f, 0f)
            )

            val cube_r64 = bone6.addOrReplaceChild(
                "cube_r64",
                CubeListBuilder.create().texOffs(292, 192)
                    .addBox(-4f, -2.5391f, -31f, 8f, 2f, 62f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-15.8957f, 8.6291f, 0f, 0f, 0f, -0.2618f)
            )

            val cube_r65 = bone6.addOrReplaceChild(
                "cube_r65",
                CubeListBuilder.create().texOffs(32, 488)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-1.4827f, 8.6291f, -2f, -1.1781f, 0f, 0.2618f)
            )

            val cube_r66 = bone6.addOrReplaceChild(
                "cube_r66",
                CubeListBuilder.create().texOffs(460, 316)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-15.8957f, 8.6291f, -2f, -1.1781f, 0f, -0.2618f)
            )

            val cube_r67 = bone6.addOrReplaceChild(
                "cube_r67",
                CubeListBuilder.create().texOffs(96, 458)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-15.8957f, 8.6291f, 2f, 1.1781f, 0f, -0.2618f)
            )

            val cube_r68 = bone6.addOrReplaceChild(
                "cube_r68",
                CubeListBuilder.create().texOffs(0, 488)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-1.4827f, 8.6291f, 2f, 1.1781f, 0f, 0.2618f)
            )

            val cube_r69 = bone6.addOrReplaceChild(
                "cube_r69",
                CubeListBuilder.create().texOffs(292, 128)
                    .addBox(-4f, -2.5391f, -31f, 8f, 2f, 62f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-1.4827f, 8.6291f, 0f, 0f, 0f, 0.2618f)
            )

            val bone7 = san.addOrReplaceChild(
                "bone7",
                CubeListBuilder.create().texOffs(288, 328)
                    .addBox(-19.1642f, 21.2278f, -30f, 8f, 2f, 60f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(84.4413f, -155.5386f, 0f, 0f, 0f, 0.5236f)
            )

            val cube_r70 = bone7.addOrReplaceChild(
                "cube_r70",
                CubeListBuilder.create().texOffs(160, 494).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-15.1642f, 25.306f, 30.6069f, -1.1781f, 0f, 0f)
            )

            val cube_r71 = bone7.addOrReplaceChild(
                "cube_r71",
                CubeListBuilder.create().texOffs(128, 494).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-15.1642f, 25.306f, -30.6069f, 1.1781f, 0f, 0f)
            )

            val cube_r72 = bone7.addOrReplaceChild(
                "cube_r72",
                CubeListBuilder.create().texOffs(0, 334).addBox(-4f, -2.5391f, -30f, 8f, 2f, 60f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-22.3707f, 24.7157f, 0f, 0f, 0f, -0.2618f)
            )

            val cube_r73 = bone7.addOrReplaceChild(
                "cube_r73",
                CubeListBuilder.create().texOffs(368, 492)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-7.9576f, 24.7157f, -3f, -1.1781f, 0f, 0.2618f)
            )

            val cube_r74 = bone7.addOrReplaceChild(
                "cube_r74",
                CubeListBuilder.create().texOffs(200, 454)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-22.3707f, 24.7157f, -3f, -1.1781f, 0f, -0.2618f)
            )

            val cube_r75 = bone7.addOrReplaceChild(
                "cube_r75",
                CubeListBuilder.create().texOffs(168, 454)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-22.3707f, 24.7157f, 3f, 1.1781f, 0f, -0.2618f)
            )

            val cube_r76 = bone7.addOrReplaceChild(
                "cube_r76",
                CubeListBuilder.create().texOffs(492, 316)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-7.9576f, 24.7157f, 3f, 1.1781f, 0f, 0.2618f)
            )

            val cube_r77 = bone7.addOrReplaceChild(
                "cube_r77",
                CubeListBuilder.create().texOffs(140, 330)
                    .addBox(-4f, -2.5391f, -30f, 8f, 2f, 60f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-7.9576f, 24.7157f, 0f, 0f, 0f, 0.2618f)
            )

            val bone8 = san.addOrReplaceChild(
                "bone8",
                CubeListBuilder.create().texOffs(276, 390).addBox(-4f, -3.673f, -30f, 8f, 2f, 60f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-58.8583f, -141.556f, 0f, 0f, 0f, -0.5236f)
            )

            val cube_r78 = bone8.addOrReplaceChild(
                "cube_r78",
                CubeListBuilder.create().texOffs(224, 494).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0.4052f, 30.6069f, -1.1781f, 0f, 0f)
            )

            val cube_r79 = bone8.addOrReplaceChild(
                "cube_r79",
                CubeListBuilder.create().texOffs(192, 494).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(0f, 0.4052f, -30.6069f, 1.1781f, 0f, 0f)
            )

            val cube_r80 = bone8.addOrReplaceChild(
                "cube_r80",
                CubeListBuilder.create().texOffs(0, 396).addBox(-4f, -2.5391f, -30f, 8f, 2f, 60f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(7.2065f, -0.1851f, 0f, 0f, 0f, 0.2618f)
            )

            val cube_r81 = bone8.addOrReplaceChild(
                "cube_r81",
                CubeListBuilder.create().texOffs(128, 464)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-7.2065f, -0.1851f, -3f, -1.1781f, 0f, -0.2618f)
            )

            val cube_r82 = bone8.addOrReplaceChild(
                "cube_r82",
                CubeListBuilder.create().texOffs(0, 458)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(7.2065f, -0.1851f, -3f, -1.1781f, 0f, 0.2618f)
            )

            val cube_r83 = bone8.addOrReplaceChild(
                "cube_r83",
                CubeListBuilder.create().texOffs(232, 454)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(7.2065f, -0.1851f, 3f, 1.1781f, 0f, 0.2618f)
            )

            val cube_r84 = bone8.addOrReplaceChild(
                "cube_r84",
                CubeListBuilder.create().texOffs(368, 462)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-7.2065f, -0.1851f, 3f, 1.1781f, 0f, -0.2618f)
            )

            val cube_r85 = bone8.addOrReplaceChild(
                "cube_r85",
                CubeListBuilder.create().texOffs(136, 392)
                    .addBox(-4f, -2.5391f, -30f, 8f, 2f, 60f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-7.2065f, -0.1851f, 0f, 0f, 0f, -0.2618f)
            )

            val bone9 = san.addOrReplaceChild(
                "bone9",
                CubeListBuilder.create().texOffs(428, 256)
                    .addBox(-2.7139f, -3.9092f, -29f, 8f, 2f, 58f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-76.5002f, -128.7789f, 0f, 0f, 0f, -0.6981f)
            )

            val cube_r86 = bone9.addOrReplaceChild(
                "cube_r86",
                CubeListBuilder.create().texOffs(64, 498).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(1.2861f, 0.169f, 29.6069f, -1.1781f, 0f, 0f)
            )

            val cube_r87 = bone9.addOrReplaceChild(
                "cube_r87",
                CubeListBuilder.create().texOffs(32, 498).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(1.2861f, 0.169f, -29.6069f, 1.1781f, 0f, 0f)
            )

            val cube_r88 = bone9.addOrReplaceChild(
                "cube_r88",
                CubeListBuilder.create().texOffs(432, 60).addBox(-4f, -2.5391f, -29f, 8f, 2f, 58f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(8.4927f, -0.4213f, 0f, 0f, 0f, 0.2618f)
            )

            val cube_r89 = bone9.addOrReplaceChild(
                "cube_r89",
                CubeListBuilder.create().texOffs(0, 498)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-5.9204f, -0.4213f, -4f, -1.1781f, 0f, -0.2618f)
            )

            val cube_r90 = bone9.addOrReplaceChild(
                "cube_r90",
                CubeListBuilder.create().texOffs(432, 240)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(8.4927f, -0.4213f, -4f, -1.1781f, 0f, 0.2618f)
            )

            val cube_r91 = bone9.addOrReplaceChild(
                "cube_r91",
                CubeListBuilder.create().texOffs(428, 316)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(8.4927f, -0.4213f, 4f, 1.1781f, 0f, 0.2618f)
            )

            val cube_r92 = bone9.addOrReplaceChild(
                "cube_r92",
                CubeListBuilder.create().texOffs(496, 240)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-5.9204f, -0.4213f, 4f, 1.1781f, 0f, -0.2618f)
            )

            val cube_r93 = bone9.addOrReplaceChild(
                "cube_r93",
                CubeListBuilder.create().texOffs(412, 390).addBox(-4f, -1f, -29f, 8f, 2f, 58f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-11.5752f, 2.1255f, 0f, 0f, 0f, -1.0472f)
            )

            val cube_r94 = bone9.addOrReplaceChild(
                "cube_r94",
                CubeListBuilder.create().texOffs(432, 0).addBox(-4f, -1f, -29f, 8f, 2f, 58f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-6.3187f, -1.908f, 0f, 0f, 0f, -0.2618f)
            )

            val bone10 = san.addOrReplaceChild(
                "bone10",
                CubeListBuilder.create().texOffs(432, 120)
                    .addBox(-5.2861f, -3.9092f, -29f, 8f, 2f, 58f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(76.5002f, -128.7789f, 0f, 0f, 0f, 0.6981f)
            )

            val cube_r95 = bone10.addOrReplaceChild(
                "cube_r95",
                CubeListBuilder.create().texOffs(160, 504).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-1.2861f, 0.169f, 29.6069f, -1.1781f, 0f, 0f)
            )

            val cube_r96 = bone10.addOrReplaceChild(
                "cube_r96",
                CubeListBuilder.create().texOffs(128, 504).addBox(-4f, -1f, -4f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-1.2861f, 0.169f, -29.6069f, 1.1781f, 0f, 0f)
            )

            val cube_r97 = bone10.addOrReplaceChild(
                "cube_r97",
                CubeListBuilder.create().texOffs(412, 450)
                    .addBox(-4f, -2.5391f, -29f, 8f, 2f, 58f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-8.4927f, -0.4213f, 0f, 0f, 0f, -0.2618f)
            )

            val cube_r98 = bone10.addOrReplaceChild(
                "cube_r98",
                CubeListBuilder.create().texOffs(368, 502)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(5.9204f, -0.4213f, -4f, -1.1781f, 0f, 0.2618f)
            )

            val cube_r99 = bone10.addOrReplaceChild(
                "cube_r99",
                CubeListBuilder.create().texOffs(136, 454)
                    .addBox(-4f, -31.4597f, 10.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-8.4927f, -0.4213f, -4f, -1.1781f, 0f, -0.2618f)
            )

            val cube_r100 = bone10.addOrReplaceChild(
                "cube_r100",
                CubeListBuilder.create().texOffs(368, 452)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(-8.4927f, -0.4213f, 4f, 1.1781f, 0f, -0.2618f)
            )

            val cube_r101 = bone10.addOrReplaceChild(
                "cube_r101",
                CubeListBuilder.create().texOffs(96, 498)
                    .addBox(-4f, -31.4597f, -18.2827f, 8f, 2f, 8f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(5.9204f, -0.4213f, 4f, 1.1781f, 0f, 0.2618f)
            )

            val cube_r102 = bone10.addOrReplaceChild(
                "cube_r102",
                CubeListBuilder.create().texOffs(424, 328).addBox(-4f, -1f, -29f, 8f, 2f, 58f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(11.5752f, 2.1255f, 0f, 0f, 0f, 1.0472f)
            )

            val cube_r103 = bone10.addOrReplaceChild(
                "cube_r103",
                CubeListBuilder.create().texOffs(432, 180).addBox(-4f, -1f, -29f, 8f, 2f, 58f, CubeDeformation(0f)),
                PartPose.offsetAndRotation(6.3187f, -1.908f, 0f, 0f, 0f, 0.2618f)
            )

            return LayerDefinition.create(meshdefinition, 1024, 1024)
        }
    }
}