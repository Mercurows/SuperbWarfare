package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class MelonBombEntity extends DestroyableProjectile {

    public MelonBombEntity(EntityType<? extends MelonBombEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.explosionRadius = 10;
        this.explosionDamage = 500;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return Items.MELON;
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (entity == this.getOwner() || (this.getOwner() != null && entity == this.getOwner().getVehicle()) || entity instanceof MelonBombEntity)
            return;
        if (this.level() instanceof ServerLevel) {
            if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                AABB aabb = new AABB(result.getLocation(), result.getLocation()).inflate(5);
                BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
                    float hard = this.level().getBlockState(pos).getBlock().defaultDestroyTime();
                    if (hard != -1 && new Vec3(pos.getX(), pos.getY(), pos.getZ()).distanceTo(result.getLocation()) < 3) {
                        this.level().destroyBlock(pos, true);
                    }
                });
            }

            causeExplode(result.getLocation());
            this.discard();
        }
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (this.level() instanceof ServerLevel) {
            if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                AABB aabb = new AABB(blockHitResult.getLocation(), blockHitResult.getLocation()).inflate(5);
                BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
                    float hard = this.level().getBlockState(pos).getBlock().defaultDestroyTime();
                    if (hard != -1 && new Vec3(pos.getX(), pos.getY(), pos.getZ()).distanceTo(blockHitResult.getLocation()) < 3) {
                        this.level().destroyBlock(pos, true);
                    }
                });
            }

            causeExplode(blockHitResult.getLocation());
            this.discard();
        }
    }

    @Override
    public float getMaxHealth() {
        return 15;
    }

    @Override
    public @NotNull SoundEvent getSound() {
        return ModSounds.SHELL_FLY.get();
    }

    @Override
    public float getVolume() {
        return 0.7f;
    }
}
