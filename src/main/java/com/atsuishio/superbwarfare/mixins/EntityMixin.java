package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.mixin.OBBHitter;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.launcher.SuperStarShooterItem;
import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements OBBHitter {

    /**
     * From Automobility
     */
    @Unique
    private boolean sbw$cacheOnGround;

    @Shadow
    private boolean onGround;

    @Shadow
    public abstract Level level();

    @Shadow
    public abstract AABB getBoundingBox();

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract void setDeltaMovement(Vec3 pDeltaMovement);

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Inject(method = "collide", at = @At("HEAD"))
    private void sbw$spoofGroundStart(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (VehicleEntity.ignoreEntityGroundCheckStepping) {
            this.sbw$cacheOnGround = this.onGround;
            this.onGround = true;
        }
    }

    @Inject(method = "collide", at = @At("TAIL"))
    private void sbw$spoofGroundEnd(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (VehicleEntity.ignoreEntityGroundCheckStepping) {
            this.onGround = this.sbw$cacheOnGround;
            VehicleEntity.ignoreEntityGroundCheckStepping = false;
        }
    }

    @Unique
    public OBB.Part sbw$currentHitPart;

    @Override
    public OBB.Part sbw$getCurrentHitPart() {
        return this.sbw$currentHitPart;
    }

    @Override
    public void sbw$setCurrentHitPart(OBB.Part part) {
        this.sbw$currentHitPart = part;
    }

    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    public void turn(double pYRot, double pXRot, CallbackInfo ci) {
        var entity = (Entity) (Object) this;
        if (entity instanceof Player player && player.getMainHandItem().getItem() instanceof GunItem && player.getPose() == Pose.SWIMMING && !player.isSwimming()) {
            ci.cancel();
            float f = (float) pXRot * 0.15F;
            float f1 = (float) pYRot * 0.15F;
            player.setXRot(player.getXRot() + f);
            player.setYRot(player.getYRot() + f1);
            Vec3 forward = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
            if (player.level().getBlockState(BlockPos.containing(player.getX() + 0.25 * forward.x, player.getY() - 0.1, player.getZ() + 0.25 * forward.z)).canOcclude()) {
                player.setXRot(Mth.clamp(player.getXRot(), -45F, 30F));
            } else {
                player.setXRot(Mth.clamp(player.getXRot(), -45F, 89F));
            }
            player.xRotO += f;
            player.yRotO += f1;
            player.xRotO = Mth.clamp(player.xRotO, -90F, 90F);

            float diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot));
            player.setYBodyRot(player.yBodyRot + 0.5f * diffY);

            if (player.getVehicle() != null) {
                player.getVehicle().onPassengerTurned(player);
            }
        }
        if (entity instanceof Player player && player.getMainHandItem().getItem() instanceof SuperStarShooterItem) {
            ci.cancel();
            float f = (float)pXRot * 0.15F;
            float f1 = (float)pYRot * 0.15F;
            player.setXRot(player.getXRot() + f);
            player.setYRot(player.getYRot() + f1);
            player.setXRot(Mth.clamp(player.getXRot(), -90.0F, 90.0F));
            player.xRotO += f;
            player.yRotO += f1;
            player.xRotO = Mth.clamp(player.xRotO, -90.0F, 90.0F);
            if (player.getVehicle() != null) {
                player.getVehicle().onPassengerTurned(player);
            }
            float diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot));
            player.setYBodyRot(player.yBodyRot + 0.5f * diffY);
        }
    }
}
