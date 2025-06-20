package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    public abstract Vec3 getEyePosition();

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

    // TODO 优化OBB面算法并排除AABB影响，现在下车就动不了了
    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void onHitOBB(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        AABB boundingBox = this.getBoundingBox();
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player) {
            boundingBox = player.getLocalBoundsForPose(player.getPose());
        }
        var list = this.level().getEntities(self, boundingBox.expandTowards(movement));
        var entity = list.stream().filter(e -> e instanceof OBBEntity).findFirst().orElse(null);
        if (entity == null || entity == self) return;
        OBBEntity obbEntity = (OBBEntity) entity;
        Vec3 feetPos = this.position().subtract(this.getEyePosition());
        // 第一版实现
        var faceInfo = OBB.findClosestFace(obbEntity.getOBBs(), feetPos);
        if (faceInfo == null) return;
        double dot = movement.dot(new Vec3(faceInfo.faceNormal()));
        var vec = new Vec3(faceInfo.faceNormal()).multiply(dot, dot, dot);
        cir.setReturnValue(movement.subtract(vec));
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
