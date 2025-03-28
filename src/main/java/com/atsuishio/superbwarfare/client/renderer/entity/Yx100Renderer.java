package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.Yx100GlowLayer;
import com.atsuishio.superbwarfare.client.model.entity.Yx100Model;
import com.atsuishio.superbwarfare.entity.vehicle.Yx100Entity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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

import static com.atsuishio.superbwarfare.entity.vehicle.Yx100Entity.YAW;

public class Yx100Renderer extends GeoEntityRenderer<Yx100Entity> {

    public Yx100Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Yx100Model());
        this.addRenderLayer(new Yx100GlowLayer(this));
    }

    @Override
    public RenderType getRenderType(Yx100Entity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, Yx100Entity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    @Override
    public void render(Yx100Entity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entityIn.prevRoll, entityIn.getRoll())));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Yx100Entity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        String name = bone.getName();
        for (int i = 0; i < 8; i++) {
            if (name.equals("wheelL" + i)) {
                bone.setRotX(1.5f * Mth.lerp(partialTick, animatable.leftWheelRotO, animatable.getLeftWheelRot()));
            }
            if (name.equals("wheelR" + i)) {
                bone.setRotX(1.5f * Mth.lerp(partialTick, animatable.rightWheelRotO, animatable.getRightWheelRot()));
            }
        }

        if (name.equals("turret")) {
            bone.setRotY(Mth.lerp(partialTick, animatable.turretYRotO, animatable.getTurretYRot()) * Mth.DEG_TO_RAD);
        }

        if (name.equals("barrel")) {
            float a = animatable.getTurretYaw(partialTick);
            float r = (Mth.abs(a) - 90f) / 90f;

            float r2;

            if (Mth.abs(a) <= 90f) {
                r2 = a / 90f;
            } else {
                if (a < 0) {
                    r2 = -(180f + a) / 90f;
                } else {
                    r2 = (180f - a) / 90f;
                }
            }

            bone.setRotX(
                    -Mth.lerp(partialTick, animatable.turretXRotO, animatable.getTurretXRot()) * Mth.DEG_TO_RAD
                            - r * animatable.getPitch(partialTick) * Mth.DEG_TO_RAD
                            - r2 * animatable.getRoll(partialTick) * Mth.DEG_TO_RAD
            );
        }

        if (name.equals("jiqiang")) {
            bone.setRotY(Mth.lerp(partialTick, animatable.gunYRotO, animatable.getGunYRot()) * Mth.DEG_TO_RAD - Mth.lerp(partialTick, animatable.turretYRotO, animatable.getTurretYRot()) * Mth.DEG_TO_RAD);
        }
        if (name.equals("qiangguan")) {
            float a = animatable.getTurretYaw(partialTick);
            float r = (Mth.abs(a) - 90f) / 90f;

            float r2;

            if (Mth.abs(a) <= 90f) {
                r2 = a / 90f;
            } else {
                if (a < 0) {
                    r2 = -(180f + a) / 90f;
                } else {
                    r2 = (180f - a) / 90f;
                }
            }

            bone.setRotX(Mth.clamp(
                    -Mth.lerp(partialTick, animatable.gunXRotO, animatable.getGunXRot()) * Mth.DEG_TO_RAD
                            - r * animatable.getPitch(partialTick) * Mth.DEG_TO_RAD
                            - r2 * animatable.getRoll(partialTick) * Mth.DEG_TO_RAD,
                    -10 * Mth.DEG_TO_RAD, 60 * Mth.DEG_TO_RAD)
            );
        }

        if (name.equals("flare")) {
            bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
        }
        if (name.equals("flare2")) {
            bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
        }

        if (name.equals("leida") && animatable.getEnergy() > 0) {
            bone.setRotY((System.currentTimeMillis() % 36000000) / 300f);
        }

        if (name.equals("base")) {

            Player player = Minecraft.getInstance().player;
            bone.setHidden(ClientEventHandler.zoomVehicle && animatable.getFirstPassenger() == player);

            float a = animatable.getEntityData().get(YAW);
            float r = (Mth.abs(a) - 90f) / 90f;

            bone.setPosZ(r * Mth.lerp(partialTick, (float) animatable.recoilShakeO, (float) animatable.getRecoilShake()) * 1.75f);
            bone.setRotX(r * Mth.lerp(partialTick, (float) animatable.recoilShakeO, (float) animatable.getRecoilShake()) * Mth.DEG_TO_RAD);

            float r2;

            if (Mth.abs(a) <= 90f) {
                r2 = a / 90f;
            } else {
                if (a < 0) {
                    r2 = -(180f + a) / 90f;
                } else {
                    r2 = (180f - a) / 90f;
                }
            }

            bone.setPosX(r2 * Mth.lerp(partialTick, (float) animatable.recoilShakeO, (float) animatable.getRecoilShake()) * 1f);
            bone.setRotZ(r2 * Mth.lerp(partialTick, (float) animatable.recoilShakeO, (float) animatable.getRecoilShake()) * Mth.DEG_TO_RAD * 1.5f);
        }

        for (int i = 0; i < 41; i++) {
            float tO = animatable.leftTrackO + 2 * i;
            float t = animatable.getLeftTrack() + 2 * i;

            while (t >= 80) {
                t -= 80;
            }
            while (t <= 0) {
                t += 80;
            }
            while (tO >= 80) {
                tO -= 80;
            }
            while (tO <= 0) {
                tO += 80;
            }

            float tO2 = animatable.rightTrackO + 2 * i;
            float t2 = animatable.getRightTrack() + 2 * i;

            while (t2 >= 80) {
                t2 -= 80;
            }
            while (t2 <= 0) {
                t2 += 80;
            }
            while (tO2 >= 80) {
                tO2 -= 80;
            }
            while (tO2 <= 0) {
                tO2 += 80;
            }

            if (name.equals("trackL" + i)) {
                bone.setPosY(Mth.lerp(partialTick, getBoneMoveY(tO), getBoneMoveY(t)));
                bone.setPosZ(Mth.lerp(partialTick, getBoneMoveZ(tO), getBoneMoveZ(t)));
            }

            if (name.equals("trackR" + i)) {
                bone.setPosY(Mth.lerp(partialTick, getBoneMoveY(tO2), getBoneMoveY(t2)));
                bone.setPosZ(Mth.lerp(partialTick, getBoneMoveZ(tO2), getBoneMoveZ(t2)));
            }

            if (name.equals("trackLRot" + i)) {
                bone.setRotX(-Mth.lerp(partialTick, getBoneRotX(tO), getBoneRotX(t)) * Mth.DEG_TO_RAD);
            }

            if (name.equals("trackRRot" + i)) {
                bone.setRotX(-Mth.lerp(partialTick, getBoneRotX(tO2), getBoneRotX(t2)) * Mth.DEG_TO_RAD);
            }

        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    public float getBoneRotX(float t) {
        if (t <= 32.5833) return 0F;
        if (t <= 33.5833) return Mth.lerp(t - 32.5833F, 0F, -45F);
        if (t <= 34.5833) return Mth.lerp(t - 33.5833F, -45F, -90F);
        if (t <= 36.8333) return Mth.lerp((t - 34.5833F) / (36.8333F - 34.5833F), -90F, -150F);
        if (t <= 40.5833) return -150F;
        if (t <= 41.0833) return Mth.lerp((t - 40.5833F) / (41.0833F - 40.5833F), -150F, -180F);
        if (t <= 70) return -180F;
        if (t <= 71) return Mth.lerp(t - 70F, -180F, -210F);
        if (t <= 74.25) return -210F;
        if (t <= 76.5) return Mth.lerp((t - 74.25F) / (76.5F - 74.25F), -210F, -270F);
        if (t <= 77.5) return Mth.lerp(t - 76.5F, -270F, -315F);
        if (t <= 79.75) return Mth.lerp((t - 77.5F) / (79.75F - 77.5F), -315F, -360F);

        return 0F;
    }

    public float getBoneMoveY(float t) {
        if (t <= 32.5833) return 0F;
        if (t <= 33.0833) return Mth.lerp((t - 32.5833F) / (33.0833F - 32.5833F), 0F, -1F);
        if (t <= 34.0833) return Mth.lerp(t - 33.0833F, -1F, -2.5F);
        if (t <= 35.5833) return Mth.lerp((t - 34.0833F) / (35.5833F - 34.0833F), -2.5F, -7.5F);
        if (t <= 36.8333) return Mth.lerp((t - 35.5833F) / (36.8333F - 35.5833F), -7.5F, -12.25F);
        if (t <= 41.0833) return Mth.lerp((t - 36.8333F) / (41.0833F - 36.8333F), -12.25F, -20.9F);
        if (t <= 70) return -20.9F;
        if (t <= 74.25) return Mth.lerp((t - 70F) / (74.25F - 70F), -20.9F, -12.25F);
        if (t <= 76) return Mth.lerp((t - 74.25F) / (76F - 74.25F), -12.25F, -7.5F);
        if (t <= 77.75) return Mth.lerp((t - 76F) / (77.75F - 76F), -7.5F, -2F);
        if (t <= 79.25) return Mth.lerp((t - 77.75F) / (79.25F - 77.75F), -2F, -0.3F);

        return Mth.lerp((t - 79.25F) / (80F - 79.25F), -0.3F, 0F);
    }

    public float getBoneMoveZ(float t) {
        if (t <= 32.5833) return Mth.lerp(t / (32.5833F - 0F), 0F, 135.6F);
        if (t <= 33.0833) return Mth.lerp((t - 32.5833F) / (33.0833F - 32.5833F), 135.6F, 137.75F);
        if (t <= 34.0833) return Mth.lerp(t - 33.0833F, 137.75F, 140.25F);
        if (t <= 35.5833) return 140.25F;
        if (t <= 36.8333) return Mth.lerp((t - 35.5833F) / (36.8333F - 35.5833F), 140.25F, 137.25F);
        if (t <= 41.0833) return Mth.lerp((t - 36.8333F) / (41.0833F - 36.8333F), 137.25F, 121.5F);
        if (t <= 70) return Mth.lerp((t - 41.0833F) / (70F - 41.0833F), 121.5F, 11.5F);
        if (t <= 74.25) return Mth.lerp((t - 70F) / (74.25F - 70F), 11.5F, -3.75F);
        if (t <= 76) return Mth.lerp((t - 74.25F) / (76F - 74.25F), -3.75F, -10F);
        if (t <= 77.75) return Mth.lerp((t - 76F) / (77.75F - 76F), -10F, -8.25F);
        if (t <= 79.25) return Mth.lerp((t - 77.75F) / (79.25F - 77.75F), -8.25F, -4.12F);

        return Mth.lerp((t - 79.25F) / (80F - 79.25F), -4.12F, 0F);
    }
}
