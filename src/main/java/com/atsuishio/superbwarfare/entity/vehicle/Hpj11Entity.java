package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.*;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.SmallCannonShellWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.ContainerBlockItem;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.math.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static com.atsuishio.superbwarfare.tools.SeekTool.smokeFilter;

public class Hpj11Entity extends ContainerMobileVehicleEntity implements GeoEntity, CannonEntity, OwnableEntity, AutoAimable {

    public static Consumer<MobileVehicleEntity> fireSound = vehicle -> {
    };

    public static final EntityDataAccessor<Integer> ANIM_TIME = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> GUN_ROTATE = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<Integer> FIRE_TIME = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean wasFiring = false;

    public int changeTargetTimer = 60;

    public float gunRot;
    public float gunRotO;

    public Hpj11Entity(EntityType<Hpj11Entity> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIM_TIME, 0)
                .define(GUN_ROTATE, 0f)
                .define(TARGET_UUID, "none")
                .define(OWNER_UUID, Optional.empty())
                .define(ACTIVE, false)
                .define(FIRE_TIME, 0);
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new SmallCannonShellWeapon()
                                .damage(VehicleConfig.HPJ11_DAMAGE.get().floatValue())
                                .explosionDamage(VehicleConfig.HPJ11_EXPLOSION_DAMAGE.get().floatValue())
                                .explosionRadius(VehicleConfig.HPJ11_EXPLOSION_RADIUS.get().floatValue())
                                .aa(true)
                                .icon(Mod.loc("textures/screens/vehicle_weapon/cannon_30mm.png"))
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(2 + 0.75 * ClientMouseHandler.custom3pDistanceLerp, 0.75, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("AnimTime", this.entityData.get(ANIM_TIME));
        compound.putBoolean("Active", this.entityData.get(ACTIVE));
        if (this.getOwnerUUID() != null) {
            compound.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(ANIM_TIME, compound.getInt("AnimTime"));
        this.entityData.set(ACTIVE, compound.getBoolean("Active"));

        UUID uuid;
        if (compound.hasUUID("Owner")) {
            uuid = compound.getUUID("Owner");
        } else {
            String s = compound.getString("Owner");

            assert this.getServer() != null;
            uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), s);
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
            if (stack.is(ModItems.CROWBAR.get()) && (getOwner() == null || player == getOwner())) {
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
                .custom((source, damage) -> getSourceAngle(source, 1f) * damage);
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return new Vec3(0, Math.min(super.getDeltaMovement().y, 0), 0);
    }

    @Override
    public void baseTick() {
        if (!this.wasFiring && this.isFiring() && this.level().isClientSide()) {
            fireSound.accept(this);
        }
        this.wasFiring = this.isFiring();

        this.gunRotO = this.getGunRot();
        super.baseTick();

        if (this.entityData.get(ANIM_TIME) > 0) {
            this.entityData.set(ANIM_TIME, this.entityData.get(ANIM_TIME) - 1);
        }

        if (this.level() instanceof ServerLevel) {
            this.handleAmmo();
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }

        if (this.getFirstPassenger() instanceof Player player && fireInputDown) {
            if ((this.entityData.get(AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(player)) && !cannotFire) {
                vehicleShoot(player, 0);
            }
        }

        this.entityData.set(GUN_ROTATE, this.entityData.get(GUN_ROTATE) * 0.8f);
        setGunRot(getGunRot() + entityData.get(GUN_ROTATE));

        autoAim();

        if (entityData.get(FIRE_TIME) > 0) {
            entityData.set(FIRE_TIME, entityData.get(FIRE_TIME) - 1);
        }

        lowHealthWarning();
    }

    private void handleAmmo() {
        if (hasItem(ModItems.CREATIVE_AMMO_BOX.get())) {
            entityData.set(AMMO, 9999);
        } else {
            entityData.set(AMMO, countItem(ModItems.SMALL_SHELL.get()));
        }
    }

    public boolean isFiring() {
        return this.entityData.get(FIRE_TIME) > 0;
    }

    public void autoAim() {
        if (this.getFirstPassenger() != null || !entityData.get(ACTIVE)) {
            return;
        }

        if (this.getEnergy() <= VehicleConfig.HPJ11_SEEK_COST.get()) return;

        Matrix4f transform = getBarrelTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0f, 0.4f, 0);
        Vec3 barrelRootPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);

        if (entityData.get(TARGET_UUID).equals("none") && tickCount % 2 == 0) {
            Entity naerestEntity = seekNearLivingEntity(this, barrelRootPos, -32.5, 90, 3, 160, 0.3);
            if (naerestEntity != null) {
                entityData.set(TARGET_UUID, naerestEntity.getStringUUID());
                this.consumeEnergy(VehicleConfig.HPJ11_SEEK_COST.get());
            }
        }

        Entity target = EntityFindUtil.findEntity(level(), entityData.get(TARGET_UUID));

        if (target != null && this.getOwner() instanceof Player player && smokeFilter(target)) {
            if (target instanceof Player player1 && (player1.isSpectator() || player1.isCreative())) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }
            if (target.distanceTo(this) > 160) {
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
            if (target instanceof Projectile && (VectorTool.calculateAngle(target.getDeltaMovement().normalize(), target.position().vectorTo(this.position()).normalize()) > 60 || target.onGround())) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }

            if (target.getVehicle() != null) {
                this.entityData.set(TARGET_UUID, target.getVehicle().getStringUUID());
            }

            Vec3 targetPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() / 4, target.getZ()).add(target.getDeltaMovement().scale(1.0 + 0.04 * target.distanceTo(this)));
            Vec3 targetVec = barrelRootPos.vectorTo(targetPos).normalize();

            double d0 = targetVec.x;
            double d1 = targetVec.y;
            double d2 = targetVec.z;
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            this.setXRot(Mth.clamp(Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * 57.2957763671875))), -90, 40));
            float targetY = Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F);

            float diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(targetY - this.getYRot()));

            turretTurnSound(0, diffY, 1.1f);
            this.setYRot(this.getYRot() + Mth.clamp(0.9f * diffY, -20f, 20f));

            if (target.distanceTo(this) <= 144 && VectorTool.calculateAngle(getViewVector(1), targetVec) < 10) {
                if (checkNoClip(this, target, barrelRootPos) && entityData.get(AMMO) > 0) {
                    vehicleShoot(player, 0);
                    findEntityOnPath(barrelRootPos, targetVec, 0.3);
                } else {
                    changeTargetTimer++;
                }

                if (!target.isAlive()) {
                    entityData.set(TARGET_UUID, "none");
                }
            }

        } else {
            entityData.set(TARGET_UUID, "none");
        }

        if (changeTargetTimer > 60) {
            entityData.set(TARGET_UUID, "none");
            changeTargetTimer = 0;
        }
    }

    @Override
    public boolean basicEnemyFilter(Entity pEntity) {
        if (pEntity instanceof Projectile) return false;
        if (this.getOwner() == null) return false;
        if (pEntity.getTeam() == null) return false;

        return !pEntity.isAlliedTo(this.getOwner()) || (pEntity.getTeam() != null && pEntity.getTeam().getName().equals("TDM"));
    }

    @Override
    public boolean basicEnemyProjectileFilter(Projectile projectile) {
        if (this.getOwner() == null) return false;
        if (projectile.getOwner() == null) return false;
        if (projectile.getOwner() == this.getOwner()) return false;
        return !projectile.getOwner().isAlliedTo(this.getOwner()) || (projectile.getOwner().getTeam() != null && projectile.getOwner().getTeam().getName().equals("TDM"));
    }

    public void findEntityOnPath(Vec3 pos, Vec3 toVec, double size) {
        for (Entity target : level().getEntitiesOfClass(Entity.class, new AABB(pos, pos).inflate(0.125).expandTowards(toVec.scale(30)), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(pos))).toList()) {
            var condition = target instanceof Projectile && isThreateningEntity(this, target, size, pos) && smokeFilter(target);
            if (condition) {
                causeAirExplode(target.position());
                target.discard();
            }
        }
    }

    private void causeAirExplode(Vec3 vec3) {
        CustomExplosion explosion = new CustomExplosion(this.level(), this,
                ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(),
                        this,
                        this.getOwner()),
                VehicleConfig.HPJ11_EXPLOSION_DAMAGE.get().floatValue(),
                vec3.x,
                vec3.y,
                vec3.z,
                VehicleConfig.HPJ11_EXPLOSION_RADIUS.get().floatValue(),
                ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP).
                setDamageMultiplier(1.25f);
        explosion.explode();
        EventHooks.onExplosionStart(this.level(), explosion);
        explosion.finalizeExplosion(false);
        ParticleTool.spawnMediumExplosionParticles(this.level(), vec3);
    }

    public float getGunRot() {
        return this.gunRot;
    }

    public void setGunRot(float pGunRot) {
        this.gunRot = pGunRot;
    }

    @Override
    public void handleClientSync() {
        if (isControlledByLocalInstance()) {
            interpolationSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (interpolationSteps <= 0) {
            return;
        }

        double interpolatedYaw = Mth.wrapDegrees(serverYRot - (double) getYRot());
        setYRot(getYRot() + (float) interpolatedYaw / (float) interpolationSteps);
        setXRot(getXRot() + (float) (serverXRot - (double) getXRot()) / (float) interpolationSteps);
        setRot(getYRot(), getXRot());

    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        serverYRot = yRot;
        serverXRot = xRot;
        this.interpolationSteps = 10;
    }

    @Override
    public void destroy() {
        if (level() instanceof ServerLevel) {
            CustomExplosion explosion = new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), getAttacker(), getAttacker()), 100f,
                    this.getX(), this.getY(), this.getZ(), 7f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP).setDamageMultiplier(1);
            explosion.explode();
            EventHooks.onExplosionStart(this.level(), explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnMediumExplosionParticles(this.level(), this.position());
        }

        explodePassengers();
        super.destroy();
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        passenger.setPos(getX(), getY(), getZ());
        callback.accept(passenger, getX(), getY(), getZ());
        copyEntityData(passenger);
    }

    public void copyEntityData(Entity entity) {
        float f = Mth.wrapDegrees(entity.getYRot() - getYRot());
        float g = Mth.clamp(f, -90.0f, 90.0f);
        entity.yRotO += g - f;
        entity.setYRot(entity.getYRot() + g - f);
        entity.setYHeadRot(entity.getYRot());
        entity.setYBodyRot(getYRot());
    }

    public Vec3 driverPos(float ticks) {
        Matrix4f transform = getVehicleFlatTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, -1.0625f, 3.25f, -1.0625f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getBarrelTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0f, 1f, 0);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public void vehicleShoot(Player player, int type) {
        if (cannotFire) return;
        if (this.getEnergy() < VehicleConfig.HPJ11_SHOOT_COST.get()) return;

        boolean hasCreativeAmmo = (getFirstPassenger() instanceof Player pPlayer && InventoryTool.hasCreativeAmmoBox(pPlayer)) || hasItem(ModItems.CREATIVE_AMMO_BOX.get());

        entityData.set(FIRE_TIME, Math.min(entityData.get(FIRE_TIME) + 3, 5));

        var entityToSpawn = ((SmallCannonShellWeapon) getWeapon(0)).create(player);

        Matrix4f transform = getBarrelTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0f, 0.4f, 2.6875f);

        entityToSpawn.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        entityToSpawn.shoot(getLookAngle().x, getLookAngle().y + 0.001, getLookAngle().z, 30, 0.75f);
        level().addFreshEntity(entityToSpawn);

        this.entityData.set(GUN_ROTATE, entityData.get(GUN_ROTATE) + 0.5f);
        this.entityData.set(HEAT, this.entityData.get(HEAT) + 2);
        this.entityData.set(ANIM_TIME, 1);

        this.consumeEnergy(VehicleConfig.HPJ11_SHOOT_COST.get());

        if (hasCreativeAmmo) return;

        this.getItemStacks().stream().filter(stack -> stack.is(ModItems.SMALL_SHELL.get())).findFirst().ifPresent(stack -> stack.shrink(1));
    }

    public float shootingVolume() {
        return entityData.get(FIRE_TIME) * 0.4f;
    }

    public float shootingPitch() {
        return 0.8f + entityData.get(FIRE_TIME) * 0.1f;
    }

    public Matrix4f getBarrelTransform(float ticks) {
        Matrix4f transformV = getVehicleFlatTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0, 1.375f, 0.25f);

        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        transformV.rotate(Axis.XP.rotationDegrees(Mth.lerp(ticks, xRotO, getXRot())));
        return transformV;
    }

    @Override
    public void travel() {
        if (this.getEnergy() <= 0) return;

        Entity passenger = this.getFirstPassenger();
        if (passenger != null) {
            float diffY = Mth.wrapDegrees(passenger.getYHeadRot() - this.getYRot());
            float diffX = Mth.wrapDegrees(passenger.getXRot() - this.getXRot());

            turretTurnSound(diffX, diffY, 0.95f);

            this.setYRot(this.getYRot() + Mth.clamp(0.9f * diffY, -20f, 20f));
            this.setXRot(Mth.clamp(this.getXRot() + Mth.clamp(0.9f * diffX, -15f, 15f), -90, 32.5f));
        }
    }

    protected void clampRotation(Entity entity) {
        float f = Mth.wrapDegrees(entity.getXRot());
        float f1 = Mth.clamp(f, -90.0F, 32.5F);
        entity.xRotO += f1 - f;
        entity.setXRot(entity.getXRot() + f1 - f);
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public int mainGunRpm(Player player) {
        return 0;
    }

    @Override
    public boolean canShoot(Player player) {
        return false;
    }

    @Override
    public int getAmmoCount(Player player) {
        return this.entityData.get(AMMO);
    }

    @Override
    public boolean hidePassenger(Entity entity) {
        return true;
    }

    @Override
    public int zoomFov() {
        return 2;
    }

    @Override
    public int getWeaponHeat(Player player) {
        return entityData.get(HEAT);
    }

    @Override
    public Vec3 getBarrelVector(float pPartialTicks) {
        if (getFirstPassenger() != null) {
            return getFirstPassenger().getViewVector(pPartialTicks);
        }
        return super.getBarrelVector(pPartialTicks);
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/hpj_11.png");
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return zoom ? 0.25 : 0.3;
    }

    @Override
    public boolean isEnclosed(int index) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            return new Vec2(Mth.lerp(partialTicks, player.yRotO, player.getYRot()), Mth.lerp(partialTicks, player.xRotO, player.getXRot()));
        }
        return super.getCameraRotation(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            if (zoom) {
                return new Vec3(this.driverZoomPos(partialTicks).x, this.driverZoomPos(partialTicks).y, this.driverZoomPos(partialTicks).z);
            } else {
                return new Vec3(this.driverPos(partialTicks).x, this.driverPos(partialTicks).y, this.driverPos(partialTicks).z);
            }
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }

    @Override
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/defense.png");
    }
}
