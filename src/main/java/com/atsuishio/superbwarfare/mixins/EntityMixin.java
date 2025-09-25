package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.mixin.OBBHitter;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

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
    public abstract boolean isPassengerOfSameVehicle(Entity pEntity);

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public boolean noPhysics;

    @Shadow
    public abstract void setDeltaMovement(Vec3 pDeltaMovement);

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Inject(method = "collide", at = @At("HEAD"))
    private void sbw$spoofGroundStart(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (VehicleEntity.IGNORE_ENTITY_GROUND_CHECK_STEPPING) {
            this.sbw$cacheOnGround = this.onGround;
            this.onGround = true;
        }
    }

    @Inject(method = "collide", at = @At("TAIL"))
    private void sbw$spoofGroundEnd(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if (VehicleEntity.IGNORE_ENTITY_GROUND_CHECK_STEPPING) {
            this.onGround = this.sbw$cacheOnGround;
            VehicleEntity.IGNORE_ENTITY_GROUND_CHECK_STEPPING = false;
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
//    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
//    private void onHitOBB(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
//        AABB boundingBox = this.getBoundingBox();
//        Entity self = (Entity) (Object) this;
//        var list = this.level().getEntities(self, boundingBox.expandTowards(movement).inflate(1), e -> true);
//        var entity = list.stream().filter(e -> e instanceof OBBEntity).min((e1, e2) -> (int) (e1.position().distanceTo(self.position()) - e2.position().distanceTo(self.position()))).orElse(null);
//        if (entity == null || entity == self) return;
//
//        OBBEntity obbEntity = (OBBEntity) entity;
//        Vec3 position = self.position();
//        // 第一版实现
//        var faceInfo = OBB.findClosestFace(obbEntity.getOBBs(), position);
//        if (faceInfo == null) return;
//        double dot = movement.dot(new Vec3(faceInfo.faceNormal()));
//        var vec = new Vec3(faceInfo.faceNormal()).multiply(dot, dot, dot);
//
//        if (self instanceof Player player) {
//            player.displayClientMessage(Component.literal("Vec: [" + vec.x + ", " + vec.y + ", " + vec.z + "]," +
//                    " Face: [" + faceInfo.faceNormal().x + ", " + faceInfo.faceNormal().y + ", " + faceInfo.faceNormal().z + "]"), true);
//        }
//
//        cir.setReturnValue(movement.subtract(vec));
//    }

    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    public void turn(double pYRot, double pXRot, CallbackInfo ci) {
        var entity = (Entity) (Object) this;
        if (entity instanceof Player player && player.getMainHandItem().getItem() instanceof GunItem && isProne(player) && !player.isSwimming()) {
            ci.cancel();
            float f = (float) pXRot * 0.15F;
            float f1 = (float) pYRot * 0.15F;
            player.setXRot(player.getXRot() + f);
            player.setYRot(player.getYRot() + f1);
            Vec3 forward = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
            if (player.level().getBlockState(BlockPos.containing(player.getX() + 0.25 * forward.x, player.getY() - 0.1, player.getZ() + 0.25 * forward.z)).canOcclude()) {
                player.setXRot(Mth.clamp(player.getXRot(), -45.0F, 30.0F));
            } else {
                player.setXRot(Mth.clamp(player.getXRot(), -45.0F, 89.0F));
            }
            player.xRotO += f;
            player.yRotO += f1;
            player.xRotO = Mth.clamp(player.xRotO, -90.0F, 90.0F);

            float diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot));
            player.setYBodyRot(player.yBodyRot + 0.5f * diffY);

            if (player.getVehicle() != null) {
                player.getVehicle().onPassengerTurned(player);
            }
        }
    }

//    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
//    public void push(Entity entity, CallbackInfo ci) {
//        //noinspection ConstantValue
//        if (!(((Entity) (Object) this) instanceof VehicleEntity) && entity instanceof VehicleEntity vehicle && vehicle instanceof OBBEntity obbEntity) {
//            if (this.isPassengerOfSameVehicle(entity)) {
//                ci.cancel();
//                return;
//            }
//            var feetPos = this.position().add(new Vec3(0, 0.001f, 0));
//            for (OBB obb : obbEntity.getOBBs()) {
//                if (obb.contains(feetPos)) {
//                    if (!entity.noPhysics && !this.noPhysics) {
//                        this.setDeltaMovement(
//                                new Vec3(
//                                        this.getDeltaMovement().x + entity.getDeltaMovement().x,
//                                        Math.max(0, this.getDeltaMovement().y + entity.getDeltaMovement().y),
//                                        this.getDeltaMovement().z + entity.getDeltaMovement().z
//                                )
//                        );
//                    }
//                }
//            }
//        }
//    }
}
