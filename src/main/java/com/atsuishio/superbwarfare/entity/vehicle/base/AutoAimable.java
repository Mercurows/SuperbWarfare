package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.stream.StreamSupport;

import static com.atsuishio.superbwarfare.tools.SeekTool.smokeFilter;

public interface AutoAimable {

    // 防御类载具实体搜寻周围实体
    default Entity seekNearLivingEntity(Entity attacker, Vec3 pos, double minAngle, double maxAngle, double minRange, double seekRange, double size) {
        return StreamSupport.stream(EntityFindUtil.getEntities(attacker.level()).getAll().spliterator(), false)
                .filter(e -> {
                    // TODO 自定义目标列表
                    if (e.distanceTo(attacker) > minRange
                            && e.distanceTo(attacker) <= seekRange
                            && canAim(pos, e, minAngle, maxAngle)
                            && !(e instanceof Player player && (player.isSpectator() || player.isCreative()))
                            && ((e instanceof LivingEntity living && living instanceof Enemy && living.getHealth() > 0) || e == seekThreateningEntity(attacker, size, pos) || basicEnemyFilter(e))
                            && smokeFilter(e)) {
                        return checkNoClip(attacker, e, pos);
                    }
                    return false;
                }).min(Comparator.comparingDouble(e -> e.distanceTo(attacker))).orElse(null);
    }

    // 判断具有威胁的弹射物
    default Entity seekThreateningEntity(Entity attacker, double size, Vec3 pos) {
        return StreamSupport.stream(EntityFindUtil.getEntities(attacker.level()).getAll().spliterator(), false)
                .filter(e -> {
                    if (!e.onGround() && e instanceof Projectile projectile && (e.getBbWidth() >= size || e.getBbHeight() >= size) &&
                            VectorTool.calculateAngle(e.getDeltaMovement().normalize(), e.position().vectorTo(attacker.position()).normalize()) < 30) {
                        return checkNoClip(attacker, e, pos) && basicEnemyProjectileFilter(projectile);
                    }
                    return false;
                }).min(Comparator.comparingDouble(e -> e.distanceTo(attacker))).orElse(null);
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
