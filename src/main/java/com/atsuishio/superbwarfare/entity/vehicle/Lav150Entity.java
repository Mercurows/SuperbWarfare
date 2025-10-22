package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.ProjectileWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.SmallCannonShellWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class Lav150Entity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity, OBBEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obb6;
    public OBB obb7;
    public OBB obb8;
    public OBB obbTurret;

    public Lav150Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.LAV_150.get(), world);
    }

    public Lav150Entity(EntityType<Lav150Entity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(0.3f, 0.75f, 0.75f), new Quaternionf(), OBB.Part.WHEEL_RIGHT);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(0.3f, 0.75f, 0.75f), new Quaternionf(), OBB.Part.WHEEL_LEFT);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(0.3f, 0.75f, 0.75f), new Quaternionf(), OBB.Part.WHEEL_LEFT);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.3f, 0.75f, 0.75f), new Quaternionf(), OBB.Part.WHEEL_RIGHT);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(1.3125f, 0.90625f, 2.4375f), new Quaternionf(), OBB.Part.BODY);
        this.obb6 = new OBB(this.position().toVector3f(), new Vector3f(1.3125f, 0.53125f, 0.34375f), new Quaternionf(), OBB.Part.BODY);
        this.obb7 = new OBB(this.position().toVector3f(), new Vector3f(1.3125f, 0.625f, 0.53125f), new Quaternionf(), OBB.Part.BODY);
        this.obb8 = new OBB(this.position().toVector3f(), new Vector3f(0.71875f, 0.46875f, 0.875f), new Quaternionf(), OBB.Part.ENGINE1);
        this.obbTurret = new OBB(this.position().toVector3f(), new Vector3f(0.875f, 0.3625f, 1.25f), new Quaternionf(), OBB.Part.TURRET);
    }

    @Override
    public void changeWeapon(int index, int value, boolean isScroll) {
        var gunData = getGunData(index);
        if (gunData == null) return;

        var ammoList = gunData.get(GunProp.AMMO_CONSUMER);
        var targetIndex = isScroll ? (value + gunData.selectedAmmoType.get()) % ammoList.size() : value;
        setWeaponIndex(index, targetIndex);

        // TODO 正确播放武器切换音效
        this.level().playSound(null, this, SoundEvents.ARROW_HIT_PLAYER, this.getSoundSource(), 1, 1);
    }

    @Override
    public void setWeaponIndex(int index, int type) {
        modifyGunData(index, gunData -> gunData.changeAmmoConsumer(type, getAmmoSupplier()));
    }

    @Override
    public int getAmmoCount(LivingEntity passenger, int weaponIndex) {
        var gunData = getGunData(getSeatIndex(passenger));
        if (gunData == null || gunData.selectedAmmoType.get() != weaponIndex) return 0;

        return gunData.backupAmmoCount.get();
    }

    // TODO 移除这个
    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new SmallCannonShellWeapon()
                                .sound(ModSounds.INTO_MISSILE.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/cannon_20mm.png"))
                                .sound1p(ModSounds.LAV_CANNON_FIRE_1P.get())
                                .sound3p(ModSounds.LAV_CANNON_FIRE_3P.get())
                                .sound3pFar(ModSounds.LAV_CANNON_FAR.get())
                                .sound3pVeryFar(ModSounds.LAV_CANNON_VERYFAR.get()),
                        new ProjectileWeapon()
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

    // TODO 正确实现武器信息
    @Override
    public List<VehicleWeapon> getAvailableWeapons(int index) {
        var weapons = getAllWeapons();
        if (index < 0 || index >= weapons.length) return List.of();

        return List.of(weapons[index]);
    }

    @Override
    public VehicleWeapon[][] getAllWeapons() {
        return getGunDataMap().values().stream().map(data -> {
            if (data == null) return List.of();

            var ammoTypes = data.get(GunProp.AMMO_CONSUMER);

            return ammoTypes.stream().map(a -> new ProjectileWeapon()
                    .zoom(false)
                    .sound(ModSounds.INTO_CANNON.get())
                    .icon(ResourceLocation.tryParse(a.icon))
                    .sound1p(ModSounds.COAX_FIRE_1P.get())
                    .sound3p(ModSounds.RPK_FIRE_3P.get())
                    .sound3pFar(ModSounds.RPK_FAR.get())
                    .sound3pVeryFar(ModSounds.RPK_VERYFAR.get())).toArray(VehicleWeapon[]::new);
        }).toArray(VehicleWeapon[][]::new);
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(2.75 + ClientMouseHandler.custom3pDistanceLerp, 1, 0);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 0.1), random.nextFloat() * 0.15f + 1.05f);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        updateOBB();

        if (this.level() instanceof ServerLevel) {
            updateBackupAmmoCount();
        }

        lowHealthWarning();
        this.terrainCompact(2.7f, 3.61f);
        inertiaRotate(1.25f);
        releaseSmokeDecoy(getTurretVector(1));

        this.refreshDimensions();
    }

    protected void updateBackupAmmoCount() {
        for (int i = 0; i < getMaxPassengers(); i++) {
            modifyGunData(i, data -> {
                if (data.useBackpackAmmo()) {
                    data.backupAmmoCount.set(data.countBackupAmmo(getAmmoSupplier()));
                } else {
                    data.backupAmmoCount.reset();
                }
            });
        }
    }

    protected Entity getAmmoSupplier() {
        return this;
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
        return -15f;
    }

    // 炮塔最大仰角
    @Override
    public float turretMaxPitch() {
        return 32.5f;
    }

    // 炮弹发射速度
    @Override
    public float projectileVelocity(Entity entity) {
        var gunData = getGunData(getSeatIndex(entity));
        if (gunData == null) return 25;

        return gunData.get(GunProp.VELOCITY).floatValue();
    }

    // 炮弹重力
    @Override
    public float projectileGravity(Entity entity) {
        var gunData = getGunData(getSeatIndex(entity));
        if (gunData == null) return 0;

        return gunData.get(GunProp.GRAVITY).floatValue();
    }

    @Override
    public boolean canCollideHardBlock() {
        return getDeltaMovement().horizontalDistance() > 0.09 || Mth.abs(this.entityData.get(POWER)) > 0.15;
    }

//    // TODO 正确计算位置
//    public Function<VehicleEntity, ShootRay> MACHINE_GUN_POS = createShootAnchorPoint("MachineGun", v -> {
//        var worldPosition = transformPosition(getBarrelTransform(1), 0.3f, 0.08f, 0);
//
//        return new ShootRay(
//                new Vec3(worldPosition.x, worldPosition.y, worldPosition.z),
//                getBarrelVector(1)
//        );
//    });
//
//    public Function<VehicleEntity, ShootRay> CANNON_POS = createShootAnchorPoint("Cannon", v -> {
//        var worldPosition = transformPosition(getBarrelTransform(1), 0.0609375f, 0.0517f, 0);
//
//        return new ShootRay(
//                new Vec3(worldPosition.x, worldPosition.y, worldPosition.z),
//                getBarrelVector(1)
//        );
//    });

    @Override
    public int getWeaponIndex(int index) {
        var gunData = getGunData(index);
        if (gunData == null) return 0;

        var consumersSize = gunData.get(GunProp.AMMO_CONSUMER).size();
        return Mth.clamp(gunData.selectedAmmoType.get(), 0, consumersSize - 1);
    }

    @Override
    public void vehicleShoot(LivingEntity living, int type) {

        // TODO 移除WeaponIndex
//        if (getWeaponIndex(0) == 0) {
        var seatIndex = getSeatIndex(living);

        modifyGunData(seatIndex, data -> {
            if (!data.canShoot(getAmmoSupplier())) return;
            data.shoot(new ShootParameters(getAmmoSupplier(), living, (ServerLevel) this.level(), getShootPos(living, 1), getShootVec(living, 1), data, data.get(GunProp.SPREAD), true, null, null));
        });

        sendParticle((ServerLevel) this.level(), ParticleTypes.LARGE_SMOKE, getShootPos(living, 1).x, getShootPos(living, 1).y, getShootPos(living, 1).z, 1, 0.02, 0.02, 0.02, 0, false);
        playShootSound3p(living, 0, 4, 12, 24, getShootPos(living, 1));

//        ShakeClientMessage.sendToNearbyPlayers(this, 5, 6, 5, 9);

        this.entityData.set(CANNON_RECOIL_TIME, 40);
        this.entityData.set(YAW, getTurretYRot());

        this.entityData.set(FIRE_ANIM, 3);

        var data = getGunData(getSeatIndex(living));
        if (data != null) {
            var list = data.get(GunProp.SHOOT_POS).positions.list;
            this.currentFirePosIndex = ++this.currentFirePosIndex % list.size();
        }

//        } else if (getWeaponIndex(0) == 1) {
//            if (this.cannotFireCoax) return;
//            if (this.entityData.get(AMMO) > 0 || hasCreativeAmmo) {
//                var projectile = ((ProjectileWeapon) getWeapon(0)).create(living).setGunItemId(this.getType().getDescriptionId());
//
//                projectile.bypassArmorRate(0.2f);
//                projectile.setPos(getShootPos(living, 1).x, getShootPos(living, 1).y, getShootPos(living, 1).z);
//                projectile.shoot(living, getBarrelVector(1).x, getBarrelVector(1).y, getBarrelVector(1).z, 36,
//                        0.25f);
//                this.level().addFreshEntity(projectile);
//
//                if (!hasCreativeAmmo) {
//                    ItemStack ammoBox = this.getItemStacks().stream().filter(stack -> {
//                        if (stack.is(ModItems.AMMO_BOX.get())) {
//                            return Ammo.RIFLE.get(stack) > 0;
//                        }
//                        return false;
//                    }).findFirst().orElse(ItemStack.EMPTY);
//
//                    if (!ammoBox.isEmpty()) {
//                        Ammo.RIFLE.add(ammoBox, -1);
//                    } else {
//                        this.getItemStacks().stream().filter(stack -> stack.is(ModItems.RIFLE_AMMO.get())).findFirst().ifPresent(stack -> stack.shrink(1));
//                    }
//                }
//            }
//
//            this.entityData.set(COAX_HEAT, this.entityData.get(COAX_HEAT) + 3);
//            this.entityData.set(FIRE_ANIM, 2);
//            playShootSound3p(living, 0, 3, 6, 12, new Vec3(getShootPos(living, 1).x, getShootPos(living, 1).y, getShootPos(living, 1).z));
//        }
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.LAV_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return Mth.abs(entityData.get(POWER)) * 0.4f;
    }

    @Override
    public Vec3 getBarrelPosition() {
        return new Vec3(0.0234375, 0.33795, 0.825);
    }

    @Override
    public Vec3 getTurretPosition() {
        return new Vec3(0, 2.4003, 0);
    }

    @Override
    public float rotateYOffset() {
        return 2.2f;
    }


    // TODO 正确播放动画
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
    public int mainGunRpm(LivingEntity living) {
        var data = getGunData(getSeatIndex(living));
        if (data == null) return 0;

        return data.get(GunProp.RPM);
    }

    // client side
    @Override
    public boolean canShoot(LivingEntity living) {
        var gunData = getGunData(getSeatIndex(living));

        return gunData != null && gunData.canShoot(getAmmoSupplier());
    }

    // TODO 正确计算AmmoCount
    @Override
    public int getAmmoCount(LivingEntity living) {
        var data = getGunData(getSeatIndex(living));
        if (data == null) return 0;
        return data.useBackpackAmmo() ? data.backupAmmoCount.get() : data.ammo.get();
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
        var gunData = getGunData(getSeatIndex(living));
        if (gunData == null) return 0;

        return Math.toIntExact(Math.round(gunData.heat.get()));
    }

    @Override
    public boolean hasDecoy() {
        return true;
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return zoom ? 0.23 : Minecraft.getInstance().options.getCameraType().isFirstPerson() ? 0.3 : 0.4;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            if (this.getSeatIndex(player) == 0) {
                return new Vec2((float) -getYRotFromVector(this.getBarrelVector(partialTicks)), (float) -getXRotFromVector(this.getBarrelVector(partialTicks)));
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
                    return new Vec3(this.zoomPos(player, partialTicks).x, this.zoomPos(player, partialTicks).y, this.zoomPos(player, partialTicks).z);
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
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obb6, this.obb7, this.obb8, this.obbTurret);
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

        Vector4f worldPosition8 = transformPosition(transform, 0.65625f, 2.03125f, -2.0625f);
        this.obb8.center().set(new Vector3f(worldPosition8.x, worldPosition8.y, worldPosition8.z));
        this.obb8.setRotation(VectorTool.combineRotations(1, this));

        Matrix4f transformT = getTurretTransform(1);
        Vector4f worldPositionT = transformPosition(transformT, 0, 0.3625f, 0f);
        this.obbTurret.center().set(new Vector3f(worldPositionT.x, worldPositionT.y, worldPositionT.z));
        this.obbTurret.setRotation(VectorTool.combineRotationsTurret(1, this));
    }

    @Override
    public boolean hasTurret() {
        return true;
    }
}
