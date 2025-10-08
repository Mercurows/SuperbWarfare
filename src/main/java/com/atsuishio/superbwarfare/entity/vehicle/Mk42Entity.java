package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.RemoteControllableTurret;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.CannonShellWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.ArtilleryIndicator;
import com.atsuishio.superbwarfare.item.common.ammo.CannonShellItem;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
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

import static com.atsuishio.superbwarfare.tools.RangeTool.calculateLaunchVector;

public class Mk42Entity extends VehicleEntity implements GeoEntity, CannonEntity, RemoteControllableTurret {
    @Override
    public boolean hasMenu() {
        return false;
    }
    public static final EntityDataAccessor<Integer> COOL_DOWN = SynchedEntityData.defineId(Mk42Entity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(Mk42Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(Mk42Entity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Boolean> DEPRESSED = SynchedEntityData.defineId(Mk42Entity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Vector3f> TARGET_POS = SynchedEntityData.defineId(Mk42Entity.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Integer> RADIUS = SynchedEntityData.defineId(Mk42Entity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Integer> AMMO_COUNT = SynchedEntityData.defineId(Mk42Entity.class, EntityDataSerializers.INT);

    public Mk42Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.MK_42.get(), world);
    }

    public Mk42Entity(EntityType<Mk42Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();

        entityData.set(YAW, getYRot());
        entityData.set(PITCH, getXRot());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COOL_DOWN, 0);
        this.entityData.define(PITCH, 0f);
        this.entityData.define(YAW, 0f);

        this.entityData.define(DEPRESSED, false);
        this.entityData.define(TARGET_POS, new Vector3f());
        this.entityData.define(RADIUS, 0);
        this.entityData.define(AMMO_COUNT, 0);
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.MK42_AP_DAMAGE.get())
                                .explosionDamage(VehicleConfig.MK42_AP_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.MK42_AP_EXPLOSION_RADIUS.get().floatValue())
                                .durability(60)
                                .gravity(projectileGravity())
                                .sound(ModSounds.CANNON_RELOAD.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/ap_shell.png")),
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.MK42_HE_DAMAGE.get())
                                .explosionDamage(VehicleConfig.MK42_HE_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.MK42_HE_EXPLOSION_RADIUS.get().floatValue())
                                .durability(1)
                                .fireProbability(0.18F)
                                .fireTime(2)
                                .gravity(projectileGravity())
                                .sound(ModSounds.CANNON_RELOAD.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/he_shell.png")),
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.MK42_HE_DAMAGE.get())
                                .explosionDamage(VehicleConfig.MK42_HE_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.MK42_HE_EXPLOSION_RADIUS.get().floatValue())
                                .durability(1)
                                .gravity(projectileGravity())
                                .type(CannonShellEntity.Type.CM)
                                .spreadAmount(30)
                                .spreadTime(7)
                                .spreadAngle(15)
                                .sound(ModSounds.CANNON_RELOAD.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/cm_shell.png")),
                        // GRAPESHOT
                        new CannonShellWeapon()
                                .hitDamage(700)
                                .explosionDamage(VehicleConfig.MK42_AP_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.MK42_AP_EXPLOSION_RADIUS.get().floatValue())
                                .velocity(30)
                                .type(CannonShellEntity.Type.GRAPE)
                                .spreadAmount(30)
                                .spreadAngle(3)
                                .sound(ModSounds.INTO_CANNON.get())
                                .ammo(ModItems.GS_5_INCHES.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/grape_shell.png"))
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(8 + ClientMouseHandler.custom3pDistanceLerp, 1, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("CoolDown", this.entityData.get(COOL_DOWN));
        compound.putFloat("Pitch", this.entityData.get(PITCH));
        compound.putFloat("Yaw", this.entityData.get(YAW));

        compound.putBoolean("Depressed", this.entityData.get(DEPRESSED));
        compound.putInt("Radius", this.entityData.get(RADIUS));
        compound.putFloat("TargetX", this.entityData.get(TARGET_POS).x);
        compound.putFloat("TargetY", this.entityData.get(TARGET_POS).y);
        compound.putFloat("TargetZ", this.entityData.get(TARGET_POS).z);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(COOL_DOWN, compound.getInt("CoolDown"));
        this.entityData.set(PITCH, compound.getFloat("Pitch"));
        this.entityData.set(YAW, compound.getFloat("Yaw"));

        if (compound.contains("Depressed")) {
            this.entityData.set(DEPRESSED, compound.getBoolean("Depressed"));
        }
        if (compound.contains("Radius")) {
            this.entityData.set(RADIUS, compound.getInt("Radius"));
        }
        if (compound.contains("TargetX") && compound.contains("TargetY") && compound.contains("TargetZ")) {
            this.entityData.set(TARGET_POS, new Vector3f(compound.getFloat("TargetX"), compound.getFloat("TargetX"), compound.getFloat("TargetZ")));
        }
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();

        if (stack.getItem() instanceof ArtilleryIndicator indicator) {
            return indicator.bind(stack, player, this);
        }

        if (stack.is(ModTags.Items.TOOLS_CROWBAR) && !player.isShiftKeyDown()) {
            if (this.items.get(0).getItem() instanceof CannonShellItem) {
                ItemStack item = this.getItem(0);

                int type = 0;
                if (item.is(ModItems.HE_5_INCHES.get())) {
                    type = 1;
                } else if (item.is(ModItems.CM_5_INCHES.get())) {
                    type = 2;
                } else if (item.is(ModItems.GS_5_INCHES.get())) {
                    type = 3;
                }
                setWeaponIndex(0, type);
                vehicleShoot(player, 0);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.getItem() instanceof CannonShellItem) {
            if (this.entityData.get(COOL_DOWN) == 0 && this.items.get(0).isEmpty()) {
                this.setItem(0, stack.copyWithCount(1));
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.CANNON_RELOAD.get(), 2, 1);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (player.getMainHandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            setTarget(player.getMainHandItem(), player);
        }
        if (player.getOffhandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            setTarget(player.getMainHandItem(), player);
        }
        return super.interact(player, hand);
    }

    @Override
    public void setTarget(ItemStack stack, Entity entity) {
        double targetX = stack.getOrCreateTag().getDouble("TargetX");
        double targetY = stack.getOrCreateTag().getDouble("TargetY");
        double targetZ = stack.getOrCreateTag().getDouble("TargetZ");
        boolean canAim = true;

        entityData.set(TARGET_POS, new Vector3f((float) targetX, (float) targetY, (float) targetZ));
        entityData.set(DEPRESSED, stack.getOrCreateTag().getBoolean("IsDepressed"));
        entityData.set(RADIUS, stack.getOrCreateTag().getInt("Radius"));
        Vec3 randomPos = VectorTool.randomPos(new Vec3(entityData.get(TARGET_POS)), entityData.get(RADIUS));
        Vec3 launchVector = calculateLaunchVector(getEyePosition(), randomPos, shootVelocity(), projectileGravity(), entityData.get(DEPRESSED));

        Component component = Component.literal("");
        Component location = Component.translatable("tips.superbwarfare.mortar.position", this.getDisplayName())
                .append(Component.literal(" X:" + FormatTool.format0D(getX()) + " Y:" + FormatTool.format0D(getY()) + " Z:" + FormatTool.format0D(getZ()) + " "));
        float angle = getXRot();

        if (launchVector == null) {
            canAim = false;
            component = Component.translatable("tips.superbwarfare.mortar.out_of_range");
        } else {
            angle = (float) -getXRotFromVector(launchVector);
            if (angle < -maxPitch() || angle > -minPitch()) {
                canAim = false;
                component = Component.translatable("tips.superbwarfare.mortar.warn", this.getDisplayName());
                if (angle < -maxPitch()) {
                    component = Component.translatable("tips.superbwarfare.ballistics.warn");
                }
            }
        }

        if (canAim) {
            this.look(randomPos);
            entityData.set(PITCH, angle);
        } else if (entity instanceof Player player) {
            player.displayClientMessage(location.copy().append(component).withStyle(ChatFormatting.RED), false);
        }
    }

    @Override
    public void resetTarget() {
        Vec3 randomPos = VectorTool.randomPos(new Vec3(entityData.get(TARGET_POS)), entityData.get(RADIUS));
        Vec3 launchVector = calculateLaunchVector(getEyePosition(), randomPos, 15, projectileGravity(), entityData.get(DEPRESSED));
        this.look(randomPos);

        if (launchVector == null) {
            return;
        }
        float angle = (float) -getXRotFromVector(launchVector);
        if (angle > -maxPitch() && angle < -minPitch()) {
            entityData.set(PITCH, angle);
        }
    }

    @Override
    public double minPitch() {
        return -14.9;
    }

    @Override
    public double maxPitch() {
        return 85;
    }

    @Override
    public double shootVelocity() {
        return 15;
    }

    @Override
    public float projectileGravity() {
        return 0.1f;
    }

    @Override
    public void look(Vec3 pTarget) {
        Matrix4f transform = getVehicleFlatTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0f, 2.16f, 0.5175f);
        Vec3 shootPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
        double d0 = pTarget.x - shootPos.x;
        double d2 = pTarget.z - shootPos.z;
        entityData.set(YAW, Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F));
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
        super.baseTick();

        if (this.entityData.get(COOL_DOWN) > 0) {
            this.entityData.set(COOL_DOWN, this.entityData.get(COOL_DOWN) - 1);
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }

        if (getFirstPassenger() instanceof Mob mob) {
            Entity target = EntityFindUtil.findEntity(level(), entityData.get(AI_TURRET_TARGET_UUID));
            if (target != null) {
                if (target.getVehicle() != null) {
                    target = target.getVehicle();
                }
                Vec3 targetVel = target.getDeltaMovement();

                if (target instanceof LivingEntity living) {
                    double gravity = living.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get());
                    targetVel = targetVel.add(0, gravity, 0);
                }

                if (target instanceof Player) {
                    targetVel = targetVel.multiply(2, 1, 2);
                }

                Vec3 launchVector = RangeTool.calculateFiringSolution(getEyePosition(), target.getBoundingBox().getCenter(), targetVel, 15, projectileGravity());

                entityData.set(PITCH, (float) -getXRotFromVector(launchVector));
                entityData.set(YAW, (float) -getYRotFromVector(launchVector));

                if (VectorTool.calculateAngle(launchVector, getLookAngle()) < 3) {
                    shoot(mob, 0, false);
                }
            }
        }

        countAmmo();
        lowHealthWarning();
    }

    public void countAmmo () {
        if (level() instanceof ServerLevel) {
            int ammoCount = switch (getWeaponIndex(0)) {
                case 1 -> countItem(ModItems.HE_5_INCHES.get());
                case 2 -> countItem(ModItems.CM_5_INCHES.get());
                case 3 -> countItem(ModItems.GS_5_INCHES.get());
                default -> countItem(ModItems.AP_5_INCHES.get());
            };

            entityData.set(AMMO_COUNT, ammoCount);
        }
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
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        serverYRot = yaw;
        serverXRot = pitch;
        this.interpolationSteps = 10;
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        if (!this.hasPassenger(passenger)) {
            return;
        }

        Matrix4f transform = getVehicleFlatTransform(1);

        float x = 0f;
        float y = 2.3f;
        float z = 0f;

        Vector4f worldPosition = transformPosition(transform, x, y, z);
        passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getVehicleFlatTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, 0f, 2.16f + 1.4f, 0.5175f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public boolean canRemoteFire() {
        return this.getItem(0).getItem() instanceof CannonShellItem && this.entityData.get(COOL_DOWN) == 0;
    }

    @Override
    public void remoteFire(@Nullable Player player) {
        ItemStack stack = this.getItem(0);

        int type = 0;
        if (stack.is(ModItems.HE_5_INCHES.get())) {
            type = 1;
        } else if (stack.is(ModItems.CM_5_INCHES.get())) {
            type = 2;
        } else if (stack.is(ModItems.GS_5_INCHES.get())) {
            type = 3;
        }

        this.setWeaponIndex(0, type);
        this.shoot(player, 0, true);
    }

    @Override
    public void vehicleShoot(LivingEntity living, int type) {
        shoot(living, type, false);
    }

    public void shoot(LivingEntity living, int type, boolean reset) {
        if (this.entityData.get(COOL_DOWN) > 0) return;
        if (getFirstPassenger() != null && getFirstPassenger() != living) return;

        if (level() instanceof ServerLevel server) {
            if (!InventoryTool.hasCreativeAmmoBox(living) && living == getFirstPassenger()) {
                if (entityData.get(AMMO_COUNT) > 0) {
                    this.items.get(0).shrink(1);
                } else {
                    Item ammo = switch (getWeaponIndex(0))
                    {
                        case 1 -> ModItems.HE_5_INCHES.get();
                        case 2 -> ModItems.CM_5_INCHES.get();
                        case 3 -> ModItems.GS_5_INCHES.get();
                        default -> ModItems.AP_5_INCHES.get();
                    };
                    var ammoCount = InventoryTool.countItem(living, ammo);

                    if (ammoCount <= 0) return;
                    InventoryTool.consumeItem(living, ammo, 1);
                }
            }

            if (getFirstPassenger() != living) {
                this.clearContent();
            }

            var entityToSpawn = ((CannonShellWeapon) getWeapon(0)).create(living);

            Matrix4f transform = getVehicleFlatTransform(1);
            Vector4f worldPosition = transformPosition(transform, 0f, 2.16f, 0.5175f);

            entityToSpawn.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            entityToSpawn.shoot(getLookAngle().x, getLookAngle().y, getLookAngle().z, 15, 0.05f);
            level().addFreshEntity(entityToSpawn);

            if (living instanceof ServerPlayer serverPlayer) {
                if (serverPlayer == getFirstPassenger()) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.MK_42_FIRE_1P.get(), 2, 1);
                    SoundTool.playLocalSound(serverPlayer, ModSounds.CANNON_RELOAD.get(), 2, 1);
                }
            }

            if (!this.level().isClientSide()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MK_42_FIRE_3P.get(), SoundSource.PLAYERS, 24f, 1f);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MK_42_FAR.get(), SoundSource.PLAYERS, 48f, 1f);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MK_42_VERYFAR.get(), SoundSource.PLAYERS, 96f, 1f);
            }

            this.entityData.set(COOL_DOWN, 30);

            ParticleTool.spawnBigCannonMuzzleParticles(getLookAngle(), new Vec3(worldPosition.x, worldPosition.y, worldPosition.z).add(getLookAngle().scale(6.4)), server, this);

            for (int i = 0; i < 40; i += 4) {
                Mod.queueServerWork(i, () -> ParticleTool.spawnBarrelSmoke(1, server, getLookAngle(), new Vec3(worldPosition.x, worldPosition.y, worldPosition.z).add(getLookAngle().scale(6.4))));
            }

            ShakeClientMessage.sendToNearbyPlayers(this, 20, 15, 15, 45);

            if (reset) {
                resetTarget();
            }
        }
    }

    @Override
    public void travel() {
        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof Player) {
            entityData.set(YAW, passenger.getYHeadRot());
            entityData.set(PITCH, passenger.getXRot() - 2f);
        }

        float diffY = Mth.wrapDegrees(entityData.get(YAW) - this.getYRot());
        float diffX = Mth.wrapDegrees(entityData.get(PITCH) - this.getXRot());

        turretTurnSound(diffX, diffY, 0.95f);

        this.setYRot(this.getYRot() + Mth.clamp(0.5f * diffY, -1.75f, 1.75f));
        this.setXRot(Mth.clamp(this.getXRot() + Mth.clamp(0.5f * diffX, -3f, 3f), -85, 15f));
    }

    protected void clampRotation(Entity entity) {
        passengerPitch(entity, -17, 85, 0);
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    private PlayState movementPredicate(AnimationState<Mk42Entity> event) {
        if (this.entityData.get(COOL_DOWN) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mk42.fire"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mk42.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public int mainGunRpm(LivingEntity living) {
        return 0;
    }

    @Override
    public boolean canShoot(LivingEntity living) {
        return true;
    }

    @Override
    public int getAmmoCount(LivingEntity living) {
        int playerAmmo = 0;
        if (living == getFirstPassenger()) {
            Item ammo = switch (getWeaponIndex(0))
            {
                case 1 -> ModItems.HE_5_INCHES.get();
                case 2 -> ModItems.CM_5_INCHES.get();
                case 3 -> ModItems.GS_5_INCHES.get();
                default -> ModItems.AP_5_INCHES.get();
            };
            playerAmmo = InventoryTool.countItem(living, ammo);
        }

        return playerAmmo + entityData.get(AMMO_COUNT);
    }

    @Override
    public boolean hidePassenger(int index) {
        return true;
    }

    @Override
    public int zoomFov() {
        return 5;
    }

    @Override
    public int getWeaponHeat(LivingEntity living) {
        return 0;
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
        return Mod.loc("textures/vehicle_icon/sherman_icon.png");
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return zoom ? 0.15 : 0.3;
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
                return new Vec3(Mth.lerp(partialTicks, player.xo, player.getX()), Mth.lerp(partialTicks, player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(partialTicks, player.zo, player.getZ()));
            }
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean useFixedCameraPos(Entity entity) {
        return true;
    }

    @Override
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/defense.png");
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
//        if (!entityData.get(INTELLIGENT)) {
//            fire(null);
//        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return super.canPlaceItem(slot, stack) && this.entityData.get(COOL_DOWN) == 0 && stack.getItem() instanceof CannonShellItem;
    }

    @Override
    public boolean hasEnergyStorage() {
        return false;
    }

    @Override
    public VehicleType getVehicleType() {
        return VehicleType.ARTILLERY;
    }
}
