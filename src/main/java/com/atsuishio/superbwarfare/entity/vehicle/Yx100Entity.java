package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import com.atsuishio.superbwarfare.entity.projectile.SwarmDroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ContainerMobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.LandArmorEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.CannonShellWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.ProjectileWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.SwarmDroneWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class Yx100Entity extends ContainerMobileVehicleEntity implements GeoEntity, LandArmorEntity, WeaponVehicleEntity, OBBEntity {

    public static final EntityDataAccessor<Integer> MG_AMMO = SynchedEntityData.defineId(Yx100Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> LOADED_SHELL = SynchedEntityData.defineId(Yx100Entity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> SELECTED_AMMO_TYPE = SynchedEntityData.defineId(Yx100Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> GUN_FIRE_TIME = SynchedEntityData.defineId(Yx100Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> LOADED_DRONE = SynchedEntityData.defineId(Yx100Entity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public int droneReloadCoolDown;

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obbTurret;
    public OBB obbTurret2;

    public Yx100Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.YX_100.get(), world);
    }

    public Yx100Entity(EntityType<Yx100Entity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(2.375f, 0.71875f, 4f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(2.375f, 0.59375f, 0.65625f), new Quaternionf(), OBB.Part.BODY);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(0.625f, 0.84375f, 3.875f), new Quaternionf(), OBB.Part.WHEEL_LEFT);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.625f, 0.84375f, 3.875f), new Quaternionf(), OBB.Part.WHEEL_RIGHT);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(2.0625f, 0.59375f, 0.625f), new Quaternionf(), OBB.Part.ENGINE1);
        this.obbTurret = new OBB(this.position().toVector3f(), new Vector3f(2.375f, 0.5625f, 2.1875f), new Quaternionf(), OBB.Part.TURRET);
        this.obbTurret2 = new OBB(this.position().toVector3f(), new Vector3f(1.625f, 0.40625f, 0.59375f), new Quaternionf(), OBB.Part.TURRET);
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        // AP
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.YX_100_AP_CANNON_DAMAGE.get())
                                .explosionRadius(VehicleConfig.YX_100_AP_CANNON_EXPLOSION_RADIUS.get().floatValue())
                                .explosionDamage(VehicleConfig.YX_100_AP_CANNON_EXPLOSION_DAMAGE.get())
                                .fireProbability(0)
                                .fireTime(0)
                                .durability(100)
                                .velocity(40)
                                .gravity(0.1f)
                                .sound(ModSounds.INTO_MISSILE.get())
                                .ammo(ModItems.AP_5_INCHES.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/ap_shell.png"))
                                .sound1p(ModSounds.YX_100_FIRE_1P.get())
                                .sound3p(ModSounds.YX_100_FIRE_3P.get())
                                .sound3pFar(ModSounds.YX_100_FAR.get())
                                .sound3pVeryFar(ModSounds.YX_100_VERYFAR.get())
                                .mainGun(true),
                        // HE
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.YX_100_HE_CANNON_DAMAGE.get())
                                .explosionRadius(VehicleConfig.YX_100_HE_CANNON_EXPLOSION_RADIUS.get().floatValue())
                                .explosionDamage(VehicleConfig.YX_100_HE_CANNON_EXPLOSION_DAMAGE.get())
                                .fireProbability(0.18F)
                                .fireTime(2)
                                .durability(1)
                                .velocity(25)
                                .gravity(0.1f)
                                .sound(ModSounds.INTO_CANNON.get())
                                .ammo(ModItems.HE_5_INCHES.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/he_shell.png"))
                                .sound1p(ModSounds.YX_100_FIRE_1P.get())
                                .sound3p(ModSounds.YX_100_FIRE_3P.get())
                                .sound3pFar(ModSounds.YX_100_FAR.get())
                                .sound3pVeryFar(ModSounds.YX_100_VERYFAR.get())
                                .mainGun(true),
                        // CM
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.YX_100_HE_CANNON_DAMAGE.get())
                                .explosionRadius(VehicleConfig.YX_100_HE_CANNON_EXPLOSION_RADIUS.get().floatValue())
                                .explosionDamage(VehicleConfig.YX_100_HE_CANNON_EXPLOSION_DAMAGE.get())
                                .fireProbability(0.18F)
                                .fireTime(2)
                                .durability(1)
                                .velocity(25)
                                .gravity(0.1f)
                                .type(CannonShellEntity.Type.CM)
                                .spreadAmount(15)
                                .spreadTime(4)
                                .spreadAngle(5)
                                .sound(ModSounds.INTO_CANNON.get())
                                .ammo(ModItems.CM_5_INCHES.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/cm_shell.png"))
                                .sound1p(ModSounds.YX_100_FIRE_1P.get())
                                .sound3p(ModSounds.YX_100_FIRE_3P.get())
                                .sound3pFar(ModSounds.YX_100_FAR.get())
                                .sound3pVeryFar(ModSounds.YX_100_VERYFAR.get())
                                .mainGun(true),
                        // GRAPESHOT
                        new CannonShellWeapon()
                                .hitDamage(1000)
                                .explosionRadius(VehicleConfig.YX_100_AP_CANNON_EXPLOSION_RADIUS.get().floatValue())
                                .explosionDamage(VehicleConfig.YX_100_AP_CANNON_EXPLOSION_DAMAGE.get())
                                .velocity(30)
                                .type(CannonShellEntity.Type.GRAPE)
                                .spreadAmount(40)
                                .spreadAngle(3)
                                .sound(ModSounds.INTO_CANNON.get())
                                .ammo(ModItems.GS_5_INCHES.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/grape_shell.png"))
                                .sound1p(ModSounds.YX_100_FIRE_1P.get())
                                .sound3p(ModSounds.YX_100_FIRE_3P.get())
                                .sound3pFar(ModSounds.YX_100_FAR.get())
                                .sound3pVeryFar(ModSounds.YX_100_VERYFAR.get())
                                .mainGun(true),
                        // 同轴重机枪
                        new ProjectileWeapon()
                                .damage(VehicleConfig.HEAVY_MACHINE_GUN_DAMAGE.get())
                                .headShot(2)
                                .zoom(false)
                                .bypassArmorRate(0.4f)
                                .ammo(ModItems.HEAVY_AMMO.get())
                                .sound(ModSounds.INTO_CANNON.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_12_7mm.png"))
                                .sound1p(ModSounds.M_2_HB_FIRE_1P.get())
                                .sound3p(ModSounds.M_2_HB_FIRE_3P.get())
                                .sound3pFar(ModSounds.M_2_HB_FAR.get())
                                .sound3pVeryFar(ModSounds.M_2_HB_VERYFAR.get()),
                },
                new VehicleWeapon[]{
                        // 机枪
                        new ProjectileWeapon()
                                .damage(VehicleConfig.HEAVY_MACHINE_GUN_DAMAGE.get())
                                .headShot(2)
                                .zoom(false)
                                .bypassArmorRate(0.4f)
                                .ammo(ModItems.HEAVY_AMMO.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_12_7mm.png"))
                                .sound1p(ModSounds.M_2_HB_FIRE_1P.get())
                                .sound3p(ModSounds.M_2_HB_FIRE_3P.get())
                                .sound3pFar(ModSounds.M_2_HB_FAR.get())
                                .sound3pVeryFar(ModSounds.M_2_HB_VERYFAR.get()),
                },
                new VehicleWeapon[]{
                        // 蜂群无人机
                        new SwarmDroneWeapon()
                                .explosionDamage(VehicleConfig.YX_100_SWARM_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.YX_100_SWARM_EXPLOSION_RADIUS.get().floatValue())
                                .sound(ModSounds.INTO_MISSILE.get()),
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return switch (index) {
            case 0 -> new ThirdPersonCameraPosition(5 + ClientMouseHandler.custom3pDistanceLerp, 1.5, -0.8669625);
            case 1 -> new ThirdPersonCameraPosition(-1 + 0.5 * ClientMouseHandler.custom3pDistanceLerp, 0.5, 0);
            default -> null;
        };
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MG_AMMO, 0);
        this.entityData.define(LOADED_SHELL, "null");
        this.entityData.define(LOADED_DRONE, 0);
        this.entityData.define(SELECTED_AMMO_TYPE, 0);
        this.entityData.define(GUN_FIRE_TIME, 0);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("LoadedShell", this.entityData.get(LOADED_SHELL));
        compound.putInt("LoadedDrone", this.entityData.get(LOADED_DRONE));
        compound.putInt("SelectedAmmoType", this.entityData.get(SELECTED_AMMO_TYPE));
        compound.putInt("WeaponType", getWeaponIndex(0));
        compound.putInt("PassengerWeaponType", getWeaponIndex(1));
        compound.putInt("ThirdPassengerWeaponType", getWeaponIndex(2));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(LOADED_SHELL, compound.getString("LoadedShell"));
        this.entityData.set(LOADED_DRONE, compound.getInt("LoadedDrone"));
        this.entityData.set(SELECTED_AMMO_TYPE, compound.getInt("SelectedAmmoType"));
        setWeaponIndex(0, compound.getInt("WeaponType"));
        setWeaponIndex(1, compound.getInt("PassengerWeaponType"));
        setWeaponIndex(2, compound.getInt("ThirdPassengerWeaponType"));
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.3f) * damage);
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 0.15), random.nextFloat() * 0.15f + 1.05f);
    }


    @Override
    public void baseTick() {
        super.baseTick();
        this.updateOBB();

        if (getLeftTrack() < 0) {
            setLeftTrack(80);
        }

        if (getLeftTrack() > 80) {
            setLeftTrack(0);
        }

        if (getRightTrack() < 0) {
            setRightTrack(80);
        }

        if (getRightTrack() > 80) {
            setRightTrack(0);
        }

        if (this.entityData.get(GUN_FIRE_TIME) > 0) {
            this.entityData.set(GUN_FIRE_TIME, this.entityData.get(GUN_FIRE_TIME) - 1);
        }

        if (reloadCoolDown == 70 && this.getFirstPassenger() instanceof Player player) {
            SoundTool.playLocalSound(player, ModSounds.YX_100_RELOAD.get());
        }

        if (this.level() instanceof ServerLevel) {
            boolean hasCreativeAmmo = false;
            for (int i = 0; i < getMaxPassengers(); i++) {
                if (InventoryTool.hasCreativeAmmoBox(getNthEntity(i))) {
                    hasCreativeAmmo = true;
                }
            }

            if (reloadCoolDown > 0 && getWeapon(0).mainGun && (hasCreativeAmmo || countItem(getWeapon(0).ammo) > 0)) {
                reloadCoolDown--;
            }

            if (droneReloadCoolDown > 0) {
                droneReloadCoolDown--;
            }

            if ((hasItem(ModItems.SWARM_DRONE.get()) || hasCreativeAmmo) && droneReloadCoolDown == 0 && this.getEntityData().get(LOADED_DRONE) < 14) {
                this.entityData.set(LOADED_DRONE, this.getEntityData().get(LOADED_DRONE) + 1);
                droneReloadCoolDown = 20;
                if (!hasCreativeAmmo) {
                    this.getItemStacks().stream().filter(stack -> stack.is(ModItems.SWARM_DRONE.get())).findFirst().ifPresent(stack -> stack.shrink(1));
                }
                this.level().playSound(null, this, ModSounds.MISSILE_RELOAD.get(), this.getSoundSource(), 1, 1);
            }

            this.handleAmmo();
        }

        if (getNthEntity(2) instanceof Mob mob && canShoot(mob) && mob.getTarget() != null) {
            int rpm = 20 / (mainGunRpm(mob) / 60);
            if (tickCount %rpm == 0) {
                vehicleShoot(mob, 2);
            }
        }

        lowHealthWarning();
        terrainCompact(4.375f, 6.3125f);
        inertiaRotate(1.2f);
        releaseSmokeDecoy(getTurretVector(1));

        this.refreshDimensions();
    }

    // 炮塔最大水平旋转速度
    @Override
    public float turretYSpeed() {
        return 5;
    }
    // 炮塔最大俯仰旋转速度
    @Override
    public float turretXSpeed() {
        return 5F;
    }
    // 炮塔最小俯角
    @Override
    public float turretMinPitch() {
        return -10f;
    }
    // 炮塔最大仰角
    @Override
    public float turretMaxPitch() {
        return 30f;
    }
    // 炮弹发射位置
    @Override
    public Vec3 getTurretShootPos(Entity entity, float ticks) {
        Vector4f worldPosition;
        if (entity == getNthEntity(0)) {
            Matrix4f transform = getBarrelTransform(ticks);
            if (getWeapon(0).mainGun) {
                worldPosition = transformPosition(transform, 0, 0, 0);
            } else {
                worldPosition = transformPosition(transform, -0.12f, 0.15f, 0f);
            }
        } else {
            Matrix4f transform = getGunTransform(1);
            worldPosition = transformPosition(transform, 0, -0.25f, 0);
        }

        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }
    // 炮弹发射速度
    @Override
    public float projectileVelocity(Entity entity) {
        if (entity == getNthEntity(0)) {
            if (getWeapon(0).mainGun) {
                var cannonShell = (CannonShellWeapon) getWeapon(0);
                return cannonShell.velocity;
            } else {
                return 20f;
            }
        } else {
            return 20f;
        }
    }
    // 炮弹重力
    @Override
    public float projectileGravity(Entity entity) {
        if (getWeapon(0).mainGun && entity == getNthEntity(0)) {
            var cannonShell = (CannonShellWeapon) getWeapon(0);
            return cannonShell.gravity;
        } else {
            return 0.05f;
        }
    }

    // 乘客武器站最大水平旋转速度
    @Override
    public float passengerWeaponYSpeed() {
        return 15;
    }
    // 乘客武器站最大俯仰旋转速度
    @Override
    public float passengerWeaponXSpeed() {
        return 15;
    }
    // 乘客武器站最小俯角
    @Override
    public float passengerWeaponMinPitch() {
        return -10;
    }
    // 乘客武器站最大仰角
    @Override
    public float passengerWeaponMaxPitch() {
        return 60;
    }
    // 乘客武器站弹药发射位置
    @Override
    public Vec3 passengerWeaponShootPos(Entity entity, float ticks) {
        Matrix4f transform = getGunTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0, -0.25f, 0);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public boolean canCollideHardBlock() {
        return getDeltaMovement().horizontalDistance() > 0.05 || Mth.abs(this.entityData.get(POWER)) > 0.1;
    }

    @Override
    public boolean canCollideBlockBeastly() {
        return getDeltaMovement().horizontalDistance() > 0.3;
    }

    private void handleAmmo() {
        if (getWeapon(0).mainGun) {
            entityData.set(SELECTED_AMMO_TYPE, getWeaponIndex(0));
        }

        boolean hasCreativeAmmo = false;
        for (int i = 0; i < getMaxPassengers(); i++) {
            if (InventoryTool.hasCreativeAmmoBox(getNthEntity(i))) {
                hasCreativeAmmo = true;
            }
        }

        if (hasCreativeAmmo) {
            this.entityData.set(AMMO, 9999);
            this.entityData.set(MG_AMMO, 9999);
        } else {
            this.entityData.set(AMMO, countItem(getWeapon(0).ammo));
            this.entityData.set(MG_AMMO, countItem(getWeapon(1).ammo));
        }

        if (this.getEntityData().get(LOADED_SHELL).equals("null") && reloadCoolDown <= 0 && (hasCreativeAmmo || hasItem(getWeapon(0).ammo))) {
            this.entityData.set(LOADED_SHELL, String.valueOf(ForgeRegistries.ITEMS.getKey(getWeapon(0).ammo)));
            if (!hasCreativeAmmo) {
                consumeItem(getWeapon(0).ammo, 1);
            }
        }
    }

    @Override
    public void move(@NotNull MoverType movementType, @NotNull Vec3 movement) {
        super.move(movementType, movement);
        if (this.isInWater() && horizontalCollision) {
            setDeltaMovement(this.getDeltaMovement().add(0, 0.07, 0));
        }
    }

    @Override
    public void vehicleShoot(LivingEntity living, int type) {
        boolean hasCreativeAmmo = false;
        for (int i = 0; i < getMaxPassengers() - 1; i++) {
            if (InventoryTool.hasCreativeAmmoBox(getNthEntity(i))) {
                hasCreativeAmmo = true;
            }
        }

        if (type == 0) {
            if (reloadCoolDown == 0 && getWeapon(0).mainGun) {
                if (!this.canConsume(VehicleConfig.YX_100_SHOOT_COST.get()) && living instanceof Player player) {
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.annihilator.energy_not_enough").withStyle(ChatFormatting.RED), true);
                    return;
                }

                var cannonShell = (CannonShellWeapon) getWeapon(0);
                var entityToSpawn = cannonShell.create(living);

                entityToSpawn.setPos(getTurretShootPos(living, 1).x, getTurretShootPos(living, 1).y, getTurretShootPos(living, 1).z);
                entityToSpawn.shoot(getBarrelVector(1).x, getBarrelVector(1).y, getBarrelVector(1).z, cannonShell.velocity, 0.02f);
                level().addFreshEntity(entityToSpawn);

                playShootSound3p(living, 0, 8, 16, 32, new Vec3(getTurretShootPos(living, 1).x, getTurretShootPos(living, 1).y, getTurretShootPos(living, 1).z));

                this.entityData.set(CANNON_RECOIL_TIME, 40);
                this.entityData.set(LOADED_SHELL, "null");

                this.consumeEnergy(10000);
                this.entityData.set(YAW, getTurretYRot());

                reloadCoolDown = 80;

                if (this.level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            this.getX() + 5 * getBarrelVector(1).x,
                            this.getY() + 0.1,
                            this.getZ() + 5 * getBarrelVector(1).z,
                            300, 6, 0.02, 6, 0.005);

                    double x = getTurretShootPos(living, 1).x + 9 * getBarrelVector(1).x;
                    double y = getTurretShootPos(living, 1).y + 9 * getBarrelVector(1).y;
                    double z = getTurretShootPos(living, 1).z + 9 * getBarrelVector(1).z;

                    server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 10, 0.4, 0.4, 0.4, 0.0075);
                    server.sendParticles(ParticleTypes.CLOUD, x, y, z, 10, 0.4, 0.4, 0.4, 0.0075);

                    int count = 6;

                    for (float i = 9.5f; i < 23; i += .5f) {
                        server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                getTurretShootPos(living, 1).x + i * getBarrelVector(1).x,
                                getTurretShootPos(living, 1).y + i * getBarrelVector(1).y,
                                getTurretShootPos(living, 1).z + i * getBarrelVector(1).z,
                                Mth.clamp(count--, 1, 5), 0.15, 0.15, 0.15, 0.0025);
                    }

                    Matrix4f transform = getBarrelTransform(1);
                    Vector4f worldPositionL = transformPosition(transform, -0.35f, 0, 0);
                    Vector4f worldPositionR = transformPosition(transform, 0.35f, 0, 0);

                    for (float i = 3f; i < 6; i += .5f) {
                        server.sendParticles(ParticleTypes.CLOUD,
                                worldPositionL.x + i * getBarrelVector(1).x,
                                worldPositionL.y + i * getBarrelVector(1).y,
                                worldPositionL.z + i * getBarrelVector(1).z,
                                1, 0.025, 0.025, 0.025, 0.0015);

                        server.sendParticles(ParticleTypes.CLOUD,
                                worldPositionR.x + i * getBarrelVector(1).x,
                                worldPositionR.y + i * getBarrelVector(1).y,
                                worldPositionR.z + i * getBarrelVector(1).z,
                                1, 0.025, 0.025, 0.025, 0.0015);
                    }
                }

                ShakeClientMessage.sendToNearbyPlayers(this, 8, 10, 8, 60);
            } else if (getWeaponIndex(0) == 4) {
                if (this.cannotFireCoax) return;

                if (this.entityData.get(MG_AMMO) > 0 || hasCreativeAmmo) {
                    var projectileRight = ((ProjectileWeapon) getWeapon(0)).create(living).setGunItemId(this.getType().getDescriptionId() + ".1");

                    projectileRight.setPos(getTurretShootPos(living, 1).x, getTurretShootPos(living, 1).y, getTurretShootPos(living, 1).z);
                    projectileRight.shoot(living, getBarrelVector(1).x, getBarrelVector(1).y, getBarrelVector(1).z, 20,
                            0.25f);
                    this.level().addFreshEntity(projectileRight);

                    this.entityData.set(COAX_HEAT, this.entityData.get(COAX_HEAT) + 4);
                    this.entityData.set(FIRE_ANIM, 2);

                    playShootSound3p(living, 0, 4, 12, 24, new Vec3(getTurretShootPos(living, 1).x, getTurretShootPos(living, 1).y, getTurretShootPos(living, 1).z));

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
            }
        }

        if (type == 1) {
            if (!canShoot(living)) return;

            var projectile = (ProjectileWeapon) getWeapon(1);
            var projectileEntity = projectile.create(living).setGunItemId(this.getType().getDescriptionId() + ".2");

            projectileEntity.setPos(passengerWeaponShootPos(living, 1).x, passengerWeaponShootPos(living, 1).y, passengerWeaponShootPos(living, 1).z);
            projectileEntity.shoot(getGunnerVector(1).x, getGunnerVector(1).y, getGunnerVector(1).z, 20, 0.3f);

            this.level().addFreshEntity(projectileEntity);
            playShootSound3p(living, 1, 4, 12, 24, new Vec3(passengerWeaponShootPos(living, 1).x, passengerWeaponShootPos(living, 1).y, passengerWeaponShootPos(living, 1).z));

            this.entityData.set(GUN_FIRE_TIME, 2);
            this.entityData.set(HEAT, this.entityData.get(HEAT) + 4);

            ShakeClientMessage.sendToNearbyPlayers(this, 4, 6, 4, 6);

            if (hasCreativeAmmo) return;

            ItemStack ammoBox = this.getItemStacks().stream().filter(stack -> {
                if (stack.is(ModItems.AMMO_BOX.get())) {
                    return Ammo.HEAVY.get(stack) > 0;
                }
                return false;
            }).findFirst().orElse(ItemStack.EMPTY);

            if (!ammoBox.isEmpty()) {
                Ammo.HEAVY.add(ammoBox, -1);
            } else {
                consumeItem(getWeapon(1).ammo, 1);
            }
        }

        if (type == 2) {
            Matrix4f transformT = getTurretTransform(1);
            Vector4f worldPosition = new Vector4f();

            int ammo = this.getEntityData().get(LOADED_DRONE);

            if (ammo == 1) {
                worldPosition = transformPosition(transformT, -1.6290875f, 0.75536875f, -1.76616875f);
            }
            if (ammo == 2) {
                worldPosition = transformPosition(transformT, 1.6290875f, 0.75536875f, -1.76616875f);
            }
            if (ammo == 3) {
                worldPosition = transformPosition(transformT, -1.7822125f, 0.75536875f, -1.9193f);
            }
            if (ammo == 4) {
                worldPosition = transformPosition(transformT, 1.7822125f, 0.75536875f, -1.9193f);
            }
            if (ammo == 5) {
                worldPosition = transformPosition(transformT, -1.6290875f, 0.75536875f, -2.07241875f);
            }
            if (ammo == 6) {
                worldPosition = transformPosition(transformT, 1.6290875f, 0.75536875f, -2.07241875f);
            }
            if (ammo == 7) {
                worldPosition = transformPosition(transformT, -1.7822125f, 0.75536875f, -2.22555f);
            }
            if (ammo == 8) {
                worldPosition = transformPosition(transformT, 1.7822125f, 0.75536875f, -2.22555f);
            }
            if (ammo == 9) {
                worldPosition = transformPosition(transformT, -1.6290875f, 0.75536875f, -2.37866875f);
            }
            if (ammo == 10) {
                worldPosition = transformPosition(transformT, 1.6290875f, 0.75536875f, -2.37866875f);
            }
            if (ammo == 11) {
                worldPosition = transformPosition(transformT, -1.7822125f, 0.75536875f, -2.5318f);
            }
            if (ammo == 12) {
                worldPosition = transformPosition(transformT, 1.7822125f, 0.75536875f, -2.5318f);
            }
            if (ammo == 13) {
                worldPosition = transformPosition(transformT, -1.6290875f, 0.75536875f, -2.68491875f);
            }
            if (ammo == 14) {
                worldPosition = transformPosition(transformT, 1.6290875f, 0.75536875f, -2.68491875f);
            }

            Vec3 lookVec = living.getViewVector(1);
            Entity lookingEntity = SeekTool.seekLivingEntity(living, level(), 384, 6);

            var swarmDroneEntity = ((SwarmDroneWeapon) getWeapon(2)).create(living);

            Vector4f shootPosition1 = transformPosition(transformT, 0, 0, 0);
            Vector4f shootPosition2 = transformPosition(transformT, 0, 1, 0);
            Vec3 direct = new Vec3(shootPosition1.x, shootPosition1.y, shootPosition1.z).vectorTo(new Vec3(shootPosition2.x, shootPosition2.y, shootPosition2.z));

            swarmDroneEntity.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            swarmDroneEntity.shoot(direct.x, direct.y, direct.z, 1.2f, 10);

            if (living instanceof Mob mob && mob.getTarget() != null) {
                Entity target = mob.getTarget();
                if (target.getVehicle() != null) {
                    target = target.getVehicle();
                }

                swarmDroneEntity.setGuideType(0);
                swarmDroneEntity.setTargetUuid(target.getStringUUID());
                swarmDroneEntity.setTargetVec(target.getBoundingBox().getCenter());

            } else if (living instanceof Player) {
                if (lookingEntity != null && !(lookingEntity instanceof SwarmDroneEntity swarmDrone && swarmDrone.getOwner() == living)) {
                    swarmDroneEntity.setGuideType(0);
                    swarmDroneEntity.setTargetUuid(lookingEntity.getStringUUID());
                    swarmDroneEntity.setTargetVec(lookingEntity.getEyePosition());
                } else {
                    swarmDroneEntity.setGuideType(1);
                    BlockHitResult result = level().clip(new ClipContext(living.getEyePosition(), living.getEyePosition().add(lookVec.scale(384)),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                    Vec3 hitPos = result.getLocation();
                    swarmDroneEntity.setTargetVec(hitPos);
                }
            }




            living.level().addFreshEntity(swarmDroneEntity);

            this.level().playSound(null, BlockPos.containing(new Vec3(worldPosition.x, worldPosition.y, worldPosition.z)), ModSounds.DECOY_FIRE.get(), SoundSource.PLAYERS, 1, random.nextFloat() * 0.05f + 1);

            this.entityData.set(LOADED_DRONE, this.getEntityData().get(LOADED_DRONE) - 1);
            droneReloadCoolDown = 100;
        }

    }

    @Override
    public void travel() {
        trackEngine(false, 0, VehicleConfig.YX_100_ENERGY_COST.get(), 1.25, 0.75, 1.3, 0.6, 0.25f, -0.2f, 0.0026f, 0.002f, 0.1f);
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.YX_100_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return Math.max(Mth.abs(entityData.get(POWER)), Mth.abs(0.1f * this.entityData.get(DELTA_ROT))) * 2.5f;
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        // From Immersive_Aircraft
        if (!this.hasPassenger(passenger)) {
            return;
        }

        Matrix4f transform = getTurretTransform(1);

        int i = this.getOrderedPassengers().indexOf(passenger);

        var worldPosition = switch (i) {
            case 0 -> transformPosition(transform, 0.6669625f, 0.07f, 0.4776875f);
            case 1 -> transformPosition(transform, -0.75805625f, 0.3f, -0.57275625f);
            case 2 -> transformPosition(transform, 0.86219375f, 0.07f, -0.5696875f);
            default -> throw new IllegalStateException("Unexpected value: " + i);
        };

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
    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getTurretTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0, 1f, 0.6076875f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }



    public int getMaxPassengers() {
        return 3;
    }

    @Override
    public Vec3 getBarrelPosition() {
        return new Vec3(0f, 0.56345, 0.6477125);
    }

    @Override
    public Vec3 getTurretPosition() {
        return new Vec3(0, 2.1059375, -0.43125);
    }

    @Override
    public Vec3 getGunnerPosition() {
        return new Vec3(-0.75805625f, 1.1446375, -0.57275625);
    }
    @Override
    public Vec3 getGunnerBarrelPosition() {
        return new Vec3(0f, 0.35984375f, 0.0551625f);
    }

    @Override
    public float rotateYOffset() {
        return 3.5f;
    }

    protected void clampRotation(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (entity.level().isClientSide && entity == getFirstPassenger()) {
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

            float min = -turretMaxPitch() - r * getXRot() - r2 * getRoll();
            float max = -turretMinPitch() - r * getXRot() - r2 * getRoll();

            float f = Mth.wrapDegrees(entity.getXRot());
            float f1 = Mth.clamp(f, min, max);
            entity.xRotO += f1 - f;
            entity.setXRot(entity.getXRot() + f1 - f);

            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                float f2 = Mth.wrapDegrees(entity.getYRot() - this.getBarrelYRot(1));
                float f3 = Mth.clamp(f2, -20.0F, 20.0F);
                entity.yRotO += f3 - f2;
                entity.setYRot(entity.getYRot() + f3 - f2);
                entity.setYBodyRot(getBarrelYRot(1));
            }
        } else if (entity == getNthEntity(1)) {

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

            float min = -passengerWeaponMaxPitch() - r * getXRot() - r2 * getRoll();
            float max = -passengerWeaponMinPitch() - r * getXRot() - r2 * getRoll();

            float f = Mth.wrapDegrees(entity.getXRot());
            float f1 = Mth.clamp(f, min, max);
            entity.xRotO += f1 - f;
            entity.setXRot(entity.getXRot() + f1 - f);

            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                float f2 = Mth.wrapDegrees(entity.getYRot() - this.getGunYRot(1));
                float f3 = Mth.clamp(f2, -150.0F, 150.0F);
                entity.yRotO += f3 - f2;
                entity.setYRot(entity.getYRot() + f3 - f2);
                entity.setYBodyRot(entity.getYRot());
            }
        } else if (entity == getNthEntity(2)) {
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

            float min = -90f - r * getXRot() - r2 * getRoll();
            float max = 22.5f - r * getXRot() - r2 * getRoll();

            float f = Mth.wrapDegrees(entity.getXRot());
            float f1 = Mth.clamp(f, min, max);
            entity.xRotO += f1 - f;
            entity.setXRot(entity.getXRot() + f1 - f);
        }
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    @Override
    public int passengerSeatLocation(Entity entity) {
        return entity == getNthEntity(0) ? 1 : 0;
    }

    private PlayState cannonShootPredicate(AnimationState<Yx100Entity> event) {
        if (this.entityData.get(CANNON_RECOIL_TIME) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.yx100.fire"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.yx100.idle"));
    }

    private PlayState coaxShootPredicate(AnimationState<Yx100Entity> event) {
        if (this.entityData.get(FIRE_ANIM) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.yx100.fire_coax"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.yx100.idle_coax"));
    }

    private PlayState gunShootPredicate(AnimationState<Yx100Entity> event) {
        if (this.entityData.get(GUN_FIRE_TIME) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.yx100.fire2"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.yx100.idle2"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "cannon", 0, this::cannonShootPredicate));
        data.add(new AnimationController<>(this, "coax", 0, this::coaxShootPredicate));
        data.add(new AnimationController<>(this, "gun", 0, this::gunShootPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public int mainGunRpm(LivingEntity living) {
        if (living == getNthEntity(0)) {
            if (getWeapon(0).mainGun) {
                return 15;
            } else if (getWeaponIndex(0) == 4) {
                return 500;
            }
        }

        if (living == getNthEntity(1)) {
            return 500;
        }

        if (living == getNthEntity(2)) {
            return 600;
        }

        return 15;
    }

    @Override
    public boolean canShoot(LivingEntity living) {
        if (living == getNthEntity(0)) {
            if (getWeapon(0).mainGun) {
                return !this.entityData.get(LOADED_SHELL).equals("null") && getEnergy() > VehicleConfig.YX_100_SHOOT_COST.get();
            } else if (getWeaponIndex(0) == 4) {
                return (this.entityData.get(MG_AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(living)) && !cannotFireCoax;
            }
        }

        if (living == getNthEntity(1)) {
            return (this.entityData.get(MG_AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(living)) && !cannotFire;
        }

        if (living == getNthEntity(2)) {
            return this.entityData.get(LOADED_DRONE) > 0;
        }
        return false;
    }

    @Override
    public int getAmmoCount(LivingEntity living) {
        if (living == getNthEntity(0)) {
            if (getWeapon(0).mainGun) {
                return this.entityData.get(LOADED_SHELL).equals("null") ? 0 : 1;
            } else if (getWeaponIndex(0) == 4) {
                return this.entityData.get(MG_AMMO);
            }
        }

        if (living == getNthEntity(1)) {
            return this.entityData.get(MG_AMMO);
        }

        if (living == getNthEntity(2)) {
            return this.entityData.get(LOADED_DRONE);
        }
        return 0;
    }

    @Override
    public boolean banHand(LivingEntity entity) {
        if (entity == getNthEntity(0) || entity == getNthEntity(1)) {
            return true;
        }
        return entity == getNthEntity(2) && !entity.isShiftKeyDown();
    }

    @Override
    public boolean hidePassenger(int index) {
        return index == 0 || index == 1;
    }

    @Override
    public int zoomFov() {
        return 3;
    }

    @Override
    public boolean hasTracks() {
        return true;
    }

    @Override
    public int getWeaponHeat(LivingEntity living) {
        if (living == getNthEntity(0)) {
            return entityData.get(COAX_HEAT);
        }

        if (living == getNthEntity(1)) {
            return entityData.get(HEAT);
        }

        return 0;
    }

    @Override
    public void changeWeapon(int index, int value, boolean isScroll) {
        if (index != 0) return;

        var weapons = getAvailableWeapons(index);
        if (weapons.isEmpty()) return;
        var count = weapons.size();

        var typeIndex = isScroll ? (value + getWeaponIndex(index) + count) % count : value;

        if (typeIndex == 0 || typeIndex == 1 || typeIndex == 2 || typeIndex == 3) {
            boolean hasCreativeAmmo = false;
            for (int i = 0; i < getMaxPassengers(); i++) {
                if (getNthEntity(i) instanceof Player pPlayer && InventoryTool.hasCreativeAmmoBox(pPlayer)) {
                    hasCreativeAmmo = true;
                }
            }


            if (typeIndex != entityData.get(SELECTED_AMMO_TYPE)) {
                this.reloadCoolDown = 80;
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(entityData.get(LOADED_SHELL)));
                if (item == null) {
                    return;
                }
                if (!this.entityData.get(LOADED_SHELL).equals("null") && !hasCreativeAmmo) {
                    this.insertItem(new ItemStack(item).getItem(), 1);
                }
                entityData.set(LOADED_SHELL, "null");
            }

            if (this.getFirstPassenger() instanceof ServerPlayer player) {
                var clientboundstopsoundpacket = new ClientboundStopSoundPacket(ModSounds.YX_100_RELOAD.get().getLocation(), SoundSource.PLAYERS);
                player.connection.send(clientboundstopsoundpacket);
            }
        }

        WeaponVehicleEntity.super.changeWeapon(index, value, isScroll);
    }

    public Vec3 getGunVec(float ticks) {
        return getGunnerVector(ticks);
    }

    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/yx_100_icon.png");
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderFirstPersonOverlay(GuiGraphics guiGraphics, PoseStack poseStack, Font font, Player player, int screenWidth, int screenHeight, float scale, int color) {
        float minWH = (float) Math.min(screenWidth, screenHeight);
        float scaledMinWH = Mth.floor(minWH * scale);
        float centerW = ((screenWidth - scaledMinWH) / 2);
        float centerH = ((screenHeight - scaledMinWH) / 2);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        // 准心
        if (this.getWeapon(0).mainGun) {
            RenderHelper.blit(poseStack, Mod.loc("textures/screens/land/tank_cannon_cross.png"), centerW, centerH, 0, 0.0F, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
        } else  {
            RenderHelper.blit(poseStack, Mod.loc("textures/screens/land/lav_gun_cross.png"), centerW, centerH, 0, 0.0F, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
        }

        // 武器名称
        if (this.getWeaponIndex(0) == 0) {
            guiGraphics.drawString(font, Component.literal("AP SHELL  " + this.getAmmoCount(player) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getEntityData().get(AMMO))), screenWidth / 2 - 33, screenHeight - 65, color, false);
        } else if (this.getWeaponIndex(0) == 1) {
            guiGraphics.drawString(font, Component.literal("HE SHELL  " + this.getAmmoCount(player) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getEntityData().get(AMMO))), screenWidth / 2 - 33, screenHeight - 65, color, false);
        } else if (this.getWeaponIndex(0) == 2) {
            guiGraphics.drawString(font, Component.literal("CM SHELL  " + this.getAmmoCount(player) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getEntityData().get(AMMO))), screenWidth / 2 - 33, screenHeight - 65, color, false);
        } else if (this.getWeaponIndex(0) == 3) {
            guiGraphics.drawString(font, Component.literal("GRAPESHOT " + this.getAmmoCount(player) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getEntityData().get(AMMO))), screenWidth / 2 - 33, screenHeight - 65, color, false);
        } else if (this.getWeaponIndex(0) == 4) {
            int heat = this.getEntityData().get(COAX_HEAT);
            guiGraphics.drawString(font, Component.literal(" 12.7MM HMG " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getAmmoCount(player))), screenWidth / 2 - 33, screenHeight - 65, MathTool.getGradientColor(color, 0xFF0000, heat, 2), false);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderThirdPersonOverlay(GuiGraphics guiGraphics, Font font, Player player, int screenWidth, int screenHeight, float scale) {
        if (this.getWeaponIndex(0) == 0) {
            guiGraphics.drawString(font, Component.literal("AP SHELL " + this.getAmmoCount(player) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getEntityData().get(AMMO))), 30, -9, -1, false);
        } else if (this.getWeaponIndex(0) == 1) {
            guiGraphics.drawString(font, Component.literal("HE SHELL " + this.getAmmoCount(player) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getEntityData().get(AMMO))), 30, -9, -1, false);
        } else if (this.getWeaponIndex(0) == 2) {
            guiGraphics.drawString(font, Component.literal("CM SHELL " + this.getAmmoCount(player) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getEntityData().get(AMMO))), 30, -9, -1, false);
        } else if (this.getWeaponIndex(0) == 3) {
            guiGraphics.drawString(font, Component.literal("GRAPESHOT" + this.getAmmoCount(player) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getEntityData().get(AMMO))), 30, -9, -1, false);
        } else if (this.getWeaponIndex(0) == 4) {
            double heat2 = this.getEntityData().get(COAX_HEAT) / 100.0F;
            guiGraphics.drawString(font, Component.literal("12.7MM HMG " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getAmmoCount(player))), 30, -9, Mth.hsvToRgb(0F, (float) heat2, 1.0F), false);
        }
    }

    @Override
    public boolean hasDecoy() {
        return true;
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        if (seatIndex == 0) {
            return zoom ? 0.17 : Minecraft.getInstance().options.getCameraType().isFirstPerson() ? 0.22 : 0.35;
        } else if (seatIndex == 1) {
            return zoom ? 0.25 : Minecraft.getInstance().options.getCameraType().isFirstPerson() ? 0.35 : 0.4;
        } else return original;
    }

    @Override
    public boolean isEnclosed(int index) {
        return index != 2;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            if (this.getSeatIndex(player) == 0) {
                return new Vec2((float) -getYRotFromVector(this.getBarrelVec(partialTicks)), (float) -getXRotFromVector(this.getBarrelVec(partialTicks)));
            } else if (this.getSeatIndex(player) == 1) {
                return new Vec2((float) -getYRotFromVector(this.getGunnerVector(partialTicks)), (float) -getXRotFromVector(this.getGunnerVector(partialTicks)));
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
                    return new Vec3(this.driverZoomPos(partialTicks).x, this.driverZoomPos(partialTicks).y, this.driverZoomPos(partialTicks).z);
                } else {
                    return new Vec3(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
                }
            } else if (this.getSeatIndex(player) == 1) {
                return new Vec3(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
            }
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }

    @Override
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/land.png");
    }

    @Override
    public float getTurretMaxHealth() {
        return 100;
    }

    @Override
    public float getWheelMaxHealth() {
        return 100;
    }

    @Override
    public float getEngineMaxHealth() {
        return 150;
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obbTurret, this.obbTurret2);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 1.40625f, -0.375f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 0, 1.28125f, 4.28125f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition3 = transformPosition(transform, 1.8125f, 0.84375f, 0.0625f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition4 = transformPosition(transform, -1.8125f, 0.84375f, 0.0625f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition5 = transformPosition(transform, 0, 1.65625f, -3.9375f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotations(1, this));

        Matrix4f transformT = getTurretTransform(1);

        Vector4f worldPositionT = transformPosition(transformT, 0, 0.5625f, -1.125f);
        this.obbTurret.center().set(new Vector3f(worldPositionT.x, worldPositionT.y, worldPositionT.z));
        this.obbTurret.setRotation(VectorTool.combineRotationsTurret(1, this));

        Vector4f worldPositionT2 = transformPosition(transformT, 0, 0.40625f, 1.65625f);
        this.obbTurret2.center().set(new Vector3f(worldPositionT2.x, worldPositionT2.y, worldPositionT2.z));
        this.obbTurret2.setRotation(VectorTool.combineRotationsTurret(1, this));
    }

    @Override
    public int getHudColor() {
        return 0x00FFF6;
    }

    @Override
    public boolean hasPassengerTurretWeapon() {
        return true;
    }
}
