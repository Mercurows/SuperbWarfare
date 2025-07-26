package com.atsuishio.superbwarfare.client.model.curio;

import com.atsuishio.superbwarfare.Mod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ParachuteModel extends HumanoidModel<LivingEntity> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Mod.loc("parachute"), "main");

    private final ModelPart parachute;

    public ParachuteModel(ModelPart root) {
        super(root);
        this.parachute = root.getChild("parachute");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(new CubeDeformation(0.0f), 0.0f);
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition parachute = partdefinition.addOrReplaceChild("parachute", CubeListBuilder.create(), PartPose.offset(0.0F, -22.0F, 0.0F));

        PartDefinition sheng = parachute.addOrReplaceChild("sheng", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition cube_r1 = sheng.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(280, 330).addBox(-1.0F, -20.5F, 0.0F, 1.0F, 33.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.2256F, -11.9688F, -0.5F, 0.0F, 0.0F, -0.3491F));

        PartDefinition cube_r2 = sheng.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(344, 452).addBox(0.0F, -113.0F, -1.0F, 1.0F, 113.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.8841F, -30.357F, 1.0F, -0.2784F, 0.0038F, -0.597F));

        PartDefinition cube_r3 = sheng.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(140, 270).addBox(-1.0F, -19.5F, 0.0F, 1.0F, 40.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-30.5703F, -47.2295F, -0.5F, 0.0F, 0.0F, -0.6545F));

        PartDefinition cube_r4 = sheng.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(360, 452).addBox(-1.0F, -41.899F, -14.0343F, 1.0F, 75.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-63.2025F, -89.7566F, 0.0F, 0.3927F, 0.0F, -0.6545F));

        PartDefinition cube_r5 = sheng.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(364, 452).addBox(-1.0F, -41.899F, 13.0343F, 1.0F, 75.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-63.2025F, -89.7566F, 0.0F, -0.3927F, 0.0F, -0.6545F));

        PartDefinition cube_r6 = sheng.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(352, 452).addBox(0.0F, -41.899F, 13.0343F, 1.0F, 75.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(63.2025F, -89.7566F, 0.0F, -0.3927F, 0.0F, 0.6545F));

        PartDefinition cube_r7 = sheng.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(356, 452).addBox(0.0F, -41.899F, -14.0343F, 1.0F, 75.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(63.2025F, -89.7566F, 0.0F, 0.3927F, 0.0F, 0.6545F));

        PartDefinition cube_r8 = sheng.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(276, 330).addBox(0.0F, -20.5F, 0.0F, 1.0F, 33.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.2256F, -11.9688F, -0.5F, 0.0F, 0.0F, 0.3491F));

        PartDefinition cube_r9 = sheng.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(144, 204).addBox(0.0F, -19.5F, 0.0F, 1.0F, 40.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(30.5703F, -47.2295F, -0.5F, 0.0F, 0.0F, 0.6545F));

        PartDefinition cube_r10 = sheng.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(292, 256).addBox(3.0F, 15.5F, -2.0F, 45.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-25.5F, -47.2295F, 0.5F, 0.0F, 0.0F, 0.0F));

        PartDefinition cube_r11 = sheng.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(336, 452).addBox(-1.0F, -113.0F, 0.0F, 1.0F, 113.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8841F, -30.357F, -1.0F, 0.2784F, 0.0038F, 0.597F));

        PartDefinition cube_r12 = sheng.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(340, 452).addBox(-1.0F, -113.0F, -1.0F, 1.0F, 113.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8841F, -30.357F, 1.0F, -0.2784F, -0.0038F, 0.597F));

        PartDefinition cube_r13 = sheng.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(328, 452).addBox(0.0F, -117.0F, -1.0F, 1.0F, 117.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.8841F, -30.357F, 1.0F, -0.2744F, 0.048F, -0.4417F));

        PartDefinition cube_r14 = sheng.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(332, 452).addBox(-1.0F, -117.0F, -1.0F, 1.0F, 117.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8841F, -30.357F, 1.0F, -0.2744F, -0.048F, 0.4417F));

        PartDefinition cube_r15 = sheng.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(324, 452).addBox(-1.0F, -117.0F, 0.0F, 1.0F, 117.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8841F, -30.357F, -1.0F, 0.2744F, 0.048F, 0.4417F));

        PartDefinition cube_r16 = sheng.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(312, 452).addBox(0.0F, -120.0F, -1.0F, 1.0F, 120.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.8841F, -30.357F, 1.0F, -0.2754F, 0.0896F, -0.2784F));

        PartDefinition cube_r17 = sheng.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(316, 452).addBox(-1.0F, -120.0F, -1.0F, 1.0F, 120.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8841F, -30.357F, 1.0F, -0.2754F, -0.0896F, 0.2784F));

        PartDefinition cube_r18 = sheng.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(308, 452).addBox(-1.0F, -120.0F, 0.0F, 1.0F, 120.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8841F, -30.357F, -1.0F, 0.2754F, 0.0896F, 0.2784F));

        PartDefinition cube_r19 = sheng.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(296, 452).addBox(0.0F, -124.0F, -1.0F, 1.0F, 124.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.8841F, -30.357F, 1.0F, -0.2754F, 0.0896F, -0.1126F));

        PartDefinition cube_r20 = sheng.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(300, 452).addBox(-1.0F, -124.0F, -1.0F, 1.0F, 124.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8841F, -30.357F, 1.0F, -0.2754F, -0.0896F, 0.1126F));

        PartDefinition cube_r21 = sheng.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(292, 452).addBox(-1.0F, -124.0F, 0.0F, 1.0F, 124.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8841F, -30.357F, -1.0F, 0.2754F, 0.0896F, 0.1126F));

        PartDefinition cube_r22 = sheng.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(280, 452).addBox(0.0F, -127.0F, -1.0F, 1.0F, 127.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.8841F, -30.357F, 1.0F, -0.2754F, 0.0896F, 0.0402F));

        PartDefinition cube_r23 = sheng.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(284, 452).addBox(-1.0F, -127.0F, -1.0F, 1.0F, 127.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8841F, -30.357F, 1.0F, -0.2754F, -0.0896F, -0.0402F));

        PartDefinition cube_r24 = sheng.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(276, 452).addBox(-1.0F, -127.0F, 0.0F, 1.0F, 127.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8841F, -30.357F, -1.0F, 0.2754F, 0.0896F, -0.0402F));

        PartDefinition cube_r25 = sheng.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(272, 392).addBox(0.0F, -127.0F, 0.0F, 1.0F, 127.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.8841F, -30.357F, -1.0F, 0.2754F, -0.0896F, 0.0402F));

        PartDefinition cube_r26 = sheng.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(288, 452).addBox(0.0F, -124.0F, 0.0F, 1.0F, 124.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.8841F, -30.357F, -1.0F, 0.2754F, -0.0896F, -0.1126F));

        PartDefinition cube_r27 = sheng.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(304, 452).addBox(0.0F, -120.0F, 0.0F, 1.0F, 120.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.8841F, -30.357F, -1.0F, 0.2754F, -0.0896F, -0.2784F));

        PartDefinition cube_r28 = sheng.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs(320, 452).addBox(0.0F, -117.0F, 0.0F, 1.0F, 117.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.8841F, -30.357F, -1.0F, 0.2744F, -0.048F, -0.4417F));

        PartDefinition cube_r29 = sheng.addOrReplaceChild("cube_r29", CubeListBuilder.create().texOffs(348, 452).addBox(0.0F, -113.0F, 0.0F, 1.0F, 113.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.8841F, -30.357F, -1.0F, 0.2784F, -0.0038F, -0.597F));

        PartDefinition san = parachute.addOrReplaceChild("san", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition bone2 = san.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.673F, -33.0F, 8.0F, 2.0F, 66.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -157.327F, 0.0F));

        PartDefinition cube_r30 = bone2.addOrReplaceChild("cube_r30", CubeListBuilder.create().texOffs(464, 240).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.4052F, 33.6069F, -1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r31 = bone2.addOrReplaceChild("cube_r31", CubeListBuilder.create().texOffs(224, 464).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.4052F, -33.6069F, 1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r32 = bone2.addOrReplaceChild("cube_r32", CubeListBuilder.create().texOffs(0, 136).addBox(-4.0F, -2.5391F, -33.0F, 8.0F, 2.0F, 66.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.2065F, -0.1851F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition cube_r33 = bone2.addOrReplaceChild("cube_r33", CubeListBuilder.create().texOffs(64, 468).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.2065F, -0.1851F, 0.0F, -1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r34 = bone2.addOrReplaceChild("cube_r34", CubeListBuilder.create().texOffs(96, 468).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.2065F, -0.1851F, 0.0F, -1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r35 = bone2.addOrReplaceChild("cube_r35", CubeListBuilder.create().texOffs(32, 468).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.2065F, -0.1851F, 0.0F, 1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r36 = bone2.addOrReplaceChild("cube_r36", CubeListBuilder.create().texOffs(0, 468).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.2065F, -0.1851F, 0.0F, 1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r37 = bone2.addOrReplaceChild("cube_r37", CubeListBuilder.create().texOffs(0, 68).addBox(-4.0F, -2.5391F, -33.0F, 8.0F, 2.0F, 66.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.2065F, -0.1851F, 0.0F, 0.0F, 0.0F, 0.2618F));

        PartDefinition bone3 = san.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(148, 0).addBox(16.4413F, -5.4614F, -32.0F, 8.0F, 2.0F, 64.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -157.327F, 0.0F, 0.0F, 0.0F, 0.1745F));

        PartDefinition cube_r38 = bone3.addOrReplaceChild("cube_r38", CubeListBuilder.create().texOffs(0, 478).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(20.4413F, -1.3832F, 32.6069F, -1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r39 = bone3.addOrReplaceChild("cube_r39", CubeListBuilder.create().texOffs(224, 474).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(20.4413F, -1.3832F, -32.6069F, 1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r40 = bone3.addOrReplaceChild("cube_r40", CubeListBuilder.create().texOffs(148, 132).addBox(-4.0F, -2.5391F, -32.0F, 8.0F, 2.0F, 64.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(13.2347F, -1.9735F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition cube_r41 = bone3.addOrReplaceChild("cube_r41", CubeListBuilder.create().texOffs(192, 474).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(27.6478F, -1.9735F, -1.0F, -1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r42 = bone3.addOrReplaceChild("cube_r42", CubeListBuilder.create().texOffs(160, 474).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(13.2347F, -1.9735F, -1.0F, -1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r43 = bone3.addOrReplaceChild("cube_r43", CubeListBuilder.create().texOffs(128, 474).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(13.2347F, -1.9735F, 1.0F, 1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r44 = bone3.addOrReplaceChild("cube_r44", CubeListBuilder.create().texOffs(368, 472).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(27.6478F, -1.9735F, 1.0F, 1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r45 = bone3.addOrReplaceChild("cube_r45", CubeListBuilder.create().texOffs(148, 66).addBox(-4.0F, -2.5391F, -32.0F, 8.0F, 2.0F, 64.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(27.6478F, -1.9735F, 0.0F, 0.0F, 0.0F, 0.2618F));

        PartDefinition bone4 = san.addOrReplaceChild("bone4", CubeListBuilder.create().texOffs(148, 198).addBox(-4.0F, -3.673F, -32.0F, 8.0F, 2.0F, 64.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-20.4413F, -155.5386F, 0.0F, 0.0F, 0.0F, -0.1745F));

        PartDefinition cube_r46 = bone4.addOrReplaceChild("cube_r46", CubeListBuilder.create().texOffs(368, 482).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.4052F, 32.6069F, -1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r47 = bone4.addOrReplaceChild("cube_r47", CubeListBuilder.create().texOffs(96, 478).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.4052F, -32.6069F, 1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r48 = bone4.addOrReplaceChild("cube_r48", CubeListBuilder.create().texOffs(144, 264).addBox(-4.0F, -2.5391F, -32.0F, 8.0F, 2.0F, 64.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.2065F, -0.1851F, 0.0F, 0.0F, 0.0F, 0.2618F));

        PartDefinition cube_r49 = bone4.addOrReplaceChild("cube_r49", CubeListBuilder.create().texOffs(64, 478).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.2065F, -0.1851F, -1.0F, -1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r50 = bone4.addOrReplaceChild("cube_r50", CubeListBuilder.create().texOffs(192, 464).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.2065F, -0.1851F, -1.0F, -1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r51 = bone4.addOrReplaceChild("cube_r51", CubeListBuilder.create().texOffs(160, 464).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.2065F, -0.1851F, 1.0F, 1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r52 = bone4.addOrReplaceChild("cube_r52", CubeListBuilder.create().texOffs(32, 478).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.2065F, -0.1851F, 1.0F, 1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r53 = bone4.addOrReplaceChild("cube_r53", CubeListBuilder.create().texOffs(0, 204).addBox(-4.0F, -2.5391F, -32.0F, 8.0F, 2.0F, 64.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.2065F, -0.1851F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition bone5 = san.addOrReplaceChild("bone5", CubeListBuilder.create().texOffs(0, 270).addBox(4.6892F, 5.1413F, -31.0F, 8.0F, 2.0F, 62.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-51.4413F, -155.5386F, 0.0F, 0.0F, 0.0F, -0.3491F));

        PartDefinition cube_r54 = bone5.addOrReplaceChild("cube_r54", CubeListBuilder.create().texOffs(224, 484).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.6892F, 9.2195F, 31.6069F, -1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r55 = bone5.addOrReplaceChild("cube_r55", CubeListBuilder.create().texOffs(192, 484).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.6892F, 9.2195F, -31.6069F, 1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r56 = bone5.addOrReplaceChild("cube_r56", CubeListBuilder.create().texOffs(292, 0).addBox(-4.0F, -2.5391F, -31.0F, 8.0F, 2.0F, 62.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(15.8957F, 8.6291F, 0.0F, 0.0F, 0.0F, 0.2618F));

        PartDefinition cube_r57 = bone5.addOrReplaceChild("cube_r57", CubeListBuilder.create().texOffs(160, 484).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4827F, 8.6291F, -2.0F, -1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r58 = bone5.addOrReplaceChild("cube_r58", CubeListBuilder.create().texOffs(64, 458).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(15.8957F, 8.6291F, -2.0F, -1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r59 = bone5.addOrReplaceChild("cube_r59", CubeListBuilder.create().texOffs(32, 458).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(15.8957F, 8.6291F, 2.0F, 1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r60 = bone5.addOrReplaceChild("cube_r60", CubeListBuilder.create().texOffs(128, 484).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4827F, 8.6291F, 2.0F, 1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r61 = bone5.addOrReplaceChild("cube_r61", CubeListBuilder.create().texOffs(288, 264).addBox(-4.0F, -2.5391F, -31.0F, 8.0F, 2.0F, 62.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4827F, 8.6291F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition bone6 = san.addOrReplaceChild("bone6", CubeListBuilder.create().texOffs(292, 64).addBox(-12.6892F, 5.1413F, -31.0F, 8.0F, 2.0F, 62.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(51.4413F, -155.5386F, 0.0F, 0.0F, 0.0F, 0.3491F));

        PartDefinition cube_r62 = bone6.addOrReplaceChild("cube_r62", CubeListBuilder.create().texOffs(96, 488).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.6892F, 9.2195F, 31.6069F, -1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r63 = bone6.addOrReplaceChild("cube_r63", CubeListBuilder.create().texOffs(64, 488).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.6892F, 9.2195F, -31.6069F, 1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r64 = bone6.addOrReplaceChild("cube_r64", CubeListBuilder.create().texOffs(292, 192).addBox(-4.0F, -2.5391F, -31.0F, 8.0F, 2.0F, 62.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-15.8957F, 8.6291F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition cube_r65 = bone6.addOrReplaceChild("cube_r65", CubeListBuilder.create().texOffs(32, 488).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4827F, 8.6291F, -2.0F, -1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r66 = bone6.addOrReplaceChild("cube_r66", CubeListBuilder.create().texOffs(460, 316).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-15.8957F, 8.6291F, -2.0F, -1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r67 = bone6.addOrReplaceChild("cube_r67", CubeListBuilder.create().texOffs(96, 458).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-15.8957F, 8.6291F, 2.0F, 1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r68 = bone6.addOrReplaceChild("cube_r68", CubeListBuilder.create().texOffs(0, 488).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4827F, 8.6291F, 2.0F, 1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r69 = bone6.addOrReplaceChild("cube_r69", CubeListBuilder.create().texOffs(292, 128).addBox(-4.0F, -2.5391F, -31.0F, 8.0F, 2.0F, 62.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4827F, 8.6291F, 0.0F, 0.0F, 0.0F, 0.2618F));

        PartDefinition bone7 = san.addOrReplaceChild("bone7", CubeListBuilder.create().texOffs(288, 328).addBox(-19.1642F, 21.2278F, -30.0F, 8.0F, 2.0F, 60.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(84.4413F, -155.5386F, 0.0F, 0.0F, 0.0F, 0.5236F));

        PartDefinition cube_r70 = bone7.addOrReplaceChild("cube_r70", CubeListBuilder.create().texOffs(160, 494).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-15.1642F, 25.306F, 30.6069F, -1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r71 = bone7.addOrReplaceChild("cube_r71", CubeListBuilder.create().texOffs(128, 494).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-15.1642F, 25.306F, -30.6069F, 1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r72 = bone7.addOrReplaceChild("cube_r72", CubeListBuilder.create().texOffs(0, 334).addBox(-4.0F, -2.5391F, -30.0F, 8.0F, 2.0F, 60.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-22.3707F, 24.7157F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition cube_r73 = bone7.addOrReplaceChild("cube_r73", CubeListBuilder.create().texOffs(368, 492).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.9576F, 24.7157F, -3.0F, -1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r74 = bone7.addOrReplaceChild("cube_r74", CubeListBuilder.create().texOffs(200, 454).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-22.3707F, 24.7157F, -3.0F, -1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r75 = bone7.addOrReplaceChild("cube_r75", CubeListBuilder.create().texOffs(168, 454).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-22.3707F, 24.7157F, 3.0F, 1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r76 = bone7.addOrReplaceChild("cube_r76", CubeListBuilder.create().texOffs(492, 316).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.9576F, 24.7157F, 3.0F, 1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r77 = bone7.addOrReplaceChild("cube_r77", CubeListBuilder.create().texOffs(140, 330).addBox(-4.0F, -2.5391F, -30.0F, 8.0F, 2.0F, 60.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.9576F, 24.7157F, 0.0F, 0.0F, 0.0F, 0.2618F));

        PartDefinition bone8 = san.addOrReplaceChild("bone8", CubeListBuilder.create().texOffs(276, 390).addBox(-4.0F, -3.673F, -30.0F, 8.0F, 2.0F, 60.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-58.8583F, -141.556F, 0.0F, 0.0F, 0.0F, -0.5236F));

        PartDefinition cube_r78 = bone8.addOrReplaceChild("cube_r78", CubeListBuilder.create().texOffs(224, 494).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.4052F, 30.6069F, -1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r79 = bone8.addOrReplaceChild("cube_r79", CubeListBuilder.create().texOffs(192, 494).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.4052F, -30.6069F, 1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r80 = bone8.addOrReplaceChild("cube_r80", CubeListBuilder.create().texOffs(0, 396).addBox(-4.0F, -2.5391F, -30.0F, 8.0F, 2.0F, 60.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.2065F, -0.1851F, 0.0F, 0.0F, 0.0F, 0.2618F));

        PartDefinition cube_r81 = bone8.addOrReplaceChild("cube_r81", CubeListBuilder.create().texOffs(128, 464).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.2065F, -0.1851F, -3.0F, -1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r82 = bone8.addOrReplaceChild("cube_r82", CubeListBuilder.create().texOffs(0, 458).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.2065F, -0.1851F, -3.0F, -1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r83 = bone8.addOrReplaceChild("cube_r83", CubeListBuilder.create().texOffs(232, 454).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.2065F, -0.1851F, 3.0F, 1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r84 = bone8.addOrReplaceChild("cube_r84", CubeListBuilder.create().texOffs(368, 462).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.2065F, -0.1851F, 3.0F, 1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r85 = bone8.addOrReplaceChild("cube_r85", CubeListBuilder.create().texOffs(136, 392).addBox(-4.0F, -2.5391F, -30.0F, 8.0F, 2.0F, 60.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.2065F, -0.1851F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition bone9 = san.addOrReplaceChild("bone9", CubeListBuilder.create().texOffs(428, 256).addBox(-2.7139F, -3.9092F, -29.0F, 8.0F, 2.0F, 58.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-76.5002F, -128.7789F, 0.0F, 0.0F, 0.0F, -0.6981F));

        PartDefinition cube_r86 = bone9.addOrReplaceChild("cube_r86", CubeListBuilder.create().texOffs(64, 498).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.2861F, 0.169F, 29.6069F, -1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r87 = bone9.addOrReplaceChild("cube_r87", CubeListBuilder.create().texOffs(32, 498).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.2861F, 0.169F, -29.6069F, 1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r88 = bone9.addOrReplaceChild("cube_r88", CubeListBuilder.create().texOffs(432, 60).addBox(-4.0F, -2.5391F, -29.0F, 8.0F, 2.0F, 58.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.4927F, -0.4213F, 0.0F, 0.0F, 0.0F, 0.2618F));

        PartDefinition cube_r89 = bone9.addOrReplaceChild("cube_r89", CubeListBuilder.create().texOffs(0, 498).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.9204F, -0.4213F, -4.0F, -1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r90 = bone9.addOrReplaceChild("cube_r90", CubeListBuilder.create().texOffs(432, 240).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.4927F, -0.4213F, -4.0F, -1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r91 = bone9.addOrReplaceChild("cube_r91", CubeListBuilder.create().texOffs(428, 316).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.4927F, -0.4213F, 4.0F, 1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r92 = bone9.addOrReplaceChild("cube_r92", CubeListBuilder.create().texOffs(496, 240).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.9204F, -0.4213F, 4.0F, 1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r93 = bone9.addOrReplaceChild("cube_r93", CubeListBuilder.create().texOffs(412, 390).addBox(-4.0F, -1.0F, -29.0F, 8.0F, 2.0F, 58.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.5752F, 2.1255F, 0.0F, 0.0F, 0.0F, -1.0472F));

        PartDefinition cube_r94 = bone9.addOrReplaceChild("cube_r94", CubeListBuilder.create().texOffs(432, 0).addBox(-4.0F, -1.0F, -29.0F, 8.0F, 2.0F, 58.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.3187F, -1.908F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition bone10 = san.addOrReplaceChild("bone10", CubeListBuilder.create().texOffs(432, 120).addBox(-5.2861F, -3.9092F, -29.0F, 8.0F, 2.0F, 58.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(76.5002F, -128.7789F, 0.0F, 0.0F, 0.0F, 0.6981F));

        PartDefinition cube_r95 = bone10.addOrReplaceChild("cube_r95", CubeListBuilder.create().texOffs(160, 504).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2861F, 0.169F, 29.6069F, -1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r96 = bone10.addOrReplaceChild("cube_r96", CubeListBuilder.create().texOffs(128, 504).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2861F, 0.169F, -29.6069F, 1.1781F, 0.0F, 0.0F));

        PartDefinition cube_r97 = bone10.addOrReplaceChild("cube_r97", CubeListBuilder.create().texOffs(412, 450).addBox(-4.0F, -2.5391F, -29.0F, 8.0F, 2.0F, 58.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.4927F, -0.4213F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition cube_r98 = bone10.addOrReplaceChild("cube_r98", CubeListBuilder.create().texOffs(368, 502).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.9204F, -0.4213F, -4.0F, -1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r99 = bone10.addOrReplaceChild("cube_r99", CubeListBuilder.create().texOffs(136, 454).addBox(-4.0F, -31.4597F, 10.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.4927F, -0.4213F, -4.0F, -1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r100 = bone10.addOrReplaceChild("cube_r100", CubeListBuilder.create().texOffs(368, 452).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.4927F, -0.4213F, 4.0F, 1.1781F, 0.0F, -0.2618F));

        PartDefinition cube_r101 = bone10.addOrReplaceChild("cube_r101", CubeListBuilder.create().texOffs(96, 498).addBox(-4.0F, -31.4597F, -18.2827F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.9204F, -0.4213F, 4.0F, 1.1781F, 0.0F, 0.2618F));

        PartDefinition cube_r102 = bone10.addOrReplaceChild("cube_r102", CubeListBuilder.create().texOffs(424, 328).addBox(-4.0F, -1.0F, -29.0F, 8.0F, 2.0F, 58.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.5752F, 2.1255F, 0.0F, 0.0F, 0.0F, 1.0472F));

        PartDefinition cube_r103 = bone10.addOrReplaceChild("cube_r103", CubeListBuilder.create().texOffs(432, 180).addBox(-4.0F, -1.0F, -29.0F, 8.0F, 2.0F, 58.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.3187F, -1.908F, 0.0F, 0.0F, 0.0F, 0.2618F));

        return LayerDefinition.create(meshdefinition, 1024, 1024);
    }

    @Override
    public void setupAnim(@NotNull LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        parachute.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}