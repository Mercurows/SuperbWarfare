package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.Optional;

public class SmallCannonShellEntity extends FastThrowableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean aa;

    public SmallCannonShellEntity(EntityType<? extends SmallCannonShellEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.damage = 40f;
        this.explosionDamage = 80f;
        this.explosionRadius = 5f;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.SMALL_SHELL.get();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();

        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
            return;
        if (this.level() instanceof ServerLevel) {
            if (this.getOwner() instanceof LivingEntity living) {
                if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                    living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);
                    NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                }
            }

            DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), damage);

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            if (this.tickCount > 0) {
                causeExplode(result.getLocation(), true);
            }
            this.discard();
        }
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        BlockPos resultPos = blockHitResult.getBlockPos();
        BlockState state = this.level().getBlockState(resultPos);

        if (this.level() instanceof ServerLevel) {
            float hardness = this.level().getBlockState(resultPos).getBlock().defaultDestroyTime();
            if (hardness != -1) {
                if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                    boolean destroy = Math.random() < Mth.clamp(1 - (hardness / 50), 0.1, 1);
                    if (destroy) {
                        this.level().destroyBlock(resultPos, true);
                    }
                }
            }
        }

        if (state.getBlock() instanceof BellBlock bell) {
            bell.attemptToRing(this.level(), resultPos, blockHitResult.getDirection());
        }
        if (this.level() instanceof ServerLevel) {
            causeExplode(blockHitResult.getLocation(), false);
        }
        this.discard();
    }

    private void causeExplode(Vec3 vec3, boolean hitEntity) {
        new CustomExplosion.Builder(this)
                .attacker(this.getOwner())
                .damage(explosionDamage)
                .radius(explosionRadius)
                .position(vec3)
                .withParticleType(explosionParticleType(explosionRadius))
                .destroyBlock(() -> hitEntity ? Explosion.BlockInteraction.KEEP : (ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP))
                .damageMultiplier(1.25F)
                .explode();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > 200 || this.isInWater()) {
            if (this.level() instanceof ServerLevel && !onGround()) {
                causeExplode(position(), false);
            }
            this.discard();
        }

        if (aa) {
            crushProjectile(getDeltaMovement());
        }
    }

    public void crushProjectile(Vec3 velocity) {
        if (this.level() instanceof ServerLevel) {
            var frontBox = getBoundingBox().inflate(0.5).expandTowards(velocity);

            Optional<Projectile> target = level().getEntities(
                            EntityTypeTest.forClass(Projectile.class), frontBox, entity -> entity != this).stream()
                    .filter(entity -> !(entity instanceof SmallCannonShellEntity) && (entity.getBbWidth() >= 0.3 || entity.getBbHeight() >= 0.3))
                    .min(Comparator.comparingDouble(e -> e.position().distanceTo(position())));
            if (target.isPresent()) {
                causeExplode(target.get().position(), false);
                if (target.get() instanceof DestroyableProjectile destroyableProjectile) {
                    if (this.getOwner() instanceof LivingEntity living) {
                        if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                            living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);
                            NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                        }
                    }
                    DamageHandler.doDamage(destroyableProjectile, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), damage);
                } else {
                    target.get().discard();
                }

                this.discard();
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public void antiAir(boolean antiAir) {
        this.aa = antiAir;
    }

    @Override
    public boolean isFastMoving() {
        return false;
    }
}
