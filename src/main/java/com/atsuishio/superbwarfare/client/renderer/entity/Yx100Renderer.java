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
        for (int i = 0; i < 9; i++) {
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

        if (name.equals("HMG")) {
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

        if (name.equals("base")) {
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

        if (name.equals("root")) {
            Player player = Minecraft.getInstance().player;
            bone.setHidden(ClientEventHandler.zoomVehicle && animatable.getFirstPassenger() == player);
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

            if (name.equals("TrackR" + i)) {
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
        if (t <= 34.75) return 0F;
        if (t <= 35.5) return Mth.lerp((t - 34.75F) / (35.5F - 34.75F), 0F, -45F);
        if (t <= 35.8333) return -45F;
        if (t <= 36.5) return Mth.lerp((t - 35.8333F) / (36.5F - 35.8333F), -45F, -90F);
        if (t <= 36.6667) return -90F;
        if (t <= 37) return Mth.lerp((t - 36.6667F) / (37F - 36.6667F), -90F, -112.5F);
        if (t <= 37.3333) return -112.5F;
        if (t <= 37.5) return -112.5F;
        if (t <= 38.1667) return Mth.lerp((t - 37.5F) / (38.1667F - 37.5F), -112.5F, -135F);
        if (t <= 41.9167) return -135F;
        if (t <= 42.4167) return Mth.lerp((t - 41.9167F) / (42.4167F - 41.9167F), -135F, -157.5F);
        if (t <= 43.1667) return -157.5F;
        if (t <= 43.6667) return Mth.lerp((t - 43.1667F) / (43.6667F - 43.1667F), -157.5F, -180F);
        if (t <= 68) return -180F;
        if (t <= 68.5) return Mth.lerp((t - 68F) / (68.5F - 68F), -180F, -202.5F);
        if (t <= 69.25) return -202.5F;
        if (t <= 69.8333) return Mth.lerp((t - 69.25F) / (69.8333F - 69.25F), -202.5F, -220F);
        if (t <= 73.5) return -220F;
        if (t <= 74.1667) return Mth.lerp((t - 73.5F) / (74.1667F - 73.5F), -220F, -242.5F);
        if (t <= 75.6667) return -242.5F;
        if (t <= 76.1667) return Mth.lerp((t - 75.6667F) / (76.1667F - 75.6667F), -242.5F, -295F);
        if (t <= 76.6667) return -295F;
        if (t <= 77.1667) return Mth.lerp((t - 76.6667F) / (77.1667F - 76.6667F), -295F, -340F);
        if (t <= 77.8333) return Mth.lerp((t - 77.1667F) / (77.8333F - 77.1667F), -340F, -360F);
        if (t <= 79.5) return -360F;

        return 0F;
    }

    public float getBoneMoveY(float t) {
        if (t <= 35.1667) return 0F;
        if (t <= 36.1667) return Mth.lerp(t - 35.1667F, 0F, -2.91F);
        if (t <= 37) return Mth.lerp((t - 36.1667F) / (37F - 36.1667F), -2.91F, -6.79F);
        if (t <= 37.8333) return Mth.lerp((t - 37F) / (37.8333F - 37F), -6.79F, -10.005F);
        if (t <= 42.1667) return Mth.lerp((t - 37.8333F) / (42.1667F - 37.8333F), -10.005F, -22.38F);
        if (t <= 43.4167) return Mth.lerp((t - 42.1667F) / (43.4167F - 42.1667F), -22.38F, -24.14F);
        if (t <= 68.25) return -24.14F;
        if (t <= 69.5) return Mth.lerp((t - 68.25F) / (69.5F - 68.25F), -24.14F, -22.45F);
        if (t <= 73.8333) return Mth.lerp((t - 69.5F) / (73.8333F - 69.5F), -22.45F, -11.12F);
        if (t <= 75.9167) return Mth.lerp((t - 73.8333F) / (75.9167F - 73.8333F), -11.12F, -4.155F);
        if (t <= 76.9167) return Mth.lerp(t - 75.9167F, -4.155F, -0.855F);
        if (t <= 78.0833) return Mth.lerp((t - 76.9167F) / (78.0833F - 76.9167F), -0.855F, 0F);

        return Mth.lerp((t - 79.25F) / (80F - 79.25F), -0.025F, 0F);
    }

    public float getBoneMoveZ(float t) {
        if (t <= 35.1667) return Mth.lerp(t / (35.1667F - 0F), 0F, 121.385F);
        if (t <= 36.1667) return Mth.lerp(t - 35.1667F, 121.385F, 124.37F);
        if (t <= 37) return 124.37F;
        if (t <= 37.8333) return Mth.lerp((t - 37F) / (37.8333F - 37F), 124.37F, 122.73F);
        if (t <= 42.1667) return Mth.lerp((t - 37.8333F) / (42.1667F - 37.8333F), 122.73F, 110.455F);
        if (t <= 43.4167) return Mth.lerp((t - 42.1667F) / (43.4167F - 42.1667F), 110.455F, 105.805F);
        if (t <= 68.25) return Mth.lerp((t - 43.4167F) / (68.25F - 43.4167F), 105.805F, 10.09F);
        if (t <= 69.5) return Mth.lerp((t - 68.25F) / (69.5F - 68.25F), 10.09F, 5.625F);
        if (t <= 73.8333) return Mth.lerp((t - 69.5F) / (73.8333F - 69.5F), 5.625F, -8.025F);
        if (t <= 75.9167) return Mth.lerp((t - 73.8333F) / (75.9167F - 73.8333F), -8.025F, -11.175F);
        if (t <= 76.9167) return Mth.lerp(t - 75.9167F, -11.175F, -9.35F);
        if (t <= 78.0833) return Mth.lerp((t - 76.9167F) / (78.0833F - 76.9167F), -9.35F, -5.38F);

        return Mth.lerp((t - 79.25F) / (80F - 79.25F), -4.12F, 0F);
    }
}
