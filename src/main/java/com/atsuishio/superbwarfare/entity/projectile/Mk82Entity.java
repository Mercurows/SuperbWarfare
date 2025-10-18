package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.ProjectileTool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Mk82Entity extends DestroyableProjectile implements GeoEntity, ExplosiveProjectile {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public float explosionDamage = ExplosionConfig.MK_82_EXPLOSION_DAMAGE.get();
    public float explosionRadius = ExplosionConfig.MK_82_EXPLOSION_RADIUS.get().floatValue();

    public Mk82Entity(EntityType<? extends Mk82Entity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public Mk82Entity(LivingEntity entity, Level level) {
        super(ModEntities.MK_82.get(), entity, level);
        this.noCulling = true;
    }

    public Mk82Entity(EntityType<? extends ThrowableItemProjectile> pEntityType, double pX, double pY, double pZ, Level pLevel) {
        super(pEntityType, pX, pY, pZ, pLevel);
        this.noCulling = true;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.MEDIUM_AERIAL_BOMB.get();
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (this.level() instanceof ServerLevel) {
            ProjectileTool.causeCustomExplode(this, this.explosionDamage, this.explosionRadius, 1.2f);
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount > 600 || this.entityData.get(HEALTH) <= 0) {
            if (!this.level().isClientSide) {
                ProjectileTool.causeCustomExplode(this, this.explosionDamage, this.explosionRadius, 1.2f);
            }
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
    public double getDefaultGravity() {
        return 0.06;
    }

    @Override
    public boolean shouldSyncMotion() {
        return true;
    }
}
