package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.TowEntity;
import com.atsuishio.superbwarfare.entity.vehicle.Yx100Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.item.curio.ParachuteItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class PlayerModelMixin<T extends LivingEntity> {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch, CallbackInfo ci) {
        if (pEntity instanceof Player player) {
            var model = (PlayerModel) (Object) this;
            // 降落伞
            if (ParachuteItem.isParachuteOpen(pEntity)) {
                model.leftArm.xRot = -180 * Mth.DEG_TO_RAD;
                model.rightArm.xRot = -180 * Mth.DEG_TO_RAD;
                model.leftSleeve.xRot = -180 * Mth.DEG_TO_RAD;
                model.rightSleeve.xRot = -180 * Mth.DEG_TO_RAD;

                model.leftArm.yRot = -15 * Mth.DEG_TO_RAD;
                model.rightArm.yRot = 15 * Mth.DEG_TO_RAD;
                model.leftSleeve.yRot = -15 * Mth.DEG_TO_RAD;
                model.rightSleeve.yRot = 15 * Mth.DEG_TO_RAD;

                model.leftLeg.xRot = 0;
                model.rightLeg.xRot = 0;
                model.leftLeg.yRot = 0;
                model.rightLeg.yRot = 0;

                model.leftPants.xRot = 0;
                model.rightPants.xRot = 0;
                model.leftPants.yRot = 0;
                model.rightPants.yRot = 0;

                model.body.xRot = 0;
                model.body.yRot = 0;
                model.body.zRot = 0;
            }

            // 飞行器
            if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player) && (vehicle.getVehicleType() == VehicleType.AIRPLANE || vehicle.getVehicleType() == VehicleType.HELICOPTER)) {
                model.head.xRot = 0;
                model.head.yRot = 0;
                model.head.zRot = 0;
                model.hat.xRot = 0;
                model.hat.yRot = 0;
                model.hat.zRot = 0;

                model.rightArm.xRot = -55 * Mth.DEG_TO_RAD;
                model.rightSleeve.xRot = -55 * Mth.DEG_TO_RAD;
                model.rightArm.yRot = -15f * Mth.DEG_TO_RAD;
                model.rightSleeve.yRot = -15f * Mth.DEG_TO_RAD;
                model.rightArm.zRot = -30f * Mth.DEG_TO_RAD;
                model.rightSleeve.zRot = -30f * Mth.DEG_TO_RAD;
            }

            // Tow
            if (player.getVehicle() instanceof TowEntity) {
                model.head.xRot = 0;
                model.hat.xRot = 0;
                model.head.y = 0;
                model.hat.y = 0;
                model.head.z = -4;
                model.hat.z = -4;

                model.leftArm.yRot = 45 * Mth.DEG_TO_RAD;
                model.leftArm.xRot = -115 * Mth.DEG_TO_RAD;
                model.leftSleeve.yRot = 45 * Mth.DEG_TO_RAD;
                model.leftSleeve.xRot = -115 * Mth.DEG_TO_RAD;

                model.rightArm.yRot = 25 * Mth.DEG_TO_RAD;
                model.rightArm.xRot = -115 * Mth.DEG_TO_RAD;
                model.rightSleeve.yRot = 25 * Mth.DEG_TO_RAD;
                model.rightSleeve.xRot = -115 * Mth.DEG_TO_RAD;

                model.leftLeg.xRot = 0 * Mth.DEG_TO_RAD;
                model.leftPants.xRot = 0 * Mth.DEG_TO_RAD;
                model.leftLeg.yRot = 0 * Mth.DEG_TO_RAD;
                model.leftPants.yRot = 0 * Mth.DEG_TO_RAD;
                model.leftLeg.zRot = 0 * Mth.DEG_TO_RAD;
                model.leftPants.zRot = 0 * Mth.DEG_TO_RAD;
                model.leftLeg.z = -6f;
                model.leftPants.z = -6f;
                model.leftLeg.y = 5.4f;
                model.leftPants.y = 5.4f;

                model.rightLeg.xRot = 85 * Mth.DEG_TO_RAD;
                model.rightPants.xRot = 85 * Mth.DEG_TO_RAD;
                model.rightLeg.yRot = 0 * Mth.DEG_TO_RAD;
                model.rightPants.yRot = 0 * Mth.DEG_TO_RAD;
                model.rightLeg.zRot = 0 * Mth.DEG_TO_RAD;
                model.rightPants.zRot = 0 * Mth.DEG_TO_RAD;
                model.rightLeg.z = -4f;
                model.rightPants.z = -4f;
                model.rightLeg.y = 14;
                model.rightPants.y = 14;

                model.body.xRot = 20 * Mth.DEG_TO_RAD;
                model.body.z = -5;

                model.leftArm.z = -5;
                model.leftSleeve.z = -5;

                model.rightArm.z = -5;
                model.rightSleeve.z = -5;
            }

            // 坦克挂票
            if (player.getVehicle() instanceof Yx100Entity yx100Entity && (yx100Entity.getNthEntity(3) == player || yx100Entity.getNthEntity(4) == player)) {
                model.leftArm.xRot = -112.5f * Mth.DEG_TO_RAD;
                model.leftSleeve.xRot = -112.5f * Mth.DEG_TO_RAD;

                model.rightArm.xRot = -112.5f * Mth.DEG_TO_RAD;
                model.rightSleeve.xRot = -112.5f * Mth.DEG_TO_RAD;
            }

            // 趴下持枪
            if (player.getMainHandItem().getItem() instanceof GunItem && player.getPose() == Pose.SWIMMING && !player.isSwimming()) {
                model.hat.xRot = (player.getViewXRot(1) - 90) * Mth.DEG_TO_RAD;
                model.head.xRot = (player.getViewXRot(1) - 90) * Mth.DEG_TO_RAD;
                model.hat.yRot = 0;
                model.head.yRot = 0;

                model.leftArm.xRot = (-180 + player.getViewXRot(1)) * Mth.DEG_TO_RAD;
                model.rightArm.xRot = (-180 + player.getViewXRot(1))* Mth.DEG_TO_RAD;
                model.leftSleeve.xRot = (-180 + player.getViewXRot(1)) * Mth.DEG_TO_RAD;
                model.rightSleeve.xRot = (-180 + player.getViewXRot(1)) * Mth.DEG_TO_RAD;

                model.leftArm.yRot = 0 * Mth.DEG_TO_RAD;
                model.rightArm.yRot = 0 * Mth.DEG_TO_RAD;
                model.leftSleeve.yRot = 0 * Mth.DEG_TO_RAD;
                model.rightSleeve.yRot = 0 * Mth.DEG_TO_RAD;

                model.leftArm.zRot = -30 * Mth.DEG_TO_RAD;
                model.rightArm.zRot = 0 * Mth.DEG_TO_RAD;
                model.leftSleeve.zRot = -30 * Mth.DEG_TO_RAD;
                model.rightSleeve.zRot = 0 * Mth.DEG_TO_RAD;

                model.rightArm.x = -3f;
                model.rightSleeve.x = -3f;
                model.leftArm.x = 3f;
                model.leftSleeve.x = 3f;
            }
        }
    }
}
