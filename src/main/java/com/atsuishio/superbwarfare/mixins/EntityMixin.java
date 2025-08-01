package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.mixin.OBBHitter;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.util.Mth;
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
        if (entity instanceof Player player && player.getMainHandItem().getItem() instanceof GunItem && isProne(player)) {
            ci.cancel();
            float f = (float) pXRot * 0.15F;
            float f1 = (float) pYRot * 0.15F;
            player.setXRot(player.getXRot() + f);
            player.setYRot(player.getYRot() + f1);
            player.setXRot(Mth.clamp(player.getXRot(), -45.0F, 30.0F));
            player.xRotO += f;
            player.yRotO += f1;
            player.xRotO = Mth.clamp(player.xRotO, -90.0F, 90.0F);
            if (player.getVehicle() != null) {
                player.getVehicle().onPassengerTurned(player);
            }
        }
    }
}
