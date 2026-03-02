package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.SenpaiEntity;
import com.atsuishio.superbwarfare.resource.BedrockModelLoader;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class SenpaiRenderer extends EntityRenderer<SenpaiEntity> {
    public static ResourceLocation TEXTURE = new ResourceLocation(Mod.MODID, "textures/entity/senpai.png");
    public static final EulerAdditiveBlender BLENDER = new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);


    public SenpaiRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(SenpaiEntity pEntity) {
        return TEXTURE;
    }

    @Override
    public void render(SenpaiEntity pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        BedrockModel model = BedrockModelLoader.getModel(BedrockModelLoader.SENPAI_MODEL);
        if (model != null) {
            pPoseStack.pushPose();
            pPoseStack.mulPose(Axis.YP.rotationDegrees(180));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(-pEntity.getViewYRot(pPartialTick)));

            RenderType renderType = RenderType.entityCutout(getTextureLocation(pEntity));
            VertexConsumer vertexConsumer = pBuffer.getBuffer(renderType);

            var ani = pEntity.getAnimationInstance();
            ani.getContext().setPartialTick(pPartialTick);
            ani.tick();
            model.applyPose(BLENDER.blend(model.getBindPose(), ani.getPose()));

            model.renderToBuffer(pPoseStack, vertexConsumer, pPackedLight, OverlayTexture.pack(0, pEntity.hurtTime > 0 || pEntity.deathTime > 0));
            pPoseStack.popPose();
        }
    }
}
