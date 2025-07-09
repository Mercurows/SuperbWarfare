package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.CannonShellWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.ArtilleryIndicator;
import com.atsuishio.superbwarfare.item.common.ammo.CannonShellItem;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

import static com.atsuishio.superbwarfare.tools.RangeTool.calculateLaunchVector;

public class Mk42Entity extends VehicleEntity implements GeoEntity, CannonEntity, Container, OwnableEntity {

    public static final EntityDataAccessor<Integer> COOL_DOWN = SynchedEntityData.defineId(Mk42Entity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(Mk42Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(Mk42Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(Mk42Entity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final float shellGravity = 0.1f;

    public Mk42Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.MK_42.get(), world);
    }

    public Mk42Entity(EntityType<Mk42Entity> type, Level world) {
        super(type, world);
    }

    private LazyOptional<?> itemHandler = LazyOptional.of(() -> new InvWrapper(this));
    public ItemStack stack = ItemStack.EMPTY;

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COOL_DOWN, 0);
        this.entityData.define(PITCH, 0f);
        this.entityData.define(YAW, 0f);
        this.entityData.define(OWNER_UUID, Optional.empty());
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
                                .gravity(shellGravity)
                                .sound(ModSounds.CANNON_RELOAD.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/ap_shell.png")),
                        new CannonShellWeapon()
                                .hitDamage(VehicleConfig.MK42_HE_DAMAGE.get())
                                .explosionDamage(VehicleConfig.MK42_HE_EXPLOSION_DAMAGE.get())
                                .explosionRadius(VehicleConfig.MK42_HE_EXPLOSION_RADIUS.get().floatValue())
                                .durability(1)
                                .fireProbability(0.18F)
                                .fireTime(2)
                                .gravity(shellGravity)
                                .sound(ModSounds.CANNON_RELOAD.get())
                                .icon(Mod.loc("textures/screens/vehicle_weapon/he_shell.png")),
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
        if (this.getOwnerUUID() != null) {
            compound.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(COOL_DOWN, compound.getInt("CoolDown"));
        this.entityData.set(PITCH, compound.getFloat("Pitch"));
        this.entityData.set(YAW, compound.getFloat("Yaw"));
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

        if (player.isShiftKeyDown() && this.getOwner() == null) {
            setOwnerUUID(player.getUUID());
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5F, 1);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.getItem() instanceof ArtilleryIndicator indicator && player == getOwner() && this.getOwner() == player) {
            if (indicator.addCannon(stack, getStringUUID())) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5F, 1);
                }
                player.displayClientMessage(Component.literal("added"), true);
                return InteractionResult.SUCCESS;
            } else if (indicator.removeCannon(stack, getStringUUID())) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5F, 1);
                }
                player.displayClientMessage(Component.literal("removed"), true);
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.FAIL;
            }
        }

        if (stack.getItem() instanceof CannonShellItem) {
            if (this.entityData.get(COOL_DOWN) == 0) {
                var weaponType = stack.is(ModItems.AP_5_INCHES.get()) ? 0 : 1;
                setWeaponIndex(0, weaponType);
                vehicleShoot(player, 0);
            }
            return InteractionResult.SUCCESS;
        }

        if (player.getMainHandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            if (setTarget(player.getMainHandItem())) {
                player.swing(InteractionHand.MAIN_HAND);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
        }
        if (player.getOffhandItem().getItem() == ModItems.FIRING_PARAMETERS.get()) {
            if (setTarget(player.getOffhandItem())) {
                player.swing(InteractionHand.OFF_HAND);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
        }
        return super.interact(player, hand);
    }

    public boolean setTarget(ItemStack stack) {
        int targetX = stack.getOrCreateTag().getInt("TargetX");
        int targetY = stack.getOrCreateTag().getInt("TargetY");
        int targetZ = stack.getOrCreateTag().getInt("TargetZ");
        var isDepressed = stack.getOrCreateTag().getBoolean("IsDepressed");

        Matrix4f transform = getVehicleFlatTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0f, 2.16f, 0.5175f);
        Vec3 shootPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);

        try {
            double adjust = 0.004 * new Vec3(targetX, targetY, targetZ).distanceTo(shootPos);
            Vec3 launchVector = calculateLaunchVector(shootPos, new Vec3(targetX, targetY - adjust, targetZ), 15, -shellGravity, isDepressed);
            this.look(new Vec3(targetX, targetY, targetZ));
            float angle = (float)-getXRotFromVector(launchVector);
            if (angle < -85 || angle > 14.9) {
                return false;
            }
            entityData.set(PITCH, angle);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    //TODO 指示器能选择弹道后删掉这个

    public boolean setTarget(ItemStack stack, boolean isDepressed) {
        int targetX = stack.getOrCreateTag().getInt("TargetX");
        int targetY = stack.getOrCreateTag().getInt("TargetY");
        int targetZ = stack.getOrCreateTag().getInt("TargetZ");

        Matrix4f transform = getVehicleFlatTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0f, 2.16f, 0.5175f);
        Vec3 shootPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);

        try {
            double adjust = 0.004 * new Vec3(targetX, targetY, targetZ).distanceTo(shootPos);
            Vec3 launchVector = calculateLaunchVector(shootPos, new Vec3(targetX, targetY - adjust, targetZ), 15, -shellGravity, isDepressed);
            this.look(new Vec3(targetX, targetY, targetZ));
            float angle = (float)-getXRotFromVector(launchVector);
            if (angle < -85 || angle > 14.9) {
                return false;
            }
            entityData.set(PITCH, angle);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void look(Vec3 pTarget) {
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

        lowHealthWarning();
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
    public void destroy() {
        if (level() instanceof ServerLevel) {
            CustomExplosion explosion = new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), getAttacker(), getAttacker()), 100f,
                    this.getX(), this.getY(), this.getZ(), 7f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);
            explosion.explode();
            net.minecraftforge.event.ForgeEventFactory.onExplosionStart(this.level(), explosion);
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
        Matrix4f transform = getVehicleFlatTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0f, 2.16f + 1.4f, 0.5175f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public void vehicleShoot(Player player, int type) {
        if (this.entityData.get(COOL_DOWN) > 0) return;

        Level level = player.level();
        if (level instanceof ServerLevel server) {
            if (!InventoryTool.hasCreativeAmmoBox(player)) {
                var ammo = getWeaponIndex(0) == 0 ? ModItems.AP_5_INCHES.get() : ModItems.HE_5_INCHES.get();
                var ammoCount = InventoryTool.countItem(player.getInventory().items, ammo);

                if (ammoCount <= 0) return;
                InventoryTool.consumeItem(player.getInventory().items, ammo, 1);
            }

            if (getFirstPassenger() != getOwner()) {
                this.stack = ItemStack.EMPTY;
            }

            var entityToSpawn = ((CannonShellWeapon) getWeapon(0)).create(player);

            Matrix4f transform = getVehicleFlatTransform(1);
            Vector4f worldPosition = transformPosition(transform, 0f, 2.16f, 0.5175f);

            entityToSpawn.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            entityToSpawn.shoot(getLookAngle().x, getLookAngle().y, getLookAngle().z, 15, 0.05f);
            level.addFreshEntity(entityToSpawn);

            if (player instanceof ServerPlayer serverPlayer) {
                if (player == getFirstPassenger()) {
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

            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.getX() + 5 * this.getLookAngle().x,
                    this.getY(),
                    this.getZ() + 5 * this.getLookAngle().z,
                    100, 7, 0.02, 7, 0.005);

            double x = worldPosition.x + 9 * this.getLookAngle().x;
            double y = worldPosition.y + 9 * this.getLookAngle().y;
            double z = worldPosition.z + 9 * this.getLookAngle().z;

            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 10, 0.4, 0.4, 0.4, 0.0075);
            server.sendParticles(ParticleTypes.CLOUD, x, y, z, 10, 0.4, 0.4, 0.4, 0.0075);

            int count = 6;

            for (float i = 9.5f; i < 16; i += .5f) {
                server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        this.getX() + i * this.getLookAngle().x,
                        this.getEyeY() + i * this.getLookAngle().y,
                        this.getZ() + i * this.getLookAngle().z,
                        Mth.clamp(count--, 1, 5), 0.15, 0.15, 0.15, 0.0025);
            }

            ShakeClientMessage.sendToNearbyPlayers(this, 20, 15, 15, 45);
        }
    }


    @Override
    public void travel() {
        Entity passenger = this.getFirstPassenger();
        if (passenger != null) {
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
        float f = Mth.wrapDegrees(entity.getXRot());
        float f1 = Mth.clamp(f, -85.0F, 17F);
        entity.xRotO += f1 - f;
        entity.setXRot(entity.getXRot() + f1 - f);
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
    public int mainGunRpm(Player player) {
        return 0;
    }

    @Override
    public boolean canShoot(Player player) {
        return true;
    }

    @Override
    public int getAmmoCount(Player player) {
        var ammo = getWeaponIndex(0) == 0 ? ModItems.AP_5_INCHES.get() : ModItems.HE_5_INCHES.get();
        return InventoryTool.countItem(player.getInventory().items, ammo);
    }

    @Override
    public boolean hidePassenger(Entity entity) {
        return true;
    }

    @Override
    public int zoomFov() {
        return 5;
    }

    @Override
    public int getWeaponHeat(Player player) {
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
    public boolean isEmpty() {
        return stack == ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return slot == 0 ? stack : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0 || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            stack = ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return removeItem(0, 1);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot != 0) return;
        this.stack = stack;
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
    public void clearContent() {
        this.stack = ItemStack.EMPTY;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot != 0 || this.entityData.get(COOL_DOWN) != 0) return false;
        return stack.getItem() instanceof CannonShellItem;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing) {
        if (this.isAlive() && capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        return this.getCapability(cap, null);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = LazyOptional.of(() -> new InvWrapper(this));
    }
}
