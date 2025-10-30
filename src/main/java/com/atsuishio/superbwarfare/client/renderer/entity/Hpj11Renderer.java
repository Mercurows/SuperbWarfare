package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.Hpj11HeatLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.Hpj11Layer;
import com.atsuishio.superbwarfare.client.model.entity.Hpj11Model;
import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.GeoBone;

import static com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity.ANIM_TIME;

public class Hpj11Renderer extends VehicleRenderer<Hpj11Entity> {

    public Hpj11Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Hpj11Model());
        this.shadowRadius = 1.5f;
        this.addRenderLayer(new Hpj11Layer(this));
        this.addRenderLayer(new Hpj11HeatLayer(this));
    }

    @Override
    public void vehicleAxis(Hpj11Entity entityIn, PoseStack poseStack, float entityYaw, float partialTicks) {
        Vec3 root = new Vec3(0, entityIn.rotateYOffset(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Hpj11Entity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        String name = bone.getName();

        if (name.equals("paotiroll")) {
            bone.setRotY(-Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot()) * Mth.DEG_TO_RAD);
        }

        if (name.equals("radar2")) {
            Player player = Minecraft.getInstance().player;
            bone.setHidden(animatable.getFirstPassenger() == player && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON);
        }

        if (name.equals("roll") || name.equals("rdr") || name.equals("rdr2")) {
            bone.setRotX(-Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot()) * Mth.DEG_TO_RAD);
        }

        if (name.equals("paoguanroll")) {
            bone.setRotZ(-Mth.lerp(partialTick, animatable.gunRotO, animatable.getGunRot()));
        }

        if (name.equals("flare")) {
            bone.setHidden(animatable.getEntityData().get(ANIM_TIME) == 0);
            bone.setScaleX((float) (2 + 0.8 * (Math.random() - 0.5)));
            bone.setScaleY((float) (2 + 0.8 * (Math.random() - 0.5)));
            bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
