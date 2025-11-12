package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.RangeTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.atsuishio.superbwarfare.world.TDMSavedData;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public class Hpj11Entity extends VehicleEntity implements GeoEntity, OwnableEntity, WeaponVehicleEntity {
    public static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public int changeTargetTimer;

    public float gunRot;
    public float gunRotO;

    public Hpj11Entity(EntityType<Hpj11Entity> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_UUID, "none")
                .define(OWNER_UUID, Optional.empty())
                .define(ACTIVE, false);
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(2 + 0.75 * ClientMouseHandler.custom3pDistanceLerp, 0.75, 0);
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

    public void setOwnerUUID(@Nullable UUID pUuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(pUuid));
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
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
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.5f) * damage);
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
        var data = getGunData(0);
        if (data == null) return;
        if (data().compute().seekInfo == null) return;

        double maxSeekRange = data().compute().seekInfo.maxSeekRange;
        double minSeekRange = data().compute().seekInfo.minSeekRange;
        int changeTargetTime = data().compute().seekInfo.changeTargetTime;
        int seekIterative = data().compute().seekInfo.seekIterative;
        double minTargetSize = data().compute().seekInfo.minTargetSize;
        if (this.getEnergy() < data().compute().seekInfo.seekEnergyCost) return;

        Vec3 barrelRootPos = getShootPos(0, 1);

        if (entityData.get(TARGET_UUID).equals("none") && tickCount % seekIterative == 0) {
            Entity naerestEntity = seekNearLivingEntity(barrelRootPos, getTurretMinPitch(), getTurretMaxPitch(), minSeekRange, maxSeekRange, minTargetSize);
            if (naerestEntity != null) {
                entityData.set(TARGET_UUID, naerestEntity.getStringUUID());
                this.consumeEnergy(data().compute().seekInfo.seekEnergyCost);
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

            Vec3 targetVec = RangeTool.calculateFiringSolution(barrelRootPos, targetPos, targetVel.scale(1.1 + random.nextFloat() * 0.2f), projectileVelocity(0), projectileGravity(0));
            turretAutoAimFromVector(targetVec);

            if (VectorTool.calculateAngle(getShootVec(0, 1), targetVec) < 1) {
                int rpm = Mth.clamp(20 / Mth.clamp((vehicleWeaponRpm(0) / 60), 1, 2147483647), 1, 2147483647);
                if (checkNoClip(target, barrelRootPos) && getAmmoCount(0) > 0 && !data.overHeat.get() && tickCount % rpm == 0) {
                    if (player.level() instanceof ServerLevel) {
                        vehicleShoot(player, 0);
                    }
                } else {
                    changeTargetTimer++;
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
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
}
