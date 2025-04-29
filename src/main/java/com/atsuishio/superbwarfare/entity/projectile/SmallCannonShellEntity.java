package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

public class SmallCannonShellEntity extends FastThrowableProjectile implements GeoEntity {

    private float damage = 40.0f;
    private float explosionDamage = 80f;
    private float explosionRadius = 5f;
    private boolean antiAir = false;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SmallCannonShellEntity(EntityType<? extends SmallCannonShellEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public SmallCannonShellEntity(LivingEntity entity, Level level, float damage, float explosionDamage, float explosionRadius, boolean antiAir) {
        super(ModEntities.SMALL_CANNON_SHELL.get(), entity, level);
        this.damage = damage;
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
        this.antiAir = antiAir;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.SMALL_SHELL.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        if (this.level() instanceof ServerLevel) {
            Entity entity = result.getEntity();
            if (this.getOwner() instanceof LivingEntity living) {
                if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                    living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);
                    PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
                }
            }

            entity.hurt(ModDamageTypes.causeCannonFireDamage(this.level().registryAccess(), this, this.getOwner()), damage);

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            if (this.tickCount > 0) {
                causeExplode(result.getLocation());
            }
            this.discard();
        }
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        BlockPos resultPos = blockHitResult.getBlockPos();
        BlockState state = this.level().getBlockState(resultPos);
        if (state.getBlock() instanceof BellBlock bell) {
            bell.attemptToRing(this.level(), resultPos, blockHitResult.getDirection());
        }
        if (this.level() instanceof ServerLevel) {
            causeExplode(blockHitResult.getLocation());
        }
        this.discard();
    }

    private void causeExplode(Vec3 vec3) {
        CustomExplosion explosion = new CustomExplosion(this.level(), this,
                ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(),
                        this,
                        this.getOwner()),
                explosionDamage,
                vec3.x,
                vec3.y,
                vec3.z,
                explosionRadius,
                ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP).
                setDamageMultiplier(1.25f);
        explosion.explode();
        EventHooks.onExplosionStart(this.level(), explosion);
        explosion.finalizeExplosion(false);
        ParticleTool.spawnSmallExplosionParticles(this.level(), vec3);
    }

    private void causeAirExplode(Vec3 vec3) {
        CustomExplosion explosion = new CustomExplosion(this.level(), this,
                ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(),
                        this,
                        this.getOwner()),
                explosionDamage,
                vec3.x,
                vec3.y,
                vec3.z,
                explosionRadius,
                ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP).
                setDamageMultiplier(1.25f);
        explosion.explode();
        EventHooks.onExplosionStart(this.level(), explosion);
        explosion.finalizeExplosion(false);
        ParticleTool.spawnMediumExplosionParticles(this.level(), vec3);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            ParticleTool.sendParticle(serverLevel, ParticleTypes.SMOKE, this.xo, this.yo, this.zo,
                    1, 0, 0, 0, 0.02, true);
        }

        if (onGround()) {
            this.setDeltaMovement(0, 0, 0);
        }

        if (antiAir) {
            findEntityOnPath();
        }

        if (this.tickCount > 200 || this.isInWater()) {
            if (this.level() instanceof ServerLevel && !onGround()) {
                causeExplode(position());
            }
            this.discard();
        }
    }

    public void findEntityOnPath() {
        List<Entity> entities = this.level()
                .getEntities(this,
                        this.getBoundingBox()
                                .expandTowards(this.getDeltaMovement())
                                .inflate(0.1)
                );

        for (Entity entity : entities) {
            Entity target = StreamSupport.stream(EntityFindUtil.getEntities(level()).getAll().spliterator(), false)
                    .filter(e -> {
                        if (e == entity && e instanceof Projectile && !(e instanceof ProjectileEntity || e instanceof SmallCannonShellEntity)) {
                            return checkNoClip(e);
                        }
                        return false;
                    }).min(Comparator.comparingDouble(e -> e.distanceTo(this))).orElse(null);

            if (target != null) {
                target.discard();
                causeAirExplode(target.position());
            }

        }
    }

    public boolean checkNoClip(Entity target) {
        return level().clip(new ClipContext(this.getEyePosition(), target.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.BLOCK;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
