package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.tools.OBB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("RETURN"), cancellable = true)
    private static void getEntityHitResult(Level pLevel, Entity pProjectile, Vec3 pStartVec, Vec3 pEndVec, AABB pBoundingBox, Predicate<Entity> pFilter, float pInflationAmount, CallbackInfoReturnable<EntityHitResult> cir) {
        Entity res = null;

        for (var entity : pLevel.getEntities(pProjectile, pBoundingBox, pFilter)) {
            if (entity instanceof OBBEntity obbEntity) {
                OBB obb = obbEntity.getOBB().inflate(pInflationAmount * 2);

                Optional<Vector3f> optional = obb.clip(pStartVec.toVector3f(), pEndVec.toVector3f());
                if (optional.isPresent()) {
                    double d1 = pStartVec.distanceToSqr(new Vec3(optional.get()));
                    if (d1 < Double.MAX_VALUE) {
                        res = entity;
                    }
                    if (res != null) {
                        cir.setReturnValue(new EntityHitResult(res, new Vec3(optional.get())));
                    }
                }
            }
        }
    }
}
