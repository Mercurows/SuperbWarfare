package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.Hpj11HeatLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.Hpj11Layer;
import com.atsuishio.superbwarfare.client.model.entity.Hpj11Model;
import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity.ANIM_TIME;

public class Hpj11Renderer extends GeoEntityRenderer<Hpj11Entity> {

    public Hpj11Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Hpj11Model());
        this.shadowRadius = 1.5f;
        this.addRenderLayer(new Hpj11Layer(this));
        this.addRenderLayer(new Hpj11HeatLayer(this));
    }

    @Override
    public RenderType getRenderType(Hpj11Entity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, Hpj11Entity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void render(Hpj11Entity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn) {
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Hpj11Entity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        String name = bone.getName();

        if (name.equals("root")) {
            Player player = Minecraft.getInstance().player;
            bone.setHidden(ClientEventHandler.zoomVehicle && animatable.getFirstPassenger() == player);
        }

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

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }
}
