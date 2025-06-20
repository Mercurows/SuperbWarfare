package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ContainerMobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.LandArmorEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.ProjectileWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.SmallCannonShellWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.BlockState;
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

public class Lav150Entity extends ContainerMobileVehicleEntity implements GeoEntity, LandArmorEntity, WeaponVehicleEntity, OBBEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obb6;
    public OBB obb7;
    public OBB obbTurret;

    public Lav150Entity(EntityType<Lav150Entity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(0.25f, 0.75f, 0.75f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(0.25f, 0.75f, 0.75f), new Quaternionf(), OBB.Part.BODY);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(0.25f, 0.75f, 0.75f), new Quaternionf(), OBB.Part.BODY);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.25f, 0.75f, 0.75f), new Quaternionf(), OBB.Part.BODY);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(1.3125f, 0.90625f, 2.4375f), new Quaternionf(), OBB.Part.BODY);
        this.obb6 = new OBB(this.position().toVector3f(), new Vector3f(1.3125f, 0.53125f, 0.34375f), new Quaternionf(), OBB.Part.BODY);
        this.obb7 = new OBB(this.position().toVector3f(), new Vector3f(1.3125f, 0.625f, 0.53125f), new Quaternionf(), OBB.Part.BODY);
        this.obbTurret = new OBB(this.position().toVector3f(), new Vector3f(0.875f, 0.3625f, 1.25f), new Quaternionf(), OBB.Part.BODY);
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new SmallCannonShellWeapon()
                                .damage(VehicleConfig.LAV_150_CANNON_DAMAGE.get())
                                .explosionDamage(VehicleConfig.LAV_150_CANNON_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.LAV_150_CANNON_EXPLOSION_RADIUS.get().floatValue())
                                .sound(ModSounds.INTO_MISSILE.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/cannon_20mm.png"))
                                .sound1p(ModSounds.LAV_CANNON_FIRE_1P.get())
                                .sound3p(ModSounds.LAV_CANNON_FIRE_3P.get())
                                .sound3pFar(ModSounds.LAV_CANNON_FAR.get())
                                .sound3pVeryFar(ModSounds.LAV_CANNON_VERYFAR.get()),
                        new ProjectileWeapon()
                                .damage(VehicleConfig.LAV_150_MACHINE_GUN_DAMAGE.get())
                                .headShot(2)
                                .zoom(false)
                                .sound(ModSounds.INTO_CANNON.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_7_62mm.png"))
                                .sound1p(ModSounds.COAX_FIRE_1P.get())
                                .sound3p(ModSounds.RPK_FIRE_3P.get())
                                .sound3pFar(ModSounds.RPK_FAR.get())
                                .sound3pVeryFar(ModSounds.RPK_VERYFAR.get()),
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(2.75 + ClientMouseHandler.custom3pDistanceLerp, 1, 0);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 0.3), random.nextFloat() * 0.15f + 1.05f);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage);
    }

    @Override
    public void baseTick() {
        turretYRotO = this.getTurretYRot();
        turretXRotO = this.getTurretXRot();
        rudderRotO = this.getRudderRot();
        leftWheelRotO = this.getLeftWheelRot();
        rightWheelRotO = this.getRightWheelRot();

        super.baseTick();
        updateOBB();

        if (this.level() instanceof ServerLevel) {
            this.handleAmmo();
        }

        double fluidFloat = 0.052 * getSubmergedHeight(this);
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, fluidFloat, 0.0));

        if (this.onGround()) {
            float f0 = 0.54f + 0.25f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90;
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).normalize().scale(0.05 * getDeltaMovement().dot(getViewVector(1)))));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f0, 0.99, f0));

        } else if (this.isInWater()) {
            float f1 = 0.74f + 0.09f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90;
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).normalize().scale(0.04 * getDeltaMovement().dot(getViewVector(1)))));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f1, 0.85, f1));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.99, 0.99));
        }

        if (this.level() instanceof ServerLevel serverLevel && this.isInWater() && this.getDeltaMovement().length() > 0.1) {
            sendParticle(serverLevel, ParticleTypes.CLOUD, this.getX() + 0.5 * this.getDeltaMovement().x, this.getY() + getSubmergedHeight(this) - 0.2, this.getZ() + 0.5 * this.getDeltaMovement().z, (int) (2 + 4 * this.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
            sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, this.getX() + 0.5 * this.getDeltaMovement().x, this.getY() + getSubmergedHeight(this) - 0.2, this.getZ() + 0.5 * this.getDeltaMovement().z, (int) (2 + 10 * this.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
        }

        turretAngle(10, 12.5f);
        lowHealthWarning();
        this.terrainCompact(2.7f, 3.61f);
        inertiaRotate(1.25f);

        releaseSmokeDecoy(getTurretVector(1));

        this.refreshDimensions();
    }

    @Override
    public boolean canCollideHardBlock() {
        return getDeltaMovement().horizontalDistance() > 0.09 || Mth.abs(this.entityData.get(POWER)) > 0.15;
    }

    private void handleAmmo() {
        if (!(this.getFirstPassenger() instanceof Player)) return;

        int ammoCount = this.getItemStacks().stream().filter(stack -> {
            if (stack.is(ModItems.AMMO_BOX.get())) {
                return Ammo.RIFLE.get(stack) > 0;
            }
            return false;
        }).mapToInt(Ammo.RIFLE::get).sum() + countItem(ModItems.RIFLE_AMMO.get());

        if (getWeaponIndex(0) == 0) {
            this.entityData.set(AMMO, countItem(ModItems.SMALL_SHELL.get()));
        } else if (getWeaponIndex(0) == 1) {
            this.entityData.set(AMMO, ammoCount);
        }
    }

    @Override
    public Vec3 getBarrelVector(float pPartialTicks) {
        Matrix4f transform = getBarrelTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    @Override
    public void vehicleShoot(Player player, int type) {
        boolean hasCreativeAmmo = false;
        for (int i = 0; i < getMaxPassengers() - 1; i++) {
            if (getNthEntity(i) instanceof Player pPlayer && InventoryTool.hasCreativeAmmoBox(pPlayer)) {
                hasCreativeAmmo = true;
            }
        }

        Matrix4f transform = getBarrelTransform(1);
        if (getWeaponIndex(0) == 0) {
            if (this.cannotFire) return;
            float x = 0.0609375f;
            float y = 0.0517f;
            float z = 3.0927625f;

            Vector4f worldPosition = transformPosition(transform, x, y, z);
            var smallCannonShell = ((SmallCannonShellWeapon) getWeapon(0)).create(player);

            smallCannonShell.setPos(worldPosition.x - 1.1 * this.getDeltaMovement().x, worldPosition.y, worldPosition.z - 1.1 * this.getDeltaMovement().z);
            smallCannonShell.shoot(getBarrelVector(1).x, getBarrelVector(1).y + 0.005f, getBarrelVector(1).z, 35,
                    0.25f);
            this.level().addFreshEntity(smallCannonShell);

            sendParticle((ServerLevel) this.level(), ParticleTypes.LARGE_SMOKE, worldPosition.x - 1.1 * this.getDeltaMovement().x, worldPosition.y, worldPosition.z - 1.1 * this.getDeltaMovement().z, 1, 0.02, 0.02, 0.02, 0, false);

            if (!player.level().isClientSide) {
                playShootSound3p(player, 0, 4, 12, 24);
            }

            ShakeClientMessage.sendToNearbyPlayers(this, 5, 6, 5, 9);

            this.entityData.set(CANNON_RECOIL_TIME, 40);
            this.entityData.set(YAW, getTurretYRot());

            this.entityData.set(HEAT, this.entityData.get(HEAT) + 7);
            this.entityData.set(FIRE_ANIM, 3);

            if (hasCreativeAmmo) return;

            this.getItemStacks().stream().filter(stack -> stack.is(ModItems.SMALL_SHELL.get())).findFirst().ifPresent(stack -> stack.shrink(1));

        } else if (getWeaponIndex(0) == 1) {
            if (this.cannotFireCoax) return;
            float x = 0.3f;
            float y = 0.08f;
            float z = 0.7f;

            Vector4f worldPosition = transformPosition(transform, x, y, z);

            if (this.entityData.get(AMMO) > 0 || hasCreativeAmmo) {
                var projectile = ((ProjectileWeapon) getWeapon(0)).create(player).setGunItemId(this.getType().getDescriptionId());

                projectile.bypassArmorRate(0.2f);
                projectile.setPos(worldPosition.x - 1.1 * this.getDeltaMovement().x, worldPosition.y, worldPosition.z - 1.1 * this.getDeltaMovement().z);
                projectile.shoot(player, getBarrelVector(1).x, getBarrelVector(1).y + 0.002f, getBarrelVector(1).z, 36,
                        0.25f);
                this.level().addFreshEntity(projectile);

                if (!hasCreativeAmmo) {
                    ItemStack ammoBox = this.getItemStacks().stream().filter(stack -> {
                        if (stack.is(ModItems.AMMO_BOX.get())) {
                            return Ammo.RIFLE.get(stack) > 0;
                        }
                        return false;
                    }).findFirst().orElse(ItemStack.EMPTY);

                    if (!ammoBox.isEmpty()) {
                        Ammo.RIFLE.add(ammoBox, -1);
                    } else {
                        this.getItemStacks().stream().filter(stack -> stack.is(ModItems.RIFLE_AMMO.get())).findFirst().ifPresent(stack -> stack.shrink(1));
                    }
                }
            }

            this.entityData.set(COAX_HEAT, this.entityData.get(COAX_HEAT) + 3);
            this.entityData.set(FIRE_ANIM, 2);

            if (!player.level().isClientSide) {
                playShootSound3p(player, 0, 3, 6, 12);
            }
        }
    }

    @Override
    public void travel() {
        Entity passenger0 = this.getFirstPassenger();

        if (this.getEnergy() <= 0) return;

        if (passenger0 == null) {
            this.leftInputDown = false;
            this.rightInputDown = false;
            this.forwardInputDown = false;
            this.backInputDown = false;
            this.entityData.set(POWER, 0f);
        }

        if (forwardInputDown) {
            this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + (this.entityData.get(POWER) < 0 ? 0.012f : 0.0024f), 0.18f));
        }

        if (backInputDown) {
            this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - (this.entityData.get(POWER) > 0 ? 0.012f : 0.0024f), -0.13f));
        }

        if (rightInputDown) {
            this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.1f);
        } else if (this.leftInputDown) {
            this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.1f);
        }

        if (this.forwardInputDown || this.backInputDown) {
            this.consumeEnergy(VehicleConfig.LAV_150_ENERGY_COST.get());
        }

        this.entityData.set(POWER, this.entityData.get(POWER) * (upInputDown ? 0.5f : (rightInputDown || leftInputDown) ? 0.977f : 0.99f));
        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * (float) Math.max(0.76f - 0.1f * this.getDeltaMovement().horizontalDistance(), 0.3));

        double s0 = getDeltaMovement().dot(this.getViewVector(1));

        this.setLeftWheelRot((float) ((this.getLeftWheelRot() - 1.25 * s0) - this.getDeltaMovement().horizontalDistance() * Mth.clamp(1.5f * this.entityData.get(DELTA_ROT), -5f, 5f)));
        this.setRightWheelRot((float) ((this.getRightWheelRot() - 1.25 * s0) + this.getDeltaMovement().horizontalDistance() * Mth.clamp(1.5f * this.entityData.get(DELTA_ROT), -5f, 5f)));

        this.setRudderRot(Mth.clamp(this.getRudderRot() - this.entityData.get(DELTA_ROT), -0.8f, 0.8f) * 0.75f);

        this.setYRot((float) (this.getYRot() - Math.max((isInWater() && !onGround() ? 5 : 10) * this.getDeltaMovement().horizontalDistance(), 0) * this.getRudderRot() * (this.entityData.get(POWER) > 0 ? 1 : -1)));
        if (this.isInWater() || onGround()) {
            float power = this.entityData.get(POWER) * Mth.clamp(1 + (s0 > 0 ? 1 : -1) * getXRot() / 35, 0, 2);
            this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale((!isInWater() && !onGround() ? 0.05f : (isInWater() && !onGround() ? 0.3f : 1)) * power)));
        }
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.LAV_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return Mth.abs(entityData.get(POWER)) * 2f;
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        // From Immersive_Aircraft
        if (!this.hasPassenger(passenger)) {
            return;
        }

        Matrix4f transform = getTurretTransform(1);
        Matrix4f transformV = getVehicleTransform(1);

        int i = this.getSeatIndex(passenger);

        Vector4f worldPosition;
        if (i == 0) {
            worldPosition = transformPosition(transform, 0.36f, -0.65f, 0.56f);
        } else {
            worldPosition = transformPosition(transformV, 0, 1, 0);
        }
        passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);

        copyEntityData(passenger);
    }

    public void copyEntityData(Entity entity) {
        if (entity == getNthEntity(0)) {
            entity.setYBodyRot(getBarrelYRot(1));
        }
    }

    @Override
    public int getMaxPassengers() {
        return 5;
    }

    @Override
    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getTurretTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0.3f, 0.75f, 0.56f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    public Matrix4f getBarrelTransform(float ticks) {
        Matrix4f transformT = getTurretTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0.0234375f, 0.33795f, 0.825f);

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

    public Vec3 getTurretVector(float pPartialTicks) {
        Matrix4f transform = getTurretTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    public Matrix4f getTurretTransform(float ticks) {
        Matrix4f transformV = getVehicleTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0, 2.4003f, 0);

        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        transformV.rotate(Axis.YP.rotationDegrees(Mth.lerp(ticks, turretYRotO, getTurretYRot())));
        return transformV;
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

        float min = -32.5f - r * getXRot() - r2 * getRoll();
        float max = 15f - r * getXRot() - r2 * getRoll();

        float f = Mth.wrapDegrees(entity.getXRot());
        float f1 = Mth.clamp(f, min, max);
        entity.xRotO += f1 - f;
        entity.setXRot(entity.getXRot() + f1 - f);

        entity.setYBodyRot(getBarrelYRot(1));
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    private PlayState firePredicate(AnimationState<Lav150Entity> event) {
        if (this.entityData.get(FIRE_ANIM) > 1 && getWeaponIndex(0) == 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lav.fire"));
        }

        if (this.entityData.get(FIRE_ANIM) > 0 && getWeaponIndex(0) == 1) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lav.fire2"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.lav.idle"));
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
    public int mainGunRpm(Player player) {
        if (getWeaponIndex(0) == 0) {
            return 300;
        } else if (getWeaponIndex(0) == 1) {
            return 600;
        }
        return 300;
    }

    @Override
    public boolean canShoot(Player player) {
        if (getWeaponIndex(0) == 0) {
            return (this.entityData.get(AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(player)) && !cannotFire;
        } else if (getWeaponIndex(0) == 1) {
            return (this.entityData.get(AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(player)) && !cannotFireCoax;
        }
        return false;
    }

    @Override
    public int getAmmoCount(Player player) {
        return this.entityData.get(AMMO);
    }

    @Override
    public boolean banHand(Player player) {
        return true;
    }

    @Override
    public boolean hidePassenger(Entity entity) {
        return true;
    }

    @Override
    public int zoomFov() {
        return 3;
    }

    @Override
    public int getWeaponHeat(Player player) {
        if (getWeaponIndex(0) == 0) {
            return entityData.get(HEAT);
        } else if (getWeaponIndex(0) == 1) {
            return entityData.get(COAX_HEAT);
        }
        return 0;
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/lav150_icon.png");
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderFirstPersonOverlay(GuiGraphics guiGraphics, Font font, Player player, int screenWidth, int screenHeight, float scale) {
        super.renderFirstPersonOverlay(guiGraphics, font, player, screenWidth, screenHeight, scale);

        if (this.getWeaponIndex(0) == 0) {
            double heat = 1 - this.getEntityData().get(HEAT) / 100.0F;
            guiGraphics.drawString(font, Component.literal("20MM CANNON " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getAmmoCount(player))), screenWidth / 2 - 33, screenHeight - 65, Mth.hsvToRgb((float) heat / 3.745318352059925F, 1.0F, 1.0F), false);
        } else {
            double heat = 1 - this.getEntityData().get(COAX_HEAT) / 100.0F;
            guiGraphics.drawString(font, Component.literal("7.62MM COAX " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getAmmoCount(player))), screenWidth / 2 - 33, screenHeight - 65, Mth.hsvToRgb((float) heat / 3.745318352059925F, 1.0F, 1.0F), false);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderThirdPersonOverlay(GuiGraphics guiGraphics, Font font, Player player, int screenWidth, int screenHeight, float scale) {
        super.renderThirdPersonOverlay(guiGraphics, font, player, screenWidth, screenHeight, scale);

        if (this.getWeaponIndex(0) == 0) {
            double heat = this.getEntityData().get(HEAT) / 100.0F;
            guiGraphics.drawString(font, Component.literal("20MM CANNON " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getAmmoCount(player))), 30, -9, Mth.hsvToRgb(0F, (float) heat, 1.0F), false);
        } else {
            double heat2 = this.getEntityData().get(COAX_HEAT) / 100.0F;
            guiGraphics.drawString(font, Component.literal("7.62MM COAX " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getAmmoCount(player))), 30, -9, Mth.hsvToRgb(0F, (float) heat2, 1.0F), false);
        }
    }

    @Override
    public boolean hasDecoy() {
        return true;
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return zoom ? 0.23 : Minecraft.getInstance().options.getCameraType().isFirstPerson() ? 0.3 : 0.4;
    }

    @Override
    public boolean isEnclosed(int index) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            if (this.getSeatIndex(player) == 0) {
                return new Vec2((float) -getYRotFromVector(this.getBarrelVec(partialTicks)), (float) -getXRotFromVector(this.getBarrelVec(partialTicks)));
            } else {
                return new Vec2(Mth.lerp(partialTicks, player.yHeadRotO, player.getYHeadRot()), Mth.lerp(partialTicks, player.xRotO, player.getXRot()));
            }
        }
        return super.getCameraRotation(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            if (this.getSeatIndex(player) == 0) {
                if (zoom) {
                    return new Vec3(this.driverZoomPos(partialTicks).x, Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), this.driverZoomPos(partialTicks).z);
                } else {
                    return new Vec3(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
                }
            } else {
                return new Vec3(Mth.lerp(partialTicks, player.xo, player.getX()) - 6 * player.getViewVector(partialTicks).x,
                        Mth.lerp(partialTicks, player.yo + player.getEyeHeight() + 1, player.getEyeY() + 1) - 6 * player.getViewVector(partialTicks).y,
                        Mth.lerp(partialTicks, player.zo, player.getZ()) - 6 * player.getViewVector(partialTicks).z);
            }
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }

    @Override
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/land.png");
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obb6, this.obb7, this.obbTurret);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, -1.140625f, 0.75f, 1.584375f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 1.140625f, 0.75f, 1.584375f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition3 = transformPosition(transform, 1.140625f, 0.75f, -1.571875f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition4 = transformPosition(transform, -1.140625f, 0.75f, -1.571875f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition5 = transformPosition(transform, 0, 1.53125f, -0.4375f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition6 = transformPosition(transform, 0, 1.90625f, -3.21875f);
        this.obb6.center().set(new Vector3f(worldPosition6.x, worldPosition6.y, worldPosition6.z));
        this.obb6.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition7 = transformPosition(transform, 0, 1.4375f, 2.53125f);
        this.obb7.center().set(new Vector3f(worldPosition7.x, worldPosition7.y, worldPosition7.z));
        this.obb7.setRotation(VectorTool.combineRotations(1, this));

        Matrix4f transformT = getTurretTransform(1);
        Vector4f worldPositionT = transformPosition(transformT, 0, 0.3625f, 0f);
        this.obbTurret.center().set(new Vector3f(worldPositionT.x, worldPositionT.y, worldPositionT.z));
        this.obbTurret.setRotation(VectorTool.combineRotationsTurret(1, this));
    }
}
