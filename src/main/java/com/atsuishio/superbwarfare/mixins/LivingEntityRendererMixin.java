package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.data.vehicle.VehicleData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M> {
    @Shadow
    protected M model;

    protected LivingEntityRendererMixin(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Inject(method = "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V", at = @At("HEAD"), cancellable = true)
    protected void setupRotations(T entity, PoseStack matrices, float pAgeInTicks, float pRotationYaw, float tickDelta, CallbackInfo ci) {
        if (entity.getRootVehicle() != entity && entity.getRootVehicle() instanceof VehicleEntity vehicle) {
            var seats = VehicleData.compute(vehicle).seats();
            int index = vehicle.getSeatIndex(entity);
            if (index < 0 || index >= seats.size()) return;

            ci.cancel();
            var seat = seats.get(index);

            float transformYaw = (float) VehicleVecUtils.getYRotFromVector(vehicle.getTransformDirectionNoOrientation(tickDelta, entity));
            var passengerWeaponStationYawRot = Axis.YP.rotationDegrees(-transformYaw);

            Quaterniond quaterniond = vehicle.getRotationFromString(seat.transform, tickDelta).mul(new Quaterniond(passengerWeaponStationYawRot));
            Quaternionf quaternionf = new Quaternionf(quaterniond.x, quaterniond.y, quaterniond.z, quaterniond.w);

            matrices.mulPose(quaternionf);
            matrices.mulPose(Axis.YP.rotationDegrees(180.0F - pRotationYaw));

            float scale = vehicle.getPassengerRenderScale();

            if (Minecraft.getInstance().player != null && ClientEventHandler.zoomVehicle && entity.getRootVehicle() == Minecraft.getInstance().player.getRootVehicle()) {
                scale = 0;
            }

            matrices.scale(scale, scale, scale);
        }
    }

    @Inject(method = "isBodyVisible(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    protected void isBodyVisible(T pLivingEntity, CallbackInfoReturnable<Boolean> cir) {
        if (ClientEventHandler.activeThermalImaging) {
            cir.cancel();
            cir.setReturnValue(true);
        }
    }
}
