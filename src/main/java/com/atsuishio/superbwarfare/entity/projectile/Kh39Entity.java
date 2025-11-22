package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class Kh39Entity extends MissileProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Kh39Entity(EntityType<? extends Kh39Entity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.damage = 1100;
        this.explosionDamage = 180;
        this.explosionRadius = 12;
        this.distracted = false;
        this.durability = 25;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.LARGE_ANTI_GROUND_MISSILE.get();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (entity == this.getOwner() || (this.getOwner() != null && entity == this.getOwner().getVehicle()))
            return;
        if (this.level() instanceof ServerLevel) {
            if (this.getOwner() instanceof LivingEntity living) {
                if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                    living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                    PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
                }
            }

            DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            causeExplode(result.getLocation());

            discard();
        }
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
                    if (ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                        this.level().destroyBlock(resultPos, true);
                    }
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
    public void tick() {
        super.tick();

        largeTrail();

        Entity entity = EntityFindUtil.findEntity(this.level(), entityData.get(TARGET_UUID));
        List<Entity> decoy = SeekTool.seekLivingEntities(this, 32, 90);

        for (var e : decoy) {
            if (e.getType().is(ModTags.EntityTypes.DECOY) && !this.distracted) {
                this.entityData.set(TARGET_UUID, e.getStringUUID());
                this.distracted = true;
                break;
            }
        }

        Vec3 toVec = getLookAngle();

        if (guideType == 0) {
            if (!entityData.get(TARGET_UUID).equals("none")) {
                if (entity != null) {
                    if (level() instanceof ServerLevel) {
                        if ((!entity.getPassengers().isEmpty() || entity instanceof VehicleEntity) && entity.tickCount % ((int) Math.max(0.04 * this.distanceTo(entity), 2)) == 0) {
                            entity.level().playSound(null, entity.getOnPos(), entity instanceof Pig ? SoundEvents.PIG_HURT : ModSounds.MISSILE_WARNING.get(), SoundSource.PLAYERS, 2, 1f);
                        }
                        double dis = entity.position().vectorTo(position()).horizontalDistance();
                        double height = dis > 30 ? 0.4 * (dis - 30) : 0;
                        Vec3 targetPos = new Vec3(entity.getX(), entity.getY() + (entity instanceof EnderDragon ? -2 : 0) + height, entity.getZ());
                        toVec = RangeTool.calculateFiringSolution(position(), targetPos, entity.getDeltaMovement(), getDeltaMovement().length(), 0);
                    }
                }
            }
        } else {
            if (level() instanceof ServerLevel) {
                double dis = targetPos.vectorTo(position()).horizontalDistance();
                double height = dis > 30 ? 0.4 * (dis - 30) : 0;
                Vec3 targetPos = this.targetPos.add(0, height, 0);
                toVec = RangeTool.calculateFiringSolution(position(), targetPos, Vec3.ZERO, getDeltaMovement().length(), 0);
            }
        }

        if (this.tickCount > 8) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.05).add(getLookAngle().scale(8)));
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.85, 0.85, 0.85));
            boolean lostTarget = (VectorTool.calculateAngle(getLookAngle(), toVec) > 170);
            if (!lostTarget) {
                turn(toVec, Mth.clamp((tickCount - 8) * 0.5f, 0, 15));
            }
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.06, 0));
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.99, 0.99));
        }

        if (this.tickCount == 8) {
            this.level().playSound(null, BlockPos.containing(position()), ModSounds.MISSILE_START.get(), SoundSource.PLAYERS, 4, 1);
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, this.xo, this.yo, this.zo, 15, 0.8, 0.8, 0.8, 0.01, true);
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.xo, this.yo, this.zo, 10, 0.8, 0.8, 0.8, 0.01, true);
            }
        }

        if (this.tickCount > 600 || this.isInWater() || this.entityData.get(HEALTH) <= 0) {
            if (this.level() instanceof ServerLevel) {
                ProjectileTool.causeCustomExplode(this,
                        ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), this, this.getOwner()),
                        this, this.explosionDamage, this.explosionRadius, 1);
            }
            this.discard();
        }

        destroyBlock();
    }

    private PlayState movementPredicate(AnimationState<Kh39Entity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.jvm.idle"));
    }

    @Override
    public double getDefaultGravity() {
        return tickCount < 8 ? 0.15F : super.getGravity();
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
    public @NotNull SoundEvent getCloseSound() {
        return ModSounds.ROCKET_ENGINE.get();
    }

    @Override
    public @NotNull SoundEvent getSound() {
        return ModSounds.ROCKET_FLY.get();
    }

    @Override
    public float getVolume() {
        return 0.7f;
    }

    @Override
    public float getMaxHealth() {
        return 70;
    }
}
