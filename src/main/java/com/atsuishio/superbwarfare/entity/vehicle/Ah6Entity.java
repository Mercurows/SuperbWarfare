package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.gun.Ammo;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.SmallCannonShellWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.SmallRocketWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.CameraTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraPitch;
import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraYaw;
import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class Ah6Entity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity, OBBEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final EntityDataAccessor<Integer> LOADED_ROCKET = SynchedEntityData.defineId(Ah6Entity.class, EntityDataSerializers.INT);

    public int fireIndex;

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obb6;
    public OBB obb7;

    public Ah6Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.AH_6.get(), world);
    }

    public Ah6Entity(EntityType<Ah6Entity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(1.0625f, 1.18125f, 1.625f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(0.875f, 0.6875f, 0.59375f), new Quaternionf(), OBB.Part.BODY);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(0.25f, 0.3125f, 2.25f), new Quaternionf(), OBB.Part.BODY);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.0625f, 1.15625f, 0.40625f), new Quaternionf(), OBB.Part.BODY);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(1f, 0.25f, 0.21875f), new Quaternionf(), OBB.Part.BODY);
        this.obb6 = new OBB(this.position().toVector3f(), new Vector3f(0.3125f, 0.40625f, 0.84375f), new Quaternionf(), OBB.Part.ENGINE1);
        this.obb7 = new OBB(this.position().toVector3f(), new Vector3f(0.3125f, 0.40625f, 0.40625f), new Quaternionf(), OBB.Part.ENGINE2);
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new SmallCannonShellWeapon()
                                .damage(VehicleConfig.AH_6_CANNON_DAMAGE.get().floatValue())
                                .explosionDamage(VehicleConfig.AH_6_CANNON_EXPLOSION_DAMAGE.get().floatValue())
                                .explosionRadius(VehicleConfig.AH_6_CANNON_EXPLOSION_RADIUS.get().floatValue())
                                .sound(ModSounds.INTO_CANNON.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/cannon_20mm.png"))
                                .sound1p(ModSounds.HELICOPTER_CANNON_FIRE_1P.get())
                                .sound3p(ModSounds.HELICOPTER_CANNON_FIRE_3P.get())
                                .sound3pFar(ModSounds.HELICOPTER_CANNON_FAR.get())
                                .sound3pVeryFar(ModSounds.HELICOPTER_CANNON_VERYFAR.get()),
                        new SmallRocketWeapon()
                                .damage(VehicleConfig.AH_6_ROCKET_DAMAGE.get().floatValue())
                                .explosionDamage(VehicleConfig.AH_6_ROCKET_EXPLOSION_DAMAGE.get().floatValue())
                                .explosionRadius(VehicleConfig.AH_6_ROCKET_EXPLOSION_RADIUS.get().floatValue())
                                .sound(ModSounds.INTO_MISSILE.get())
                                .sound1p(ModSounds.SMALL_ROCKET_FIRE_1P.get())
                                .sound3p(ModSounds.SMALL_ROCKET_FIRE_3P.get()),
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(7, 1, -2.7);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LOADED_ROCKET, 0);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("LoadedRocket", this.entityData.get(LOADED_ROCKET));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(LOADED_ROCKET, compound.getInt("LoadedRocket"));
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> {
                    var entity = source.getDirectEntity();
                    if (entity != null && entity.getType().is(ModTags.EntityTypes.AERIAL_BOMB)) {
                        damage *= 2;
                    }
                    damage *= getHealth() > 0.1f ? 0.7f : 0.05f;
                    return damage;
                });
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() == ModItems.SMALL_ROCKET.get() && this.entityData.get(LOADED_ROCKET) < 14) {
            // 装载火箭
            this.entityData.set(LOADED_ROCKET, this.entityData.get(LOADED_ROCKET) + 1);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            this.level().playSound(null, this, ModSounds.MISSILE_RELOAD.get(), this.getSoundSource(), 2, 1);
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.interact(player, hand);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        updateOBB();

        if (this.level() instanceof ServerLevel) {
            if (reloadCoolDown > 0) {
                reloadCoolDown--;
            }
            handleAmmo();
        }

        releaseDecoy();
        lowHealthWarning();
        this.terrainCompact(2.7f, 2.7f);

        this.refreshDimensions();
    }

    private void handleAmmo() {
        if (!(this.getFirstPassenger() instanceof Player player)) return;

        int ammoCount = countItem(ModItems.SMALL_SHELL.get());

        if ((hasItem(ModItems.SMALL_ROCKET.get()) || InventoryTool.hasCreativeAmmoBox(player)) && reloadCoolDown == 0 && this.getEntityData().get(LOADED_ROCKET) < 14) {
            this.entityData.set(LOADED_ROCKET, this.getEntityData().get(LOADED_ROCKET) + 1);
            reloadCoolDown = 25;
            if (!InventoryTool.hasCreativeAmmoBox(player)) {
                this.getItemStacks().stream().filter(stack -> stack.is(ModItems.SMALL_ROCKET.get())).findFirst().ifPresent(stack -> stack.shrink(1));
            }
            this.level().playSound(null, this, ModSounds.MISSILE_RELOAD.get(), this.getSoundSource(), 1, 1);
        }

        if (this.getWeaponIndex(0) == 0) {
            this.entityData.set(AMMO, ammoCount);
        } else {
            this.entityData.set(AMMO, this.getEntityData().get(LOADED_ROCKET));
        }
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.HELICOPTER_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return entityData.get(PROPELLER_ROT) * 2f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public Vec3 getShootPos(int seatIndex, float tickDelta) {
        Matrix4f transform = getVehicleTransform(tickDelta);
        Vector4f worldPosition = transformPosition(transform, 0f, 0.62f, 0.8f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 getShootVec(int seatIndex, float tickDelta) {
        Matrix4f transform = getVehicleTransform(tickDelta);
        Vector4f worldPosition = transformPosition(transform, 0, 0, 0);
        Vector4f worldPosition2 = transformPosition(transform, 0, 0.01f, 1);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(new Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z)).normalize();
    }

    @Override
    public void vehicleShoot(LivingEntity living, int type) {
        boolean hasCreativeAmmo = false;
        for (int i = 0; i < getMaxPassengers() - 1; i++) {
            if (InventoryTool.hasCreativeAmmoBox(getNthEntity(i))) {
                hasCreativeAmmo = true;
            }
        }

        Matrix4f transform = getVehicleTransform(1);

        if (getWeaponIndex(0) == 0) {
            if (this.cannotFire) return;

            Vector4f worldPosition;
            Vector4f worldPosition2;

            if (fireIndex == 0) {
                worldPosition = transformPosition(transform, 1.15f, 0.62f, 0.8f);
                worldPosition2 = transformPosition(transform, 1.157f, 0.632f, 1.8f);
                fireIndex = 1;
            } else {
                worldPosition = transformPosition(transform, -1.15f, 0.62f, 0.8f);
                worldPosition2 = transformPosition(transform, -1.139f, 0.632f, 1.8f);
                fireIndex = 0;
            }

            Vec3 shootVec = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(new Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z)).normalize();

            if (this.entityData.get(AMMO) > 0 || hasCreativeAmmo) {
                var entityToSpawn = ((SmallCannonShellWeapon) getWeapon(0)).create(living);

                entityToSpawn.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
                entityToSpawn.shoot(shootVec.x, shootVec.y, shootVec.z, 30, 0.15f);
                level().addFreshEntity(entityToSpawn);

                sendParticle((ServerLevel) this.level(), ParticleTypes.LARGE_SMOKE, worldPosition.x, worldPosition.y, worldPosition.z, 1, 0, 0, 0, 0, false);

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
                        this.getItemStacks().stream().filter(stack -> stack.is(ModItems.SMALL_SHELL.get())).findFirst().ifPresent(stack -> stack.shrink(1));
                    }
                }

            }

            this.entityData.set(HEAT, this.entityData.get(HEAT) + 4);

            playShootSound3p(living, 0, 4, 12, 24, new Vec3(worldPosition.x, worldPosition.y, worldPosition.z));


        } else if (getWeaponIndex(0) == 1 && this.getEntityData().get(LOADED_ROCKET) > 0) {

            var heliRocketEntity = ((SmallRocketWeapon) getWeapon(0)).create(living);

            Vector4f worldPosition;
            Vector4f worldPosition2;

            if (fireIndex == 0) {
                worldPosition = transformPosition(transform, 1.7f, 0.62f, 0.8f);
                worldPosition2 = transformPosition(transform, 1.695f, 0.632f, 1.8f);
                fireIndex = 1;
            } else {
                worldPosition = transformPosition(transform, -1.7f, 0.62f, 0.8f);
                worldPosition2 = transformPosition(transform, -1.695f, 0.632f, 1.8f);
                fireIndex = 0;
            }

            Vec3 shootVec = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(new Vec3(worldPosition2.x, worldPosition2.y, worldPosition2.z)).normalize();

            heliRocketEntity.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            heliRocketEntity.shoot(shootVec.x, shootVec.y, shootVec.z, 16, 0.25f);
            living.level().addFreshEntity(heliRocketEntity);

            playShootSound3p(living, 0, 6, 6, 6, new Vec3(worldPosition.x, worldPosition.y, worldPosition.z));

            this.entityData.set(LOADED_ROCKET, this.getEntityData().get(LOADED_ROCKET) - 1);
            reloadCoolDown = 30;
        }
    }

    @Override
    public int mainGunRpm(LivingEntity living) {
        return 500;
    }

    @Override
    public boolean canShoot(LivingEntity living) {
        if (getWeaponIndex(0) == 0) {
            return (this.entityData.get(AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(living)) && !cannotFire;
        } else if (getWeaponIndex(0) == 1) {
            return this.entityData.get(AMMO) > 0;
        }
        return false;
    }

    @Override
    public int getAmmoCount(LivingEntity living) {
        return this.entityData.get(AMMO);
    }

    @Override
    public int zoomFov() {
        return 2;
    }

    @Override
    public int getWeaponHeat(LivingEntity living) {
        return entityData.get(HEAT);
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return seatIndex == 0 ? 0 : original;
    }

    @Override
    public double getMouseSensitivity() {
        return 0.25;
    }

    @Override
    public double getMouseSpeedX() {
        return 0.4;
    }

    @Override
    public double getMouseSpeedY() {
        return 0.25;
    }

    @Override
    public float rotateYOffset() {
        return 1.45f;
    }

    @Override
    public boolean useFixedCameraPos(Entity entity) {
        return !level().isClientSide || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON;
    }

    @Override
    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getVehicleTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, -1.75f, 2.2f, -4.5f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForIndex(LivingEntity passenger, int index) {
        Matrix4f transform = getVehicleTransform(1);
        Vector4f worldPosition;

        if (index == 0) {
            worldPosition = transformPosition(transform, 2f, 1.2f, 1f);
        } else if (index == 1) {
            worldPosition = transformPosition(transform, -2f, 1.2f, 1f);
        } else if (index == 2) {
            worldPosition = transformPosition(transform, -2f, 1.2f, 0);
        } else {
            worldPosition = transformPosition(transform, 2f, 1.2f, 0);
        }

        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (this.getSeatIndex(player) == 0) {
            return new Vec2((float) (getYaw(partialTicks) - freeCameraYaw), (float) (getPitch(partialTicks) + freeCameraPitch));
        }

        return super.getCameraRotation(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (this.getSeatIndex(player) == 0) {
            Matrix4f transform = getClientVehicleTransform(partialTicks);
            Vector4f maxCameraPosition = transformPosition(transform, -2.1f, 2.45f, -10 - (float) ClientMouseHandler.custom3pDistanceLerp);
            Vec3 finalPos = CameraTool.getMaxZoom(transform, maxCameraPosition);

            if (isFirstPerson) {
                return new Vec3(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
            } else {
                if (zoom) {
                    return driverZoomPos(partialTicks);
                } else {
                    return finalPos;
                }
            }
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }

    @Override
    public int getHudColor() {
        return super.getHudColor();
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obb6, this.obb7);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 1.86875f, -0.15625f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 0, 1.5f, 1.90625f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition3 = transformPosition(transform, 0, 2.3125f, -4.1875f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition4 = transformPosition(transform, -0.125f, 2.34375f, -6.34375f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition5 = transformPosition(transform, -0.125f, 3.5625f, -6.65625f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition6 = transformPosition(transform, 0, 3.28125f, -0.53125f);
        this.obb6.center().set(new Vector3f(worldPosition6.x, worldPosition6.y, worldPosition6.z));
        this.obb6.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition7 = transformPosition(transform, 0.1875f, 2.09375f, -6.15625f);
        this.obb7.center().set(new Vector3f(worldPosition7.x, worldPosition7.y, worldPosition7.z));
        this.obb7.setRotation(VectorTool.combineRotations(1, this));
    }

    @Override
    public int getContainerSize() {
        return 102;
    }
}
