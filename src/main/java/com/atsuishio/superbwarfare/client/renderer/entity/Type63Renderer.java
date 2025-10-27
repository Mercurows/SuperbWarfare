package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Type63Model;
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;

import static com.atsuishio.superbwarfare.entity.vehicle.Type63Entity.LOADED_AMMO;


public class Type63Renderer extends VehicleRenderer<Type63Entity> {

    public Type63Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Type63Model());
        this.shadowRadius = 0.8f;
    }

    @Override
    public void render(Type63Entity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot())));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Type63Entity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        String name = bone.getName();

        if (name.equals("wheel1")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.leftWheelRotO, animatable.getLeftWheelRot()));
        }
        if (name.equals("wheel2")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.rightWheelRotO, animatable.getRightWheelRot()));
        }

        if (name.equals("main")) {
            bone.setRotY(turretYRot * Mth.DEG_TO_RAD);
        }

        if (name.equals("paotou")) {
            bone.setRotX(-turretXRot * Mth.DEG_TO_RAD);
        }

        if (name.equals("shoulunx")) {
            bone.setRotX(-turretXRot * 3);
        }

        if (name.equals("shouluny")) {
            bone.setRotZ(-turretYRot * 6);
        }

        if (name.startsWith("shell") && name.length() > 5) {
            var items = animatable.getEntityData().get(LOADED_AMMO);
            int i = Integer.parseInt(name.substring(5));
            bone.setHidden(items.get(i) == -1);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
