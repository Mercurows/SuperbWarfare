package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.projectile.GunGrenadeEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.SmallCannonShellWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.mojang.math.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Hpj11Entity extends VehicleEntity implements GeoEntity, CannonEntity {
    public static final EntityDataAccessor<Integer> ANIM_TIME = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> GUN_ROTATE = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Hpj11Entity(EntityType<Hpj11Entity> type, Level world) {
        super(type, world);
    }

    public float gunRot;
    public float gunRotO;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIM_TIME, 0)
                .define(GUN_ROTATE, 0f);
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new SmallCannonShellWeapon()
                                .damage(VehicleConfig.HPJ11_DAMAGE.get().floatValue())
                                .explosionDamage(VehicleConfig.HPJ11_EXPLOSION_DAMAGE.get().floatValue())
                                .explosionRadius(VehicleConfig.HPJ11_EXPLOSION_RADIUS.get().floatValue())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/cannon_30mm.png"))
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(2, 0.75, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("AnimTime", this.entityData.get(ANIM_TIME));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(ANIM_TIME, compound.getInt("AnimTime"));
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .multiply(0.2f)
                .multiply(1.5f, DamageTypes.ARROW)
                .multiply(1.5f, DamageTypes.TRIDENT)
                .multiply(2.5f, DamageTypes.MOB_ATTACK)
                .multiply(2f, DamageTypes.MOB_ATTACK_NO_AGGRO)
                .multiply(1.5f, DamageTypes.MOB_PROJECTILE)
                .multiply(12.5f, DamageTypes.LAVA)
                .multiply(6f, DamageTypes.EXPLOSION)
                .multiply(6f, DamageTypes.PLAYER_EXPLOSION)
                .multiply(2.4f, ModDamageTypes.CUSTOM_EXPLOSION)
                .multiply(2f, ModDamageTypes.PROJECTILE_BOOM)
                .multiply(0.75f, ModDamageTypes.MINE)
                .multiply(1.5f, ModDamageTypes.CANNON_FIRE)
                .multiply(0.25f, ModTags.DamageTypes.PROJECTILE)
                .multiply(0.85f, ModTags.DamageTypes.PROJECTILE_ABSOLUTE)
                .multiply(10f, ModDamageTypes.VEHICLE_STRIKE)
                .custom((source, damage) -> getSourceAngle(source, 1f) * damage)
                .custom((source, damage) -> {
                    if (source.getDirectEntity() instanceof GunGrenadeEntity) {
                        return 1.5f * damage;
                    }
                    return damage;
                })
                .reduce(8);
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return new Vec3(0, Math.min(super.getDeltaMovement().y, 0), 0);
    }

    @Override
    public void baseTick() {
        gunRotO = this.getGunRot();
        super.baseTick();

        if (this.entityData.get(ANIM_TIME) > 0) {
            this.entityData.set(ANIM_TIME, this.entityData.get(ANIM_TIME) - 1);
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }

        this.entityData.set(GUN_ROTATE, this.entityData.get(GUN_ROTATE) * 0.8f);
        setGunRot(getGunRot() + entityData.get(GUN_ROTATE));

        lowHealthWarning();
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
        this.discard();
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

        if (!InventoryTool.hasCreativeAmmoBox(player)) {
            var ammo = ModItems.SMALL_SHELL.get();
            var ammoCount = InventoryTool.countItem(player.getInventory().items, ammo);

            if (ammoCount <= 0) return;
            InventoryTool.consumeItem(player.getInventory().items, ammo, 1);
        }

        var entityToSpawn = ((SmallCannonShellWeapon) getWeapon(0)).create(player);

        Matrix4f transform = getBarrelTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0f, 0.4f, 2.6875f);

        entityToSpawn.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        entityToSpawn.shoot(getLookAngle().x, getLookAngle().y, getLookAngle().z, 40, 0.3f);
        level().addFreshEntity(entityToSpawn);

        if (!player.level().isClientSide) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.HPJ_11_FIRE_3P.get(), SoundSource.PLAYERS, 8, random.nextFloat() * 0.05f + 1);
            }
        }

        this.entityData.set(GUN_ROTATE, entityData.get(GUN_ROTATE) + 0.5f);
        this.entityData.set(HEAT, this.entityData.get(HEAT) + 1);
        this.entityData.set(ANIM_TIME, 1);
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
    public float getMaxHealth() {
        return VehicleConfig.HPJ11_HP.get();
    }

    @Override
    public int mainGunRpm(Player player) {
        return 2400;
    }

    @Override
    public boolean canShoot(Player player) {
        var ammo = ModItems.SMALL_SHELL.get();
        return (InventoryTool.countItem(player.getInventory().items, ammo) > 0 || InventoryTool.hasCreativeAmmoBox(player)) && !cannotFire;
    }

    @Override
    public int getAmmoCount(Player player) {
        var ammo = ModItems.SMALL_SHELL.get();
        return InventoryTool.countItem(player.getInventory().items, ammo);
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
}
