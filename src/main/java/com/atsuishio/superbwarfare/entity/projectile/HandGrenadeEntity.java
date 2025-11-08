package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.ProjectileTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HandGrenadeEntity extends FastThrowableProjectile implements GeoEntity, ExplosiveProjectile {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int fuse = 100;

    public HandGrenadeEntity(EntityType<? extends HandGrenadeEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.damage = 1;
        this.explosionDamage = ExplosionConfig.M67_GRENADE_EXPLOSION_DAMAGE.get();
        this.explosionRadius = ExplosionConfig.M67_GRENADE_EXPLOSION_RADIUS.get();
    }

    public HandGrenadeEntity(EntityType<? extends HandGrenadeEntity> type, double x, double y, double z, Level level) {
        super(type, x, y, z, level);
        this.noCulling = true;
        this.damage = 1;
        this.explosionDamage = ExplosionConfig.M67_GRENADE_EXPLOSION_DAMAGE.get();
        this.explosionRadius = ExplosionConfig.M67_GRENADE_EXPLOSION_RADIUS.get();
    }

    public HandGrenadeEntity(LivingEntity entity, Level level, int fuse) {
        super(ModEntities.HAND_GRENADE.get(), entity, level);
        this.noCulling = true;
        this.damage = 1;
        this.explosionDamage = ExplosionConfig.M67_GRENADE_EXPLOSION_DAMAGE.get();
        this.explosionRadius = ExplosionConfig.M67_GRENADE_EXPLOSION_RADIUS.get();
        this.fuse = fuse;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.HAND_GRENADE.get();
    }

    @Override
    protected void onHit(HitResult result) {
        switch (result.getType()) {
            case BLOCK:
                BlockHitResult blockResult = (BlockHitResult) result;
                BlockPos resultPos = blockResult.getBlockPos();
                BlockState state = this.level().getBlockState(resultPos);
                SoundEvent event = state.getBlock().getSoundType(state, this.level(), resultPos, this).getBreakSound();
                double speed = this.getDeltaMovement().length();
                if (speed > 0.1) {
                    this.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z, event, SoundSource.AMBIENT, 1F, 1F);
                }
                this.bounce(blockResult.getDirection());

                if (state.getBlock() instanceof BellBlock bell) {
                    bell.attemptToRing(this.level(), resultPos, blockResult.getDirection());
                }

                break;
            case ENTITY:
                EntityHitResult entityResult = (EntityHitResult) result;
                Entity entity = entityResult.getEntity();
                if (entity == this.getOwner() || entity == this.getVehicle()) return;
                double speed_e = this.getDeltaMovement().length();
                if (speed_e > 0.1) {
                    if (this.getOwner() instanceof LivingEntity living) {
                        if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                            living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                            PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
                        }
                    }
                    entity.hurt(entity.damageSources().thrown(this, this.getOwner()), this.damage);
                }
                this.bounce(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.25, 1.0, 0.25));
                break;
            default:
                break;
        }
    }

    private void bounce(Direction direction) {
        switch (direction.getAxis()) {
            case X:
                this.setDeltaMovement(this.getDeltaMovement().multiply(-0.5, 0.75, 0.75));
                break;
            case Y:
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.75, -0.25, 0.75));
                if (this.getDeltaMovement().y() < this.getGravity()) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0, 1));
                }
                break;
            case Z:
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.75, 0.75, -0.5));
                break;
        }
    }

    @Override
    public void tick() {
        super.tick();
        --this.fuse;

        if (this.fuse <= 0) {
            this.discard();
            if (!this.level().isClientSide) {
                ProjectileTool.causeCustomExplode(this, this.explosionDamage, this.explosionRadius, 1.2f);
            }
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            ParticleTool.sendParticle(serverLevel, ParticleTypes.SMOKE, this.xo, this.yo, this.zo,
                    1, 0, 0, 0, 0.01, true);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
