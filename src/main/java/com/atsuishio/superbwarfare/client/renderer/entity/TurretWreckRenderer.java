package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.TurretWreckModel;
import com.atsuishio.superbwarfare.client.model.entity.VehicleModel;
import com.atsuishio.superbwarfare.client.renderer.SmartTextureBrightener;
import com.atsuishio.superbwarfare.client.renderer.TextureBrightnessHandler;
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TurretWreckRenderer extends GeoEntityRenderer<TurretWreckEntity> {
    public TurretWreckRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TurretWreckModel());
        this.shadowRadius = 1f;
    }

    @Override
    public RenderType getRenderType(TurretWreckEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, TurretWreckEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        // TODO 加个缓存
        var type = EntityType.byString(animatable.getVehicleName());
        if (type.isEmpty()) return;
        var entity = type.get().create(animatable.level());
        if (entity == null) return;
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (entity instanceof VehicleEntity vehicle && entity instanceof GeoAnimatable geoAnimatable && renderer instanceof GeoEntityRenderer geoEntityRenderer) {

            var model = geoEntityRenderer.getGeoModel();
            if (!(model instanceof VehicleModel vehicleModel)) return;

            var modelResource = vehicleModel.getPreciseModelResource(vehicle);
            if (modelResource == null) return;
            var textureResource = vehicleModel.getPreciseTextureResource(vehicle);
            if (textureResource == null) return;

            var bakedModel = vehicleModel.getBakedModel(modelResource);
            var optionalBone = bakedModel.getBone("turret");
            if (optionalBone.isEmpty()) return;

            var barrelBone = bakedModel.getBone("barrel");
            barrelBone.ifPresent(geoBone -> geoBone.setRotX(-animatable.xRotO * Mth.DEG_TO_RAD));

            var passerWeaponPitch = bakedModel.getBone("passengerWeaponStationPitch");
            passerWeaponPitch.ifPresent(geoBone -> geoBone.setRotX(0));

            var passerWeaponYaw = bakedModel.getBone("passengerWeaponStationYaw");
            passerWeaponYaw.ifPresent(geoBone -> geoBone.setRotY(0));

            optionalBone.get().setHidden(false);

            Vec3 turretPos = vehicle.getTurretPos();

            poseStack.pushPose();

            if (turretPos != null) {
                poseStack.translate(turretPos.x, -turretPos.y, turretPos.z);
            }

            var tBone = optionalBone.get();
            var source = bufferSource.getBuffer(RenderType.entityTranslucent(textureResource));
            geoEntityRenderer.renderCubesOfBone(poseStack, tBone, source, packedLight, packedOverlay, 0.3f, 0.3f, 0.3f, alpha);
            geoEntityRenderer.renderChildBones(poseStack, geoAnimatable, tBone, renderType, bufferSource, source, isReRender, partialTick, packedLight, packedOverlay, 0.3f, 0.3f, 0.3f, alpha);
            poseStack.popPose();
        }
    }

    @Override
    public void render(TurretWreckEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf(entityIn.getQuaternion(partialTicks)));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }
}
