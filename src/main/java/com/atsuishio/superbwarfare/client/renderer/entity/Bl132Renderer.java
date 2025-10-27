package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Bl132Model;
import com.atsuishio.superbwarfare.entity.vehicle.Bl132Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;

import static com.atsuishio.superbwarfare.entity.vehicle.Bl132Entity.*;

public class Bl132Renderer extends VehicleRenderer<Bl132Entity> {

    public Bl132Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Bl132Model());
        this.shadowRadius = 2f;
    }

    @Override
    public void render(Bl132Entity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot())));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Bl132Entity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        String name = bone.getName();

        if (name.equals("main")) {
            bone.setHidden(hideFor1stPassengerWhileZooming);
        }

        if (name.equals("flare")) {
            bone.setHidden(animatable.getEntityData().get(COOL_DOWN) <= 75);
        }

        if (name.equals("flare2")) {
            bone.setHidden(animatable.getEntityData().get(BARREL_ANIM_2) <= 10);
        }

        if (name.equals("flare3")) {
            bone.setHidden(animatable.getEntityData().get(BARREL_ANIM_3) <= 10);
        }

        if (name.equals("flare4")) {
            bone.setHidden(animatable.getEntityData().get(BARREL_ANIM_4) <= 10);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }
}
