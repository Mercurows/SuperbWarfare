package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Mle1934Model;
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;

import static com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity.COOL_DOWN;
import static com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity.RIGHT_BARREL_ANIM;

public class Mle1934Renderer extends VehicleRenderer<Mle1934Entity> {

    public Mle1934Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Mle1934Model());
        this.shadowRadius = 2f;
    }

    @Override
    public RenderType getRenderType(Mle1934Entity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void render(Mle1934Entity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot())));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Mle1934Entity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        String name = bone.getName();

        if (name.equals("bone")) {
            bone.setHidden(hideFor1stPassengerWhileZooming);
        }

        if (name.equals("flare2")) {
            bone.setHidden(animatable.getEntityData().get(COOL_DOWN) <= 64);
        }

        if (name.equals("flare")) {
            bone.setHidden(animatable.getEntityData().get(RIGHT_BARREL_ANIM) <= 10);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }
}
