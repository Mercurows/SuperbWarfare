package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.mixin.CustomStopRiding;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, priority = 1145)
public abstract class PlayerMixin extends Entity implements CustomStopRiding {

    public PlayerMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    /**
     * Code based on @Luke100000's ImmersiveAircraft
     */
    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    public void shouldDismountInjection(CallbackInfoReturnable<Boolean> cir) {
        if (this.getRootVehicle() instanceof VehicleEntity) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "updatePlayerPose()V", at = @At("TAIL"))
    public void updatePostInjection(CallbackInfo ci) {
        if (getRootVehicle() instanceof VehicleEntity) {
            this.setPose(Pose.STANDING);
        }
    }

    @Override
    public void superbwarfare$stopRiding() {
        Entity entity = this.getVehicle();
        this.removeVehicle();
        if (entity != null && entity != this.getVehicle() && !this.level().isClientSide) {
            Vec3 vec3;
            if (this.isRemoved()) {
                vec3 = this.position();
            } else if (!entity.isRemoved() && !this.level().getBlockState(entity.blockPosition()).is(BlockTags.PORTALS)) {
                vec3 = entity.getDismountLocationForPassenger((Player) (Object) this);
            } else {
                double d0 = Math.max(this.getY(), entity.getY());
                vec3 = new Vec3(this.getX(), d0, this.getZ());
            }

            this.dismountTo(vec3.x, vec3.y, vec3.z);
        }
    }
}
