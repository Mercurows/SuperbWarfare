package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.tools.ProjectileTool;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class MelonBombEntity extends DestroyableProjectile implements ExplosiveProjectile {

    public MelonBombEntity(EntityType<? extends MelonBombEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.explosionRadius = 10;
        this.explosionDamage = 500;
    }

    public MelonBombEntity(LivingEntity entity, Level level) {
        super(ModEntities.MELON_BOMB.get(), entity, level);
        this.noCulling = true;
        this.explosionRadius = 10;
        this.explosionDamage = 500;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return Items.MELON;
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

            ProjectileTool.causeCustomExplode(this, this.explosionDamage, this.explosionRadius, 1.5f);
            this.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 600 || this.entityData.get(HEALTH) <= 0) {
            this.discard();
            if (!this.level().isClientSide) {
                ProjectileTool.causeCustomExplode(this, this.explosionDamage, this.explosionRadius, 1.5f);
            }
        }
    }

    @Override
    public float getMaxHealth() {
        return 15;
    }
}
