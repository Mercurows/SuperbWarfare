package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

import static com.atsuishio.superbwarfare.tools.SeekTool.smokeFilter;

public interface AutoAimable {

    // 防御类载具实体搜寻周围实体
    default Entity seekNearLivingEntity(Entity attacker, Vec3 pos, double minAngle, double maxAngle, double minRange, double seekRange, double size) {
        for (Entity target : attacker.level().getEntitiesOfClass(Entity.class, new AABB(pos, pos).inflate(seekRange), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(pos))).toList()) {
            var condition = target.distanceToSqr(attacker) > minRange * minRange
                    && target.distanceToSqr(attacker) <= seekRange * seekRange
                    && canAim(pos, target, minAngle, maxAngle)
                    && !(target instanceof Player player && (player.isSpectator() || player.isCreative()))
                    && ((target instanceof LivingEntity living && living instanceof Enemy && living.getHealth() > 0) || isThreateningEntity(attacker, target, size, pos) || basicEnemyFilter(target))
                    && smokeFilter(target);
            if (condition) {
                return target;
            }
        }
        return null;
    }

    // 判断具有威胁的弹射物
    default boolean isThreateningEntity(Entity attacker, Entity target, double size, Vec3 pos) {
        if (target instanceof SmallCannonShellEntity) return false;
        if (!target.onGround() && target instanceof Projectile projectile && (target.getBbWidth() >= size || target.getBbHeight() >= size) &&
                VectorTool.calculateAngle(target.getDeltaMovement().normalize(), target.position().vectorTo(attacker.position()).normalize()) < 30) {
            return checkNoClip(attacker, target, pos) && basicEnemyProjectileFilter(projectile);
        }
        return false;
    }

    // 判断载具和目标之间有无障碍物
    default boolean checkNoClip(Entity attacker, Entity target, Vec3 pos) {
        return attacker.level().clip(new ClipContext(pos, target.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker)).getType() != HitResult.Type.BLOCK;
    }

    boolean basicEnemyFilter(Entity pEntity);

    boolean basicEnemyProjectileFilter(Projectile projectile);

    static boolean canAim(Vec3 pos, Entity target, double minAngle, double maxAngle) {
        Vec3 targetPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ());
        Vec3 toVec = pos.vectorTo(targetPos).normalize();
        double targetAngle = VehicleEntity.getXRotFromVector(toVec);
        return minAngle < targetAngle && targetAngle < maxAngle;
    }
}
