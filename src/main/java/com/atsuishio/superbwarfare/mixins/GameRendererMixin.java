package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.PrismTankEntity;
import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModTags;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("HEAD"), cancellable = true)
    public void bobView(PoseStack p_109139_, float p_109140_, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null) {
            ItemStack stack = player.getMainHandItem();
            if (stack.is(ModTags.Items.GUN) && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
                ci.cancel();
            }
        }
    }

    // From Immersive_Aircraft
    @Shadow
    @Final
    private Camera mainCamera;

    @SuppressWarnings("ConstantValue")
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"))
    public void superbWarfare$renderWorld(float tickDelta, long limitTime, PoseStack matrices, CallbackInfo ci) {
        Entity entity = mainCamera.getEntity();

        if (entity != null && !mainCamera.isDetached() && entity.getRootVehicle() instanceof VehicleEntity vehicle) {
            // rotate camera
            float a = vehicle.getTurretYaw(tickDelta);

            float r = (Mth.abs(a) - 90f) / 90f;

            float r2;

            if (Mth.abs(a) <= 90f) {
                r2 = a / 90f;
            } else {
                if (a < 0) {
                    r2 = - (180f + a) / 90f;
                } else {
                    r2 = (180f - a) / 90f;
                }
            }

            matrices.mulPose(Axis.ZP.rotationDegrees(-r * vehicle.getRoll(tickDelta) + r2 * vehicle.getViewXRot(tickDelta)));

            if (
                    !(vehicle instanceof SpeedboatEntity speedboat && entity == speedboat.getFirstPassenger()) &&
                    !(vehicle instanceof PrismTankEntity prismTank && entity == prismTank.getFirstPassenger()) &&
                    !(vehicle instanceof CannonEntity)
            ) {
                // fetch eye offset
                float eye = entity.getEyeHeight();

                // transform eye offset to match aircraft rotation
                Vector3f offset = new Vector3f(0, -eye, 0);
                Quaternionf quaternion = Axis.XP.rotationDegrees(0.0f);
                quaternion.mul(Axis.YP.rotationDegrees(-vehicle.getViewYRot(tickDelta)));
                quaternion.mul(Axis.XP.rotationDegrees(vehicle.getViewXRot(tickDelta)));
                quaternion.mul(Axis.ZP.rotationDegrees(vehicle.getRoll(tickDelta)));
                offset.rotate(quaternion);

                // apply camera offset
                matrices.mulPose(Axis.XP.rotationDegrees(mainCamera.getXRot()));
                matrices.mulPose(Axis.YP.rotationDegrees(mainCamera.getYRot() + 180.0f));
                matrices.translate(offset.x(), offset.y() + eye, offset.z());
                matrices.mulPose(Axis.YP.rotationDegrees(-mainCamera.getYRot() - 180.0f));
                matrices.mulPose(Axis.XP.rotationDegrees(-mainCamera.getXRot()));
            }
        }
    }
}
