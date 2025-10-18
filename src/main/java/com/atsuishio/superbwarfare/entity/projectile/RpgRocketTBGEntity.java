package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.ProjectileTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
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

public class RpgRocketTBGEntity extends FastThrowableProjectile implements GeoEntity, ExplosiveProjectile {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public float damage = 250f;
    public float explosionDamage = 200f;
    public float explosionRadius = 10;
    public float gravity = 0.03f;

    public RpgRocketTBGEntity(EntityType<? extends RpgRocketTBGEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
        this.durability = 20;
    }

    public RpgRocketTBGEntity(EntityType<? extends ThrowableItemProjectile> pEntityType, double pX, double pY, double pZ, Level pLevel, float damage, float explosionDamage, float explosionRadius, float gravity) {
        super(pEntityType, pX, pY, pZ, pLevel);
        this.noCulling = true;
        this.durability = 20;
        this.damage = damage;
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
        this.gravity = gravity;
    }

    public RpgRocketTBGEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.RPG_ROCKET_TBG.get(), level);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.RPG_ROCKET_TBG_TBG.get();
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (this.level() instanceof ServerLevel) {
            BlockPos resultPos = blockHitResult.getBlockPos();
            float hardness = this.level().getBlockState(resultPos).getBlock().defaultDestroyTime();
            if (hardness != -1) {
                if (ExplosionConfig.EXPLOSION_DESTROY.get()) {
                    if (firstHit) {
                        causeExplode(blockHitResult.getLocation());
                        firstHit = false;
                        Mod.queueServerWork(3, this::discard);
                    }
                    this.level().destroyBlock(resultPos, true);
                }
            } else {
                causeExplode(blockHitResult.getLocation());
                this.discard();
            }
            if (!ExplosionConfig.EXPLOSION_DESTROY.get()) {
                causeExplode(blockHitResult.getLocation());
                this.discard();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
            return;
        if (this.level() instanceof ServerLevel) {
            if (entity == this.getOwner() || (this.getOwner() != null && entity == this.getOwner().getVehicle()))
                return;
            if (this.getOwner() instanceof LivingEntity living) {
                if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                    living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                    Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                }
            }

            DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            causeExplode(result.getLocation());
            this.discard();
        }
    }

    @Override
    public void causeExplode(Vec3 vec3) {
        new CustomExplosion.Builder(this)
                .attacker(this.getOwner())
                .damage(explosionDamage)
                .radius(explosionRadius)
                .position(vec3)
                .withParticleType(explosionRadius >= 10 ? ParticleTool.ParticleType.HUGE : ParticleTool.ParticleType.MEDIUM)
                .explode();
    }

    @Override
    public void tick() {
        super.tick();

        mediumTrail();

        if (this.tickCount == 3) {
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, this.xo, this.yo, this.zo, 15, 0.8, 0.8, 0.8, 0.01, true);
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.xo, this.yo, this.zo, 10, 0.8, 0.8, 0.8, 0.01, true);
            }
        }
        if (this.tickCount > 2) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.03, 1.03, 1.03));
        }

        if (this.tickCount > 100 || this.isInWater()) {
            if (this.level() instanceof ServerLevel) {
                ProjectileTool.causeCustomExplode(this, this.explosionDamage, this.explosionRadius);
            }
            this.discard();
        }
        destroyBlock();
    }

    private PlayState movementPredicate(AnimationState<RpgRocketTBGEntity> event) {
        if (getDeltaMovement().lengthSqr() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.rpg.idle"));
        } else {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.rpg.none"));
        }
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
    public boolean shouldSyncMotion() {
        return true;
    }

    @Override
    public @NotNull SoundEvent getCloseSound() {
        return ModSounds.ROCKET_ENGINE.get();
    }

    @Override
    public @NotNull SoundEvent getSound() {
        return ModSounds.ROCKET_FLY.get();
    }

    @Override
    public boolean forceLoadChunk() {
        return true;
    }


}
