package com.atsuishio.superbwarfare.client.renderer.special;

import com.atsuishio.superbwarfare.capability.living.PhosphorusFireCapability;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Quaternionf;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class PhosphorusFireRenderer {

    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void onRenderCurseFlame(RenderLivingEvent.Pre<LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
        LivingEntity entity = event.getEntity();
        if (!PhosphorusFireCapability.of(entity).isOnFire())
            return;

        var stack = event.getPoseStack();

        var sprite1 = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/soul_fire_0")).sprite();
        var sprite2 = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/soul_fire_1")).sprite();

        stack.pushPose();
        float size = entity.getBbWidth() * 1.6F;
        stack.scale(size, size, size);

        float hwRatio = entity.getBbHeight() / size;
        float xOffset = 0.5F;
        float yOffset = (float) (entity.getY() - entity.getBoundingBox().minY);
        float zOffset = 0.0F;

        var camera = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        stack.mulPose(new Quaternionf(0, camera.y, 0, camera.w));

        stack.translate(0.0F, 0.0F, hwRatio * 0.02F);

        int i = 0;
        var vertexconsumer = event.getMultiBufferSource().getBuffer(Sheets.cutoutBlockSheet());

        for (var pose = stack.last(); hwRatio > 0.0F; ++i) {
            var atlasSprite = i % 2 == 0 ? sprite1 : sprite2;
            float u0 = atlasSprite.getU0();
            float v0 = atlasSprite.getV0();
            float u1 = atlasSprite.getU1();
            float v1 = atlasSprite.getV1();
            if (i / 2 % 2 == 0) {
                float temp = u1;
                u1 = u0;
                u0 = temp;
            }

            fireVertex(pose, vertexconsumer, xOffset - 0.0F, 0.0F - yOffset, zOffset, u1, v1);
            fireVertex(pose, vertexconsumer, -xOffset - 0.0F, 0.0F - yOffset, zOffset, u0, v1);
            fireVertex(pose, vertexconsumer, -xOffset - 0.0F, 1.4F - yOffset, zOffset, u0, v0);
            fireVertex(pose, vertexconsumer, xOffset - 0.0F, 1.4F - yOffset, zOffset, u1, v0);

            hwRatio -= 0.45F;
            yOffset -= 0.45F;
            xOffset *= 0.9F;
            zOffset += 0.03F;
        }

        stack.popPose();
    }

    private static void fireVertex(PoseStack.Pose pMatrixEntry, VertexConsumer pBuffer, float pX, float pY, float pZ, float pTexU, float pTexV) {
        pBuffer.addVertex(pMatrixEntry.pose(), pX, pY, pZ).setColor(150, 150, 255, 255).setUv(pTexU, pTexV).setUv1(0, 10).setLight(240).setNormal(pMatrixEntry, 0.0F, 1.0F, 0.0F);
    }
}
