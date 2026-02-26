package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;

public class MediumRocketEntity extends FastThrowableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public enum Type {
        AP, HE, CM
    }

    private Type type = Type.AP;
    private float fireProbability = 0;
    private int fireTime = 0;
    private int spreadAmount = 50;
    private int spreadAngle = 15;

    public MediumRocketEntity(EntityType<? extends MediumRocketEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public MediumRocketEntity(EntityType<? extends ThrowableItemProjectile> pEntityType, double pX, double pY, double pZ, Level pLevel, float damage, float radius, float explosionDamage, float fireProbability, int fireTime, Type type, int spreadAmount, int spreadAngle) {
        super(pEntityType, pX, pY, pZ, pLevel);
        this.noCulling = true;
        this.damage = damage;
        this.explosionRadius = radius;
        this.explosionDamage = explosionDamage;
        this.fireProbability = fireProbability;
        this.fireTime = fireTime;
        this.type = type;
        this.spreadAmount = spreadAmount;
        this.spreadAngle = spreadAngle;
    }

    public MediumRocketEntity durability(int durability) {
        this.durability = durability;
        return this;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean isColliding(BlockPos pPos, BlockState pState) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("FireProbability", this.fireProbability);
        pCompound.putInt("FireTime", this.fireTime);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("FireProbability")) {
            this.fireProbability = pCompound.getFloat("FireProbability");
        }

        if (pCompound.contains("FireTime")) {
            this.fireTime = pCompound.getInt("FireTime");
        }
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.SMALL_ROCKET.get();
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (this.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = blockHitResult.getBlockPos();
            BlockState blockState = level().getBlockState(pos);
            if (type == Type.HE || type == Type.CM) {
                causeExplode(blockHitResult.getLocation());
                this.discard();
                return;
            }
            if (ExplosionConfig.EXPLOSION_DESTROY.get()) {
                float hardness = this.level().getBlockState(pos).getBlock().defaultDestroyTime();

                double resistance = 0.95 - Mth.clamp(hardness / 100, 0, 1);

                if (blockState.canOcclude() || blockState.getSoundType() == SoundType.GLASS) {
                    durability -= 5 + (int) (hardness);
                }

                if (blockState.getSoundType() == SoundType.STONE) {
                    durability -= 5;
                }

                if (blockState.getSoundType() == SoundType.METAL || blockState.getSoundType() == SoundType.COPPER || blockState.getSoundType() == SoundType.NETHERITE_BLOCK) {
                    durability -= 25;
                }

                if (hardness <= durability && hardness != -1) {
                    this.level().destroyBlock(pos, true);
                }

                if (hardness == -1 || hardness > durability || durability <= 0) {
                    causeExplode(pos.getCenter());
                    discard();
                } else {
                    ParticleTool.cannonHitParticles(serverLevel, blockHitResult.getLocation());
                    MediumRocketEntity mediumRocket = new MediumRocketEntity(ModEntities.MEDIUM_ROCKET.get(), serverLevel);
                    mediumRocket.setPos(blockHitResult.getLocation().add(getDeltaMovement().normalize().scale(0.99)));
                    mediumRocket.shoot(getDeltaMovement().x, getDeltaMovement().y - gravity, getDeltaMovement().z, (float) (getDeltaMovement().length() * resistance), 0);
                    mediumRocket.setOwner(getOwner());
                    mediumRocket.durability(durability);
                    mediumRocket.setType(Type.AP);
                    mediumRocket.setGravity(gravity);
                    mediumRocket.setLife(life - tickCount);
                    mediumRocket.setDamage((float) (damage * resistance));
                    mediumRocket.setExplosionDamage((float) (explosionDamage * resistance));
                    mediumRocket.setExplosionRadius((float) (explosionRadius * resistance));
                    serverLevel.addFreshEntity(mediumRocket);
                    discard();
                }
            } else {
                destroyBlock(blockHitResult);
            }
        }
    }

    @Override
    public void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (tickCount < 2) return;
        if (this.level() instanceof ServerLevel serverLevel) {
            Entity entity = entityHitResult.getEntity();
            if (this.getOwner() != null && entity == this.getOwner().getVehicle())
                return;

            DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);
            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            if (entity instanceof VehicleEntity) {
                causeExplode(entityHitResult.getLocation());
                this.discard();
            }

            if (type == Type.AP) {
                Vec3 pos = entity.getBoundingBox().getCenter();
                Predicate<Entity> predicate = (entity1) -> true;
                List<TraceTool.RayTraceResultEntity> resultEntities = TraceTool.getEntitiesAlongVector(serverLevel, pos, getDeltaMovement(), predicate);
                double resistance = 1;

                for (TraceTool.RayTraceResultEntity rayTraceResultEntity : resultEntities) {
                    if (rayTraceResultEntity.entity != null) {
                        resistance *= 0.95;
                        Entity target = rayTraceResultEntity.entity;
                        if (rayTraceResultEntity.entity != entity) {
                            DamageHandler.doDamage(target, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), (float) (this.damage * resistance));
                            if (target instanceof LivingEntity) {
                                target.invulnerableTime = 0;
                            }
                        }
                    }
                }

                setDeltaMovement(getDeltaMovement().scale(resistance));
                setDamage((float) (this.damage * resistance));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        largeTrail();

        if (type == Type.CM) {
            // 使用Minecraft内置的光线追踪进行碰撞检测
            BlockHitResult hitResult = level().clip(new ClipContext(
                    position(),
                    position().add(getDeltaMovement().scale(8)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.ANY,
                    this
            ));

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                releaseClusterMunitions(getOwner());
            }
        }
    }

    @Override
    public void syncMotion() {
        if (!this.level().isClientSide) {
            PacketDistributor.sendToPlayersTrackingEntity(this, new ClientMotionSyncMessage(this));
        }
    }

    @Override
    public boolean discardAfterExplode() {
        return true;
    }

    private void releaseClusterMunitions(Entity shooter) {
        if (level() instanceof ServerLevel serverLevel) {
            ParticleTool.spawnMediumExplosionParticles(serverLevel, position());
            for (int index0 = 0; index0 < spreadAmount; index0++) {
                GunGrenadeEntity gunGrenadeEntity = new GunGrenadeEntity(shooter, serverLevel,
                        6 * damage / spreadAmount,
                        5 * explosionDamage / spreadAmount,
                        explosionRadius / 2
                );

                gunGrenadeEntity.setPos(position().x, position().y, position().z);
                gunGrenadeEntity.shoot(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z, (float) (random.nextFloat() * 0.2f + 0.4f * getDeltaMovement().length()),
                        spreadAngle);
                serverLevel.addFreshEntity(gunGrenadeEntity);
            }
            discard();
        }
    }

    private PlayState movementPredicate(AnimationState<MediumRocketEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.rpg.idle"));
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
        return ModSounds.ROCKET_FLY.get();
    }

    @Override
    public float getVolume() {
        return 0.7f;
    }

    @Override
    public boolean forceLoadChunk() {
        return true;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setSpreadAmount(int spreadAmount) {
        this.spreadAmount = spreadAmount;
    }

    public void setSpreadAngle(int spreadAngle) {
        this.spreadAngle = spreadAngle;
    }
}
