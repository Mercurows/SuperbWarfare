package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.TurretWreckModel;
import com.atsuishio.superbwarfare.client.model.entity.VehicleModel;
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
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
    public void renderRecursively(PoseStack poseStack, TurretWreckEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
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
            var c = FastColor.ARGB32.color(FastColor.ARGB32.alpha(color), (int) (FastColor.ARGB32.red(color) * 0.3F), (int) (FastColor.ARGB32.green(color) * 0.3F), (int) (FastColor.ARGB32.blue(color) * 0.3F));
            geoEntityRenderer.renderCubesOfBone(poseStack, tBone, source, packedLight, packedOverlay, c);
            geoEntityRenderer.renderChildBones(poseStack, geoAnimatable, tBone, renderType, bufferSource, source, isReRender, partialTick, packedLight, packedOverlay, c);
            poseStack.popPose();
        }
    }

    @Override
    public void render(TurretWreckEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.rotateAround(new Quaternionf(entityIn.getQuaternion(partialTicks)), 0, 0.6f, 0);
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();

        if (this.entityRenderDispatcher.shouldRenderHitBoxes() && !entityIn.isInvisible() && !Minecraft.getInstance().showOnlyReducedInfo()) {
            var pose = poseStack.last();
            var matrix4f = pose.pose();
            var matrix3f = pose.normal();
            var buffer = bufferIn.getBuffer(RenderType.lines());

            Vec3 frontVec = entityIn.getFrontVec(partialTicks);
            renderAxis(entityIn, pose, matrix4f, frontVec, buffer, 0, 0, 255);

            Vec3 upVec = entityIn.getUpVec(partialTicks);
            renderAxis(entityIn, pose, matrix4f, upVec, buffer, 0, 255, 0);

            Vec3 RightVec = entityIn.getRightVec(partialTicks);
            renderAxis(entityIn, pose, matrix4f, RightVec, buffer, 255, 0, 0);
        }
    }

    public void renderAxis(TurretWreckEntity entityIn, PoseStack.Pose pose, Matrix4f matrix4f, Vec3 vec3, VertexConsumer buffer, int r, int g, int b) {
        buffer.addVertex(matrix4f, 0.0F, 0.6F, 0.0F).setColor(r, g, b, 255).setNormal(pose, (float) vec3.x, (float) vec3.y, (float) vec3.z);
        buffer.addVertex(matrix4f, (float) (vec3.x * 4.0D), (float) ((double) entityIn.getEyeHeight() + vec3.y * 4.0D), (float) (vec3.z * 4.0D)).setColor(r, g, b, 255).setNormal(pose, (float) vec3.x, (float) vec3.y, (float) vec3.z);
    }
}
