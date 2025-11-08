package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Set;

@Mixin(Explosion.class)
public class ExplosionMixin {

    @Final
    @Shadow
    private double x;

    @Final
    @Shadow
    private double y;

    @Final
    @Shadow
    private double z;

    @Inject(method = "explode()V",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Ljava/util/List;get(I)Ljava/lang/Object;",
                    ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void explode(CallbackInfo ci, Set<?> set, int i, float f2, int k1, int l1, int i2, int i1, int j2, int j1, List list, Vec3 vec3, int k2) {
        if (list.size() >= k2) {
            var obj = list.get(k2);
            if (obj instanceof VehicleEntity vehicle) {
                if (!vehicle.ignoreExplosion()) {
                    double d12 = Math.sqrt(vehicle.distanceToSqr(vec3)) / (double) f2;
                    if (d12 <= 1) {
                        double d5 = vehicle.getX() - this.x;
                        double d7 = vehicle.getEyeY() - this.y;
                        double d9 = vehicle.getZ() - this.z;
                        double d13 = Math.sqrt(d5 * d5 + d7 * d7 + d9 * d9);
                        if (d13 != 0) {
                            d5 /= d13;
                            d7 /= d13;
                            d9 /= d13;
                            double d14 = Explosion.getSeenPercent(vec3, vehicle);
                            double d10 = (1 - d12) * d14;

                            d5 *= d10;
                            d7 *= d10;
                            d9 *= d10;
                            Vec3 vec31 = new Vec3(d5, d7, d9).multiply(-1, -1, -1);
                            vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vec31));
                        }
                    }
                }
            }
        }
    }
}
