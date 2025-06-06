
package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.DroneModel;
import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import com.atsuishio.superbwarfare.entity.projectile.RgoGrenadeEntity;
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketEntity;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static com.atsuishio.superbwarfare.entity.vehicle.DroneEntity.KAMIKAZE_MODE;
import static com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity.AMMO;

public class DroneRenderer extends GeoEntityRenderer<DroneEntity> {
    public DroneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DroneModel());
        this.shadowRadius = 0.2f;
    }

    @Override
    public RenderType getRenderType(DroneEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, DroneEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    @Override
    public void render(DroneEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityIn.getYaw(partialTicks)));
        poseStack.mulPose(Axis.XP.rotationDegrees(entityIn.getBodyPitch(partialTicks)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entityIn.getRoll(partialTicks)));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack stack = player.getMainHandItem();
            var tag = NBTTool.getTag(stack);
            DroneEntity drone = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"));

            if (!(stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked") && drone != null && drone.getUUID() == entityIn.getUUID())) {
                if (entityIn.getEntityData().get(KAMIKAZE_MODE) == 1) {
                    Entity entity = new MortarShellEntity(ModEntities.MORTAR_SHELL.get(), entityIn.level());
                    entityRenderDispatcher.render(entity, 0, 0.03, 0.25, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
                }

                if (entityIn.getEntityData().get(KAMIKAZE_MODE) == 3) {
                    Entity entity = new RpgRocketEntity(ModEntities.RPG_ROCKET.get(), entityIn.level());

                    entityRenderDispatcher.render(entity, 0, -0.03, -1.8, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
                }

                for (int i = 0; i < entityIn.getEntityData().get(AMMO); i++) {
                    double yOffset = 0;
                    double xOffset = 0;

                    if (i == 0) {
                        yOffset = 0.2;
                        xOffset = 0.1;
                    }
                    if (i == 1) {
                        yOffset = 0.2;
                        xOffset = -0.1;
                    }
                    if (i == 2) {
                        yOffset = -0.05;
                        xOffset = 0.1;
                    }
                    if (i == 3) {
                        yOffset = -0.05;
                        xOffset = -0.1;
                    }
                    if (i == 4) {
                        yOffset = -0.3;
                        xOffset = 0.1;
                    }
                    if (i == 5) {
                        yOffset = -0.3;
                        xOffset = -0.1;
                    }


                    poseStack.pushPose();
                    poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    poseStack.scale(0.35f, 0.35f, 0.35f);
                    Entity entity = new RgoGrenadeEntity(ModEntities.RGO_GRENADE.get(), entityIn.level());
                    entityRenderDispatcher.render(entity, xOffset, yOffset, 0, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
                    poseStack.popPose();
                }
            }
        }

        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, DroneEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        String name = bone.getName();
        if (!animatable.onGround()) {
            if (name.equals("wingFL")) {
                bone.setRotY((System.currentTimeMillis() % 36000000) / 12f);
            }
            if (name.equals("wingFR")) {
                bone.setRotY((System.currentTimeMillis() % 36000000) / 12f);
            }
            if (name.equals("wingBL")) {
                bone.setRotY((System.currentTimeMillis() % 36000000) / 12f);
            }
            if (name.equals("wingBR")) {
                bone.setRotY((System.currentTimeMillis() % 36000000) / 12f);
            }
        }

        if (name.equals("c4")) {
            bone.setHidden(animatable.getEntityData().get(KAMIKAZE_MODE) != 2);

        }


//		Player player = Minecraft.getInstance().player;
//		if (player != null && animatable.getEntityData().get(KAMIKAZE_MODE) == 2 && name.equals("c4")) {
//			ItemStack stack = player.getMainHandItem();
//			DroneEntity drone = EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone"));
//
//			if (!(stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked") && drone != null && drone.getUUID() == animatable.getUUID())) {
//				bone.setHidden(true);
//			} else {
//				bone.setHidden(false);
//			}
//		}

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }
}
