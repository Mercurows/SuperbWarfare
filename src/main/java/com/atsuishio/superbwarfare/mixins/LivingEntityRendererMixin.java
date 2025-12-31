package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.data.vehicle.VehicleData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M> {

    @Shadow
    protected M model;

    protected LivingEntityRendererMixin(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Inject(method = "setupRotations", at = @At("TAIL"))
    public void render(T entity, PoseStack matrices, float animationProgress, float bodyYaw, float tickDelta, CallbackInfo ci) {
        if (entity.getRootVehicle() != entity && entity.getRootVehicle() instanceof VehicleEntity vehicle) {
            var seats = VehicleData.compute(vehicle).seats();
            int index = vehicle.getSeatIndex(entity);
            if (index < 0 || index >= seats.size()) return;

            var seat = seats.get(index);
            float lerpRot = Mth.lerp(tickDelta, entity.yBodyRotO, entity.yBodyRot);

            if (!seat.canRotateBody && Mth.abs(entity.yBodyRot) > 150) {
                lerpRot = entity.yBodyRot;
            }

            float transformYaw = (float) VehicleVecUtils.getYRotFromVector(vehicle.getTransformDirectionNoOrientation(tickDelta, entity));
            var bodyDiffY = lerpRot + transformYaw;
            var passengerWeaponStationYawRot = Axis.YP.rotationDegrees(-bodyDiffY);

            Quaterniond quaterniond = vehicle.getRotationFromString(seat.transform, tickDelta).mul(new Quaterniond(passengerWeaponStationYawRot));
            Quaternionf quaternionf = new Quaternionf(-quaterniond.x, quaterniond.y, -quaterniond.z, quaterniond.w);

            matrices.mulPose(Axis.YP.rotationDegrees(lerpRot));
            matrices.mulPose(quaternionf);
        }
    }

//    @Inject(method = "getRenderType(Lnet/minecraft/world/entity/LivingEntity;ZZZ)Lnet/minecraft/client/renderer/RenderType;",
//            at = @At("HEAD"), cancellable = true)
//    protected void getRenderType(T pLivingEntity, boolean pBodyVisible, boolean pTranslucent, boolean pGlowing, CallbackInfoReturnable<RenderType> cir) {
//        ResourceLocation resourcelocation = this.getTextureLocation(pLivingEntity);
//
//        if (ClientEventHandler.activeThermalImaging) {
//            resourcelocation = getSmartBrightenedTexture(resourcelocation, 5f);
//        }
//
//        if (pTranslucent) {
//            cir.setReturnValue(RenderType.itemEntityTranslucentCull(resourcelocation));
//        } else if (pBodyVisible) {
//            cir.setReturnValue(this.model.renderType(resourcelocation));
//        } else {
//            cir.setReturnValue(pGlowing ? RenderType.outline(resourcelocation) : null);
//        }
//    }

//    @Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
//    private static void getOverlayCoords(LivingEntity pLivingEntity, float pU, CallbackInfoReturnable<Integer> cir) {
////        if (ClientEventHandler.activeThermalImaging) {
////
////        }
//        cir.cancel();
//        cir.setReturnValue(OverlayTexture.pack(OverlayTexture.u(1), OverlayTexture.v(false)));
//    }
}
