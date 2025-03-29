package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity;
import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity;
import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity;
import com.atsuishio.superbwarfare.entity.vehicle.WheelChairEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// From Immersive_Aircraft
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity> {

    @Inject(method = "setupRotations", at = @At("TAIL"))
    public void render(T entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale, CallbackInfo ci) {
        if (entity.getRootVehicle() != entity && entity.getRootVehicle() instanceof VehicleEntity vehicle) {
            if (vehicle instanceof Tom6Entity || vehicle instanceof WheelChairEntity || vehicle instanceof SpeedboatEntity) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-vehicle.getViewXRot(partialTick)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-vehicle.getRoll(partialTick)));
            } else if (vehicle instanceof Ah6Entity ah6Entity) {
                if (entity == ah6Entity.getNthEntity(2)) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(-ah6Entity.getRoll(partialTick)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(ah6Entity.getViewXRot(partialTick)));
                } else if (entity == ah6Entity.getNthEntity(3)) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(ah6Entity.getRoll(partialTick)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-ah6Entity.getViewXRot(partialTick)));
                } else {
                    poseStack.mulPose(Axis.XP.rotationDegrees(-ah6Entity.getViewXRot(partialTick)));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-ah6Entity.getRoll(partialTick)));
                }
            } else {
                float a = Mth.wrapDegrees(Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot) - Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot()));

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

                poseStack.mulPose(Axis.XP.rotationDegrees(r * vehicle.getViewXRot(partialTick) - r2 * vehicle.getRoll(partialTick)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(r * vehicle.getRoll(partialTick) + r2 * vehicle.getViewXRot(partialTick)));
            }
        }
    }
}
