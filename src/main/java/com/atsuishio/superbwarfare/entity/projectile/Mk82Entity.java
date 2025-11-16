package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.ProjectileTool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Mk82Entity extends DestroyableProjectile implements GeoEntity, ExplosiveProjectile {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Mk82Entity(EntityType<? extends Mk82Entity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.explosionRadius = 22;
        this.explosionDamage = 650;
        this.gravity = 0.06f;
    }

    public Mk82Entity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.MK_82.get(), level);
    }

    public Mk82Entity(EntityType<? extends ThrowableItemProjectile> pEntityType, double pX, double pY, double pZ, Level pLevel) {
        super(pEntityType, pX, pY, pZ, pLevel);
        this.noCulling = true;
        this.explosionRadius = 22;
        this.explosionDamage = 650;
        this.gravity = 0.06f;
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
    public boolean shouldSyncMotion() {
        return true;
    }
}
