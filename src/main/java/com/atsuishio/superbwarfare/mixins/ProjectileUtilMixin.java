package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
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

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("HEAD"), cancellable = true)
    private static void getEntityHitResult(Level pLevel, Entity pProjectile, Vec3 pStartVec, Vec3 pEndVec, AABB pBoundingBox, Predicate<Entity> pFilter, float pInflationAmount, CallbackInfoReturnable<EntityHitResult> cir) {
        for (var entity : pLevel.getEntities(pProjectile, pBoundingBox, pFilter)) {
            if (entity instanceof OBBEntity obbEntity) {
                if (pProjectile instanceof Projectile projectile &&
                        (projectile.getOwner() == entity || entity.getPassengers().contains(projectile.getOwner()))) {
                    continue;
                }
                var obbList = obbEntity.getOBBs();
                for (var obb : obbList) {
                    obb = obb.inflate(entity.getPickRadius() * 2);
                    Optional<Vector3f> optional = obb.clip(pStartVec.toVector3f(), pEndVec.toVector3f());
                    double pDistance = pStartVec.distanceToSqr(pEndVec);
                    if (obb.contains(pStartVec)) {
                        if (pDistance >= 0) {
                            EntityHitResult hitResult = new EntityHitResult(entity, new Vec3(optional.orElse(pStartVec.toVector3f())));
                            var acc = OBBHitter.getInstance(pProjectile);
                            acc.sbw$setCurrentHitPart(obb.part());
                            cir.setReturnValue(hitResult);
                            if (pLevel instanceof ServerLevel serverLevel && pProjectile.getDeltaMovement().lengthSqr() > 0.01 && pProjectile instanceof Projectile) {
                                Vec3 hitPos = hitResult.getLocation();
                                pLevel.playSound(null, BlockPos.containing(hitPos), ModSounds.HIT.get(), SoundSource.PLAYERS, 1, 1);
                                sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), hitPos.x, hitPos.y, hitPos.z, 2, 0, 0, 0, 0.2, false);
                                sendParticle(serverLevel, ParticleTypes.SMOKE, hitPos.x, hitPos.y, hitPos.z, 2, 0, 0, 0, 0.01, false);
                            }
                            return;
                        }
                    } else if (optional.isPresent()) {
                        var vec = new Vec3(optional.get());
                        double d1 = pStartVec.distanceToSqr(vec);
                        if (d1 < pDistance || pDistance == 0) {
                            EntityHitResult hitResult = new EntityHitResult(entity, vec);
                            var acc = OBBHitter.getInstance(pProjectile);
                            acc.sbw$setCurrentHitPart(obb.part());
                            cir.setReturnValue(hitResult);
                            if (pLevel instanceof ServerLevel serverLevel && pProjectile.getDeltaMovement().lengthSqr() > 0.01 && pProjectile instanceof Projectile) {
                                Vec3 hitPos = hitResult.getLocation();
                                pLevel.playSound(null, BlockPos.containing(hitPos), ModSounds.HIT.get(), SoundSource.PLAYERS, 1, 1);
                                sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), hitPos.x, hitPos.y, hitPos.z, 2, 0, 0, 0, 0.2, false);
                                sendParticle(serverLevel, ParticleTypes.SMOKE, hitPos.x, hitPos.y, hitPos.z, 2, 0, 0, 0, 0.01, false);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("HEAD"), cancellable = true)
    private static void getEntityHitResult(Entity pShooter, Vec3 pStartVec, Vec3 pEndVec, AABB pBoundingBox, Predicate<Entity> pFilter, double pDistance, CallbackInfoReturnable<EntityHitResult> cir) {
        Level level = pShooter.level();
        var entities = level.getEntities(pShooter, pBoundingBox.inflate(5), pFilter);

        for (Entity entity : entities) {
            if (!(entity instanceof OBBEntity obbEntity)) {
                continue;
            }

            if (entity.getPassengers().contains(pShooter)) {
                continue;
            }

            var obbList = obbEntity.getOBBs();
            for (var obb : obbList) {
                obb = obb.inflate(entity.getPickRadius() * 2);
                Optional<Vector3f> optional = obb.clip(pStartVec.toVector3f(), pEndVec.toVector3f());
                if (obb.contains(pStartVec)) {
                    if (pDistance >= 0) {
                        cir.setReturnValue(new EntityHitResult(entity, new Vec3(optional.orElse(pStartVec.toVector3f()))));
                        return;
                    }
                } else if (optional.isPresent()) {
                    var vec = new Vec3(optional.get());
                    double d1 = pStartVec.distanceToSqr(vec);
                    if (d1 < pDistance || pDistance == 0) {
                        if (entity.getRootVehicle() == pShooter.getRootVehicle() && !entity.canRiderInteract()) {
                            if (pDistance == 0) {
                                cir.setReturnValue(new EntityHitResult(entity, vec));
                                return;
                            }
                        } else {
                            cir.setReturnValue(new EntityHitResult(entity, vec));
                            return;
                        }
                    }
                }
            }
        }
    }
}
