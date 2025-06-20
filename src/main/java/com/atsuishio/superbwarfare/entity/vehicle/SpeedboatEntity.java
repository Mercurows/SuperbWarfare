package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.*;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.ProjectileWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class SpeedboatEntity extends ContainerMobileVehicleEntity implements GeoEntity, ArmedVehicleEntity, WeaponVehicleEntity, LandArmorEntity, OBBEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public OBB obb;
    public OBB obb2;

    public SpeedboatEntity(EntityType<SpeedboatEntity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(1.5625f, 0.75f, 3.1875f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(1.0625f, 0.5f, 1.90625f), new Quaternionf(), OBB.Part.BODY);
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new ProjectileWeapon()
                                .damage(VehicleConfig.HEAVY_MACHINE_GUN_DAMAGE.get())
                                .headShot(2)
                                .zoom(false)
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_12_7mm.png"))
                                .sound1p(ModSounds.M_2_FIRE_1P.get())
                                .sound3p(ModSounds.M_2_FIRE_3P.get())
                                .sound3pFar(ModSounds.M_2_FAR.get())
                                .sound3pVeryFar(ModSounds.M_2_VERYFAR.get())
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(3 + ClientMouseHandler.custom3pDistanceLerp, 1, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
    }

    @Override
    public @NotNull Vec3 getPassengerRidingPosition(@NotNull Entity entity) {
        return super.getPassengerRidingPosition(entity).add(0, -0.8, 0);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.updateOBB();

        double fluidFloat = 0.12 * getSubmergedHeight(this);
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, fluidFloat, 0.0));

        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 0.99, 0.2));
        } else if (isInWater()) {
            float f = (float) (0.75f - (0.04f * java.lang.Math.min(getSubmergedHeight(this), this.getBbHeight())) + 0.09f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90);
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).normalize().scale(0.04 * getDeltaMovement().dot(getViewVector(1)))));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f, 0.85, f));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.99, 0.99));
        }

        if (this.level() instanceof ServerLevel serverLevel && this.isInWater() && this.getDeltaMovement().length() > 0.1) {
            sendParticle(serverLevel, ParticleTypes.CLOUD, this.getX() + 0.5 * this.getDeltaMovement().x, this.getY() + getSubmergedHeight(this) - 0.2, this.getZ() + 0.5 * this.getDeltaMovement().z, (int) (2 + 4 * this.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
            sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, this.getX() + 0.5 * this.getDeltaMovement().x, this.getY() + getSubmergedHeight(this) - 0.2, this.getZ() + 0.5 * this.getDeltaMovement().z, (int) (2 + 10 * this.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
            sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, this.getX() - 4.5 * this.getLookAngle().x, this.getY() - 0.25, this.getZ() - 4.5 * this.getLookAngle().z, (int) (40 * Mth.abs(this.entityData.get(POWER))), 0.15, 0.15, 0.15, 0.02, true);
        }

        if (this.level() instanceof ServerLevel) {
            this.handleAmmo();
        }

        this.turretAngle(40, 40);
        this.lowHealthWarning();
        this.inertiaRotate(2);
        this.terrainCompact(2f, 3f);

        this.refreshDimensions();
    }

    @Override
    public boolean canCollideHardBlock() {
        return getDeltaMovement().horizontalDistance() > 0.15;
    }

    private void handleAmmo() {
        if (!(this.getFirstPassenger() instanceof Player)) return;

        int ammoCount = this.getItemStacks().stream().filter(stack -> {
            if (stack.is(ModItems.AMMO_BOX.get())) {
                return Ammo.HEAVY.get(stack) > 0;
            }
            return false;
        }).mapToInt(Ammo.HEAVY::get).sum() + countItem(ModItems.HEAVY_AMMO.get());

        this.entityData.set(AMMO, ammoCount);
    }

    /**
     * 机枪塔开火
     */
    @Override
    public void vehicleShoot(Player player, int type) {
        if (this.cannotFire) return;

        Matrix4f transform = getBarrelTransform(1);

        float x = 0f;
        float y = 0.00106875f;
        float z = 1.9117f;

        Vector4f worldPosition = transformPosition(transform, x, y, z);

        var projectile = ((ProjectileWeapon) getWeapon(0)).create(player).setGunItemId(this.getType().getDescriptionId());

        projectile.bypassArmorRate(0.4f);
        projectile.setPos(worldPosition.x + 0.5 * this.getDeltaMovement().x, worldPosition.y, worldPosition.z + 0.5 * this.getDeltaMovement().z);
        projectile.shoot(player, getBarrelVector(1).x, getBarrelVector(1).y + 0.005f, getBarrelVector(1).z, 20,
                (float) 0.4);
        this.level().addFreshEntity(projectile);

        if (!player.level().isClientSide) {
            playShootSound3p(player, 0, 4, 12, 24);
        }

        ShakeClientMessage.sendToNearbyPlayers(this, 5, 6, 5, 5);

        this.entityData.set(CANNON_RECOIL_TIME, 30);
        this.entityData.set(YAW, getTurretYRot());

        this.entityData.set(HEAT, this.entityData.get(HEAT) + 4);
        this.entityData.set(FIRE_ANIM, 3);

        boolean hasCreativeAmmo = false;
        for (int i = 0; i < getMaxPassengers() - 1; i++) {
            if (getNthEntity(i) instanceof Player pPlayer && InventoryTool.hasCreativeAmmoBox(pPlayer)) {
                hasCreativeAmmo = true;
            }
        }

        if (!hasCreativeAmmo) {
            ItemStack ammoBox = this.getItemStacks().stream().filter(stack -> {
                if (stack.is(ModItems.AMMO_BOX.get())) {
                    return Ammo.HEAVY.get(stack) > 0;
                }
                return false;
            }).findFirst().orElse(ItemStack.EMPTY);

            if (!ammoBox.isEmpty()) {
                Ammo.HEAVY.add(ammoBox, -1);
            } else {
                this.getItemStacks().stream().filter(stack -> stack.is(ModItems.HEAVY_AMMO.get())).findFirst().ifPresent(stack -> stack.shrink(1));
            }
        }
    }

    @Override
    public void travel() {
        Entity passenger0 = this.getFirstPassenger();

        if (this.getEnergy() > 0) {
            if (passenger0 == null) {
                this.leftInputDown = false;
                this.rightInputDown = false;
                this.forwardInputDown = false;
                this.backInputDown = false;
            }

            if (forwardInputDown) {
                this.entityData.set(POWER, this.entityData.get(POWER) + 0.005f);
            }

            if (backInputDown) {
                this.entityData.set(POWER, this.entityData.get(POWER) - 0.005f);
                if (rightInputDown) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.1f);
                } else if (leftInputDown) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.1f);
                }
            } else {
                if (rightInputDown) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.1f);
                } else if (this.leftInputDown) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.1f);
                }
            }

            if (this.forwardInputDown || this.backInputDown) {
                this.consumeEnergy(VehicleConfig.SPEEDBOAT_ENERGY_COST.get());
            }

            this.entityData.set(POWER, this.entityData.get(POWER) * 0.96f);
            this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * 0.8f);

            this.setRotorRot(this.getRotorRot() + 30 * this.entityData.get(POWER));
            this.setRudderRot(Mth.clamp(this.getRudderRot() - this.entityData.get(DELTA_ROT), -1.25f, 1.25f) * 0.7f * (this.entityData.get(POWER) > 0 ? 1 : -1));

            if (this.isInWater() || this.isUnderWater()) {
                this.setXRot(this.getXRot() * 0.85f);
                float direct = (90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90;

                this.setXRot((float) (this.getXRot() - direct * (this.onGround() ? 0 : 1) * 1.1f * this.getDeltaMovement().horizontalDistance()));
                this.setYRot((float) (this.getYRot() - Math.max(12 * this.getDeltaMovement().length(), 0.8) * this.entityData.get(DELTA_ROT)));
                this.setZRot((float) (this.getRoll() - direct * this.entityData.get(DELTA_ROT) * (this.onGround() ? 0 : 1) * 10 * this.getDeltaMovement().horizontalDistance()));

                this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale(this.entityData.get(POWER) * 1.75f)));
            } else {
                this.setXRot(this.getXRot() * 0.99f);
            }
        }

        this.setZRot(this.roll * 0.85f);
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.BOAT_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return (Mth.abs(entityData.get(POWER)) - 0.01f) * 2f;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        Matrix4f transform = getVehicleTransform(1);
        int i = this.getOrderedPassengers().indexOf(passenger);

        float y = -0.65f;

        Vector4f worldPosition = switch (i) {
            case 0 -> transformPosition(transform, 0, y + 0.25f, -0.2f);
            case 1 -> transformPosition(transform, -0.8f, y, -1.2f);
            case 2 -> transformPosition(transform, 0.8f, y, -1.2f);
            case 3 -> transformPosition(transform, -0.8f, y, -2.2f);
            case 4 -> transformPosition(transform, 0.8f, y, -2.2f);
            default -> null;
        };

        if (worldPosition != null) {
            passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);
        }

        if (passenger != this.getFirstPassenger()) {
            passenger.setXRot(passenger.getXRot() + (getXRot() - xRotO));
        }

        copyEntityData(passenger);
    }

    public void copyEntityData(Entity entity) {
        float f = Mth.wrapDegrees(entity.getYRot() - getYRot());
        float g = Mth.clamp(f, -105.0f, 105.0f);
        entity.yRotO += g - f;
        entity.setYRot(entity.getYRot() + g - f);
        entity.setYHeadRot(entity.getYRot());
        entity.setYBodyRot(getYRot());
    }

    @Override
    public void destroy() {
        if (level() instanceof ServerLevel) {
            CustomExplosion explosion = new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), getAttacker(), getAttacker()), 80f,
                    this.getX(), this.getY(), this.getZ(), 5f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);
            explosion.explode();
            EventHooks.onExplosionStart(this.level(), explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnMediumExplosionParticles(this.level(), this.position());
        }

        explodePassengers();
        super.destroy();
    }

    protected void clampRotation(Entity entity) {
        float a = getTurretYaw(1);
        float r = (Mth.abs(a) - 90f) / 90f;

        float r2;

        if (Mth.abs(a) <= 90f) {
            r2 = a / 90f;
        } else {
            if (a < 0) {
                r2 = -(180f + a) / 90f;
            } else {
                r2 = (180f - a) / 90f;
            }
        }

        float min = -40f - r * getXRot() - r2 * getRoll();
        float max = 20f - r * getXRot() - r2 * getRoll();

        float f = Mth.wrapDegrees(entity.getXRot());
        float f1 = Mth.clamp(f, min, max);
        entity.xRotO += f1 - f;
        entity.setXRot(entity.getXRot() + f1 - f);

        float f2 = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
        float f3 = Mth.clamp(f2, -105.0F, 105.0F);
        entity.yRotO += f3 - f2;
        entity.setYRot(entity.getYRot() + f3 - f2);
        entity.setYBodyRot(this.getYRot());
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    @Override
    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getBarrelTransform(ticks);

        float x = 0f;
        float y = 0.5f;
        float z = -0.25f;

        Vector4f worldPosition = transformPosition(transform, x, y, z);

        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 getBarrelVector(float pPartialTicks) {
        Matrix4f transform = getBarrelTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    public Matrix4f getBarrelTransform(float ticks) {
        Matrix4f transformT = getTurretTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0f, 0.5088375f, 0.04173125f);

        transformT.translate(worldPosition.x, worldPosition.y, worldPosition.z);

        float a = getTurretYaw(ticks);

        float r = (Mth.abs(a) - 90f) / 90f;

        float r2;

        if (Mth.abs(a) <= 90f) {
            r2 = a / 90f;
        } else {
            if (a < 0) {
                r2 = -(180f + a) / 90f;
            } else {
                r2 = (180f - a) / 90f;
            }
        }

        float x = Mth.lerp(ticks, turretXRotO, getTurretXRot());
        float xV = Mth.lerp(ticks, xRotO, getXRot());
        float z = Mth.lerp(ticks, prevRoll, getRoll());

        transformT.rotate(Axis.XP.rotationDegrees(x + r * xV + r2 * z));
        return transformT;
    }

    public Matrix4f getTurretTransform(float ticks) {
        Matrix4f transformV = getVehicleTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0, 1.5616625f, -0.565625f);

        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        transformV.rotate(Axis.YP.rotationDegrees(Mth.lerp(ticks, turretYRotO, getTurretYRot())));
        return transformV;
    }

    @Override
    public Matrix4f getVehicleTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo + 0.9f, getY() + 0.9f), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        transform.rotate(Axis.XP.rotationDegrees(Mth.lerp(ticks, xRotO, getXRot())));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, prevRoll, getRoll())));
        return transform;
    }

    private PlayState firePredicate(AnimationState<SpeedboatEntity> event) {
        if (this.entityData.get(FIRE_ANIM) > 1) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.speedboat.fire"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.speedboat.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::firePredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public int getMaxPassengers() {
        return 5;
    }

    @Override
    public int mainGunRpm(Player player) {
        return 500;
    }

    @Override
    public boolean canShoot(Player player) {
        return (this.entityData.get(AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(player))
                && !cannotFire;
    }

    @Override
    public int getAmmoCount(Player player) {
        return this.entityData.get(AMMO);
    }

    @Override
    public boolean hidePassenger(Entity entity) {
        return false;
    }

    @Override
    public int zoomFov() {
        return 1;
    }

    @Override
    public int getWeaponHeat(Player player) {
        return entityData.get(HEAT);
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/speedboat_icon.png");
    }

    @Override
    public Vec3 getGunVec(float ticks) {
        return getBarrelVector(ticks);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (this.getSeatIndex(player) == 0 && zoom) {
            return new Vec2((float) -getYRotFromVector(this.getBarrelVec(partialTicks)), (float) -getXRotFromVector(this.getBarrelVec(partialTicks)));
        }
        return super.getCameraRotation(partialTicks, player, zoom, isFirstPerson);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (this.getSeatIndex(player) == 0 && zoom) {
            return new Vec3(this.driverZoomPos(partialTicks).x, this.driverZoomPos(partialTicks).y, this.driverZoomPos(partialTicks).z);
        }
        return super.getCameraPosition(partialTicks, player, zoom, isFirstPerson);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean useFixedCameraPos(Entity entity) {
        return this.getSeatIndex(entity) == 0;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public Pair<Quaternionf, Quaternionf> getPassengerRotation(Entity entity, float tickDelta) {
        return Pair.of(Axis.XP.rotationDegrees(-this.getViewXRot(tickDelta)), Axis.ZP.rotationDegrees(-this.getRoll(tickDelta)));
    }

    @Override
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/water.png");
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 0.875f - 0.9f, 0.375f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 0, 2.0625f - 0.9f, -0.71875f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));
    }
}
