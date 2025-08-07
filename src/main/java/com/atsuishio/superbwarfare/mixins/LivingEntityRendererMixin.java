package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

// From Immersive_Aircraft
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity> {

    @Inject(method = "setupRotations", at = @At("TAIL"))
    public void render(T entity, PoseStack matrices, float animationProgress, float bodyYaw, float tickDelta, CallbackInfo ci) {
        if (entity.getRootVehicle() != entity && entity.getRootVehicle() instanceof VehicleEntity vehicle) {
            var rotation = vehicle.getPassengerRotation(entity, tickDelta);
            if (rotation != null) {
                matrices.mulPose(rotation.first());
                matrices.mulPose(rotation.second());
            } else {
                float a = Mth.wrapDegrees(Mth.lerp(tickDelta, entity.yBodyRotO, entity.yBodyRot) - Mth.lerp(tickDelta, vehicle.yRotO, vehicle.getYRot()));
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

                matrices.mulPose(Axis.XP.rotationDegrees(r * vehicle.getViewXRot(tickDelta) - r2 * vehicle.getRoll(tickDelta)));
                matrices.mulPose(Axis.ZP.rotationDegrees(r * vehicle.getRoll(tickDelta) + r2 * vehicle.getViewXRot(tickDelta)));
            }
        }
        if (entity instanceof Player player && player.getMainHandItem().getItem() instanceof GunItem && isProne(player) && !player.isSwimming()) {
            matrices.translate(0, -0.18, 0);
        }
    }
}
