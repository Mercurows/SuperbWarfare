package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Mk82Entity extends DestroyableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Mk82Entity(EntityType<? extends Mk82Entity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.explosionRadius = 22;
        this.explosionDamage = 650;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        var entity = source.getDirectEntity();
        if (entity instanceof Mk82Entity mk82Entity && mk82Entity.getOwner() == this.getOwner()) {
            return false;
        }

        return super.hurt(source, amount);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.MEDIUM_AERIAL_BOMB.get();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (entity == this.getOwner() || (this.getOwner() != null && entity == this.getOwner().getVehicle()) || entity instanceof Mk82Entity)
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

    private PlayState movementPredicate(AnimationState<Mk82Entity> event) {
        return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.mk_82.start"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public @NotNull SoundEvent getSound() {
        return ModSounds.SHELL_FLY.get();
    }

    @Override
    public float getVolume() {
        return 0.7f;
    }

    @Override
    public float getMaxHealth() {
        return 50;
    }
}
