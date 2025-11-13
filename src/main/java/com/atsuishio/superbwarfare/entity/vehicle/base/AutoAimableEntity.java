package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.projectile.DestroyableProjectile;
import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.atsuishio.superbwarfare.tools.*;
import com.atsuishio.superbwarfare.world.TDMSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class AutoAimableEntity extends VehicleEntity implements WeaponVehicleEntity, OwnableEntity {
    public static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(AutoAimableEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(AutoAimableEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(AutoAimableEntity.class, EntityDataSerializers.STRING);
    public int changeTargetTimer;

    public AutoAimableEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        if (player.isCrouching()) {
            if (stack.is(ModTags.Items.TOOLS_CROWBAR) && (getOwner() == null || player == getOwner())) {
                ItemStack container = ContainerBlockItem.createInstance(this);
                if (!player.addItem(container)) {
                    player.drop(container, false);
                }
                this.remove(RemovalReason.DISCARDED);
                this.discard();
                return InteractionResult.SUCCESS;
            } else {
                if (this.getOwnerUUID() == null) {
                    this.setOwnerUUID(player.getUUID());
                }
                if (this.getOwner() == player) {
                    entityData.set(ACTIVE, !entityData.get(ACTIVE));

                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5F, 1);
                    }
                    return InteractionResult.sidedSuccess(this.level().isClientSide());
                } else {
                    return InteractionResult.PASS;
                }
            }
        }
        entityData.set(TARGET_UUID, "none");
        return super.interact(player, hand);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TARGET_UUID, "none");
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(ACTIVE, false);
    }

    public void setOwnerUUID(@Nullable UUID pUuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(pUuid));
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Active", this.entityData.get(ACTIVE));
        if (this.getOwnerUUID() != null) {
            compound.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(ACTIVE, compound.getBoolean("Active"));

        UUID uuid;
        if (compound.hasUUID("Owner")) {
            uuid = compound.getUUID("Owner");
        } else {
            String s = compound.getString("Owner");

            try {
                if (this.getServer() == null) {
                    uuid = UUID.fromString(s);
                } else {
                    uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), s);
                }
            } catch (Exception exception) {
                Mod.LOGGER.error("Couldn't load owner UUID of {}: {}", this, exception);
                uuid = null;
            }
        }

        if (uuid != null) {
            try {
                this.setOwnerUUID(uuid);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void baseTick() {
        super.baseTick();
        autoAim();
        lowHealthWarning();
    }

    public void autoAim() {
        if (this.getFirstPassenger() != null || !entityData.get(ACTIVE)) {
            return;
        }
        String weaponName = "Main";
        var data = getGunData(weaponName);
        if (data == null) return;

        var seekInfo = data().compute().seekInfo;
        if (seekInfo == null) return;

        double maxSeekRange = seekInfo.maxSeekRange;
        double minSeekRange = seekInfo.minSeekRange;
        int changeTargetTime = seekInfo.changeTargetTime;
        int seekIterative = seekInfo.seekIterative;
        double minTargetSize = seekInfo.minTargetSize;

        if (this.getEnergy() < seekInfo.seekEnergyCost) return;

        var projectileInfo = data.compute().projectile();
        var projectileType = projectileInfo.type;
        var projectileTypeStr = projectileType.trim().toLowerCase(Locale.ROOT);
        int rpm = (int) Mth.clamp(20 / ((float) Math.max(vehicleWeaponRpm(weaponName),1) / 60), 1, 2147483647);

        if (projectileTypeStr.equals("ray") && this.entityData.get(CHARGE_PROGRESS) < 1 && getEnergy() > data.compute().ammoCostPerShoot) {
            float chargeSpeed = 1f / rpm;
            this.entityData.set(CHARGE_PROGRESS, Mth.clamp(this.entityData.get(CHARGE_PROGRESS) + chargeSpeed, 0, 1));
        }

        Vec3 barrelRootPos = getShootPos(weaponName, 1);

        if (entityData.get(TARGET_UUID).equals("none") && tickCount % seekIterative == 0) {
            Entity naerestEntity = seekNearLivingEntity(barrelRootPos, getTurretMinPitch(), getTurretMaxPitch(), minSeekRange, maxSeekRange, minTargetSize);
            if (naerestEntity != null) {
                entityData.set(TARGET_UUID, naerestEntity.getStringUUID());
                this.consumeEnergy(seekInfo.seekEnergyCost);
            }
        }

        Entity target = EntityFindUtil.findEntity(level(), entityData.get(TARGET_UUID));

        if (target != null && this.getOwner() instanceof Player player && SeekTool.NOT_IN_SMOKE.test(target)) {
            if (target instanceof Player player1 && (player1.isSpectator() || player1.isCreative())) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }
            if (VehicleVecUtils.getSubmergedHeight(target) >= target.getBbHeight()) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }

            if (target.distanceTo(this) > maxSeekRange) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }

            if (target.distanceTo(this) < minSeekRange) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }

            if (target instanceof LivingEntity living && living.getHealth() <= 0) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }
            if (target == this || target instanceof TargetEntity) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }
            if (target instanceof Projectile && (target.onGround() || target.getDeltaMovement().lengthSqr() < 0.001) || target.isInWater()) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }

            if (target.getVehicle() != null) {
                this.entityData.set(TARGET_UUID, target.getVehicle().getStringUUID());
            }

            if (!target.isAlive()) {
                entityData.set(TARGET_UUID, "none");
            }

            Vec3 targetPos = target.getBoundingBox().getCenter();
            Vec3 targetVel = target.getDeltaMovement();

            Vec3 targetVec;

            if (projectileTypeStr.equals("ray")) {
                targetVec = barrelRootPos.vectorTo(targetPos).normalize();
            } else {
                targetVec = RangeTool.calculateFiringSolution(barrelRootPos, targetPos, targetVel.scale(1.1 + random.nextFloat() * 0.2f), projectileVelocity(weaponName), projectileGravity(weaponName));
            }

            if (entityData.get(LASER_SCALE) == 0) {
                turretAutoAimFromVector(targetVec);
                if (VectorTool.calculateAngle(getShootVec(weaponName, 1), targetVec) < 1) {
                    if (checkNoClip(target, barrelRootPos)&& !data.overHeat.get()) {
                        if (projectileTypeStr.equals("ray") && getEntityData().get(CHARGE_PROGRESS) == 1) {
                            if (player.level() instanceof ServerLevel) {
                                rayShoot(player, target, data);
                            }
                            changeTargetTimer = 0;
                        } else if (getAmmoCount(weaponName) > 0 && tickCount % rpm == 0) {
                            if (player.level() instanceof ServerLevel) {
                                vehicleShoot(player, "Main");
                            }
                            changeTargetTimer = 0;
                        }
                    } else {
                        changeTargetTimer++;
                    }
                }
            }

        } else {
            entityData.set(TARGET_UUID, "none");
        }

        if (changeTargetTimer > changeTargetTime) {
            entityData.set(TARGET_UUID, "none");
            changeTargetTimer = 0;
        }
    }

    public boolean basicEnemyFilter(Entity pEntity) {
        if (pEntity instanceof Projectile) return false;
        if (this.getOwner() == null) return false;
        if (pEntity.getTeam() == null) return false;

        return !pEntity.isAlliedTo(this.getOwner()) || (pEntity.getTeam() != null && TDMSavedData.enabledTDM(pEntity));
    }

    public boolean basicEnemyProjectileFilter(Projectile projectile) {
        if (this.getOwner() == null) return false;
        if (projectile.getOwner() != null && projectile.getOwner() == this.getOwner()) return false;
        return (projectile.getOwner() != null && !projectile.getOwner().isAlliedTo(this.getOwner()))
                || (projectile.getOwner() != null && projectile.getOwner().getTeam() != null && TDMSavedData.enabledTDM(projectile.getOwner()))
                || projectile.getOwner() == null;
    }

    // 防御类载具实体搜寻周围实体
    public Entity seekNearLivingEntity(Vec3 pos, double minAngle, double maxAngle, double minRange, double seekRange, double size) {
        for (Entity target : this.level().getEntitiesOfClass(Entity.class, new AABB(pos, pos).inflate(seekRange), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(pos))).toList()) {
            var condition = target.distanceToSqr(this) > minRange * minRange
                    && target.distanceToSqr(this) <= seekRange * seekRange
                    && canAim(pos, target, minAngle, maxAngle)
                    && VehicleVecUtils.getSubmergedHeight(target) <= target.getBbHeight()
                    && checkNoClip(target, pos)
                    && !(target instanceof Player player && (player.isSpectator() || player.isCreative()))
                    && ((target instanceof LivingEntity living && living instanceof Enemy && living.getHealth() > 0) || isThreateningEntity(target, size, pos) || basicEnemyFilter(target))
                    && SeekTool.NOT_IN_SMOKE.test(target);
            if (condition) {
                return target;
            }
        }
        return null;
    }

    // 判断具有威胁的弹射物
    public boolean isThreateningEntity(Entity target, double size, Vec3 pos) {
        if (target instanceof SmallCannonShellEntity) return false;
        if (!target.onGround() && target instanceof Projectile projectile && (target.getBbWidth() >= size || target.getBbHeight() >= size)) {
            return checkNoClip(target, pos) && basicEnemyProjectileFilter(projectile);
        }
        return false;
    }

    // 判断载具和目标之间有无障碍物
    public boolean checkNoClip(Entity target, Vec3 pos) {
        return this.level().clip(new ClipContext(pos, target.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this)).getType() != HitResult.Type.BLOCK;
    }

    static boolean canAim(Vec3 pos, Entity target, double minAngle, double maxAngle) {
        Vec3 targetPos = target.getBoundingBox().getCenter();
        Vec3 toVec = pos.vectorTo(targetPos).normalize();
        double targetAngle = VehicleVecUtils.getXRotFromVector(toVec);
        return minAngle < targetAngle && targetAngle < maxAngle;
    }

    public void rayShoot(LivingEntity living, Entity target, GunData gunData) {
        if (level() instanceof ServerLevel serverLevel) {
            sendParticle(serverLevel, ParticleTypes.END_ROD, target.getX(), target.getEyeY(), target.getZ(), 12, 0, 0, 0, 0.05, true);
            sendParticle(serverLevel, ParticleTypes.LAVA, target.getX(), target.getEyeY(), target.getZ(), 4, 0, 0, 0, 0.15, true);
        }

        Vec3 pos = target.getBoundingBox().getCenter();

        entityData.set(LASER_LENGTH, (float) getShootPos("Main", 1).distanceTo(pos));

        DamageHandler.doDamage(target, ModDamageTypes.causeLaserStaticDamage(this.level().registryAccess(), this, living), (float) gunData.compute().damage);
        target.invulnerableTime = 0;

        if (gunData.compute().explosionRadius > 0) {
            causeLaserExplode(pos, gunData, living);
        }

        if (Math.random() < 0.25 && target instanceof LivingEntity pLiving) {
            pLiving.setSecondsOnFire(2);
        }

        if (target instanceof Projectile && !(target instanceof DestroyableProjectile)) {
            causeAirExplode(pos);
            target.discard();
        }

        if (!target.isAlive()) {
            entityData.set(TARGET_UUID, "none");
        }

        entityData.set(LASER_SCALE, (float) gunData.compute().shootAnimationTime);
        this.entityData.set(CHARGE_PROGRESS, 0f);
        entityData.set(FIRE_TIME, Math.min(entityData.get(FIRE_TIME) + 3, 5));
        playShootSound3p(living, "Main");

        this.consumeEnergy(gunData.compute().ammoCostPerShoot);
    }

    private void causeLaserExplode(Vec3 vec3, GunData gunData, Entity living) {
        float radius = (float) gunData.compute().explosionRadius;
        ParticleTool.ParticleType particleType;

        if (radius <= 4) {
            particleType = ParticleTool.ParticleType.SMALL;
        } else if (radius > 4 && radius < 10) {
            particleType = ParticleTool.ParticleType.MEDIUM;
        } else if (radius >= 10 && radius < 20) {
            particleType = ParticleTool.ParticleType.HUGE;
        } else {
            particleType = ParticleTool.ParticleType.GIANT;
        }

        createCustomExplosion()
                .damage((float) gunData.compute().explosionDamage)
                .radius(radius)
                .attacker(living)
                .position(vec3)
                .withParticleType(particleType)
                .explode();
    }

    private void causeAirExplode(Vec3 vec3) {
        createCustomExplosion()
                .damage(5)
                .radius(1)
                .keepBlock()
                .attacker(getOwner())
                .position(vec3)
                .withParticleType(ParticleTool.ParticleType.MEDIUM)
                .explode();
    }
}
