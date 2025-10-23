package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.gun.Ammo;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.ProjectileWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.SmallCannonShellWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.WgMissileWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class Bmp2Entity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity, OBBEntity {

    public static final EntityDataAccessor<Integer> CANNON_FIRE_TIME = SynchedEntityData.defineId(Bmp2Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> LOADED_MISSILE = SynchedEntityData.defineId(Bmp2Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> MISSILE_COUNT = SynchedEntityData.defineId(Bmp2Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> MG_AMMO = SynchedEntityData.defineId(Bmp2Entity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public int reloadCoolDown;

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obbTurret;

    public Bmp2Entity(EntityType<Bmp2Entity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(2.1875f, 0.875f, 3.90625f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(2.1875f, 0.5f, 0.65625f), new Quaternionf(), OBB.Part.BODY);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(0.375f, 0.78125f, 3.46875f), new Quaternionf(), OBB.Part.WHEEL_LEFT);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.375f, 0.78125f, 3.46875f), new Quaternionf(), OBB.Part.WHEEL_RIGHT);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(1.15625f, 0.375f, 1.125f), new Quaternionf(), OBB.Part.ENGINE1);
        this.obbTurret = new OBB(this.position().toVector3f(), new Vector3f(1.5f, 0.34375f, 1.5f), new Quaternionf(), OBB.Part.TURRET);
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new SmallCannonShellWeapon()
                                .damage(VehicleConfig.BMP_2_CANNON_DAMAGE.get())
                                .explosionDamage(VehicleConfig.BMP_2_CANNON_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.BMP_2_CANNON_EXPLOSION_RADIUS.get().floatValue())
                                .sound(ModSounds.INTO_MISSILE.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/cannon_30mm.png"))
                                .sound1p(ModSounds.BMP_CANNON_FIRE_1P.get())
                                .sound3p(ModSounds.BMP_CANNON_FIRE_3P.get())
                                .sound3pFar(ModSounds.LAV_CANNON_FAR.get())
                                .sound3pVeryFar(ModSounds.LAV_CANNON_VERYFAR.get()),
                        new ProjectileWeapon()
                                .damage(9.5f)
                                .headShot(2)
                                .zoom(false)
                                .sound(ModSounds.INTO_CANNON.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_7_62mm.png"))
                                .sound1p(ModSounds.COAX_FIRE_1P.get())
                                .sound3p(ModSounds.M_60_FIRE_3P.get())
                                .sound3pFar(ModSounds.M_60_FAR.get())
                                .sound3pVeryFar(ModSounds.M_60_VERYFAR.get()),
                        new WgMissileWeapon()
                                .damage(ExplosionConfig.WIRE_GUIDE_MISSILE_DAMAGE.get())
                                .explosionDamage(ExplosionConfig.WIRE_GUIDE_MISSILE_EXPLOSION_DAMAGE.get())
                                .explosionRadius(ExplosionConfig.WIRE_GUIDE_MISSILE_EXPLOSION_RADIUS.get())
                                .sound(ModSounds.INTO_MISSILE.get())
                                .sound1p(ModSounds.BMP_MISSILE_FIRE_1P.get())
                                .sound3p(ModSounds.BMP_MISSILE_FIRE_3P.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/missile_9m113.png")),
                },
                new VehicleWeapon[]{
                        // 成员机枪
                        new ProjectileWeapon()
                                .damage(9.5f)
                                .headShot(1.5f)
                                .zoom(false)
                                .sound(ModSounds.INTO_CANNON.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_7_62mm.png"))
                                .sound1p(ModSounds.COAX_FIRE_1P.get())
                                .sound3p(ModSounds.AK_47_FIRE_3P.get())
                                .sound3pFar(ModSounds.AK_47_FAR.get())
                                .sound3pVeryFar(ModSounds.AK_47_VERYFAR.get()),
                },
                new VehicleWeapon[]{
                        // 成员机枪
                        new ProjectileWeapon()
                                .damage(9.5f)
                                .headShot(1.5f)
                                .zoom(false)
                                .sound(ModSounds.INTO_CANNON.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_7_62mm.png"))
                                .sound1p(ModSounds.COAX_FIRE_1P.get())
                                .sound3p(ModSounds.AK_47_FIRE_3P.get())
                                .sound3pFar(ModSounds.AK_47_FAR.get())
                                .sound3pVeryFar(ModSounds.AK_47_VERYFAR.get()),
                },
                new VehicleWeapon[]{
                        // 成员机枪
                        new ProjectileWeapon()
                                .damage(9.5f)
                                .headShot(1.5f)
                                .zoom(false)
                                .sound(ModSounds.INTO_CANNON.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_7_62mm.png"))
                                .sound1p(ModSounds.COAX_FIRE_1P.get())
                                .sound3p(ModSounds.AK_47_FIRE_3P.get())
                                .sound3pFar(ModSounds.AK_47_FAR.get())
                                .sound3pVeryFar(ModSounds.AK_47_VERYFAR.get()),
                },
                new VehicleWeapon[]{
                        // 成员机枪
                        new ProjectileWeapon()
                                .damage(9.5f)
                                .headShot(1.5f)
                                .zoom(false)
                                .sound(ModSounds.INTO_CANNON.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_7_62mm.png"))
                                .sound1p(ModSounds.COAX_FIRE_1P.get())
                                .sound3p(ModSounds.AK_47_FIRE_3P.get())
                                .sound3pFar(ModSounds.AK_47_FAR.get())
                                .sound3pVeryFar(ModSounds.AK_47_VERYFAR.get()),
                },
                new VehicleWeapon[]{
                        // 成员机枪
                        new ProjectileWeapon()
                                .damage(9.5f)
                                .headShot(1.5f)
                                .zoom(false)
                                .sound(ModSounds.INTO_CANNON.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_7_62mm.png"))
                                .sound1p(ModSounds.COAX_FIRE_1P.get())
                                .sound3p(ModSounds.AK_47_FIRE_3P.get())
                                .sound3pFar(ModSounds.AK_47_FAR.get())
                                .sound3pVeryFar(ModSounds.AK_47_VERYFAR.get()),
                },
                new VehicleWeapon[]{
                        // 成员机枪
                        new ProjectileWeapon()
                                .damage(9.5f)
                                .headShot(1.5f)
                                .zoom(false)
                                .sound(ModSounds.INTO_CANNON.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_7_62mm.png"))
                                .sound1p(ModSounds.COAX_FIRE_1P.get())
                                .sound3p(ModSounds.AK_47_FIRE_3P.get())
                                .sound3pFar(ModSounds.AK_47_FAR.get())
                                .sound3pVeryFar(ModSounds.AK_47_VERYFAR.get()),
                },
                new VehicleWeapon[]{
                        // 成员机枪
                        new ProjectileWeapon()
                                .damage(9.5f)
                                .headShot(1.5f)
                                .zoom(false)
                                .sound(ModSounds.INTO_CANNON.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_7_62mm.png"))
                                .sound1p(ModSounds.COAX_FIRE_1P.get())
                                .sound3p(ModSounds.AK_47_FIRE_3P.get())
                                .sound3pFar(ModSounds.AK_47_FAR.get())
                                .sound3pVeryFar(ModSounds.AK_47_VERYFAR.get()),
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(3 + ClientMouseHandler.custom3pDistanceLerp, 1, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CANNON_FIRE_TIME, 0)
                .define(LOADED_MISSILE, 0)
                .define(MISSILE_COUNT, 0)
                .define(MG_AMMO, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("LoadedMissile", this.entityData.get(LOADED_MISSILE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(LOADED_MISSILE, compound.getInt("LoadedMissile"));
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.4f) * damage);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 0.05), random.nextFloat() * 0.15f + 1.05f);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        updateOBB();
        if (getLeftTrack() < 0) {
            setLeftTrack(100);
        }

        if (getLeftTrack() > 100) {
            setLeftTrack(0);
        }

        if (getRightTrack() < 0) {
            setRightTrack(100);
        }

        if (getRightTrack() > 100) {
            setRightTrack(0);
        }

        if (this.level() instanceof ServerLevel) {
            if (reloadCoolDown > 0) {
                reloadCoolDown--;
            }
            this.handleAmmo();
        }

        this.terrainCompact(4f, 5f);
        inertiaRotate(1);
        releaseSmokeDecoy(getTurretVector(1));
        lowHealthWarning();

        for (int i = 1; i < 7; i++) {
            if (getNthEntity(i) instanceof Mob mob && canShoot(mob) && mob.getTarget() != null) {
                int rpm = 20 / (mainGunRpm(mob) / 60);
                if (tickCount % rpm == 0) {
                    vehicleShoot(mob, i);
                }
            }
        }

        this.refreshDimensions();
    }

    // 炮塔最大水平旋转速度
    @Override
    public float turretYSpeed() {
        return 10;
    }

    // 炮塔最大俯仰旋转速度
    @Override
    public float turretXSpeed() {
        return 12.5F;
    }

    // 炮塔最小俯角
    @Override
    public float turretMinPitch() {
        return -7.5f;
    }

    // 炮塔最大仰角
    @Override
    public float turretMaxPitch() {
        return 74;
    }

    // 炮弹发射位置
    @Override
    public Vec3 getShootPos(Entity entity, float ticks) {
        Matrix4f transform = getBarrelTransform(1);
        Vector4f worldPosition;
        if (getWeaponIndex(0) == 0) {
            worldPosition = transformPosition(transform, -0.45f, 0.4f, 0);
        } else if (getWeaponIndex(0) == 1) {
            worldPosition = transformPosition(transform, -0.2f, 0.3f, 0);
        } else {
            worldPosition = transformPosition(transform, 0, 1, 0);
        }
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    // 炮弹发射速度
    @Override
    public float projectileVelocity(Entity entity) {
        if (getWeaponIndex(0) == 0) {
            return 20;
        } else if (getWeaponIndex(0) == 1) {
            return 25;
        } else {
            return 2;
        }
    }

    // 炮弹重力
    @Override
    public float projectileGravity(Entity entity) {
        if (getWeaponIndex(0) == 0) {
            return 0.03f;
        } else if (getWeaponIndex(0) == 1) {
            return 0.05f;
        } else {
            return 0;
        }
    }

    @Override
    public boolean canCollideHardBlock() {
        return getDeltaMovement().horizontalDistance() > 0.07 || Mth.abs(this.entityData.get(POWER)) > 0.12;
    }


    private void handleAmmo() {

        boolean hasCreativeAmmo = false;
        for (int i = 0; i < getMaxPassengers(); i++) {
            if (InventoryTool.hasCreativeAmmoBox(getNthEntity(i))) {
                hasCreativeAmmo = true;
            }
        }

        int mgAmmoCount = this.getItemStacks().stream().filter(stack -> {
            if (stack.is(ModItems.AMMO_BOX.get())) {
                return Ammo.RIFLE.get(stack) > 0;
            }
            return false;
        }).mapToInt(Ammo.RIFLE::get).sum() + countItem(ModItems.RIFLE_AMMO.get());

        if ((hasItem(ModItems.WIRE_GUIDE_MISSILE.get()) || hasCreativeAmmo)
                && this.reloadCoolDown <= 0 && this.getEntityData().get(LOADED_MISSILE) < 1) {
            this.entityData.set(LOADED_MISSILE, this.getEntityData().get(LOADED_MISSILE) + 1);
            this.reloadCoolDown = 160;
            if (!hasCreativeAmmo) {
                this.getItemStacks().stream().filter(stack -> stack.is(ModItems.WIRE_GUIDE_MISSILE.get())).findFirst().ifPresent(stack -> stack.shrink(1));
            }
            this.level().playSound(null, this, ModSounds.BMP_MISSILE_RELOAD.get(), this.getSoundSource(), 1, 1);
        }

        if (getWeaponIndex(0) == 0) {
            this.entityData.set(AMMO, countItem(ModItems.SMALL_SHELL.get()));
        } else if (getWeaponIndex(0) == 2) {
            this.entityData.set(AMMO, this.getEntityData().get(LOADED_MISSILE));
        }

        this.entityData.set(MG_AMMO, mgAmmoCount);
        this.entityData.set(MISSILE_COUNT, countItem(ModItems.WIRE_GUIDE_MISSILE.get()));
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
            if (getWeaponIndex(0) == 0) {
                if (this.cannotFire) return;
                var smallCannonShell = ((SmallCannonShellWeapon) getWeapon(0)).create(living);

                smallCannonShell.setPos(getShootPos(living, 1).x, getShootPos(living, 1).y, getShootPos(living, 1).z);
                smallCannonShell.shoot(getBarrelVector(1).x, getBarrelVector(1).y, getBarrelVector(1).z, projectileVelocity(living),
                        0.25f);
                this.level().addFreshEntity(smallCannonShell);

                sendParticle((ServerLevel) this.level(), ParticleTypes.LARGE_SMOKE, getShootPos(living, 1).x, getShootPos(living, 1).y, getShootPos(living, 1).z, 1, 0.02, 0.02, 0.02, 0, false);
                playShootSound3p(living, 0, 4, 12, 24, getShootPos(living, 1));
                ShakeClientMessage.sendToNearbyPlayers(this, 5, 6, 5, 9);

                this.entityData.set(CANNON_RECOIL_TIME, 40);
                this.entityData.set(YAW, getTurretYRot());

                this.entityData.set(HEAT, this.entityData.get(HEAT) + 7);
                this.entityData.set(FIRE_ANIM, 3);

                if (hasCreativeAmmo) return;

                this.getItemStacks().stream().filter(stack -> stack.is(ModItems.SMALL_SHELL.get())).findFirst().ifPresent(stack -> stack.shrink(1));
            } else if (getWeaponIndex(0) == 1) {
                if (this.cannotFireCoax) return;

                if (this.entityData.get(MG_AMMO) > 0 || hasCreativeAmmo) {
                    var projectileRight = ((ProjectileWeapon) getWeapon(0)).create(living).setGunItemId(this.getType().getDescriptionId());

                    projectileRight.bypassArmorRate(0.2f);
                    projectileRight.setPos(getShootPos(living, 1).x, getShootPos(living, 1).y, getShootPos(living, 1).z);
                    projectileRight.shoot(living, getBarrelVector(1).x, getBarrelVector(1).y, getBarrelVector(1).z, projectileVelocity(living),
                            0.25f);
                    this.level().addFreshEntity(projectileRight);

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
                playShootSound3p(living, 0, 3, 6, 12, getShootPos(living, 1));

            } else if (getWeaponIndex(0) == 2 && this.getEntityData().get(LOADED_MISSILE) > 0) {
                var wgMissileEntity = ((WgMissileWeapon) getWeapon(0)).create(living);

                wgMissileEntity.setPos(getShootPos(living, 1).x, getShootPos(living, 1).y, getShootPos(living, 1).z);
                wgMissileEntity.shoot(getBarrelVector(1).x, getBarrelVector(1).y, getBarrelVector(1).z, projectileVelocity(living), 0f);
                wgMissileEntity.setLauncherVehicle(this.uuid);
                living.level().addFreshEntity(wgMissileEntity);
                playShootSound3p(living, 0, 6, 0, 0, getShootPos(living, 1));

                this.entityData.set(LOADED_MISSILE, this.getEntityData().get(LOADED_MISSILE) - 1);
                reloadCoolDown = 160;
            }
        }

        for (int i = 1; i < 7; i++) {
            if (type == i) {
                Vec3 shootPosition = passengerCameraPos(1, living);

                if (this.entityData.get(MG_AMMO) > 0 || hasCreativeAmmo) {
                    var projectile = ((ProjectileWeapon) getWeapon(i)).create(living).setGunItemId(this.getType().getDescriptionId());

                    Vec3 shootVec = living.getLookAngle();
                    float spread = 0.25f;

                    if (living instanceof Mob mob && mob.getTarget() != null) {
                        Entity target = mob.getTarget();
                        if (target.getVehicle() != null) {
                            target = target.getVehicle();
                        }

                        Vec3 targetVel = target.getDeltaMovement();

                        if (target instanceof LivingEntity pLiving) {
                            double gravity = pLiving.getAttributeValue(Attributes.GRAVITY);
                            targetVel = targetVel.add(0, gravity, 0);
                        }

                        if (target instanceof Player) {
                            targetVel = targetVel.multiply(2, 1, 2);
                        }

                        shootVec = RangeTool.calculateFiringSolution(shootPosition, target.getBoundingBox().getCenter(), targetVel, 18, 0.05);
                        spread = 1.2f;

                        double angle = VectorTool.calculateAngle(shootVec, mob.getLookAngle());
                        if (angle > 2) return;
                    }

                    projectile.bypassArmorRate(0.2f);
                    projectile.setPos(shootPosition.x, shootPosition.y - 0.05, shootPosition.z);
                    projectile.shoot(living, shootVec.x, shootVec.y, shootVec.z, 18, spread);
                    this.level().addFreshEntity(projectile);

                    playShootSound3p(living, i, 3, 6, 12, shootPosition);

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
            }
        }
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.BMP_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return Math.max(Mth.abs(entityData.get(POWER)), Mth.abs(1.4f * this.entityData.get(DELTA_ROT))) * 0.4f;
    }

    @Override
    public Vec3 zoomPos(Entity entity, float ticks) {
        Matrix4f transform = getTurretTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0, 0, 0.75f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 getBarrelPosition() {
        return new Vec3(0.3625f, 0.293125, 1.18095);
    }

    @Override
    public Vec3 getTurretPosition() {
        return new Vec3(0, 2.25, -0.703125);
    }

    @Override
    public float rotateYOffset() {
        return 2.7f;
    }


    public Vec3 passengerCameraPos(float ticks, Entity entity) {
        int i = this.getSeatIndex(entity);

        Matrix4f transformV = getVehicleTransform(ticks);

        Vector4f worldPosition;
        if (i == 1) {
            worldPosition = transformPosition(transformV, 1.803125f, 1.9765625f, -0.8125f);
        } else if (i == 2) {
            worldPosition = transformPosition(transformV, -1.803125f, 1.9765625f, -0.8125f);
        } else if (i == 3) {
            worldPosition = transformPosition(transformV, 1.615625f, 1.9765625f, -2.1875f);
        } else if (i == 4) {
            worldPosition = transformPosition(transformV, -1.615625f, 1.9765625f, -2.1875f);
        } else if (i == 5) {
            worldPosition = transformPosition(transformV, 0.6875f, 1.6015625f, -4.1f);
        } else if (i == 6) {
            worldPosition = transformPosition(transformV, -1.50625f, 1.9765625f, -3.0625f);
        } else {
            worldPosition = transformPosition(transformV, 0, 1, 0);
        }
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    private PlayState firePredicate(AnimationState<Bmp2Entity> event) {
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
    public int mainGunRpm(LivingEntity living) {
        if (living == getNthEntity(0)) {
            if (getWeaponIndex(0) == 0) {
                return 250;
            } else if (getWeaponIndex(0) == 1) {
                return 600;
            }
        }

        return 600;
    }

    @Override
    public boolean canShoot(LivingEntity living) {
        if (living == getNthEntity(0)) {
            if (getWeaponIndex(0) == 0) {
                return (this.entityData.get(AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(living)) && !cannotFire;
            } else if (getWeaponIndex(0) == 1) {
                return (this.entityData.get(MG_AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(living)) && !cannotFireCoax;
            } else if (getWeaponIndex(0) == 2) {
                return (this.entityData.get(LOADED_MISSILE) > 0);
            }
        } else {
            return this.entityData.get(MG_AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(living);
        }

        return true;
    }

    @Override
    public int getAmmoCount(LivingEntity living) {
        if (living == getNthEntity(0)) {
            if (getWeaponIndex(0) == 1) {
                return this.entityData.get(MG_AMMO);
            } else {
                return this.entityData.get(AMMO);
            }
        } else {
            return this.entityData.get(MG_AMMO);
        }
    }

    @Override
    public boolean banHand(LivingEntity entity) {
        return true;
    }

    @Override
    public int zoomFov() {
        return 3;
    }

    @Override
    public int getWeaponHeat(LivingEntity living) {
        if (getWeaponIndex(0) == 0) {
            return entityData.get(HEAT);
        } else if (getWeaponIndex(0) == 1) {
            return entityData.get(COAX_HEAT);
        }
        return 0;
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
        if (this.getWeaponIndex(0) == 0) {
            RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/land/ru_apc.png"), centerW, centerH, 0, 0.0F, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
            int heat = this.getEntityData().get(HEAT);
            guiGraphics.drawString(font, Component.literal(" 30MM 2A42 " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getAmmoCount(player))), screenWidth / 2 - 33, screenHeight - 65, MathTool.getGradientColor(color, 0xFF0000, heat, 2), false);
        } else if (this.getWeaponIndex(0) == 1) {
            RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/land/common_gun.png"), centerW, centerH, 0, 0.0F, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
            int heat = this.getEntityData().get(COAX_HEAT);
            guiGraphics.drawString(font, Component.literal(" 7.62MM ПКТ " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getAmmoCount(player))), screenWidth / 2 - 33, screenHeight - 65, MathTool.getGradientColor(color, 0xFF0000, heat, 2), false);
        } else {
            RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/land/common_missile.png"), centerW, centerH, 0, 0.0F, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
            guiGraphics.drawString(font, Component.literal("    9M113  " + this.getEntityData().get(LOADED_MISSILE) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getEntityData().get(MISSILE_COUNT))), screenWidth / 2 - 33, screenHeight - 65, color, false);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderThirdPersonOverlay(GuiGraphics guiGraphics, Font font, Player player, int screenWidth, int screenHeight, float scale) {
        if (this.getWeaponIndex(0) == 0) {
            double heat = this.getEntityData().get(HEAT) / 100.0F;
            guiGraphics.drawString(font, Component.literal("30MM 2A42 " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getAmmoCount(player))), 30, -9, Mth.hsvToRgb(0F, (float) heat, 1.0F), false);
        } else if (this.getWeaponIndex(0) == 1) {
            double heat2 = this.getEntityData().get(COAX_HEAT) / 100.0F;
            guiGraphics.drawString(font, Component.literal("7.62MM ПКТ " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getAmmoCount(player))), 30, -9, Mth.hsvToRgb(0F, (float) heat2, 1.0F), false);
        } else {
            guiGraphics.drawString(font, Component.literal("9M113 " + this.getEntityData().get(LOADED_MISSILE) + " " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : this.getEntityData().get(MISSILE_COUNT))), 30, -9, -1, false);
        }
    }

    @Override
    public boolean hasDecoy() {
        return true;
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return seatIndex == 0 ? (zoom ? 0.22 : Minecraft.getInstance().options.getCameraType().isFirstPerson() ? 0.27 : 0.36) : original;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean useFixedCameraPos(Entity entity) {
        return this.getSeatIndex(entity) != 0;
    }


    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            if (this.getSeatIndex(player) == 0) {
                return new Vec2((float) -getYRotFromVector(this.getBarrelVector(partialTicks)), (float) -getXRotFromVector(this.getBarrelVector(partialTicks)));
            } else {
                return new Vec2(Mth.lerp(partialTicks, player.yRotO, player.getYRot()), Mth.lerp(partialTicks, player.xRotO, player.getXRot()));
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
                    return new Vec3(this.zoomPos(player, partialTicks).x, this.zoomPos(player, partialTicks).y, this.zoomPos(player, partialTicks).z);
                } else {
                    return new Vec3(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
                }
            } else {
                return passengerCameraPos(partialTicks, player);
            }
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obbTurret);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 1.5f, -0.28125f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 0, 1.5625f, 4.28125f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition3 = transformPosition(transform, 1.9375f, 0.84375f, 0.15625f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition4 = transformPosition(transform, -1.9375f, 0.84375f, 0.15625f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition5 = transformPosition(transform, -1.09375f, 2.0625f, 1.75f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotations(1, this));

        Matrix4f transformT = getTurretTransform(1);

        Vector4f worldPositionT = transformPosition(transformT, 0, 0.46875f, 0f);
        this.obbTurret.center().set(new Vector3f(worldPositionT.x, worldPositionT.y, worldPositionT.z));
        this.obbTurret.setRotation(VectorTool.combineRotationsTurret(1, this));
    }

    @Override
    public boolean hasTurret() {
        return true;
    }
}
