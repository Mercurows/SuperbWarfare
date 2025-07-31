package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.AirEntity;
import com.atsuishio.superbwarfare.item.curio.ParachuteItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class PlayerModelMixin<T extends LivingEntity> {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch, CallbackInfo ci) {
        if (ParachuteItem.isParachuteOpen(pEntity)) {
            var model = (PlayerModel) (Object) this;
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

            model.body.xRot = 0;
            model.body.yRot = 0;
            model.body.zRot = 0;
        }

        if (pEntity instanceof Player player && player.getVehicle() instanceof AirEntity airEntity && airEntity.banHand(player)) {
            var model = (PlayerModel) (Object) this;
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
    }
}
