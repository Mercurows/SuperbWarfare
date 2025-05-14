package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    /**
     * From Automobility
     */
    @Unique
    private boolean sbw$cacheOnGround;

    @Shadow
    private boolean onGround;

    @Inject(method = "collide", at = @At("HEAD"))
    private void sbw$spoofGroundStart(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (MobileVehicleEntity.IGNORE_ENTITY_GROUND_CHECK_STEPPING) {
            this.sbw$cacheOnGround = this.onGround;
            this.onGround = true;
        }
    }

    @Inject(method = "collide", at = @At("TAIL"))
    private void sbw$spoofGroundEnd(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (MobileVehicleEntity.IGNORE_ENTITY_GROUND_CHECK_STEPPING) {
            this.onGround = this.sbw$cacheOnGround;
            MobileVehicleEntity.IGNORE_ENTITY_GROUND_CHECK_STEPPING = false;
        }
    }

    // TODO 优化后续逻辑
//    @Redirect(method = "turn(DD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setXRot(F)V", ordinal = 1))
//    public void turn(Entity instance, float pXRot) {
//        if (instance instanceof Player player) {
//            player.setXRot(player.getXRot());
//            while (player.getXRot() > 180F) {
//                player.setXRot(player.getXRot() - 360F);
//            }
//            while (player.getYRot() <= -180F) {
//                player.setXRot(player.getXRot() + 360F);
//            }
//        } else {
//            instance.setXRot(Mth.clamp(instance.getXRot(), -90.0F, 90.0F));
//        }
//    }
}
