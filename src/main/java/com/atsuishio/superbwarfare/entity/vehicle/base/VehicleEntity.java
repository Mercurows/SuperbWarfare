package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.energy.SyncedEntityEnergyStorage;
import com.atsuishio.superbwarfare.capability.energy.VehicleEnergyStorage;
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.compat.netmusic.NetMusicCompatHolder;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.DataLoader;
import com.atsuishio.superbwarfare.data.Prop;
import com.atsuishio.superbwarfare.data.StringOrVec3;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.ShootRay;
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData;
import com.atsuishio.superbwarfare.data.vehicle.VehicleData;
import com.atsuishio.superbwarfare.data.vehicle.VehicleProp;
import com.atsuishio.superbwarfare.data.vehicle.VehiclePropertyModifier;
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo;
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter;
import com.atsuishio.superbwarfare.entity.projectile.FlareDecoyEntity;
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.LaserWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.SmallRocketWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.SwarmDroneWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.atsuishio.superbwarfare.menu.VehicleMenu;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.atsuishio.superbwarfare.world.TDMSavedData;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.joml.Math;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraPitch;
import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraYaw;
import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public abstract class VehicleEntity extends Entity implements VehiclePropertyModifier, ControllableVehicle, HasCustomInventoryScreen, ContainerEntity {

    public static final String TAG_SEAT_INDEX = "SBWSeatIndex";

    public static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> OVERRIDE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> LAST_ATTACKER_UUID = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> LAST_DRIVER_UUID = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);

    public static final EntityDataAccessor<String> AI_TURRET_TARGET_UUID = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> AI_PASSENGER_WEAPON_TARGET_UUID = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);

    public static final EntityDataAccessor<Float> DELTA_ROT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> MOUSE_SPEED_X = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> MOUSE_SPEED_Y = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<List<Integer>> SELECTED_WEAPON = SynchedEntityData.defineId(VehicleEntity.class, ModSerializers.INT_LIST_SERIALIZER.get());
    public static final EntityDataAccessor<Integer> HEAT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Float> TURRET_HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> L_WHEEL_HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> R_WHEEL_HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> ENGINE_HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> L_ENGINE_HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Boolean> TURRET_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> L_WHEEL_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> R_WHEEL_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> ENGINE1_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> ENGINE2_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Float> HORN_VOLUME = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    public static Consumer<VehicleEntity> trackSound = vehicle -> {
    };
    public static Consumer<VehicleEntity> engineSound = vehicle -> {
    };
    public static Consumer<VehicleEntity> swimSound = vehicle -> {
    };
    public static Consumer<VehicleEntity> hornSound = vehicle -> {
    };
    public static Consumer<VehicleEntity> inCarMusic = vehicle -> {
    };

    public static final EntityDataAccessor<Integer> CANNON_RECOIL_TIME = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Integer> FIRE_ANIM = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> COAX_HEAT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Integer> AMMO = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DECOY_COUNT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Float> PROPELLER_ROT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> GEAR_ROT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> GEAR_UP = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> FORWARD_INPUT_DOWN = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> BACK_INPUT_DOWN = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> LEFT_INPUT_DOWN = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> RIGHT_INPUT_DOWN = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> UP_INPUT_DOWN = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DOWN_INPUT_DOWN = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DECOY_INPUT_DOWN = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> FIRE_INPUT_DOWN = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> SPRINT_INPUT_DOWN = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> LANDING_INPUT_DOWN = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> PLANE_BREAK = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    // Map SeatIndex -> GunData
    protected static final EntityDataAccessor<Map<Integer, GunData>> GUN_DATA_MAP = SynchedEntityData.defineId(VehicleEntity.class, ModSerializers.GUN_DATA_MAP_SERIALIZER.get());

    public Map<Integer, GunData> getGunDataMap() {
        var rawMap = entityData.get(GUN_DATA_MAP);
        var newMap = new HashMap<Integer, GunData>();
        var seats = data().get(VehicleProp.SEATS);

        for (int index = 0; index < seats.size(); index++) {
            var seat = seats.get(index);
            var data = rawMap.get(index);

            if (data == null) {
                if (seat.weaponData == null) continue;
                data = GunData.from(new ItemStack(ModItems.VEHICLE_GUN.get()));
            }

            data.defaultDataSupplier = () -> seat.weaponData;
            newMap.put(index, data);
        }

        return newMap;
    }

    public @Nullable GunData getGunData(int index) {
        return getGunDataMap().get(index);
    }

    public void modifyGunData(int index, @NotNull Consumer<GunData> consumer) {
        var map = getGunDataMap();
        var data = getGunData(index);
        if (data == null) return;

        data = data.copy();
        consumer.accept(data);
        data.save();
        map.put(index, data);

        entityData.set(GUN_DATA_MAP, map);
    }

    public VehicleWeapon[][] availableWeapons;

    protected int interpolationSteps;
    protected double x;
    protected double y;
    protected double z;
    protected double serverYRot;
    protected double serverXRot;

    public float roll;
    public float prevRoll;
    public int repairCoolDown = maxRepairCoolDown();
    public boolean crash;

    public float turretYRot;
    public float turretXRot;
    public float turretYRotO;
    public float turretXRotO;
    public float turretYRotLock;
    public float gunYRot;
    public float gunXRot;
    public float gunYRotO;
    public float gunXRotO;

    public boolean cannotFire;

    public int noPassengerTime;

    public double aiTurretDiff;
    public double aiPassengerDiff;

    public @Nullable Player damageDebugResultReceiver = null;

    private Vec3 previousVelocity = Vec3.ZERO;

    public double acceleration;
    public int decoyReloadCoolDown;
    public double lastTickSpeed;
    public double lastTickVerticalSpeed;
    public int collisionCoolDown;

    private boolean wasEngineRunning = false;
    private boolean wasHornWorking = false;
    private boolean wasInCarMusicPlaying = false;

    public double targetSpeed;
    public float rudderRot;
    public float rudderRotO;

    public float leftWheelRot;
    public float rightWheelRot;
    public float leftWheelRotO;
    public float rightWheelRotO;

    public float leftTrackO;
    public float rightTrackO;
    public float leftTrack;
    public float rightTrack;

    public float rotorRot;
    public float rotorRotO;

    public float propellerRot;
    public float propellerRotO;

    public double recoilShake;
    public double recoilShakeO;

    public boolean cannotFireCoax;
    public int reloadCoolDown;

    public double velocityO;
    public double velocity;

    public float flap1LRot;
    public float flap1LRotO;
    public float flap1RRot;
    public float flap1RRotO;
    public float flap1L2Rot;
    public float flap1L2RotO;
    public float flap1R2Rot;
    public float flap1R2RotO;
    public float flap2LRot;
    public float flap2LRotO;
    public float flap2RRot;
    public float flap2RRotO;
    public float flap3Rot;
    public float flap3RotO;
    public float gearRotO;

    public boolean engineStart;
    public boolean engineStartOver;

    public int holdTick;
    public int holdPowerTick;
    public float destroyRot;

    public int currentFirePosIndex;

    public VehicleEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.isInitialized = true;

        this.setHealth(this.getMaxHealth());

        if (this.hasEnergyStorage()) {
            this.energyStorage = new VehicleEnergyStorage(this);
        }
    }

    @Override
    public void processInput(short keys) {
        setLeftInputDown((keys & 0b000000001) > 0);
        setRightInputDown((keys & 0b000000010) > 0);
        setForwardInputDown((keys & 0b000000100) > 0);
        setBackInputDown((keys & 0b000001000) > 0);
        setLandingInputDown((keys & 0b000001000) > 0);
        setUpInputDown((keys & 0b000010000) > 0);
        setDownInputDown((keys & 0b000100000) > 0);
        setDecoyInputDown((keys & 0b001000000) > 0);
        setFireInputDown((keys & 0b010000000) > 0);
        setSprintInputDown((keys & 0b100000000) > 0);
    }

    public boolean forwardInputDown() {
        return entityData.get(FORWARD_INPUT_DOWN);
    }

    public boolean backInputDown() {
        return entityData.get(BACK_INPUT_DOWN);
    }

    public boolean leftInputDown() {
        return entityData.get(LEFT_INPUT_DOWN);
    }

    public boolean rightInputDown() {
        return entityData.get(RIGHT_INPUT_DOWN);
    }

    public boolean upInputDown() {
        return entityData.get(UP_INPUT_DOWN);
    }

    public boolean downInputDown() {
        return entityData.get(DOWN_INPUT_DOWN);
    }

    public boolean landingInputDown() {
        return entityData.get(LANDING_INPUT_DOWN);
    }

    public boolean fireInputDown() {
        return entityData.get(FIRE_INPUT_DOWN);
    }

    public boolean decoyInputDown() {
        return entityData.get(DECOY_INPUT_DOWN);
    }

    public boolean sprintInputDown() {
        return entityData.get(SPRINT_INPUT_DOWN);
    }

    public void setForwardInputDown(boolean set) {
        entityData.set(FORWARD_INPUT_DOWN, set);
    }

    public void setBackInputDown(boolean set) {
        entityData.set(BACK_INPUT_DOWN, set);
    }

    public void setLeftInputDown(boolean set) {
        entityData.set(LEFT_INPUT_DOWN, set);
    }

    public void setRightInputDown(boolean set) {
        entityData.set(RIGHT_INPUT_DOWN, set);
    }

    public void setUpInputDown(boolean set) {
        entityData.set(UP_INPUT_DOWN, set);
    }

    public void setDownInputDown(boolean set) {
        entityData.set(DOWN_INPUT_DOWN, set);
    }

    public void setLandingInputDown(boolean set) {
        entityData.set(LANDING_INPUT_DOWN, set);
    }

    public void setFireInputDown(boolean set) {
        entityData.set(FIRE_INPUT_DOWN, set);
    }

    public void setDecoyInputDown(boolean set) {
        entityData.set(DECOY_INPUT_DOWN, set);
    }

    public void setSprintInputDown(boolean set) {
        entityData.set(SPRINT_INPUT_DOWN, set);
    }

    @Override
    public void playerTouch(Player pPlayer) {
        if (pPlayer.isCrouching()
                && !this.level().isClientSide
                && pPlayer.getY() < this.getY() + this.getBbHeight()
                && pPlayer.getY() + pPlayer.getBbHeight() > this.getY()
        ) {
            double entitySize = pPlayer.getBbWidth() * pPlayer.getBbHeight();
            double thisSize = this.getBbWidth() * this.getBbHeight();
            double f = Math.min(entitySize / thisSize, 2);
            double f1 = Math.min(thisSize / entitySize, 4);
            this.setDeltaMovement(this.getDeltaMovement().add(new Vec3(pPlayer.position().vectorTo(this.position()).toVector3f()).scale(0.15 * f * pPlayer.getDeltaMovement().length())));
            pPlayer.setDeltaMovement(pPlayer.getDeltaMovement().add(new Vec3(this.position().vectorTo(pPlayer.position()).toVector3f()).scale(0.1 * f1 * pPlayer.getDeltaMovement().length())));
        }
    }

    protected final HashMap<String, Function<VehicleEntity, ShootRay>> shootAnchorPoints = new HashMap<>();

    public final Function<VehicleEntity, ShootRay> DEFAULT_POS = createShootAnchorPoint("Default", v -> new ShootRay(v.position(), v.getLookAngle()));

    protected Function<VehicleEntity, ShootRay> createShootAnchorPoint(String name, Function<VehicleEntity, ShootRay> func) {
        shootAnchorPoints.put(name, func);
        return func;
    }

    protected final Map<VehicleProp<?>, Prop.PropModifyContext<VehicleData, DefaultVehicleData, ?>> propertyModifiers = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Map<VehicleProp<?>, Prop.PropModifyContext<VehicleData, DefaultVehicleData, ?>> getPropModifiers() {
        return this.propertyModifiers;
    }

    public void mouseInput(double x, double y) {
        entityData.set(MOUSE_SPEED_X, (float) x);
        entityData.set(MOUSE_SPEED_Y, (float) y);
    }

    public float getMouseMoveSpeedY() {
        return entityData.get(MOUSE_SPEED_Y);
    }

    public float getMouseMoveSpeedX() {
        return entityData.get(MOUSE_SPEED_X);
    }

    // container start

    protected NonNullList<ItemStack> items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);

    protected void resizeItems() {
        int newSize = this.getContainerSize();
        int currentSize = this.items.size();

        if (newSize == currentSize) {
            return;
        }

        if (newSize > currentSize) {
            NonNullList<ItemStack> newItems = NonNullList.withSize(newSize, ItemStack.EMPTY);
            for (int i = 0; i < currentSize; i++) {
                newItems.set(i, this.items.get(i));
            }
            this.items = newItems;
        } else {
            // TODO 解决超出容量的物品没有正确保存/掉落的问题
            for (int i = newSize; i < currentSize; i++) {
                ItemStack excessStack = this.items.get(i);
                if (!excessStack.isEmpty()) {
                    this.spawnAtLocation(excessStack.copy());
                }
            }

            NonNullList<ItemStack> newItems = NonNullList.withSize(newSize, ItemStack.EMPTY);
            for (int i = 0; i < newSize; i++) {
                newItems.set(i, this.items.get(i));
            }
            this.items = newItems;
        }

        this.setChanged();
    }

    /**
     * 计算当前载具内指定物品的数量
     *
     * @param item 物品类型
     * @return 物品数量
     */
    public int countItem(@Nullable Item item) {
        if (item == null || !this.hasContainer()) return 0;
        return InventoryTool.countItem(this.items, item);
    }

    /**
     * 判断载具内是否包含指定物品
     *
     * @param item 物品类型
     */
    public boolean hasItem(Item item) {
        if (!this.hasContainer()) return false;

        return countItem(item) > 0;
    }

    /**
     * 消耗载具内指定物品
     *
     * @param item  物品类型
     * @param count 要消耗的数量
     * @return 成功消耗的物品数量
     */
    public int consumeItem(Item item, int count) {
        if (!this.hasContainer()) return 0;

        return InventoryTool.consumeItem(this.items, item, count);
    }

    /**
     * 尝试插入指定物品指定数量，如果载具内已满则生成掉落物
     *
     * @param item  物品类型
     * @param count 要插入的数量
     */
    public void insertItem(Item item, int count) {
        if (!this.hasContainer()) return;

        var rest = InventoryTool.insertItem(this.items, item, count, this.getMaxStackSize());

        if (rest > 0) {
            var stackToDrop = new ItemStack(item, rest);
            this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), stackToDrop));
        }
    }

    @Override
    public int getContainerSize() {
        return data().get(VehicleProp.VEHICLE_CONTAINER_TYPE).getSize();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) return ItemStack.EMPTY;
        return this.items.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int pAmount) {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) return ItemStack.EMPTY;

        return ContainerHelper.removeItem(this.items, slot, pAmount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) return ItemStack.EMPTY;

        ItemStack itemstack = this.items.get(slot);
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.items.set(slot, ItemStack.EMPTY);
            return itemstack;
        }
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack pStack) {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) return;

        var limit = Math.min(this.getMaxStackSize(), pStack.getMaxStackSize());
        if (!pStack.isEmpty() && pStack.getCount() > limit) {
            Mod.LOGGER.warn("try inserting ItemStack {} exceeding the maximum stack size: {}, clamped to {}", pStack.getItem(), limit, limit);
            pStack.setCount(limit);
        }
        this.items.set(slot, pStack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return this.hasContainer() && !this.isRemoved() && this.position().closerThan(pPlayer.position(), 8.0D);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    public boolean hasContainer() {
        return this.getContainerSize() > 0;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) return false;

        var currentStack = this.items.get(slot);
        if (!currentStack.isEmpty() && currentStack.getItem() != stack.getItem()) return false;

        var currentCount = currentStack.getCount();
        var stackCount = stack.getCount();
        int combinedCount = currentCount + stackCount;
        if (combinedCount > this.getMaxStackSize() || combinedCount > stack.getMaxStackSize()) return false;

        return ContainerEntity.super.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItem(@NotNull Container target, int slot, @NotNull ItemStack stack) {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) return false;
        return ContainerEntity.super.canTakeItem(target, slot, stack);
    }

    @Override
    public void remove(@NotNull RemovalReason pReason) {
        if (!this.level().isClientSide && pReason != RemovalReason.DISCARDED) {
            Containers.dropContents(this.level(), this, this);
        }
        super.remove(pReason);
    }

    @Override
    public void openCustomInventoryScreen(Player pPlayer) {
        pPlayer.openMenu(this);
        if (!pPlayer.level().isClientSide) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, pPlayer);
        }
    }

    @Override
    public ResourceKey<LootTable> getLootTable() {
        return null;
    }

    @Override
    public void setLootTable(@Nullable ResourceKey<LootTable> lootTable) {
    }

    @Override
    public long getLootTableSeed() {
        return 0;
    }

    @Override
    public void setLootTableSeed(long pLootTableSeed) {
    }

    public boolean hasMenu() {
        return data().get(VehicleProp.VEHICLE_CONTAINER_TYPE).hasMenu();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, Player pPlayer) {
        if (!pPlayer.isSpectator() && this.hasMenu()) {
            var type = data().get(VehicleProp.VEHICLE_CONTAINER_TYPE);
            var upgrade = data().get(VehicleProp.HAS_UPGRADE_SLOTS);
            var menu = switch (type) {
                case MINI ->
                        upgrade ? ModMenuTypes.VEHICLE_MENU_MINI_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_MINI.get();
                case SMALL ->
                        upgrade ? ModMenuTypes.VEHICLE_MENU_SMALL_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_SMALL.get();
                case MEDIUM ->
                        upgrade ? ModMenuTypes.VEHICLE_MENU_MEDIUM_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_MEDIUM.get();
                case LARGE ->
                        upgrade ? ModMenuTypes.VEHICLE_MENU_LARGE_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_LARGE.get();
                case HUGE ->
                        upgrade ? ModMenuTypes.VEHICLE_MENU_HUGE_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_HUGE.get();
                default -> null;
            };
            if (menu == null) return null;

            return new VehicleMenu(menu, pContainerId, pPlayerInventory, this, type.getRow(), type.getCol(), upgrade);
        }
        return null;
    }

    @Override
    public void stopOpen(@NotNull Player pPlayer) {
        this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.Context.of(pPlayer));
    }

    @Override
    public @NotNull NonNullList<ItemStack> getItemStacks() {
        return this.items;
    }

    @Override
    public void clearItemStacks() {
        this.items.clear();
    }

    // container end

    // 自定义骑乘
    private final List<Entity> orderedPassengers = generatePassengersList();

    private ArrayList<Entity> generatePassengersList() {
        var list = new ArrayList<Entity>(this.getMaxPassengers());
        for (int i = 0; i < this.getMaxPassengers(); i++) {
            list.add(null);
        }
        return list;
    }

    protected void initSeatData(int targetSize) {
        padList(orderedPassengers, targetSize, null, null);

        // 移除多余GunData
        var gunDataMap = entityData.get(GUN_DATA_MAP);
        var newMap = new HashMap<Integer, GunData>();

        for (var kv : gunDataMap.entrySet()) {
            if (kv.getKey() < targetSize) {
                newMap.put(kv.getKey(), kv.getValue());
            }
        }

        entityData.set(GUN_DATA_MAP, newMap);
    }

    protected <T> void padList(@NotNull List<T> list, int targetSize, T defaultValue, @Nullable Consumer<T> onRemove) {
        while (targetSize != list.size()) {
            if (targetSize > list.size()) {
                list.add(defaultValue);
            } else {
                var last = list.removeLast();
                if (last != null && onRemove != null) {
                    onRemove.accept(last);
                }
            }
        }
    }

    protected void checkSeatsSize() {
        int targetSize = data().get(VehicleProp.SEATS).size();
        if (targetSize == orderedPassengers.size()) return;

        initSeatData(targetSize);
    }

    /**
     * 获取按顺序排列的成员列表
     *
     * @return 按顺序排列的成员列表
     */
    public List<Entity> getOrderedPassengers() {
        checkSeatsSize();
        return orderedPassengers;
    }

    // 仅在客户端存在的实体顺序获取，用于在客户端正确同步实体座位顺序
    public Function<Entity, Integer> entityIndexOverride = null;

    @Override
    protected void addPassenger(@NotNull Entity pPassenger) {
        if (pPassenger.getVehicle() != this) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        }
        checkSeatsSize();

        int index;

        if (entityIndexOverride != null && entityIndexOverride.apply(pPassenger) != -1) {
            index = entityIndexOverride.apply(pPassenger);
        } else {
            index = 0;
            for (Entity passenger : orderedPassengers) {
                if (passenger == null) {
                    break;
                }
                index++;
            }
        }
        if (index >= getMaxPassengers() || index < 0) return;

        orderedPassengers.set(index, pPassenger);

        pPassenger.getPersistentData().putInt(TAG_SEAT_INDEX, index);

        this.passengers = ImmutableList.copyOf(orderedPassengers.stream().filter(Objects::nonNull).toList());
        this.gameEvent(GameEvent.ENTITY_MOUNT, pPassenger);
    }

    @Override
    protected void removePassenger(@NotNull Entity pPassenger) {
        if (pPassenger.getVehicle() == this) {
            throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
        }
        checkSeatsSize();

        var index = getSeatIndex(pPassenger);
        if (index == -1) return;

        orderedPassengers.set(index, null);
        this.passengers = ImmutableList.copyOf(orderedPassengers.stream().filter(Objects::nonNull).toList());

        pPassenger.boardingCooldown = 60;
        this.gameEvent(GameEvent.ENTITY_DISMOUNT, pPassenger);
    }

    public VehicleData data() {
        return VehicleData.from(this);
    }

    @Override
    public float maxUpStep() {
        return data().get(VehicleProp.UP_STEP);
    }

    @Override
    public @Nullable Entity getFirstPassenger() {
        checkSeatsSize();
        if (orderedPassengers.isEmpty()) {
            return null;
        }
        return orderedPassengers.getFirst();
    }

    /**
     * 获取第index个乘客
     *
     * @param index 目标座位
     * @return 目标座位的乘客
     */
    public @Nullable Entity getNthEntity(int index) {
        checkSeatsSize();
        if (index >= orderedPassengers.size() || index < 0) {
            return null;
        }
        return orderedPassengers.get(index);
    }

    /**
     * 尝试切换座位
     *
     * @param entity 乘客
     * @param index  目标座位
     * @return 是否切换成功
     */
    public boolean changeSeat(Entity entity, int index) {
        if (index < 0 || index >= getMaxPassengers()) return false;
        checkSeatsSize();
        if (orderedPassengers.get(index) != null) return false;
        if (!orderedPassengers.contains(entity)) return false;

        orderedPassengers.set(orderedPassengers.indexOf(entity), null);
        orderedPassengers.set(index, entity);

        entity.getPersistentData().putInt(TAG_SEAT_INDEX, index);

        // 在服务端运行时，向所有玩家同步载具座位信息
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.getPlayers(s -> true).forEach(p -> p.connection.send(new ClientboundSetPassengersPacket(this)));
        }

        return true;
    }

    /**
     * 获取乘客所在座位索引
     *
     * @param entity 乘客
     * @return 座位索引
     */
    public int getSeatIndex(Entity entity) {
        checkSeatsSize();
        return orderedPassengers.indexOf(entity);
    }

    /**
     * 获取乘客所在座位索引，用于下车时的位置判定
     * 下车前会先移除载具，因此 {@link VehicleEntity#getSeatIndex(Entity)} 会返回-1
     *
     * @param entity 乘客
     * @return 座位索引
     */
    public int getTagSeatIndex(Entity entity) {
        return entity.getPersistentData().getInt(TAG_SEAT_INDEX);
    }

    /**
     * 第三人称视角相机位置重载，返回null表示不进行修改
     *
     * @param seatIndex 座位索引
     */
    @Nullable
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int seatIndex) {
        return null;
    }

    public float getRoll() {
        return roll;
    }

    public float getRoll(float tickDelta) {
        return Mth.lerp(tickDelta, prevRoll, getRoll());
    }

    public float getYaw(float tickDelta) {
        return Mth.lerp(tickDelta, yRotO, getYRot());
    }

    public float getPitch(float tickDelta) {
        return Mth.lerp(tickDelta, xRotO, getXRot());
    }

    public void setZRot(float rot) {
        roll = rot;
    }

    public void turretTurnSound(float diffX, float diffY, float pitch) {
        if (level().isClientSide && (java.lang.Math.abs(diffY) > 0.5 || java.lang.Math.abs(diffX) > 0.5)) {
            level().playLocalSound(this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), ModSounds.TURRET_TURN.get(), this.getSoundSource(), (float) java.lang.Math.min(0.15 * (java.lang.Math.max(Mth.abs(diffX), Mth.abs(diffY))), 0.75), (random.nextFloat() * 0.05f + pitch), false);
        }
    }

    /**
     * 受击时是否出现粒子效果
     */
    public boolean shouldSendHitParticles() {
        return true;
    }

    /**
     * 受击时是否出现音效
     */
    public boolean shouldSendHitSounds() {
        return true;
    }

    public static final EntityDataAccessor<Integer> ENERGY = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    protected IEnergyStorage energyStorage = null;

    protected boolean isInitialized;

    public boolean isInitialized() {
        return this.isInitialized;
    }

    public EntityDataAccessor<Integer> getEnergyDataAccessor() {
        return ENERGY;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HEALTH, this.getMaxHealth())
                .define(OVERRIDE, "")
                .define(LAST_ATTACKER_UUID, "undefined")
                .define(LAST_DRIVER_UUID, "undefined")
                .define(GUN_DATA_MAP, new HashMap<>())

                .define(AI_TURRET_TARGET_UUID, "undefined")
                .define(AI_PASSENGER_WEAPON_TARGET_UUID, "undefined")

                .define(DELTA_ROT, 0f)
                .define(MOUSE_SPEED_X, 0f)
                .define(MOUSE_SPEED_Y, 0f)
                .define(HEAT, 0)

                .define(TURRET_HEALTH, getTurretMaxHealth())
                .define(L_WHEEL_HEALTH, getWheelMaxHealth())
                .define(R_WHEEL_HEALTH, getWheelMaxHealth())
                .define(ENGINE_HEALTH, getEngineMaxHealth())
                .define(L_ENGINE_HEALTH, getEngineMaxHealth())

                .define(TURRET_DAMAGED, false)
                .define(L_WHEEL_DAMAGED, false)
                .define(R_WHEEL_DAMAGED, false)
                .define(ENGINE1_DAMAGED, false)
                .define(ENGINE2_DAMAGED, false)

                .define(CANNON_RECOIL_TIME, 0)
                .define(POWER, 0f)
                .define(YAW, 0f)
                .define(AMMO, 0)
                .define(FIRE_ANIM, 0)
                .define(COAX_HEAT, 0)
                .define(DECOY_COUNT, 0)
                .define(GEAR_ROT, 0)
                .define(GEAR_UP, false)
                .define(FORWARD_INPUT_DOWN, false)
                .define(BACK_INPUT_DOWN, false)
                .define(LEFT_INPUT_DOWN, false)
                .define(RIGHT_INPUT_DOWN, false)
                .define(UP_INPUT_DOWN, false)
                .define(DOWN_INPUT_DOWN, false)
                .define(FIRE_INPUT_DOWN, false)
                .define(DECOY_INPUT_DOWN, false)
                .define(SPRINT_INPUT_DOWN, false)
                .define(LANDING_INPUT_DOWN, false)
                .define(PLANE_BREAK, 0f)

                // 怎么还不给玩动态注册了（恼）
                .define(SELECTED_WEAPON, IntList.of(new int[this.getMaxPassengers()]))
                .define(ENERGY, 0)
                .define(PROPELLER_ROT, 0f)

                .define(HORN_VOLUME, 0f);
    }

    // energy start

    /**
     * 消耗指定电量
     *
     * @param amount 要消耗的电量
     */
    protected void consumeEnergy(int amount) {
        if (!this.hasEnergyStorage()) {
            Mod.LOGGER.warn("Trying to consume energy of vehicle {}, but it has no energy storage", this.getName());
            return;
        }
        if (this.level() instanceof ServerLevel) {
            this.energyStorage.extractEnergy(amount, false);
        }
    }

    protected boolean canConsume(int amount) {
        if (!this.hasEnergyStorage()) {
            Mod.LOGGER.warn("Trying to check if can consume energy of vehicle {}, but it has no energy storage", this.getName());
            return false;
        }
        return this.getEnergy() >= amount;
    }

    public int getEnergy() {
        if (!this.hasEnergyStorage()) {
            Mod.LOGGER.warn("Trying to get energy of vehicle {}, but it has no energy storage", this.getName());
            return Integer.MAX_VALUE;
        }
        return this.energyStorage.getEnergyStored();
    }

    @Nullable
    public IEnergyStorage getEnergyStorage() {
        if (!this.hasEnergyStorage()) {
            Mod.LOGGER.warn("Trying to get energy storage of vehicle {}, but it has no energy storage", this.getName());
        }
        return this.energyStorage;
    }

    protected void setEnergy(int pEnergy) {
        if (!this.hasEnergyStorage()) {
            Mod.LOGGER.warn("Trying to set energy of vehicle {}, but it has no energy storage", this.getName());
            return;
        }
        int targetEnergy = Mth.clamp(pEnergy, 0, this.getMaxEnergy());

        if (targetEnergy > energyStorage.getEnergyStored()) {
            energyStorage.receiveEnergy(targetEnergy - energyStorage.getEnergyStored(), false);
        } else {
            energyStorage.extractEnergy(energyStorage.getEnergyStored() - targetEnergy, false);
        }
    }

    public int getMaxEnergy() {
        if (!this.hasEnergyStorage()) {
            Mod.LOGGER.warn("Trying to get max energy of vehicle {}, but it has no energy storage", this.getName());
            return Integer.MAX_VALUE;
        }
        return data().get(VehicleProp.MAX_ENERGY);
    }

    public boolean hasEnergyStorage() {
        return true;
    }

    // energy end

    /**
     * 初始化武器数组
     *
     * @param weaponVehicle 待初始化的载具
     * @return 武器数组
     */
    private int[] initSelectedWeaponArray(WeaponVehicleEntity weaponVehicle) {
        weaponVehicle.getAllWeapons();

        var selected = new int[this.getMaxPassengers()];
        for (int i = 0; i < this.getMaxPassengers(); i++) {
            selected[i] = weaponVehicle.hasWeapon(i) ? 0 : -1;
        }

        return selected;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.entityData.set(LAST_ATTACKER_UUID, compound.getString("LastAttacker"));
        this.entityData.set(LAST_DRIVER_UUID, compound.getString("LastDriver"));

        this.entityData.set(OVERRIDE, compound.getString("Override"));

        // GunData
        var state = compound.getCompound("WeaponState");
        var gunDataMap = new HashMap<Integer, GunData>();
        for (var key : state.getAllKeys()) {
            var tag = state.get(key);
            assert tag != null;

            ItemStack.parse(this.level().registryAccess(), tag)
                    .ifPresent(is -> gunDataMap.put(Integer.parseInt(key), GunData.from(is)));
        }
        entityData.set(GUN_DATA_MAP, gunDataMap);

        if (compound.contains("Health")) {
            this.entityData.set(HEALTH, compound.getFloat("Health"));
        } else {
            this.entityData.set(HEALTH, this.getMaxHealth());
        }

        this.entityData.set(TURRET_HEALTH, compound.getFloat("TurretHealth"));
        this.entityData.set(L_WHEEL_HEALTH, compound.getFloat("LeftWheelHealth"));
        this.entityData.set(R_WHEEL_HEALTH, compound.getFloat("RightWheelHealth"));
        this.entityData.set(ENGINE_HEALTH, compound.getFloat("EngineHealth"));
        this.entityData.set(L_ENGINE_HEALTH, compound.getFloat("LeftEngineHealth"));

        this.entityData.set(TURRET_DAMAGED, compound.getBoolean("TurretDamaged"));
        this.entityData.set(L_WHEEL_DAMAGED, compound.getBoolean("LeftDamaged"));
        this.entityData.set(R_WHEEL_DAMAGED, compound.getBoolean("RightDamaged"));
        this.entityData.set(ENGINE1_DAMAGED, compound.getBoolean("Engine1Damaged"));
        this.entityData.set(ENGINE2_DAMAGED, compound.getBoolean("Engine2Damaged"));

        this.entityData.set(POWER, compound.getFloat("Power"));
        this.entityData.set(DECOY_COUNT, compound.getInt("DecoyCount"));
        this.entityData.set(GEAR_ROT, compound.getInt("GearRot"));
        this.entityData.set(GEAR_UP, compound.getBoolean("GearUp"));
        this.entityData.set(PROPELLER_ROT, compound.getFloat("PropellerRot"));

        if (this instanceof WeaponVehicleEntity weaponVehicle && weaponVehicle.getAllWeapons().length > 0) {
            var selected = compound.getIntArray("SelectedWeapon");

            if (selected.length != this.getMaxPassengers()) {
                // 数量不符时（可能是更新或遇到损坏数据），重新初始化已选择武器
                this.entityData.set(SELECTED_WEAPON, IntList.of(initSelectedWeaponArray(weaponVehicle)));
            } else {
                this.entityData.set(SELECTED_WEAPON, IntList.of(selected));
            }
        }

        if (this.hasEnergyStorage() && compound.get("Energy") instanceof IntTag energyNBT) {
            ((SyncedEntityEnergyStorage) energyStorage).deserializeNBT(level().registryAccess(), energyNBT);
        }

        this.resizeItems();
        ContainerHelper.loadAllItems(compound, this.getItemStacks(), this.level().registryAccess());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        checkSeatsSize();

        compound.putFloat("Health", this.entityData.get(HEALTH));
        compound.putString("Override", this.entityData.get(OVERRIDE));
        compound.putString("LastAttacker", this.entityData.get(LAST_ATTACKER_UUID));
        compound.putString("LastDriver", this.entityData.get(LAST_DRIVER_UUID));

        var gunDataMap = entityData.get(GUN_DATA_MAP);
        var tag = new CompoundTag();
        for (var kv : gunDataMap.entrySet()) {
            tag.put(String.valueOf(kv.getKey()), kv.getValue().stack.save(this.level().registryAccess()));
        }
        compound.put("WeaponState", tag);

        compound.putFloat("TurretHealth", this.entityData.get(TURRET_HEALTH));
        compound.putFloat("LeftWheelHealth", this.entityData.get(L_WHEEL_HEALTH));
        compound.putFloat("RightWheelHealth", this.entityData.get(R_WHEEL_HEALTH));
        compound.putFloat("EngineHealth", this.entityData.get(ENGINE_HEALTH));
        compound.putFloat("LeftEngineHealth", this.entityData.get(L_ENGINE_HEALTH));

        compound.putBoolean("TurretDamaged", this.entityData.get(TURRET_DAMAGED));
        compound.putBoolean("LeftWheelDamaged", this.entityData.get(L_WHEEL_DAMAGED));
        compound.putBoolean("RightWheelDamaged", this.entityData.get(R_WHEEL_DAMAGED));
        compound.putBoolean("Engine1Damaged", this.entityData.get(ENGINE1_DAMAGED));
        compound.putBoolean("Engine2Damaged", this.entityData.get(ENGINE2_DAMAGED));

        compound.putFloat("Power", this.entityData.get(POWER));
        compound.putInt("DecoyCount", this.entityData.get(DECOY_COUNT));
        compound.putInt("GearRot", this.entityData.get(GEAR_ROT));
        compound.putBoolean("GearUp", this.entityData.get(GEAR_UP));
        compound.putFloat("PropellerRot", this.entityData.get(PROPELLER_ROT));

        if (this instanceof WeaponVehicleEntity weaponVehicle && weaponVehicle.getAllWeapons().length > 0) {
            compound.putIntArray("SelectedWeapon", this.entityData.get(SELECTED_WEAPON));
        }

        if (this.hasEnergyStorage()) {
            compound.put("Energy", ((SyncedEntityEnergyStorage) energyStorage).serializeNBT(level().registryAccess()));
        }

        this.resizeItems();
        ContainerHelper.saveAllItems(compound, this.getItemStacks(), this.level().registryAccess());
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        if (player.getVehicle() == this) return InteractionResult.PASS;

        if (this.hasMenu() && player.isShiftKeyDown() && !player.getMainHandItem().is(ModTags.Items.TOOLS_CROWBAR)) {
            player.openMenu(this);
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }


        if (player.getVehicle() == this) return InteractionResult.PASS;

        ItemStack stack = player.getMainHandItem();

        if (stack.is(ModItems.VEHICLE_DAMAGE_ANALYZER.get())) {
            if (!level().isClientSide) {
                if (this.damageDebugResultReceiver != null) {
                    this.damageDebugResultReceiver = null;
                    player.displayClientMessage(Component.translatable("des.superbwarfare.vehicle_damage_analyzer.unbind", this.getDisplayName()), true);
                } else {
                    this.damageDebugResultReceiver = player;
                    player.displayClientMessage(Component.translatable("des.superbwarfare.vehicle_damage_analyzer.bind", this.getDisplayName()), true);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown() && stack.is(ModTags.Items.TOOLS_CROWBAR) && this.getPassengers().isEmpty()) {
            for (var item : getRetrieveItems()) {
                ItemHandlerHelper.giveItemToPlayer(player, item);
            }
            this.remove(RemovalReason.DISCARDED);
            this.discard();
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown() && this.getMaxPassengers() > 0) {
            List<Entity> entities = getPassengers();
            for (var passenger : entities) {
                if (passenger.getTeam() != null && (TDMSavedData.enabledTDM(passenger) || passenger.getTeam() != player.getTeam())) {
                    return InteractionResult.PASS;
                }
            }

            Entity lastDriver = EntityFindUtil.findEntity(level(), entityData.get(LAST_DRIVER_UUID));
            if (lastDriver != null && !SeekTool.IN_SAME_TEAM.test(player, lastDriver) && lastDriver.getTeam() != null) {
                return InteractionResult.PASS;
            }

            if (this.getFirstPassenger() == null) {
                if (player instanceof FakePlayer) return InteractionResult.PASS;
                setDriverAngle(player);
                player.setSprinting(false);
                return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
            } else if (!(this.getFirstPassenger() instanceof Player)) {
                if (player instanceof FakePlayer) return InteractionResult.PASS;
                this.getFirstPassenger().stopRiding();
                setDriverAngle(player);
                player.setSprinting(false);
                return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
            }
            if (this.canAddPassenger(player)) {
                if (player instanceof FakePlayer) return InteractionResult.PASS;
                player.setSprinting(false);
                return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * 将有炮塔的载具驾驶员的面朝方向设置为炮塔角度
     *
     * @param player 载具驾驶员
     */
    public void setDriverAngle(Player player) {
        if (hasTurret()) {
            player.xRotO = -(float) getXRotFromVector(getBarrelVector(1));
            player.setXRot(-(float) getXRotFromVector(getBarrelVector(1)));
            player.yRotO = -(float) getYRotFromVector(getBarrelVector(1));
            player.setYRot(-(float) getYRotFromVector(getBarrelVector(1)));
            player.setYHeadRot(-(float) getYRotFromVector(getBarrelVector(1)));
        } else {
            player.xRotO = this.getXRot();
            player.setXRot(this.getXRot());
            player.yRotO = this.getYRot();
            player.setYRot(this.getYRot());
        }
    }

    public static double getYRotFromVector(Vec3 vec3) {
        return Mth.atan2(vec3.x, vec3.z) * (180F / Math.PI);
    }

    public static double getXRotFromVector(Vec3 vec3) {
        double d0 = vec3.horizontalDistance();
        return Mth.atan2(vec3.y, d0) * (180F / Math.PI);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH) || source.is(DamageTypes.IN_WALL))
            return false;

        if (DamageTypeTool.isGunDamage(source) && source.getEntity() != null && source.getEntity().getVehicle() == this) {
            return false;
        }

        if (source.getEntity() != null
                && getFirstPassenger() != null
                && SeekTool.IS_FRIENDLY.test(source.getEntity(), source.getEntity())
                && getFirstPassenger().getTeam() != null
                && source.getEntity().getTeam() != null
                && source.getEntity().getTeam() == getFirstPassenger().getTeam()
                && !source.getEntity().getTeam().isAllowFriendlyFire()) {
            return false;
        }

        if (this.damageDebugResultReceiver != null) {
            this.damageDebugResultReceiver.sendSystemMessage(DamageHandler.getDamageInfo(this, source, amount));
        }

        // 计算减伤后的伤害
        float computedAmount = amount;
        if (!source.is(ModTags.DamageTypes.BYPASSES_VEHICLE)) {
            computedAmount = this.getDamageModifier().compute(source, amount);
        }

        this.crash = source.is(ModDamageTypes.VEHICLE_STRIKE);

        if (source.getEntity() != null) {
            this.entityData.set(LAST_ATTACKER_UUID, source.getEntity().getStringUUID());
        }

        if (source.getDirectEntity() instanceof Projectile projectile && this instanceof OBBEntity) {
            OBBHitter accessor = OBBHitter.getInstance(projectile);
            var part = accessor.sbw$getCurrentHitPart();

            if (part != null) {
                switch (part) {
                    case TURRET -> entityData.set(TURRET_HEALTH, entityData.get(TURRET_HEALTH) - computedAmount);
                    case WHEEL_LEFT -> entityData.set(L_WHEEL_HEALTH, entityData.get(L_WHEEL_HEALTH) - computedAmount);
                    case WHEEL_RIGHT -> entityData.set(R_WHEEL_HEALTH, entityData.get(R_WHEEL_HEALTH) - computedAmount);
                    case ENGINE1 -> entityData.set(ENGINE_HEALTH, entityData.get(ENGINE_HEALTH) - computedAmount);
                    case ENGINE2 -> entityData.set(L_ENGINE_HEALTH, entityData.get(L_ENGINE_HEALTH) - computedAmount);
                }
            }
        }

        this.onHurt(computedAmount, source.getEntity(), true);
        return super.hurt(source, computedAmount);
    }

    /**
     * 控制载具伤害免疫
     *
     * @return DamageModifier
     */
    public DamageModifier getDamageModifier() {
        return data().damageModifier();
    }

    public float getSourceAngle(DamageSource source, float multiply) {
        Entity attacker = source.getDirectEntity();
        if (attacker == null) {
            attacker = source.getEntity();
        }

        if (attacker != null) {
            Vec3 toVec = new Vec3(getX(), getY() + getBbHeight() / 2, getZ()).vectorTo(attacker.position()).normalize();
            return (float) Math.max(1f - multiply * toVec.dot(getViewVector(1)), 0.5f);
        }
        return 1;
    }

    public void heal(float pHealAmount) {
        if (this.level() instanceof ServerLevel) {
            this.setHealth(this.getHealth() + pHealAmount);
        }
    }

    public void onHurt(float pHealAmount, Entity attacker, boolean send) {
        if (this.level() instanceof ServerLevel) {
            var holder = Holder.direct(ModSounds.INDICATION_VEHICLE.get());
            if (attacker instanceof ServerPlayer player && pHealAmount > 0 && this.getHealth() > 0 && send && !(this instanceof DroneEntity)) {
                player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getEyeY(), player.getZ(), 0.25f + (2.75f * pHealAmount / getMaxHealth()), random.nextFloat() * 0.1f + 0.9f, player.level().random.nextLong()));
                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(3, 5));
            }

            if (pHealAmount > 0 && this.getHealth() > 0 && send) {
                repairCoolDown = maxRepairCoolDown();
                List<Entity> passengers = this.getPassengers();
                for (var entity : passengers) {
                    if (entity instanceof ServerPlayer player1) {
                        player1.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player1.getX(), player1.getEyeY(), player1.getZ(), 0.25f + (4.75f * pHealAmount / getMaxHealth()), random.nextFloat() * 0.1f + 0.6f, player1.level().random.nextLong()));
                    }
                }
            }

            this.setHealth(this.getHealth() - pHealAmount);
        }
    }

    public float getHealth() {
        return this.entityData.get(HEALTH);
    }

    public void setHealth(float pHealth) {
        this.entityData.set(HEALTH, Mth.clamp(pHealth, 0.0F, this.getMaxHealth()));
    }

    public float getMaxHealth() {
        return data().get(VehicleProp.MAX_HEALTH);
    }

    public float getTurretMaxHealth() {
        return 50;
    }

    public float getWheelMaxHealth() {
        return 50;
    }

    public float getEngineMaxHealth() {
        return 50;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return super.isPushable();
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean skipAttackInteraction(@NotNull Entity attacker) {
        return hasPassenger(attacker) || super.skipAttackInteraction(attacker);
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity pPassenger) {
        return this.getPassengers().size() < this.getMaxPassengers();
    }

    public int getMaxPassengers() {
        return data().get(VehicleProp.SEATS).size();
    }

    public static double getSubmergedHeight(Entity entity) {
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            var type = fluid.getFluidType();
            if (entity.level().getFluidState(entity.blockPosition()).getFluidType() == type)
                return entity.getFluidTypeHeight(type);
        }
        return 0;
    }

    /**
     * 呼吸回血冷却时长(单位:tick)，设为小于0的值以禁用呼吸回血
     */
    public int maxRepairCoolDown() {
        return data().get(VehicleProp.REPAIR_COOLDOWN);
    }

    /**
     * 呼吸回血回血量
     */
    public float repairAmount() {
        return data().get(VehicleProp.REPAIR_AMOUNT);
    }

    @Override
    public void baseTick() {
        if (this.level().isClientSide) {
            if (!this.wasEngineRunning && this.engineRunning()) {
                engineSound.accept(this);
                swimSound.accept(this);
                if (this.hasTracks()) {
                    trackSound.accept(this);
                }
            }

            if (!this.wasHornWorking && this.hornWorking()) {
                hornSound.accept(this);
            }

            if (!this.wasInCarMusicPlaying && this.inCarMusicPlaying()) {
                inCarMusic.accept(this);
            }
        }

        var newMap = new HashMap<Integer, GunData>();
        for (var kv : entityData.get(GUN_DATA_MAP).entrySet()) {
            var newData = kv.getValue().copy();
            newData.tick(this, true);
            newMap.put(kv.getKey(), newData);
        }
        entityData.set(GUN_DATA_MAP, newMap);

        this.wasEngineRunning = this.engineRunning();
        this.wasHornWorking = this.hornWorking();
        this.wasInCarMusicPlaying = this.inCarMusicPlaying();

        turretYRotO = this.getTurretYRot();
        turretXRotO = this.getTurretXRot();

        gunYRotO = this.getGunYRot();
        gunXRotO = this.getGunXRot();

        leftWheelRotO = this.getLeftWheelRot();
        rightWheelRotO = this.getRightWheelRot();

        leftTrackO = this.getLeftTrack();
        rightTrackO = this.getRightTrack();

        rotorRotO = this.getRotorRot();

        rudderRotO = this.getRudderRot();

        propellerRotO = this.getPropellerRot();

        recoilShakeO = this.getRecoilShake();

        velocityO = this.getVelocity();

        lastTickSpeed = new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y + 0.06, this.getDeltaMovement().z).length();
        lastTickVerticalSpeed = this.getDeltaMovement().y + 0.06;
        if (collisionCoolDown > 0) {
            collisionCoolDown--;
        }

        flap1LRotO = this.getFlap1LRot();
        flap1RRotO = this.getFlap1RRot();
        flap1L2RotO = this.getFlap1L2Rot();
        flap1R2RotO = this.getFlap1R2Rot();
        flap2LRotO = this.getFlap2LRot();
        flap2RRotO = this.getFlap2RRot();
        flap3RotO = this.getFlap3Rot();
        gearRotO = entityData.get(GEAR_ROT);

        super.baseTick();

        if (repairCoolDown > 0) {
            repairCoolDown--;
        }

        if (getHealth() >= getMaxHealth()) {
            repairCoolDown = maxRepairCoolDown();
        }

        if (this.entityData.get(HEAT) > 0) {
            this.entityData.set(HEAT, this.entityData.get(HEAT) - 1);
        }

        if (this.entityData.get(HEAT) < 40) {
            cannotFire = false;
        }

        if (this.entityData.get(HEAT) > 100 && !cannotFire) {
            cannotFire = true;
            this.level().playSound(null, this.getOnPos(), ModSounds.MINIGUN_OVERHEAT.get(), SoundSource.PLAYERS, 1, 1);
        }

        this.prevRoll = this.getRoll();

        float delta = Math.abs(getYRot() - yRotO);
        while (getYRot() > 180F) {
            setYRot(getYRot() - 360F);
            yRotO = getYRot() - delta;
        }
        while (getYRot() <= -180F) {
            setYRot(getYRot() + 360F);
            yRotO = delta + getYRot();
        }

        float deltaX = Math.abs(getXRot() - xRotO);
        while (getXRot() > 180F) {
            setXRot(getXRot() - 360F);
            xRotO = getXRot() - deltaX;
        }
        while (getXRot() <= -180F) {
            setXRot(getXRot() + 360F);
            xRotO = deltaX + getXRot();
        }

        float deltaZ = Math.abs(getRoll() - prevRoll);
        while (getRoll() > 180F) {
            setZRot(getRoll() - 360F);
            prevRoll = getRoll() - deltaZ;
        }
        while (getRoll() <= -180F) {
            setZRot(getRoll() + 360F);
            prevRoll = deltaZ + getRoll();
        }

        this.handleClientSync();

        if (this.level() instanceof ServerLevel && this.getHealth() <= 0) {
            destroy();
        }

        this.travel();

        Entity attacker = EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_ATTACKER_UUID));

        var data = data();
        if (this.getHealth() <= data.get(VehicleProp.SELF_HURT_PERCENT) * this.getMaxHealth()) {
            // 血量过低时自动扣血
            this.onHurt(data.get(VehicleProp.SELF_HURT_AMOUNT), attacker, false);
        } else {
            // 呼吸回血
            if (repairCoolDown == 0) {
                this.heal(repairAmount());
            }
        }

        if (this.getMaxPassengers() > 0 && getFirstPassenger() != null) {
            this.entityData.set(LAST_DRIVER_UUID, getFirstPassenger().getStringUUID());
        }

        if (getPassengers().isEmpty()) {
            noPassengerTime++;
            if (noPassengerTime > 200) {
                this.entityData.set(LAST_DRIVER_UUID, "undefined");
            }
        } else {
            noPassengerTime = 0;
        }

        this.clearArrow();

        if (this instanceof OBBEntity obbEntity) {
            this.handlePartDamaged(obbEntity);

            // 处理部件血量
            this.handlePartHealth();
        }

        entityData.set(MOUSE_SPEED_X, getMouseMoveSpeedX() * 0.95f);
        entityData.set(MOUSE_SPEED_Y, getMouseMoveSpeedY() * 0.95f);

        if (hasTurret()) {
            if (getNthEntity(mainWeaponControllerIndex()) instanceof Player) {
                turretAngle();
            } else if (getNthEntity(mainWeaponControllerIndex()) instanceof Mob mob) {
                turretAutoAimFormUuid(entityData.get(AI_TURRET_TARGET_UUID), mob);
            }
        }

        if (turretHasPassengerWeapon()) {
            if (getNthEntity(secondWeaponControllerIndex()) instanceof Player || getNthEntity(secondWeaponControllerIndex()) == null) {
                gunnerAngle();
            } else if (getNthEntity(secondWeaponControllerIndex()) instanceof Mob mob) {
                passengerWeaponAutoAimFormUuid(entityData.get(AI_PASSENGER_WEAPON_TARGET_UUID), mob);
            }
        }

        // 获取当前速度（deltaMovement 是当前速度向量）
        Vec3 currentVelocity = this.getDeltaMovement();

        // 计算加速度向量（时间间隔 Δt = 0.05秒）
        Vec3 accelerationVec = currentVelocity.subtract(previousVelocity).scale(20); // scale(1/0.05) = scale(20)

        // 计算加速度的绝对值
        acceleration = accelerationVec.length() * 20;

        // 更新前一时刻的速度
        previousVelocity = currentVelocity;

        double direct = (90 - calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90;
        setVelocity(Mth.lerp(0.4, getVelocity(), getDeltaMovement().horizontalDistance() * direct * 20));

        float deltaT = java.lang.Math.abs(getTurretYRot() - turretYRotO);
        while (getTurretYRot() > 180F) {
            setTurretYRot(getTurretYRot() - 360F);
            turretYRotO = getTurretYRot() - deltaT;
        }
        while (getTurretYRot() <= -180F) {
            setTurretYRot(getTurretYRot() + 360F);
            turretYRotO = deltaT + getTurretYRot();
        }

        if (this.entityData.get(COAX_HEAT) > 0) {
            this.entityData.set(COAX_HEAT, this.entityData.get(COAX_HEAT) - 1);
        }

        if (this.entityData.get(FIRE_ANIM) > 0) {
            this.entityData.set(FIRE_ANIM, this.entityData.get(FIRE_ANIM) - 1);
        }

        if (this.entityData.get(COAX_HEAT) < 40) {
            cannotFireCoax = false;
        }

        if (decoyReloadCoolDown > 0) {
            decoyReloadCoolDown--;
        }

        if (this.entityData.get(COAX_HEAT) > 100) {
            cannotFireCoax = true;
            this.level().playSound(null, this.getOnPos(), ModSounds.MINIGUN_OVERHEAT.get(), SoundSource.PLAYERS, 1, 1);
        }

        if (this.entityData.get(CANNON_RECOIL_TIME) > 0) {
            this.entityData.set(CANNON_RECOIL_TIME, this.entityData.get(CANNON_RECOIL_TIME) - 1);
        }

        this.setRecoilShake(java.lang.Math.pow(entityData.get(CANNON_RECOIL_TIME), 4) * 0.0000007 * java.lang.Math.sin(0.2 * java.lang.Math.PI * (entityData.get(CANNON_RECOIL_TIME) - 2.5)));

        preventStacking();
        crushEntities(this.getDeltaMovement());

        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.06, 0.0));

        this.move(MoverType.SELF, this.getDeltaMovement());
        collideSoftBlock();
        if (canCollideHardBlock()) {
            collideHardBlock();
        }

        if (canCollideBlockBeastly()) {
            collideBlockBeastly();
        }

        collideNormalBlock();

        moveOnDragonTeeth();

        if (this.hasEnergyStorage() && this.tickCount % 20 == 0) {
            for (var stack : this.getItemStacks()) {
                int neededEnergy = this.getMaxEnergy() - this.getEnergy();
                if (neededEnergy <= 0) break;

                var energyCap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
                if (energyCap == null) continue;

                var stored = energyCap.getEnergyStored();
                if (stored <= 0) continue;

                int energyToExtract = Math.min(stored, neededEnergy);
                energyCap.extractEnergy(energyToExtract, false);
                this.setEnergy(this.getEnergy() + energyToExtract);
            }
        }

        entityData.set(HORN_VOLUME, entityData.get(HORN_VOLUME) * 0.5f);

        this.refreshDimensions();
    }

    public void handlePartDamaged(OBBEntity obbEntity) {
        var obbList = obbEntity.getOBBs();
        for (var obb : obbList) {
            Vec3 pos = new Vec3(obb.center());
            switch (obb.part()) {
                case TURRET -> {
                    if (entityData.get(TURRET_DAMAGED)) {
                        this.onTurretDamaged(pos);
                    }
                }
                case WHEEL_LEFT -> {
                    if (entityData.get(L_WHEEL_DAMAGED)) {
                        this.onLeftWheelDamaged(pos);
                    }
                }
                case WHEEL_RIGHT -> {
                    if (entityData.get(R_WHEEL_DAMAGED)) {
                        this.onRightWheelDamaged(pos);
                    }
                }
                case ENGINE1 -> {
                    if (entityData.get(ENGINE1_DAMAGED)) {
                        this.onEngine1Damaged(pos);
                    }
                }
                case ENGINE2 -> {
                    if (entityData.get(ENGINE2_DAMAGED)) {
                        this.onEngine2Damaged(pos);
                    }
                }
            }
        }
    }

    public void handlePartHealth() {
        if (entityData.get(TURRET_HEALTH) < 0) {
            entityData.set(TURRET_DAMAGED, true);
        } else if (entityData.get(TURRET_HEALTH) > 0.95 * getTurretMaxHealth()) {
            entityData.set(TURRET_DAMAGED, false);
        }

        if (entityData.get(L_WHEEL_HEALTH) < 0) {
            entityData.set(L_WHEEL_DAMAGED, true);
        } else if (entityData.get(L_WHEEL_HEALTH) > 0.95 * getWheelMaxHealth()) {
            entityData.set(L_WHEEL_DAMAGED, false);
        }

        if (entityData.get(R_WHEEL_HEALTH) < 0) {
            entityData.set(R_WHEEL_DAMAGED, true);
        } else if (entityData.get(R_WHEEL_HEALTH) > 0.95 * getWheelMaxHealth()) {
            entityData.set(R_WHEEL_DAMAGED, false);
        }

        if (entityData.get(ENGINE_HEALTH) < 0) {
            entityData.set(ENGINE1_DAMAGED, true);
        } else if (entityData.get(ENGINE_HEALTH) > 0.95 * getEngineMaxHealth()) {
            entityData.set(ENGINE1_DAMAGED, false);
        }

        if (entityData.get(L_ENGINE_HEALTH) < 0) {
            entityData.set(ENGINE2_DAMAGED, true);
        } else if (entityData.get(L_ENGINE_HEALTH) > 0.95 * getEngineMaxHealth()) {
            entityData.set(ENGINE2_DAMAGED, false);
        }

        entityData.set(TURRET_HEALTH, Math.min(entityData.get(TURRET_HEALTH) + 0.0025f * getTurretMaxHealth(), getTurretMaxHealth()));
        entityData.set(L_WHEEL_HEALTH, Math.min(entityData.get(L_WHEEL_HEALTH) + 0.0025f * getWheelMaxHealth(), getWheelMaxHealth()));
        entityData.set(R_WHEEL_HEALTH, Math.min(entityData.get(R_WHEEL_HEALTH) + 0.0025f * getWheelMaxHealth(), getWheelMaxHealth()));
        entityData.set(ENGINE_HEALTH, Math.min(entityData.get(ENGINE_HEALTH) + 0.0025f * getEngineMaxHealth(), getEngineMaxHealth()));
        entityData.set(L_ENGINE_HEALTH, Math.min(entityData.get(L_ENGINE_HEALTH) + 0.0025f * getEngineMaxHealth(), getEngineMaxHealth()));
    }

    public void addRandomParticle(ParticleOptions particleOptions, Vec3 pos, float randomPos, Level level, float speed, int count) {
        float randomX = 2 * (this.random.nextFloat() - 0.5f);
        float randomY = 2 * (this.random.nextFloat() - 0.5f);
        float randomZ = 2 * (this.random.nextFloat() - 0.5f);
        for (double i = 0; i < count; i++) {
            level.addAlwaysVisibleParticle(particleOptions, true, pos.x + randomPos * randomX, pos.y + randomPos * randomY, pos.z + randomPos * randomZ, randomX * speed, randomY * speed, randomZ * speed);
        }
    }

    public void defaultPartDamageEffect(Vec3 pos) {
        if (level().isClientSide) {
            addRandomParticle(ModParticleTypes.FIRE_STAR.get(), pos, 0, level(), 0.25f, 5);
            addRandomParticle(ParticleTypes.LARGE_SMOKE, pos, 0.5f, level(), 0.001f, 1);
        }
    }

    public void onTurretDamaged(Vec3 pos) {
        this.defaultPartDamageEffect(pos);
    }

    public void onLeftWheelDamaged(Vec3 pos) {
        this.defaultPartDamageEffect(pos);
    }

    public void onRightWheelDamaged(Vec3 pos) {
        this.defaultPartDamageEffect(pos);
    }

    public void onEngine1Damaged(Vec3 pos) {
        this.defaultPartDamageEffect(pos);
    }

    public void onEngine2Damaged(Vec3 pos) {
        this.defaultPartDamageEffect(pos);
    }

    public void clearArrow() {
        List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0F, 0.5F, 0F));
        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (entity instanceof AbstractArrow) {
                    entity.discard();
                }
            }
        }
    }

    public void lowHealthWarning() {
        if (this.getHealth() <= 0.4 * this.getMaxHealth()) {
            addRandomParticle(ParticleTypes.LARGE_SMOKE, new Vec3(this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ()), 0.35f * this.getBbWidth(), level(), 0.01f, 1);
        }

        if (this.getHealth() <= 0.25 * this.getMaxHealth()) {
            playLowHealthParticle();
        }
        if (this.getHealth() <= 0.15 * this.getMaxHealth()) {
            playLowHealthParticle();
        }

        if (this.getHealth() <= 0.1 * this.getMaxHealth()) {
            if (level().isClientSide) {
                float random = 2 * (this.random.nextFloat() - 0.5f);
                addRandomParticle(ParticleTypes.LARGE_SMOKE, new Vec3(this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ()), 0.35f * this.getBbWidth(), level(), 0.01f, 2);
                addRandomParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, new Vec3(this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ()), 0.35f * this.getBbWidth(), level(), 0.01f, 2);
                addRandomParticle(new CustomCloudOption(1f, 0.1f, 0, (int) (240 + 40 * random), 2.5f + 0.5f * random, -0.07f, true, true), new Vec3(this.getX(), this.getY() + 0.85f * getBbHeight(), this.getZ()), 0.35f * this.getBbWidth(), level(), 0.01f, 1);
                addRandomParticle(new CustomCloudOption(1f, 0.35f, 0, (int) (80 + 40 * random), 1.5f + 0.5f * random, -0.07f, false, true), new Vec3(this.getX(), this.getY() + 0.85f * getBbHeight(), this.getZ()), 0.3f * this.getBbWidth(), level(), 0.01f, 1);
            }
            if (this.tickCount % 15 == 0) {
                this.level().playSound(null, this.getOnPos(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1, 1);
            }
        }

        if (this.getHealth() < 0.1f * this.getMaxHealth() && tickCount % 13 == 0) {
            this.level().playSound(null, this.getOnPos(), ModSounds.NO_HEALTH.get(), SoundSource.PLAYERS, 1, 1);
        } else if (this.getHealth() >= 0.1f && this.getHealth() < 0.4f * this.getMaxHealth() && tickCount % 10 == 0) {
            this.level().playSound(null, this.getOnPos(), ModSounds.LOW_HEALTH.get(), SoundSource.PLAYERS, 1, 1);
        }
    }

    public void playLowHealthParticle() {
        if (level().isClientSide) {
            addRandomParticle(ParticleTypes.LARGE_SMOKE, new Vec3(this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ()), 0.35f * this.getBbWidth(), level(), 0.01f, 1);
            addRandomParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, new Vec3(this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ()), 0.35f * this.getBbWidth(), level(), 0.01f, 1);
        }
    }

    public void turretAngle() {
        float ySpeed = turretYSpeed();
        float xSpeed = turretXSpeed();
        Entity driver = getNthEntity(mainWeaponControllerIndex());
        if (driver != null) {
            float turretAngle = -Mth.wrapDegrees(driver.getYHeadRot() - this.getYRot());

            float diffY = Mth.wrapDegrees(turretAngle - getTurretYRot());
            float diffX = Mth.wrapDegrees(driver.getXRot() - this.getTurretXRot());

            this.turretTurnSound(diffX, diffY, 0.95f);

            if (entityData.get(TURRET_DAMAGED)) {
                ySpeed *= 0.2f;
                xSpeed *= 0.2f;
            }

            float min = -ySpeed + (float) (isInWater() && !onGround() ? 2.5 : 6) * entityData.get(DELTA_ROT);
            float max = ySpeed + (float) (isInWater() && !onGround() ? 2.5 : 6) * entityData.get(DELTA_ROT);

            this.setTurretXRot(Mth.clamp(this.getTurretXRot() + Mth.clamp(0.95f * diffX, -xSpeed, xSpeed), -89.5f, 89.5f));
            this.setTurretYRot(this.getTurretYRot() + Mth.clamp(0.9f * diffY, min, max));
            turretYRotLock = Mth.clamp(0.9f * diffY, min, max);
        } else {
            turretYRotLock = 0;
        }
    }

    public void aiTurretShoot(LivingEntity living) {
        if (this instanceof WeaponVehicleEntity weaponVehicle && aiTurretDiff < 2 && weaponVehicle.canShoot(living) && living.level() instanceof ServerLevel) {
            weaponVehicle.vehicleShoot(living, 0);
        }
    }

    public void aiPassengerWeaponShoot(LivingEntity living) {
        if (this instanceof WeaponVehicleEntity weaponVehicle && aiPassengerDiff < 2 && weaponVehicle.canShoot(living) && living.level() instanceof ServerLevel) {
            weaponVehicle.vehicleShoot(living, 1);
        }
    }

    /**
     * 根据方向向量，使炮塔自动瞄准
     *
     * @param shootVec 需要让炮塔以这个角度发射的向量
     */
    public void turretAutoAimFormVector(Vec3 shootVec) {
        float ySpeed = turretYSpeed();
        float xSpeed = turretXSpeed();
        float diffY = (float) Mth.wrapDegrees(-getYRotFromVector(shootVec) + getYRotFromVector(getBarrelVector(1)));
        float diffX = (float) Mth.wrapDegrees(-getXRotFromVector(shootVec) + getXRotFromVector(getBarrelVector(1)));

        this.turretTurnSound(diffX, diffY, 0.95f);

        if (entityData.get(TURRET_DAMAGED)) {
            ySpeed *= 0.2f;
            xSpeed *= 0.2f;
        }

        float min = -ySpeed + (float) (isInWater() && !onGround() ? 2.5 : 6) * entityData.get(DELTA_ROT);
        float max = ySpeed + (float) (isInWater() && !onGround() ? 2.5 : 6) * entityData.get(DELTA_ROT);

        this.setTurretXRot(Mth.clamp(this.getTurretXRot() + Mth.clamp(0.5f * diffX, -xSpeed, xSpeed), -turretMaxPitch(), -turretMinPitch()));
        this.setTurretYRot(this.getTurretYRot() - Mth.clamp(0.5f * diffY, min, max));
        turretYRotLock = Mth.clamp(0.9f * diffY, min, max);
        aiTurretDiff = VectorTool.calculateAngle(shootVec, getBarrelVector(1));
    }

    /**
     * 根据UUID，使炮塔自动瞄准
     *
     * @param uuid    目标的UUID字符串
     * @param pLiving 操控载具的实体
     */
    public void turretAutoAimFormUuid(String uuid, LivingEntity pLiving) {
        Entity target = EntityFindUtil.findEntity(level(), uuid);
        if (target != null) {
            if (target.getVehicle() != null) {
                target = target.getVehicle();
            }

            Vec3 targetPos = target.getBoundingBox().getCenter();
            Vec3 targetVel = target.getDeltaMovement();

            if (target instanceof LivingEntity living) {
                double gravity = living.getAttributeValue(Attributes.GRAVITY);
                targetVel = targetVel.add(0, gravity, 0);
            }

            if (target instanceof Player) {
                targetVel = targetVel.multiply(2, 1, 2);
            }

            Vec3 targetVec = RangeTool.calculateFiringSolution(getShootPos(pLiving, 1), targetPos, targetVel, projectileVelocity(pLiving), projectileGravity(pLiving));
            turretAutoAimFormVector(targetVec);

            if (this instanceof WeaponVehicleEntity weaponVehicle) {
                int rpm = 20 / Mth.clamp((weaponVehicle.mainGunRpm(pLiving) / 60), 1, 2147483647);
                if (tickCount % rpm == 0) {
                    aiTurretShoot(pLiving);
                }
            }
        }
    }

    /**
     * @return 炮塔最大水平旋转速度
     */
    public float turretYSpeed() {
        return 5;
    }

    /**
     * @return 炮塔最大俯仰旋转速度
     */
    public float turretXSpeed() {
        return 5;
    }

    /**
     * @return 炮塔最小俯角
     */
    public float turretMinPitch() {
        return -10;
    }

    /**
     * @return 炮塔最大仰角
     */
    public float turretMaxPitch() {
        return 30;
    }

    public void passengerPitch(Entity entity, float minPitch, float maxPitch, float passengerRot) {
        if (passengerRot != 180) {
            float a = -passengerRot;
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

            float min = -maxPitch - r * getXRot() - r2 * getRoll();
            float max = -minPitch - r * getXRot() - r2 * getRoll();

            float f = Mth.wrapDegrees(entity.getXRot());
            float f1 = Mth.clamp(f, min, max);
            entity.xRotO += f1 - f;
            entity.setXRot(entity.getXRot() + f1 - f);
        } else {
            float min = minPitch + getXRot();
            float max = maxPitch + getXRot();

            float f = Mth.wrapDegrees(entity.getXRot());
            float f1 = Mth.clamp(f, min, max);
            entity.xRotO += f1 - f;
            entity.setXRot(entity.getXRot() + f1 - f);
        }
    }

    public void passengerYaw(Entity entity, float minYaw, float maxYaw, float passengerRot) {
        float f2;
        if (passengerRot != 180) {
            f2 = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
            float f3 = Mth.clamp(f2, passengerRot + minYaw, passengerRot + maxYaw);
            entity.yRotO += f3 - f2;
            entity.setYRot(entity.getYRot() + f3 - f2);
        } else {
            f2 = Mth.wrapDegrees(entity.getYRot() - this.getYRot() + passengerRot);
            float f3 = Mth.clamp(f2, minYaw, maxYaw);
            entity.yRotO += f3 - f2;
            entity.setYRot(entity.getYRot() + f3 - f2);
        }
        entity.setYBodyRot(this.getYRot() + passengerRot);
    }

    public void passengerPitchOnTurret(Entity entity, float turretMinPitch, float turretMaxPitch) {
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

        float min = -turretMaxPitch - r * getXRot() - r2 * getRoll();
        float max = -turretMinPitch - r * getXRot() - r2 * getRoll();

        float f = Mth.wrapDegrees(entity.getXRot());
        float f1 = Mth.clamp(f, min, max);
        entity.xRotO += f1 - f;
        entity.setXRot(entity.getXRot() + f1 - f);
    }

    public void passengerYawOnTurret(Entity entity, float minYaw, float maxYaw, float passengerRot, boolean rotateWithTurret) {
        float f2;
        if (passengerRot != 180) {
            f2 = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
            float f3 = Mth.clamp(f2, passengerRot + minYaw, passengerRot + maxYaw);
            entity.yRotO += f3 - f2;
            entity.setYRot(entity.getYRot() + f3 - f2);
        } else {
            f2 = Mth.wrapDegrees(entity.getYRot() - this.getYRot() + 180);
            float f3 = Mth.clamp(f2, minYaw, maxYaw);
            entity.yRotO += f3 - f2;
            entity.setYRot(entity.getYRot() + f3 - f2);
        }

        if (rotateWithTurret) {
            entity.setYBodyRot(getBarrelYRot(1) + passengerRot);
        }

        clampZoomYaw(entity);
    }

    public void clampZoomYaw(Entity entity) {
        if (entity.level().isClientSide && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON && mainWeaponControllerIndex() == getSeatIndex(entity)) {
            float f2 = Mth.wrapDegrees(entity.getYRot() - this.getBarrelYRot(1));
            float f3 = Mth.clamp(f2, -20.0F, 20.0F);
            entity.yRotO += f3 - f2;
            entity.setYRot(entity.getYRot() + f3 - f2);
        }
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    protected void clampRotation(Entity entity) {
        int index = getSeatIndex(entity);
        var seats = data().get(VehicleProp.SEATS);
        if (index < 0 || index >= seats.size()) return;
        var seat = seats.get(index);

        if (seat.transform.equals("Vehicle")
                || seat.transform.equals("VehicleFlat")
                || (seat.transform.equals("Turret") && seat.canRotateBody)
        ) {
            if (!seat.canRotateBody) {
                passengerYaw(entity, seat.minYaw, seat.maxYaw, seat.orientation);
            }

            if (hasTurret() && index == mainWeaponControllerIndex()) {
                passengerPitchOnTurret(entity, seat.minPitch, seat.maxPitch);
                passengerYawOnTurret(entity, seat.minYaw, seat.maxYaw, seat.orientation, true);
            } else {
                float diffY = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
                passengerPitch(entity, seat.minPitch, seat.maxPitch, diffY);
            }
        }

        if (seat.transform.equals("Turret") && !seat.canRotateBody) {
            passengerPitchOnTurret(entity, seat.minPitch, seat.maxPitch);
            passengerYawOnTurret(entity, seat.minYaw, seat.maxYaw, seat.orientation, false);
        }
    }

    public void copyEntityData(Entity entity) {
        entity.setYRot(entity.getYRot() + destroyRot);

        int index = getSeatIndex(entity);
        var seat = data().get(VehicleProp.SEATS).get(index);

        if (seat.transform.equals("Vehicle") || seat.transform.equals("VehicleFlat")) {
            if (!seat.canRotateBody) {
                entity.setYBodyRot(getYRot() + seat.orientation);
            }
            if (!seat.canRotateHead) {
                entity.setYRot(getYRot() + seat.orientation);
            }
        }

        if (seat.transform.equals("Turret") && !seat.canRotateBody) {
            entity.setYBodyRot(getBarrelYRot(1) + seat.orientation);
        }
    }

    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        if (!this.hasPassenger(passenger)) {
            return;
        }

        int index = getSeatIndex(passenger);
        var seats = data().get(VehicleProp.SEATS);
        if (index < 0 || index >= seats.size()) return;

        var seat = seats.get(index);
        passengerPos(passenger, callback, seat.position, seat.transform);
    }

    public void passengerPos(Entity passenger, @NotNull MoveFunction callback, Vec3 vec3, String string) {
        Vector4f worldPosition = transformPosition(getTransformFromString(string, 1), (float) vec3.x, (float) vec3.y, (float) vec3.z);
        passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);
        copyEntityData(passenger);
    }

    public Matrix4f getTransformFromString(String string, float ticks) {
        return switch (string) {
            case "VehicleFlat" -> getVehicleFlatTransform(ticks);
            case "Turret" -> getTurretTransform(ticks);
            case "Barrel" -> getBarrelTransform(ticks);
            case "WeaponStation" -> getGunTransform(ticks);
            case "WeaponStationBarrel" -> getGunnerBarrelTransform(ticks);
            default -> getVehicleTransform(ticks);
        };
    }

    public Vec3 getVectorFromString(String string, float ticks) {
        return switch (string) {
            case "Turret" -> getTurretVector(ticks);
            case "Barrel" -> getBarrelVector(ticks);
            case "WeaponStationBarrel" -> getGunnerVector(ticks);
            default -> getViewVector(ticks);
        };
    }

    /**
     * @return 炮弹发射位置
     */
    public Vec3 getShootPos(int seatIndex, float ticks) {
        return getShootPos(getNthEntity(seatIndex), ticks);
    }

    /**
     * @param entity 操控载具的实体
     * @return 炮弹发射位置
     */
    // TODO 解耦炮镜和发射位置
    public Vec3 getShootPos(Entity entity, float ticks) {
        var data = getGunData(getSeatIndex(entity));
        if (data != null) {
            var list = data.get(GunProp.POSITION);
            var vec3 = list.get(this.currentFirePosIndex % list.size());
            this.currentFirePosIndex = ++this.currentFirePosIndex % list.size();

            Vector4f worldPosition = transformPosition(getTransformFromString(data.get(GunProp.TRANSFORM), ticks), (float) vec3.x, (float) vec3.y, (float) vec3.z);
            return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
        }
        return getEyePosition();
    }

    public Vec3 getShootVec(int seatIndex, float ticks) {
        return getShootVec(getNthEntity(seatIndex), ticks);
    }

    public Vec3 getShootVec(Entity entity, float ticks) {
        var data = getGunData(getSeatIndex(entity));
        if (data != null) {
            // TODO 发射方向读取失败？
            StringOrVec3 stringOrVec3 = data.get(GunProp.DIRECTION);
            if (stringOrVec3.isString()) {
                return getVectorFromString(stringOrVec3.string, ticks);
            } else {
                Vec3 startPos = getShootPos(entity, ticks);
                Vec3 endPos = stringOrVec3.vec3;
                return startPos.vectorTo(endPos).normalize();
            }
        }
        return this.getLookAngle();
    }

    /**
     * @param entity 操控载具的实体
     * @return 炮弹发射时的初始速度
     */
    public float projectileVelocity(Entity entity) {
        return 10;
    }

    /**
     * @param entity 操控载具的实体
     * @return 炮弹重力
     */
    public float projectileGravity(Entity entity) {
        return 0.03f;
    }

    /**
     * 本方法用于固定式火炮，其他载具应该使用 {@link VehicleEntity#projectileGravity(Entity)}
     *
     * @return 炮弹重力
     */
    public float projectileGravity() {
        return 0.03f;
    }

    /**
     * 根据UUID，使乘客位武器自动瞄准
     *
     * @param uuid    目标的UUID字符串
     * @param pLiving 操控载具的实体
     */
    public void passengerWeaponAutoAimFormUuid(String uuid, LivingEntity pLiving) {
        Entity target = EntityFindUtil.findEntity(level(), uuid);
        if (target != null) {
            if (target.getVehicle() != null) {
                target = target.getVehicle();
            }

            Vec3 targetPos = target.getBoundingBox().getCenter();
            Vec3 targetVel = target.getDeltaMovement();

            if (target instanceof LivingEntity living) {
                double gravity = living.getAttributeValue(Attributes.GRAVITY);
                targetVel = targetVel.add(0, gravity, 0);
            }

            if (target instanceof Player) {
                targetVel = targetVel.multiply(2, 1, 2);
            }

            Vec3 targetVec = RangeTool.calculateFiringSolution(passengerWeaponShootPos(pLiving, 1), targetPos, targetVel, projectileVelocity(pLiving), projectileGravity(pLiving));
            passengerWeaponAutoAimFormVector(targetVec);

            if (this instanceof WeaponVehicleEntity weaponVehicle) {
                int rpm = 20 / Mth.clamp((weaponVehicle.mainGunRpm(pLiving) / 60), 1, 2147483647);
                if (tickCount % rpm == 0) {
                    aiPassengerWeaponShoot(pLiving);
                }
            }
        }
    }

    /**
     * 根据方向向量，使乘客位武器自动瞄准
     *
     * @param shootVec 需要让武器站以这个角度发射的向量
     */
    public void passengerWeaponAutoAimFormVector(Vec3 shootVec) {
        float ySpeed = passengerWeaponYSpeed();
        float xSpeed = passengerWeaponXSpeed();
        float diffY = (float) Mth.wrapDegrees(-getYRotFromVector(shootVec) + getYRotFromVector(getGunnerVector(1)));
        float diffX = (float) Mth.wrapDegrees(-getXRotFromVector(shootVec) + getXRotFromVector(getGunnerVector(1)));

        this.turretTurnSound(diffX, diffY, 0.95f);

        this.setGunXRot(Mth.clamp(this.getGunXRot() + Mth.clamp(0.5f * diffX, -xSpeed, xSpeed), -passengerWeaponMaxPitch(), -passengerWeaponMinPitch()));
        this.setGunYRot(this.getGunYRot() - Mth.clamp(0.5f * diffY, -ySpeed, ySpeed));

        this.aiPassengerDiff = VectorTool.calculateAngle(shootVec, getGunnerVector(1));
    }

    /**
     * @return 乘客武器站最大水平旋转速度
     */
    public float passengerWeaponYSpeed() {
        return 10;
    }

    /**
     * @return 乘客武器站最大俯仰旋转速度
     */
    public float passengerWeaponXSpeed() {
        return 5;
    }

    /**
     * @return 乘客武器站最小俯角
     */
    public float passengerWeaponMinPitch() {
        return -10;
    }

    /**
     * @return 乘客武器站最大仰角
     */
    public float passengerWeaponMaxPitch() {
        return 30;
    }

    /**
     * @param entity 乘客
     * @return 乘客武器站弹药发射位置
     */
    public Vec3 passengerWeaponShootPos(Entity entity, float ticks) {
        return entity.getEyePosition();
    }

    public void gunnerAngle() {
        float ySpeed = passengerWeaponYSpeed();
        float xSpeed = passengerWeaponXSpeed();

        Entity gunner = this.getNthEntity(secondWeaponControllerIndex());

        float diffY = 0;
        float diffX = 0;
        float speed = 1;

        if (gunner instanceof Player) {
            float gunAngle = -Mth.wrapDegrees(gunner.getYHeadRot() - this.getYRot());
            diffY = Mth.wrapDegrees(gunAngle - getGunYRot());
            diffX = Mth.wrapDegrees(gunner.getXRot() - this.getGunXRot());
            turretTurnSound(diffX, diffY, 0.95f);
            speed = 0;
        }

        this.setGunXRot(this.getGunXRot() + Mth.clamp(0.95f * diffX, -xSpeed, xSpeed));
        this.setGunYRot(this.getGunYRot() + Mth.clamp(0.9f * diffY, -ySpeed, ySpeed) + speed * turretYRotLock);
    }

    public void destroy() {
        var data = data();

        var destroyInfo = data.get(VehicleProp.DESTROY_INFO);

        if (destroyInfo.explodePassengers) {
            if (this.crash && destroyInfo.crashPassengers) {
                crashPassengers();
            } else {
                explodePassengers();
            }
        }

        var radius = destroyInfo.explosionRadius;
        if (radius > 0) {
            var damage = destroyInfo.explosionDamage;
            var particleType = destroyInfo.particleType;

            var explosion = createCustomExplosion()
                    .radius(radius)
                    .damage(damage)
                    .withParticleType(particleType);

            if (!destroyInfo.explodeBlocks) {
                explosion.keepBlock();
            }

            explosion.explode();
        }

        this.discard();
    }

    public CustomExplosion.Builder createCustomExplosion() {
        return new CustomExplosion.Builder(this)
                .attacker(getAttacker());
    }

    protected Entity getAttacker() {
        return EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_ATTACKER_UUID));
    }

    protected void crashPassengers() {
        for (var entity : this.getPassengers()) {
            if (entity instanceof LivingEntity living) {
                for (int i = 0; i < VehicleConfig.AIR_CRASH_EXPLOSION_COUNT.get(); i++) {
                    var tempAttacker = living == getAttacker() ? null : getAttacker();
                    living.invulnerableTime = 0;
                    living.hurt(ModDamageTypes.causeAirCrashDamage(this.level().registryAccess(), null, tempAttacker), VehicleConfig.AIR_CRASH_EXPLOSION_DAMAGE.get());
                }
            }
        }
    }

    protected void explodePassengers() {
        for (var entity : this.getPassengers()) {
            if (entity instanceof LivingEntity living) {
                for (int i = 0; i < VehicleConfig.SELF_EXPLOSION_COUNT.get(); i++) {
                    var tempAttacker = living == getAttacker() ? null : getAttacker();
                    living.invulnerableTime = 0;
                    living.hurt(ModDamageTypes.causeVehicleExplosionDamage(this.level().registryAccess(), null, tempAttacker), VehicleConfig.SELF_EXPLOSION_DAMAGE.get());
                }
            }
        }
    }

    public void travel() {
        var engineType = data().get(VehicleProp.ENGINE_TYPE);
        if (engineType == EngineType.EMPTY) return;

        var engineInfo = data().get(VehicleProp.ENGINE_INFO);
        try {
            switch (engineType) {
                case WHEEL -> {
                    var info = DataLoader.GSON.fromJson(engineInfo, EngineInfo.Wheel.class);
                    this.wheelEngine(info);
                }
                case TRACK -> {
                    var info = DataLoader.GSON.fromJson(engineInfo, EngineInfo.Track.class);
                    this.trackEngine(info);
                }
                case HELICOPTER -> {
                    var info = DataLoader.GSON.fromJson(engineInfo, EngineInfo.Helicopter.class);
                    this.helicopterEngine(info);
                }
            }
        } catch (Exception e) {
            Mod.LOGGER.error("Failed to parse engine info for vehicle {}, {}", this, e);
        }
    }

    // From Immersive_Aircraft
    public Matrix4f getVehicleYOffsetTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo + rotateYOffset(), getY() + rotateYOffset()), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        transform.rotate(Axis.XP.rotationDegrees(Mth.lerp(ticks, xRotO, getXRot())));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, prevRoll, getRoll())));
        return transform;
    }

    public Matrix4f getVehicleTransform(float ticks) {
        Matrix4f transformV = getVehicleYOffsetTransform(ticks);
        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0, -rotateYOffset(), 0);
        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        return transformV;
    }

    public float rotateYOffset() {
        return 0;
    }

    public Matrix4f getVehicleFlatTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo, getY()), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        return transform;
    }

    public Matrix4f getClientVehicleTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo + rotateYOffset(), getY() + rotateYOffset()), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees((float) (-Mth.lerp(ticks, yRotO, getYRot()) + freeCameraYaw)));
        transform.rotate(Axis.XP.rotationDegrees((float) (Mth.lerp(ticks, xRotO, getXRot()) + freeCameraPitch)));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, prevRoll, getRoll())));
        return transform;
    }

    public Vec3 getTurretPosition() {
        return new Vec3(0, 0, 0);
    }

    public Matrix4f getTurretTransform(float ticks) {
        Matrix4f transformV = getVehicleTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, (float) getTurretPosition().x, (float) getTurretPosition().y, (float) getTurretPosition().z);

        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        transformV.rotate(Axis.YP.rotationDegrees(Mth.lerp(ticks, turretYRotO, getTurretYRot())));
        return transformV;
    }

    public Vec3 getTurretVector(float pPartialTicks) {
        Matrix4f transform = getTurretTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    public Vec3 getBarrelPosition() {
        return new Vec3(0, 0, 0);
    }

    public Matrix4f getBarrelTransform(float ticks) {
        Matrix4f transformT = getTurretTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, (float) getBarrelPosition().x, (float) getBarrelPosition().y, (float) getBarrelPosition().z);

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

    public Vec3 getGunnerPosition() {
        return new Vec3(0, 0, 0);
    }

    public Matrix4f getGunTransform(float ticks) {
        Matrix4f transformT = getTurretTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, (float) getGunnerPosition().x, (float) getGunnerPosition().y, (float) getGunnerPosition().z);

        transformT.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        transformT.rotate(Axis.YP.rotationDegrees(Mth.lerp(ticks, gunYRotO, getGunYRot()) - Mth.lerp(ticks, turretYRotO, getTurretYRot())));
        return transformT;
    }

    public Vec3 getGunnerBarrelPosition() {
        return new Vec3(0, 0, 0);
    }

    public Matrix4f getGunnerBarrelTransform(float ticks) {
        Matrix4f transformG = getGunTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, (float) getGunnerBarrelPosition().x, (float) getGunnerBarrelPosition().y, (float) getGunnerBarrelPosition().z);

        transformG.translate(worldPosition.x, worldPosition.y, worldPosition.z);

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

        float x = Mth.lerp(ticks, gunXRotO, getGunXRot());
        float xV = Mth.lerp(ticks, xRotO, getXRot());
        float z = Mth.lerp(ticks, prevRoll, getRoll());

        transformG.rotate(Axis.XP.rotationDegrees(x + r * xV + r2 * z));
        return transformG;
    }

    public Vec3 getGunnerVector(float pPartialTicks) {
        Matrix4f transform = getGunnerBarrelTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    public Vector4f transformPosition(Matrix4f transform, float x, float y, float z) {
        return transform.transform(new Vector4f(x, y, z, 1));
    }

    public int mainWeaponControllerIndex() {
        return 0;
    }

    public int secondWeaponControllerIndex() {
        return 1;
    }

    public static Quaternionf eulerToQuaternion(float yaw, float pitch, float roll) {
        double cy = Math.cos(yaw * 0.5 * Mth.DEG_TO_RAD);
        double sy = Math.sin(yaw * 0.5 * Mth.DEG_TO_RAD);
        double cp = Math.cos(pitch * 0.5 * Mth.DEG_TO_RAD);
        double sp = Math.sin(pitch * 0.5 * Mth.DEG_TO_RAD);
        double cr = Math.cos(roll * 0.5 * Mth.DEG_TO_RAD);
        double sr = Math.sin(roll * 0.5 * Mth.DEG_TO_RAD);

        Quaternionf q = new Quaternionf();
        q.w = (float) (cy * cp * cr + sy * sp * sr);
        q.x = (float) (cy * cp * sr - sy * sp * cr);
        q.y = (float) (sy * cp * sr + cy * sp * cr);
        q.z = (float) (sy * cp * cr - cy * sp * sr);

        return q;
    }

    public void handleClientSync() {
        if (isControlledByLocalInstance()) {
            interpolationSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (interpolationSteps <= 0) {
            return;
        }
        double interpolatedX = getX() + (x - getX()) / (double) interpolationSteps;
        double interpolatedY = getY() + (y - getY()) / (double) interpolationSteps;
        double interpolatedZ = getZ() + (z - getZ()) / (double) interpolationSteps;
        double interpolatedYaw = Mth.wrapDegrees(serverYRot - (double) getYRot());
        setYRot(getYRot() + (float) interpolatedYaw / (float) interpolationSteps);
        setXRot(getXRot() + (float) (serverXRot - (double) getXRot()) / (float) interpolationSteps);

        setPos(interpolatedX, interpolatedY, interpolatedZ);
        setRot(getYRot(), getXRot());

        --interpolationSteps;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.x = x;
        this.y = y;
        this.z = z;
        serverYRot = yRot;
        serverXRot = xRot;
        this.interpolationSteps = 10;
    }

    public static double calculateAngle(Vec3 move, Vec3 view) {
        move = move.multiply(1, 0, 1).normalize();
        view = view.multiply(1, 0, 1).normalize();

        return VectorTool.calculateAngle(move, view);
    }

    protected Vec3 getDismountOffset(double vehicleWidth, double passengerWidth) {
        double offset = (vehicleWidth + passengerWidth + (double) 1.0E-5f) / 1.75;
        float yaw = getYRot() + 90.0f;
        float x = -Mth.sin(yaw * ((float) Math.PI / 180));
        float z = Mth.cos(yaw * ((float) Math.PI / 180));
        float n = Math.max(Math.abs(x), Math.abs(z));
        return new Vec3((double) x * offset / (double) n, 0.0, (double) z * offset / (double) n);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        int index = this.getTagSeatIndex(passenger);
        if (index < 0) {
            return super.getDismountLocationForPassenger(passenger);
        } else {
            return this.getDismountLocationForIndex(passenger, index);
        }
    }

    /**
     * 获取第N个乘客的坐下位置
     *
     * @param passenger 乘客
     * @param index     座位
     * @return 下车的位置
     */
    public @NotNull Vec3 getDismountLocationForIndex(LivingEntity passenger, int index) {
        Vec3 vec3d = getDismountOffset(getBbWidth() * Mth.SQRT_OF_TWO, passenger.getBbWidth() * Mth.SQRT_OF_TWO);
        double ox = getX() - vec3d.x;
        double oz = getZ() + vec3d.z;
        BlockPos exitPos = new BlockPos((int) ox, (int) getY(), (int) oz);
        BlockPos floorPos = exitPos.below();
        if (!level().isWaterAt(floorPos)) {
            ArrayList<Vec3> list = Lists.newArrayList();
            double exitHeight = level().getBlockFloorHeight(exitPos);
            if (DismountHelper.isBlockFloorValid(exitHeight)) {
                list.add(new Vec3(ox, (double) exitPos.getY() + exitHeight, oz));
            }
            double floorHeight = level().getBlockFloorHeight(floorPos);
            if (DismountHelper.isBlockFloorValid(floorHeight)) {
                list.add(new Vec3(ox, (double) floorPos.getY() + floorHeight, oz));
            }
            for (Pose entityPose : passenger.getDismountPoses()) {
                for (Vec3 vec3d2 : list) {
                    if (!DismountHelper.canDismountTo(level(), vec3d2, passenger, entityPose)) continue;
                    passenger.setPose(entityPose);
                    return vec3d2;
                }
            }
        }

        return super.getDismountLocationForPassenger(passenger);
    }

    public @NotNull Vec3 getDismountMovement(LivingEntity passenger, int index) {
        return new Vec3(0, 0, 0);
    }

    public boolean allowEjection() {
        return false;
    }

    public void removeSeatIndexTag(Entity entity) {
        entity.getPersistentData().remove(TAG_SEAT_INDEX);
    }

    public ResourceLocation getVehicleIcon() {
        return ResourceLocation.tryParse(data().get(VehicleProp.ICON).vehicleIcon());
    }

    public boolean allowFreeCam() {
        return data().get(VehicleProp.ALLOW_FREE_CAM);
    }

    // 本方法留空
    @Override
    public void push(double pX, double pY, double pZ) {
    }

    public Vec3 getBarrelVector(float pPartialTicks) {
        Matrix4f transform = getBarrelTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    public float getBarrelXRot(float pPartialTicks) {
        return Mth.lerp(pPartialTicks, turretXRotO - this.xRotO, getTurretXRot() - this.getXRot());
    }

    public float getBarrelYRot(float pPartialTick) {
        return -Mth.lerp(pPartialTick, turretYRotO - this.yRotO, getTurretYRot() - this.getYRot());
    }

    public float getGunXRot(float pPartialTicks) {
        return Mth.lerp(pPartialTicks, gunXRotO - this.xRotO, getGunXRot() - this.getXRot());
    }

    public float getGunYRot(float pPartialTick) {
        return -Mth.lerp(pPartialTick, gunYRotO - this.yRotO, getGunYRot() - this.getYRot());
    }

    public float getTurretYRot() {
        return this.turretYRot;
    }

    public float getTurretYaw(float pPartialTick) {
        return Mth.lerp(pPartialTick, turretYRotO, getTurretYRot());
    }

    public void setTurretYRot(float pTurretYRot) {
        this.turretYRot = pTurretYRot;
    }

    public float getTurretXRot() {
        return this.turretXRot;
    }

    public void setTurretXRot(float pTurretXRot) {
        this.turretXRot = pTurretXRot;
    }

    public float getTurretPitch(float pPartialTick) {
        return Mth.lerp(pPartialTick, turretXRotO, getTurretXRot());
    }

    public float getGunYRot() {
        return this.gunYRot;
    }

    public void setGunYRot(float pGunYRot) {
        this.gunYRot = pGunYRot;
    }

    public float getGunXRot() {
        return this.gunXRot;
    }

    public void setGunXRot(float pGunXRot) {
        this.gunXRot = pGunXRot;
    }

    public Vec3 driverZoomPos(Entity entity, float ticks) {
        return getShootPos(entity, ticks);
    }

    public double getMouseSensitivity() {
        return 0.1;
    }

    public double getMouseSpeedX() {
        return 0.4;
    }

    public double getMouseSpeedY() {
        return 0.4;
    }

    public boolean hasTurret() {
        return false;
    }

    public boolean turretHasPassengerWeapon() {
        return false;
    }

    public float gearRot(float tickDelta) {
        return Mth.lerp(tickDelta, gearRotO, entityData.get(GEAR_ROT));
    }

    public float getMass() {
        return data().get(VehicleProp.MASS);
    }

    @Override
    public void setDeltaMovement(Vec3 pDeltaMovement) {
        Vec3 currentMomentum = this.getDeltaMovement();

        // 计算当前速度和新速度的标量大小
        double currentSpeedSq = currentMomentum.lengthSqr();
        double newSpeedSq = pDeltaMovement.lengthSqr();

        // 只在新速度大于当前速度时（加速过程）进行检查
        if (newSpeedSq > currentSpeedSq) {
            // 计算加速度向量
            Vec3 acceleration = pDeltaMovement.subtract(currentMomentum);

            // 检查加速度大小是否超过阈值
            if (acceleration.lengthSqr() > 8) {
                // 限制加速度不超过阈值
                Vec3 limitedAcceleration = acceleration.normalize().scale(0.125);
                Vec3 finalMomentum = currentMomentum.add(limitedAcceleration);

                super.setDeltaMovement(finalMomentum);
                return;
            }
        }
        // 对于减速或允许的加速，直接设置新动量
        super.setDeltaMovement(pDeltaMovement);
    }

    @Override
    public void addDeltaMovement(Vec3 pAddend) {
        var length = pAddend.length();
        if (length > 0.1) pAddend = pAddend.scale(0.1 / length);

        super.addDeltaMovement(pAddend);
    }

    /**
     * 玩家在载具上的灵敏度调整
     *
     * @param original   原始灵敏度
     * @param zoom       是否在载具上瞄准
     * @param seatIndex  玩家座位
     * @param isOnGround 载具是否在地面
     * @return 调整后的灵敏度
     */
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return original;
    }

    /**
     * 载具在集装箱物品上显示的贴图
     */
    @Nullable
    public ResourceLocation getVehicleItemIcon() {
        String location = data().get(VehicleProp.ICON).containerIcon();
        if (location == null) return null;
        return ResourceLocation.tryParse(location);
    }

    /**
     * 判断一个座位是否是封闭的（封闭载具座位具有免疫负面效果等功能）
     * 默认认为隐藏乘客的座位均为封闭座位
     *
     * @param index 位置
     */
    public boolean isEnclosed(int index) {
        var seats = data().get(VehicleProp.SEATS);
        if (index < 0 || index >= seats.size()) return false;

        var seat = seats.get(index);
        if (seat.isEnclosed == null) {
            return seat.hidePassenger;
        }

        return seat.isEnclosed;
    }

    public boolean isEnclosed(Entity passenger) {
        return isEnclosed(getSeatIndex(passenger));
    }

    /**
     * 是否隐藏载具上的玩家
     *
     * @return 是否隐藏
     */
    public boolean hidePassenger(int index) {
        var seats = data().get(VehicleProp.SEATS);
        if (index < 0 || index >= seats.size()) return false;

        var seat = seats.get(index);
        return seat.hidePassenger;
    }

    public boolean hidePassenger(Entity passenger) {
        return hidePassenger(getSeatIndex(passenger));
    }

    public int getAmmoCount(LivingEntity passenger, int weaponIndex) {
        if (this instanceof WeaponVehicleEntity weaponVehicle) {
            var weapons = weaponVehicle.getAvailableWeapons(getSeatIndex(passenger));
            if (weaponIndex < 0 || weaponIndex >= weapons.size()) return 0;

            var weapon = weapons.get(weaponIndex);
            if (InventoryTool.hasCreativeAmmoBox(passenger) && !(weapon instanceof LaserWeapon) && !(weapon instanceof SmallRocketWeapon) && !(weapon instanceof SwarmDroneWeapon)) {
                return -1;
            }

            return weaponVehicle.getAmmoCount(passenger);
        }
        return 0;
    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        if (!getRetrieveItems().isEmpty()) {
            return getRetrieveItems().getFirst();
        }
        return ContainerBlockItem.createInstance(this.getType());
    }

    /**
     * 渲染载具的第一人称UI
     * 务必标记 @OnlyIn(Dist.CLIENT)
     */
    @OnlyIn(Dist.CLIENT)
    public void renderFirstPersonOverlay(GuiGraphics guiGraphics, PoseStack poseStack, Font font, Player player, int screenWidth, int screenHeight, float scale, int color) {
    }

    /**
     * 渲染载具的第三人称UI
     * 务必标记 @OnlyIn(Dist.CLIENT) !
     */
    @OnlyIn(Dist.CLIENT)
    public void renderThirdPersonOverlay(GuiGraphics guiGraphics, Font font, Player player, int screenWidth, int screenHeight, float scale) {
    }

    /**
     * 获取视角旋转
     *
     * @param zoom          是否在载具上瞄准
     * @param isFirstPerson 是否是第一人称视角
     */
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        return null;
    }

    /**
     * 获取视角位置
     *
     * @param zoom          是否在载具上瞄准
     * @param isFirstPerson 是否是第一人称视角
     */
    @OnlyIn(Dist.CLIENT)
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        return null;
    }

    /**
     * 是否使用载具固定视角
     */
    @OnlyIn(Dist.CLIENT)
    public boolean useFixedCameraPos(Entity entity) {
        return false;
    }

    public boolean hasTracks() {
        return false;
    }

    public boolean canCrushEntities() {
        return true;
    }

    public static boolean IGNORE_ENTITY_GROUND_CHECK_STEPPING = false;

    public void trackEngine(EngineInfo.Track engineInfo) {
        this.trackEngine(
                engineInfo.buoyancy,
                (int) (engineInfo.energyCostRate * Mth.abs(this.entityData.get(POWER))),
                engineInfo.wheelRotSpeed,
                engineInfo.wheelDifferential,
                engineInfo.trackRotSpeed,
                engineInfo.trackDifferential,
                engineInfo.maxForwardSpeedRate,
                engineInfo.maxBackwardSpeedRate,
                engineInfo.increment,
                engineInfo.decrement,
                engineInfo.steeringSpeed
        );
    }

    public void trackEngine(double buoyancy, int energyCost, double wheelRotSpeed, double wheelDifferential, double trackSpeed, double trackDifferential, float maxForwardSpeedRate, float maxBackwardSpeedRate, float powerAdd, float powerReduce, float steeringSpeed) {
        if (buoyancy != 0) {
            double fluidFloat = buoyancy * getSubmergedHeight(this);
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, fluidFloat, 0.0));
        }

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

        Entity passenger0 = this.getFirstPassenger();

        if (this.getEnergy() <= 0) return;

        if (passenger0 == null) {
            setLeftInputDown(false);
            setRightInputDown(false);
            setForwardInputDown(false);
            setBackInputDown(false);
            this.entityData.set(POWER, 0f);
        }

        if (forwardInputDown()) {
            this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + (this.entityData.get(POWER) < 0 ? powerAdd * 2f : powerAdd), 1));
        }

        if (backInputDown()) {
            this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - (this.entityData.get(POWER) > 0 ? powerReduce * 2f : powerReduce), -1));
            if (rightInputDown()) {
                this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + steeringSpeed);
            } else if (this.leftInputDown()) {
                this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - steeringSpeed);
            }
        } else {
            if (rightInputDown()) {
                this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - steeringSpeed);
            } else if (this.leftInputDown()) {
                this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + steeringSpeed);
            }
        }

        if (this.entityData.get(POWER) > 0) {
            targetSpeed = maxForwardSpeedRate * (1 + getXRot() / 55);
        } else {
            targetSpeed = maxBackwardSpeedRate * (1 - getXRot() / 55);
        }

        if (!forwardInputDown() && !backInputDown()) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.96f);
        }

        if (upInputDown()) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.6f);
        }

        if (rightInputDown() || leftInputDown()) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.96f);
        }

        if (this.level() instanceof ServerLevel) {
            this.consumeEnergy(energyCost);
        }

        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * (float) Math.max(0.76f - 0.1f * this.getDeltaMovement().horizontalDistance(), 0.3));

        double s0 = getDeltaMovement().dot(this.getViewVector(1));

        this.setLeftWheelRot((float) ((this.getLeftWheelRot() - wheelRotSpeed * s0) + Mth.clamp(wheelDifferential * this.entityData.get(DELTA_ROT), -5f, 5f)));
        this.setRightWheelRot((float) ((this.getRightWheelRot() - wheelRotSpeed * s0) - Mth.clamp(wheelDifferential * this.entityData.get(DELTA_ROT), -5f, 5f)));

        setLeftTrack((float) ((getLeftTrack() - trackSpeed * Math.PI * s0) + Mth.clamp(trackDifferential * Math.PI * this.entityData.get(DELTA_ROT), -5f, 5f)));
        setRightTrack((float) ((getRightTrack() - trackSpeed * Math.PI * s0) - Mth.clamp(trackDifferential * Math.PI * this.entityData.get(DELTA_ROT), -5f, 5f)));

        int i;

        if (entityData.get(L_WHEEL_DAMAGED) && entityData.get(R_WHEEL_DAMAGED)) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.93f);
            i = 0;
        } else if (entityData.get(L_WHEEL_DAMAGED)) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.975f);
            i = 3;
        } else if (entityData.get(R_WHEEL_DAMAGED)) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.975f);
            i = -3;
        } else {
            i = 0;
        }

        if (entityData.get(ENGINE1_DAMAGED)) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.96f);
        }

        this.setYRot((float) (this.getYRot() - (isInWater() && !onGround() ? 2.5 : 6) * entityData.get(DELTA_ROT) - i * s0));
        if (this.isInWater() || onGround()) {
            double water = (!isInWater() && !onGround() ? 0.05f : (isInWater() && !onGround() ? 0.3f : 1));
            this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale(0.15 * water * targetSpeed * this.entityData.get(POWER))));
        }
    }

    public void wheelEngine(EngineInfo.Wheel engineInfo) {
        this.wheelEngine(
                engineInfo.buoyancy,
                (int) (engineInfo.energyCostRate * Mth.abs(this.entityData.get(POWER))),
                engineInfo.wheelRotSpeed,
                engineInfo.wheelDifferential,
                engineInfo.maxForwardSpeedRate,
                engineInfo.maxBackwardSpeedRate,
                engineInfo.increment,
                engineInfo.decrement,
                engineInfo.steeringSpeed
        );
    }

    public void wheelEngine(double buoyancy, int energyCost, double wheelRotSpeed, double wheelDifferential, float maxForwardSpeedRate, float maxBackwardSpeedRate, float powerAdd, float powerReduce, float steeringSpeed) {
        if (buoyancy != 0) {
            double fluidFloat = buoyancy * getSubmergedHeight(this);
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, fluidFloat, 0.0));
        }

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

        Entity passenger0 = this.getFirstPassenger();

        if (this.getEnergy() <= 0) return;

        if (passenger0 == null) {
            setLeftInputDown(false);
            setRightInputDown(false);
            setForwardInputDown(false);
            setBackInputDown(false);
            this.entityData.set(POWER, 0f);
        }

        if (forwardInputDown()) {
            this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + (this.entityData.get(POWER) < 0 ? powerAdd * 2f : powerAdd), 1));
        }

        if (backInputDown()) {
            this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - (this.entityData.get(POWER) > 0 ? powerReduce * 2f : powerReduce), -1));
        }

        if (this.entityData.get(POWER) > 0) {
            targetSpeed = maxForwardSpeedRate * (1 + getXRot() / 55);
        } else {
            targetSpeed = maxBackwardSpeedRate * (1 - getXRot() / 55);
        }

        if (!forwardInputDown() && !backInputDown()) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.97f);
        }

        if (upInputDown()) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.6f);
        }

        if (rightInputDown() || leftInputDown()) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.98f);
        }

        if (this.level() instanceof ServerLevel) {
            this.consumeEnergy(energyCost);
        }

        int i;

        if (entityData.get(L_WHEEL_DAMAGED) && entityData.get(R_WHEEL_DAMAGED)) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.93f);
            i = 0;
        } else if (entityData.get(L_WHEEL_DAMAGED)) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.975f);
            i = 3;
        } else if (entityData.get(R_WHEEL_DAMAGED)) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.975f);
            i = -3;
        } else {
            i = 0;
        }

        if (entityData.get(ENGINE1_DAMAGED)) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.875f);
        }

        if (rightInputDown()) {
            this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + steeringSpeed);
        } else if (this.leftInputDown()) {
            this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - steeringSpeed);
        }

        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * (float) Math.max(0.78f - 0.25f * this.getDeltaMovement().horizontalDistance(), 0.1));

        double s0 = getDeltaMovement().dot(this.getViewVector(1));

        this.setLeftWheelRot((float) ((this.getLeftWheelRot() - wheelRotSpeed * s0) - Mth.clamp(wheelDifferential * this.entityData.get(DELTA_ROT), -5f, 5f) * getDeltaMovement().length()));
        this.setRightWheelRot((float) ((this.getRightWheelRot() - wheelRotSpeed * s0) + Mth.clamp(wheelDifferential * this.entityData.get(DELTA_ROT), -5f, 5f) * getDeltaMovement().length()));

        this.setRudderRot(Mth.clamp(this.getRudderRot() - this.entityData.get(DELTA_ROT), -0.8f, 0.8f) * 0.75f);

        this.setYRot((float) (this.getYRot() - Math.max((isInWater() && !onGround() ? 6 : 12) * this.getDeltaMovement().horizontalDistance(), 0) * this.getRudderRot() * (this.entityData.get(POWER) > 0 ? 1 : -1) - i * s0));

        if (this.isInWater() || onGround()) {
            double water = (!isInWater() && !onGround() ? 0.05f : (isInWater() && !onGround() ? 0.3f : 1));
            this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale(0.15 * water * targetSpeed * this.entityData.get(POWER))));
        }
    }

    public void helicopterEngine(EngineInfo.Helicopter engineInfo) {
        this.helicopterEngine(
                (int) (engineInfo.energyCostRate * Mth.abs(this.entityData.get(POWER))),
                engineInfo.increment,
                engineInfo.decrement,
                engineInfo.pitchSpeed,
                engineInfo.yawSpeed,
                engineInfo.rollSpeed,
                engineInfo.liftSpeed
        );
    }

    public void helicopterEngine(int energyCost, float powerAdd, float powerReduce, float pitchSpeed, float yawSpeed, float rollSpeed, float lift) {
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 1, 0.8));
        } else {
            setZRot(getRoll() * (backInputDown() ? 0.9f : 0.99f));
            float f = (float) Mth.clamp(0.95f - 0.015 * getDeltaMovement().length() + 0.02f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90, 0.01, 0.99);
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).scale((this.getXRot() < 0 ? -0.035 : (this.getXRot() > 0 ? 0.035 : 0)) * this.getDeltaMovement().length())));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f, 0.95, f));
        }

        if (this.isInWater() && this.tickCount % 4 == 0 && getSubmergedHeight(this) > 0.5 * getBbHeight()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.6, 0.6));
            this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), 6 + (float) (20 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))));
        }

        Entity pilot = getFirstPassenger();

        boolean hasPassenger = false;

        for (int i = 0; i < getMaxPassengers() - 1; i++) {
            if (getNthEntity(i) != null) {
                hasPassenger = true;
            }
        }

        float diffX;
        float diffZ;

        if (getHealth() > 0.1f * getMaxHealth()) {
            if (pilot == null) {
                setLeftInputDown(false);
                setRightInputDown(false);
                setForwardInputDown(false);
                setBackInputDown(false);
                setUpInputDown(false);
                setDownInputDown(false);
                this.setZRot(this.roll * 0.98f);
                this.setXRot(this.getXRot() * 0.98f);
                if (hasPassenger) {
                    this.entityData.set(POWER, this.entityData.get(POWER) * 0.99f);
                }
            } else {
                if (!landingInputDown() || findNearestLandingPos(30) == null) {
                    if (rightInputDown()) {
                        holdTick++;
                        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 2f * Math.min(holdTick, 7) * this.entityData.get(POWER));
                    } else if (this.leftInputDown()) {
                        holdTick++;
                        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 2f * Math.min(holdTick, 7) * this.entityData.get(POWER));
                    } else {
                        holdTick = 0;
                    }
                    this.setXRot(this.getXRot() + ((this.onGround()) ? 0 : 1.5f) * pitchSpeed * getMouseMoveSpeedY() * this.entityData.get(PROPELLER_ROT));
                    this.setZRot(this.getRoll() - rollSpeed * (this.entityData.get(DELTA_ROT) + (this.onGround() ? 0 : 0.25f) * getMouseMoveSpeedX() * this.entityData.get(PROPELLER_ROT)));
                }

                this.setYRot(this.getYRot() + yawSpeed * Mth.clamp((this.onGround() ? 0.1f : 2f) * getMouseMoveSpeedX() * this.entityData.get(PROPELLER_ROT) + (this.entityData.get(ENGINE2_DAMAGED) ? 25 : 0) * this.entityData.get(PROPELLER_ROT), -10f, 10f));
                if (findNearestLandingPos(30) != null && !onGround() && landingInputDown()) {
                    this.updateAutoLanding(findNearestLandingPos(30));
                }

                if (pilot instanceof Player player && level().isClientSide && findNearestLandingPos(30) != null && !onGround()) {
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.press_s_to_landing"), true);
                }
            }

            if (this.getEnergy() > 0) {
                boolean up = upInputDown() || forwardInputDown();
                boolean down = this.downInputDown();

                if (!engineStart && up) {
                    engineStart = true;
                    this.level().playSound(null, this, ModSounds.HELICOPTER_ENGINE_START.get(), this.getSoundSource(), 3, 1);
                }

                if (up && engineStartOver) {
                    holdPowerTick++;
                    this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.0007f * powerAdd * Math.min(holdPowerTick, 10), 0.12f));
                }

                if (engineStartOver) {
                    if (down) {
                        holdPowerTick++;
                        this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.001f * powerReduce * Math.min(holdPowerTick, 5), this.onGround() ? 0 : 0.025f / lift));
                    } else if (backInputDown()) {
                        holdPowerTick++;
                        this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.001f * powerReduce * Math.min(holdPowerTick, 5), this.onGround() ? 0 : 0.058f / lift));
                    }
                }

                if (engineStart && !engineStartOver) {
                    this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.0012f * powerAdd, 0.045f));
                }

                if (!(up || down || backInputDown()) && engineStartOver) {
                    if (this.getDeltaMovement().y() < 0) {
                        this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.0002f, 0.12f));
                    } else {
                        this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - (this.onGround() ? 0.00005f : 0.0002f), 0));
                    }
                    holdPowerTick = 0;
                }
            } else {
                this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.0001f, 0));
                setForwardInputDown(false);
                setBackInputDown(false);
                engineStart = false;
                engineStartOver = false;
            }
        } else if (!onGround() && engineStartOver) {
            this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.0003f, 0.01f));
            destroyRot += 0.08f;

            diffX = 45 - this.getXRot();
            diffZ = -20 - this.getRoll();

            this.setXRot(this.getXRot() + diffX * 0.05f * this.entityData.get(PROPELLER_ROT));
            this.setYRot(this.getYRot() + destroyRot);
            this.setZRot(this.getRoll() + diffZ * 0.1f * this.entityData.get(PROPELLER_ROT));
            setDeltaMovement(getDeltaMovement().add(0, -destroyRot * 0.004, 0));
        }

        if (entityData.get(ENGINE1_DAMAGED)) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.98f);
        }

        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * 0.9f);
        this.entityData.set(PROPELLER_ROT, Mth.lerp(0.18f, this.entityData.get(PROPELLER_ROT), this.entityData.get(POWER)));
        this.setPropellerRot(this.getPropellerRot() + 30 * this.entityData.get(PROPELLER_ROT));
        this.entityData.set(PROPELLER_ROT, this.entityData.get(PROPELLER_ROT) * 0.9995f);

        if (engineStart) {
            this.consumeEnergy((int) (energyCost * this.entityData.get(POWER) * 8.3333f));
        }

        Matrix4f transform = getVehicleTransform(1);

        Vector4f force0 = transformPosition(transform, 0, 0, 0);
        Vector4f force1 = transformPosition(transform, 0, 1, 0);

        Vec3 force = new Vec3(force0.x, force0.y, force0.z).vectorTo(new Vec3(force1.x, force1.y, force1.z));

        setDeltaMovement(getDeltaMovement().add(force.scale(this.entityData.get(PROPELLER_ROT) * lift)));

        if (this.entityData.get(POWER) > 0.04f) {
            engineStartOver = true;
        }

        if (this.entityData.get(POWER) < 0.0004f) {
            engineStart = false;
            engineStartOver = false;
        }
    }

    /**
     * 查找实体下方半球区域内最近的降落辅助方块位置
     *
     * @param radius 搜索半径
     * @return 钻石块顶面位置，如果未找到则返回null
     */
    public Vec3 findNearestLandingPos(int radius) {
        Level world = this.level();
        BlockPos entityPos = this.blockPosition();
        List<BlockPos> landingBlocks = new ArrayList<>();

        // 遍历半球区域内的所有方块
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= 0; y++) { // 只检查实体下方的区域
                    // 检查是否在半球内 (x² + y² + z² ≤ r²)
                    if (x * x + y * y + z * z <= radius * radius) {
                        BlockPos checkPos = entityPos.offset(x, y, z);

                        // 检查是否为降落辅助方块
                        if (world.getBlockState(checkPos).is(ModTags.Blocks.AUTO_LANDING)) {
                            landingBlocks.add(checkPos);
                        }
                    }
                }
            }
        }

        // 如果没有找到降落辅助方块，返回null
        if (landingBlocks.isEmpty()) {
            return null;
        }

        // 按距离排序，找到最近的降落辅助方块
        landingBlocks.sort(Comparator.comparingDouble(pos ->
                this.position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5)));

        return landingBlocks.getFirst().getCenter();
    }

    public void updateAutoLanding(Vec3 landingTarget) {
        // 计算水平方向上的偏移向量 (忽略Y轴)
        Vec3 currentPos = this.position();
        Vec3 horizontalOffset = new Vec3(
                landingTarget.x - currentPos.x,
                0,
                landingTarget.z - currentPos.z
        );

        setDeltaMovement(getDeltaMovement().multiply(0.98, 0.99, 0.98));

        // 计算距离和方向
        double horizontalDistance = horizontalOffset.length();
        Vec3 horizontalDirection = horizontalDistance > 0 ?
                horizontalOffset.normalize() : Vec3.ZERO;

        // 如果已经非常接近目标点，保持水平姿态
        // 位置容差
        float positionTolerance = 0.1f;
        // 倾斜平滑因子
        float tiltSmoothingFactor = 0.1f;
        if (horizontalDistance < positionTolerance) {
            // 平滑过渡到水平姿态
            this.setXRot(lerpAngle(this.getXRot(), 0, tiltSmoothingFactor));
            this.setZRot(lerpAngle(this.getRoll(), 0, tiltSmoothingFactor));
            return;
        }

        // 计算需要的倾斜角度 (与距离成正比，但有最大限制)
        // 直升机辅助降落这一块
        // 最大倾斜角度(度)
        float maxTiltAngle = 15.0f;
        float targetTilt = (float) Math.min(maxTiltAngle, horizontalDistance * 2);

        // 将世界方向转换为本地倾斜方向
        // 需要考虑直升机的当前偏航角(yRot)
        float yawRad = Math.toRadians(-this.getYRot());
        Vec3 localDirection = new Vec3(
                horizontalDirection.x * Math.cos(yawRad) - horizontalDirection.z * Math.sin(yawRad),
                0,
                horizontalDirection.x * Math.sin(yawRad) + horizontalDirection.z * Math.cos(yawRad)
        );

        // 计算目标俯仰和滚转
        float targetXRot = (float) (-localDirection.z * targetTilt);
        float targetZRot = (float) (localDirection.x * targetTilt);

        // 平滑过渡到目标姿态
        this.setXRot(lerpAngle(this.getXRot(), -targetXRot, tiltSmoothingFactor));
        this.setZRot(lerpAngle(this.getRoll(), -targetZRot, tiltSmoothingFactor));
    }

    // 角度线性插值方法
    private float lerpAngle(float current, float target, float factor) {
        // 处理角度环绕
        float diff = target - current;
        while (diff < -180) diff += 360;
        while (diff > 180) diff -= 360;

        return current + diff * factor;
    }

    //烟雾诱饵
    public void releaseSmokeDecoy(Vec3 vec3) {
        if (decoyInputDown()) {
            if (this.entityData.get(DECOY_COUNT) > 0 && this.level() instanceof ServerLevel) {
                Entity passenger = getFirstPassenger();
                for (int i = 0; i < 8; i++) {
                    SmokeDecoyEntity smokeDecoyEntity = new SmokeDecoyEntity((LivingEntity) passenger, this.level());
                    smokeDecoyEntity.setPos(this.getX(), this.getY() + getBbHeight(), this.getZ());
                    smokeDecoyEntity.decoyShoot(this, vec3.yRot((-78.75f + 22.5F * i) * Mth.DEG_TO_RAD), 4f, 8);
                    this.level().addFreshEntity(smokeDecoyEntity);
                }
                this.level().playSound(null, this, ModSounds.DECOY_FIRE.get(), this.getSoundSource(), 1, 1);
                decoyReloadCoolDown = 500;
                this.getEntityData().set(DECOY_COUNT, this.getEntityData().get(DECOY_COUNT) - 1);
            }
            setDecoyInputDown(false);
        }
        if (this.entityData.get(DECOY_COUNT) < 1 && decoyReloadCoolDown == 0 && this.level() instanceof ServerLevel) {
            this.entityData.set(DECOY_COUNT, this.entityData.get(DECOY_COUNT) + 1);
            this.level().playSound(null, this, ModSounds.DECOY_RELOAD.get(), this.getSoundSource(), 1, 1);
            decoyReloadCoolDown = 500;
        }
    }

    //热诱弹诱饵
    public void releaseDecoy() {
        if (decoyInputDown()) {
            if (this.entityData.get(DECOY_COUNT) > 0 && this.level() instanceof ServerLevel) {
                Entity passenger = getFirstPassenger();
                for (int i = 0; i < 4; i++) {
                    FlareDecoyEntity flareDecoyEntity = new FlareDecoyEntity(this.level());
                    flareDecoyEntity.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + 0.5 + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
                    flareDecoyEntity.decoyShoot(this, this.getViewVector(1).yRot((45 + 90 * i) * Mth.DEG_TO_RAD), 0.8f, 8);
                    this.level().addFreshEntity(flareDecoyEntity);
                }
                this.level().playSound(null, this, ModSounds.DECOY_FIRE.get(), this.getSoundSource(), 2, 1);
                if (this.getEntityData().get(DECOY_COUNT) == 4) {
                    decoyReloadCoolDown = 300;
                }
                this.getEntityData().set(DECOY_COUNT, this.getEntityData().get(DECOY_COUNT) - 1);
            }
            setDecoyInputDown(false);
        }
        if (this.entityData.get(DECOY_COUNT) < 4 && decoyReloadCoolDown == 0 && this.level() instanceof ServerLevel) {
            this.entityData.set(DECOY_COUNT, this.entityData.get(DECOY_COUNT) + 1);
            this.level().playSound(null, this, ModSounds.DECOY_RELOAD.get(), this.getSoundSource(), 1, 1);
            decoyReloadCoolDown = 300;
        }
    }

    // 惯性倾斜

    public void inertiaRotate(float multiple) {
        float angleX = 0;
        float diffX = (float) (getAcceleration() * multiple - angleX);
        setXRot(getXRot() - 0.5f * diffX);
    }

    public static List<Entity> getPlayer(Level level) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> e instanceof Player)
                .toList();
    }

    // 地形适应测试
    public void terrainCompact(float w, float l) {
        if (onGround()) {
            Matrix4f transform = this.getWheelsTransform(1);

            // 左前
            Vector4f positionLF = transformPosition(transform, w / 2, 0, l / 2);
            // 右前
            Vector4f positionRF = transformPosition(transform, -w / 2, 0, l / 2);
            // 左后
            Vector4f positionLB = transformPosition(transform, w / 2, 0, -l / 2);
            // 右后
            Vector4f positionRB = transformPosition(transform, -w / 2, 0, -l / 2);

            Vec3 p1 = new Vec3(positionLF.x, positionLF.y, positionLF.z);
            Vec3 p2 = new Vec3(positionRF.x, positionRF.y, positionRF.z);
            Vec3 p3 = new Vec3(positionLB.x, positionLB.y, positionLB.z);
            Vec3 p4 = new Vec3(positionRB.x, positionRB.y, positionRB.z);

//            if (mainSupportingBlockPos.isPresent()) {
//                BlockPos blockpos = this.mainSupportingBlockPos.get();
//            }

            // 确定点位是否在墙里来调整点位高度
            float p1y = (float) this.traceBlockY(p1, 3);
            float p2y = (float) this.traceBlockY(p2, 3);
            float p3y = (float) this.traceBlockY(p3, 3);
            float p4y = (float) this.traceBlockY(p4, 3);

            p1 = new Vec3(positionLF.x, p1y, positionLF.z);
            p2 = new Vec3(positionRF.x, p2y, positionRF.z);
            p3 = new Vec3(positionLB.x, p3y, positionLB.z);
            p4 = new Vec3(positionRB.x, p4y, positionRB.z);

            // 测试用粒子效果，用于确定点位位置

//            List<Entity> entities = getPlayer(level());
//            for (var e : entities) {
//                if (e instanceof ServerPlayer player) {
//                    if (player.level() instanceof ServerLevel serverLevel) {
//                        sendParticle(serverLevel, ParticleTypes.END_ROD, p1.x, p1.y, p1.z, 1, 0, 0, 0, 0, true);
//                        sendParticle(serverLevel, ParticleTypes.END_ROD, p2.x, p2.y, p2.z, 1, 0, 0, 0, 0, true);
//                        sendParticle(serverLevel, ParticleTypes.END_ROD, p3.x, p3.y, p3.z, 1, 0, 0, 0, 0, true);
//                        sendParticle(serverLevel, ParticleTypes.END_ROD, p4.x, p4.y, p4.z, 1, 0, 0, 0, 0, true);
//                    }
//                }
//            }

            // 通过点位位置获取角度

            // 左后-左前
            Vec3 v0 = p3.vectorTo(p1);
            // 右后-右前
            Vec3 v1 = p4.vectorTo(p2);
            // 左前-右前
            Vec3 v2 = p1.vectorTo(p2);
            // 左后-右后
            Vec3 v3 = p3.vectorTo(p4);

            double x1 = getXRotFromVector(v0);
            double x2 = getXRotFromVector(v1);
            double z1 = getXRotFromVector(v2);
            double z2 = getXRotFromVector(v3);

            float diffX = Math.clamp(-15f, 15f, Mth.wrapDegrees((float) (-(x1 + x2)) - getXRot()));
            setXRot(Mth.clamp(getXRot() + 0.15f * diffX, -45f, 45f));

            float diffZ = Math.clamp(-15f, 15f, Mth.wrapDegrees((float) (-(z1 + z2)) - getRoll()));
            setZRot(Mth.clamp(getRoll() + 0.15f * diffZ, -45f, 45f));
        } else if (isInWater()) {
            setXRot(getXRot() * 0.9f);
            setZRot(getRoll() * 0.9f);
        }
    }

    //用于履带的地形适应
    public float[] terrainCompactTrackValue(float w, float l) {
        Matrix4f transform = this.getWheelsTransform(1);

        // 左前
        Vector4f positionLF = transformPosition(transform, w / 2, 0, l / 2);
        // 右前
        Vector4f positionRF = transformPosition(transform, -w / 2, 0, l / 2);
        // 左后
        Vector4f positionLB = transformPosition(transform, w / 2, 0, -l / 2);
        // 右后
        Vector4f positionRB = transformPosition(transform, -w / 2, 0, -l / 2);

        Vec3 p1 = new Vec3(positionLF.x, positionLF.y, positionLF.z);
        Vec3 p2 = new Vec3(positionRF.x, positionRF.y, positionRF.z);
        Vec3 p3 = new Vec3(positionLB.x, positionLB.y, positionLB.z);
        Vec3 p4 = new Vec3(positionRB.x, positionRB.y, positionRB.z);

        // 确定点位是否在墙里来调整点位高度
        float p1y = (float) this.traceBlockY(p1, 3);
        float p2y = (float) this.traceBlockY(p2, 3);
        float p3y = (float) this.traceBlockY(p3, 3);
        float p4y = (float) this.traceBlockY(p4, 3);

        p1 = new Vec3(positionLF.x, p1y, positionLF.z);
        p2 = new Vec3(positionRF.x, p2y, positionRF.z);
        p3 = new Vec3(positionLB.x, p3y, positionLB.z);
        p4 = new Vec3(positionRB.x, p4y, positionRB.z);

        Vec3 v0 = p3.vectorTo(p1);
        Vec3 v1 = p4.vectorTo(p2);
        Vec3 v2 = p1.vectorTo(p2);
        Vec3 v3 = p3.vectorTo(p4);

        double x1 = getXRotFromVector(v0);
        double x2 = getXRotFromVector(v1);

        double z1 = getXRotFromVector(v2);
        double z2 = getXRotFromVector(v3);

        float x = Math.clamp(-15f, 15f, Mth.wrapDegrees((float) (-(x1 + x2)) - getXRot()));
        float z = Math.clamp(-15f, 15f, Mth.wrapDegrees((float) (-(z1 + z2)) - getRoll()));

        return new float[]{x, z};
    }

    public Matrix4f getWheelsTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo, getY()), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        return transform;
    }

    public double traceBlockY(Vec3 pos, double maxLength) {
        var res = this.level().clip(new ClipContext(pos, pos.add(0, -maxLength, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        double targetY;

        BlockState state = level().getBlockState(BlockPos.containing(pos));
        VoxelShape shape = state.getCollisionShape(level(), BlockPos.containing(pos));
        if (!shape.isEmpty()) {
            targetY = pos.y + shape.max(Direction.Axis.Y);
        } else if (res.getType() == HitResult.Type.BLOCK && this.level().noCollision(new AABB(pos, pos))) {
            targetY = res.getLocation().y;
        } else {
            targetY = pos.y - maxLength;
        }

        double diffY = targetY - pos.y;
        return pos.y + 0.5f * diffY;
    }

    public void moveOnDragonTeeth() {
        AABB aabb = this.getBoundingBox();
        AABB aabb1 = new AABB(aabb.minX, aabb.minY - 1.0E-6D, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ);
        Optional<BlockPos> optional = this.level().findSupportingBlock(this, aabb1);
        if (optional.isPresent()) {
            BlockState state = level().getBlockState(optional.get());
            if (state.is(ModBlocks.DRAGON_TEETH.get())) {
                entityData.set(POWER, entityData.get(POWER) * 0.8f);
                setDeltaMovement(getDeltaMovement().multiply(-0.1, 0, -0.1));
            }
        }
    }

    public void collideSoftBlock() {
        if (!VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.get()) return;

        if (this instanceof OBBEntity obbEntity) {
            AABB aabb = getBoundingBox().move(this.getDeltaMovement()).inflate(5);
            BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
                BlockState blockstate = this.level().getBlockState(pos);
                if (blockstate.is(ModTags.Blocks.SOFT_COLLISION) && isInObb(obbEntity, pos, getDeltaMovement())) {
                    this.level().destroyBlock(pos, true);
                }
            });
        }
        AABB aabb = getBoundingBox().inflate(0.25, 0, 0.25).move(this.getDeltaMovement()).move(0, 0.5, 0);
        BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
            BlockState blockstate = this.level().getBlockState(pos);
            if (blockstate.is(ModTags.Blocks.SOFT_COLLISION)) {
                this.level().destroyBlock(pos, true);
            }
        });
    }

    public void collideNormalBlock() {
        if (!VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.get()) return;
        if (this instanceof OBBEntity obbEntity) {
            AABB aabb = getBoundingBox().move(this.getDeltaMovement()).inflate(5);
            BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
                BlockState blockstate = this.level().getBlockState(pos);
                if (blockstate.is(ModTags.Blocks.NORMAL_COLLISION) && isInObb(obbEntity, pos, getDeltaMovement())) {
                    this.level().destroyBlock(pos, true);
                }
            });
        }

        AABB aabb = getBoundingBox().inflate(0.25, 0, 0.25).move(this.getDeltaMovement()).move(0, 0.5, 0);
        BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
            BlockState blockstate = this.level().getBlockState(pos);
            if (blockstate.is(ModTags.Blocks.NORMAL_COLLISION)) {
                this.level().destroyBlock(pos, true);
            }
        });

    }

    public void collideHardBlock() {
        if (!VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.get()) return;
        if (this instanceof OBBEntity obbEntity) {
            AABB aabb = getBoundingBox().move(this.getDeltaMovement()).inflate(5);
            BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
                BlockState blockstate = this.level().getBlockState(pos);

                if (blockstate.is(ModTags.Blocks.HARD_COLLISION) && isInObb(obbEntity, pos, getDeltaMovement())) {
                    this.level().destroyBlock(pos, true);
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.95));
                }
            });
        }

        AABB aabb = getBoundingBox().inflate(0.25, 0, 0.25).move(this.getDeltaMovement()).move(0, 0.5, 0);
        BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
            BlockState blockstate = this.level().getBlockState(pos);
            if (blockstate.is(ModTags.Blocks.HARD_COLLISION)) {
                this.level().destroyBlock(pos, true);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.95));
            }
        });
    }

    public void collideBlockBeastly() {
        if (!VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.get()) return;

        if (this instanceof OBBEntity obbEntity) {
            AABB aabb = getBoundingBox().move(this.getDeltaMovement()).move(0, 0.5, 0).inflate(5);
            BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
                BlockState blockstate = this.level().getBlockState(pos);
                float hardness = blockstate.getBlock().defaultDestroyTime();
                if (hardness > 0 && hardness <= 4 && isInObb(obbEntity, pos, getDeltaMovement())) {
                    this.level().destroyBlock(pos, true);
                }
            });
        }

        AABB aabb = getBoundingBox().inflate(0.25, 0, 0.25).move(this.getDeltaMovement()).move(0, 0.5, 0);
        BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
            BlockState blockstate = this.level().getBlockState(pos);
            float hardness = blockstate.getBlock().defaultDestroyTime();
            if (hardness > 0 && hardness <= 4) {
                this.level().destroyBlock(pos, true);
            }
        });
    }

    public boolean canCollideHardBlock() {
        return false;
    }

    public boolean canCollideBlockBeastly() {
        return false;
    }

    @Override
    public void move(@NotNull MoverType movementType, @NotNull Vec3 movement) {
        if (!this.level().isClientSide()) {
            VehicleEntity.IGNORE_ENTITY_GROUND_CHECK_STEPPING = true;
        }

        super.move(movementType, movement);

        if (lastTickSpeed < 0.3 || collisionCoolDown > 0 || this instanceof DroneEntity) return;
        Entity driver = EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_DRIVER_UUID));

        if (verticalCollision) {
            if (this.getVehicleType() == VehicleType.HELICOPTER) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) (60 * ((lastTickSpeed - 0.3) * (lastTickSpeed - 0.3))));
                this.bounceVertical(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
            } else if (Mth.abs((float) lastTickVerticalSpeed) > 0.4) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) (96 * ((Mth.abs((float) lastTickVerticalSpeed) - 0.4) * (lastTickSpeed - 0.3) * (lastTickSpeed - 0.3))));
                if (!this.level().isClientSide) {
                    this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                }
                this.bounceVertical(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
            }
        }

        if (this.horizontalCollision) {
            this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) (126 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))));
            this.bounceHorizontal(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
            if (!this.level().isClientSide) {
                this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
            }
            collisionCoolDown = 4;
            crash = true;
            this.entityData.set(POWER, 0.8f * entityData.get(POWER));
        }
    }

    public void bounceHorizontal(Direction direction) {
        switch (direction.getAxis()) {
            case X:
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 0.99, 0.99));
                break;
            case Z:
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.99, 0.8));
                break;
        }
    }

    public void bounceVertical(Direction direction) {
        if (!this.level().isClientSide) {
            this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
        }
        collisionCoolDown = 4;
        crash = true;
        if (direction.getAxis() == Direction.Axis.Y) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.9, -0.8, 0.9));
        }
    }

    /**
     * 防止载具堆叠
     */
    public void preventStacking() {

        var entities = level().getEntities(
                EntityTypeTest.forClass(VehicleEntity.class),
                getBoundingBox().inflate(6),
                entity -> entity != this && entity != getFirstPassenger() && entity.getVehicle() == null
        );

        for (var entity : entities) {
            if (entity.getBoundingBox().intersects(getBoundingBox())) {
                Vec3 toVec = this.position().add(new Vec3(1, 1, 1).scale(random.nextFloat() * 0.01f + 1f)).vectorTo(entity.position());
                Vec3 velAdd = toVec.normalize().scale(Math.max((this.getBbWidth() + 2) - position().distanceTo(entity.position()), 0) * 0.1);
                double entitySize = entity.getBbWidth() * entity.getBbHeight();
                double thisSize = this.getBbWidth() * this.getBbHeight();
                double f = Math.min(entitySize / thisSize, 2);
                double f1 = Math.min(thisSize / entitySize, 2);

                this.pushNew(-f * velAdd.x, -f * velAdd.y, -f * velAdd.z);
                entity.push(f1 * velAdd.x, f1 * velAdd.y, f1 * velAdd.z);
            }
        }
    }

    public void pushNew(double pX, double pY, double pZ) {
        this.setDeltaMovement(this.getDeltaMovement().add(pX, pY, pZ));
    }

    /**
     * 撞击实体并造成伤害
     *
     * @param vec3 动量
     */
    public void crushEntities(Vec3 vec3) {
        if (level() instanceof ServerLevel) {
            if (!this.canCrushEntities()) return;
            if (isRemoved()) return;

            List<Entity> entities;

            if (this instanceof OBBEntity obbEntity) {
                var frontBox = getBoundingBox().move(vec3).inflate(6);
                entities = level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox,
                                entity -> entity != this && entity != getFirstPassenger() && entity.getVehicle() == null)
                        .stream().filter(entity -> {
                                    if (entity.isAlive() && isInObb(obbEntity, entity, vec3)) {
                                        var type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                                        return (entity instanceof VehicleEntity || entity instanceof Boat || entity instanceof Minecart || (entity instanceof LivingEntity living && !(living instanceof Player player && player.isSpectator()))) || VehicleConfig.COLLISION_ENTITY_WHITELIST.get().contains(type.toString());
                                    }
                                    return false;
                                }
                        )
                        .toList();

            } else {
                var frontBox = getBoundingBox().move(vec3);
                entities = level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox,
                                entity -> entity != this && entity != getFirstPassenger() && entity.getVehicle() == null)
                        .stream().filter(entity -> {
                                    if (entity.isAlive()) {
                                        var type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                                        return (entity instanceof VehicleEntity || entity instanceof Boat || entity instanceof Minecart
                                                || (entity instanceof LivingEntity living && !(living instanceof Player player && player.isSpectator())))
                                                || VehicleConfig.COLLISION_ENTITY_WHITELIST.get().contains(type.toString());
                                    }
                                    return false;
                                }
                        )
                        .toList();
            }

            for (var entity : entities) {
                double entitySize = entity.getBoundingBox().getSize();
                double thisSize = this.getBoundingBox().getSize();
                double f;
                double f1;

                Vec3 v0 = vec3.subtract(entity.getDeltaMovement());
                if (VectorTool.calculateAngle(v0, position().vectorTo(entity.position())) > 90) return;

                // TODO 给非载具实体也设置质量

                if (entity instanceof LivingEntity living && living.hasEffect(ModMobEffects.STRIKE_PROTECTION))
                    return;

                if (entity instanceof VehicleEntity vehicle) {
                    f = Mth.clamp(vehicle.getMass() / getMass(), 0.25, 4);
                    f1 = Mth.clamp(getMass() / vehicle.getMass(), 0.25, 4);
                } else {
                    f = Mth.clamp(entitySize / thisSize, 0.25, 4);
                    f1 = Mth.clamp(thisSize / entitySize, 0.25, 4);

                }

                float v = (float) v0.length();
                var velAdd = v0.normalize().scale(0.8 * v);

                if (v > 0.3) {
                    this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                }

                if (vec3.length() > 0.3) {
                    if (entity instanceof LivingEntity) {
                        DamageHandler.doDamage(entity, ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), (float) (f1 * 80 * (Mth.abs(v) - 0.3) * (Mth.abs(v) - 0.3)));
                    } else {
                        entity.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), (float) (f1 * 60 * (Mth.abs(v) - 0.3) * (Mth.abs(v) - 0.3)));
                    }

                    if (entity instanceof VehicleEntity) {
                        this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), entity, entity.getFirstPassenger() == null ? entity : entity.getFirstPassenger()), (float) (f * 40 * (Mth.abs(v) - 0.3) * (Mth.abs(v) - 0.3)));
                    }
                }

                if (!(entity instanceof TargetEntity)) {
                    this.pushNew(-0.3f * f * velAdd.x, -0.3f * f * velAdd.y, -0.3f * f * velAdd.z);
                }

                if (entity instanceof VehicleEntity mobileVehicle) {
                    if (this instanceof OBBEntity obbEntity) {
                        if (isInObb(obbEntity, entity, Vec3.ZERO)) {
                            Vec3 thisPos = this.position();
                            Vec3 otherPos = entity.position();

                            for (OBB obb : obbEntity.getOBBs()) {
                                if (entity instanceof OBBEntity obbEntity2) {
                                    var obbList2 = obbEntity2.getOBBs();
                                    for (var obb2 : obbList2) {
                                        if (OBB.isColliding(obb, obb2)) {
                                            thisPos = new Vec3(obb.center());
                                            otherPos = new Vec3(obb2.center());
                                        }
                                    }
                                } else {
                                    if (OBB.isColliding(obb, entity.getBoundingBox())) {
                                        thisPos = new Vec3(obb.center());
                                    }
                                }
                            }

                            Vec3 toVec = thisPos.add(new Vec3(1, 1, 1).scale(random.nextFloat() * 0.01f + 1f)).vectorTo(otherPos);
                            velAdd = toVec.normalize().scale(Math.max(thisPos.distanceTo(otherPos), 0) * 0.01);
                            this.pushNew(-f * velAdd.x, -f * velAdd.y, -f * velAdd.z);
                        }
                    }

                    mobileVehicle.pushNew(f1 * velAdd.x, f1 * velAdd.y, f1 * velAdd.z);
                } else {
                    entity.push(f1 * velAdd.x, f1 * velAdd.y, f1 * velAdd.z);
                }
            }
        }
    }

    public boolean isInObb(OBBEntity obbEntity, Entity entity, Vec3 vec3) {
        var obbList = obbEntity.getOBBs();
        for (var obb : obbList) {
            obb = obb.move(vec3);
            if (entity instanceof OBBEntity obbEntity2) {
                var obbList2 = obbEntity2.getOBBs();
                for (var obb2 : obbList2) {
                    if (OBB.isColliding(obb, obb2)) {
                        return true;
                    }
                }
            } else {
                if (OBB.isColliding(obb, entity.getBoundingBox())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isInObb(OBBEntity obbEntity, BlockPos pos, Vec3 vec3) {
        var obbList = obbEntity.getOBBs();
        var vec = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        AABB aabb1 = new AABB(vec, vec).inflate(0.3, 0.6, 0.3);
        for (var obb : obbList) {
            obb = obb.move(vec3);
            if (OBB.isColliding(obb, aabb1)) {
                return true;
            }
        }
        return false;
    }

    public Vector3f getForwardDirection() {
        return new Vector3f(
                Mth.sin(-getYRot() * ((float) Math.PI / 180)),
                0.0f,
                Mth.cos(getYRot() * ((float) Math.PI / 180))
        ).normalize();
    }

    public Vector3f getRightDirection() {
        return new Vector3f(
                Mth.cos(-getYRot() * ((float) Math.PI / 180)),
                0.0f,
                Mth.sin(getYRot() * ((float) Math.PI / 180))
        ).normalize();
    }

    public SoundEvent getEngineSound() {
        return SoundEvents.EMPTY;
    }

    public float getEngineSoundVolume() {
        return (float) Mth.lerp(Mth.clamp(getDeltaMovement().length(), 0F, 0.5F), 0.0F, 0.7F);
    }

    public double getVelocity() {
        return this.velocity;
    }

    public void setVelocity(double pV) {
        this.velocity = pV;
    }

    public double getAcceleration() {
        return getVelocity() - velocityO;
    }

    public float getRudderRot() {
        return this.rudderRot;
    }

    public void setRudderRot(float pRudderRot) {
        this.rudderRot = pRudderRot;
    }

    public float getLeftWheelRot() {
        return this.leftWheelRot;
    }

    public void setLeftWheelRot(float pLeftWheelRot) {
        this.leftWheelRot = pLeftWheelRot;
    }

    public float getRightWheelRot() {
        return this.rightWheelRot;
    }

    public void setRightWheelRot(float pRightWheelRot) {
        this.rightWheelRot = pRightWheelRot;
    }

    public float getLeftTrack() {
        return this.leftTrack;
    }

    public void setLeftTrack(float pLeftTrack) {
        this.leftTrack = pLeftTrack;
    }

    public float getRightTrack() {
        return this.rightTrack;
    }

    public void setRightTrack(float pRightTrack) {
        this.rightTrack = pRightTrack;
    }

    public float getRotorRot() {
        return this.rotorRot;
    }

    public void setRotorRot(float pRotorRot) {
        this.rotorRot = pRotorRot;
    }

    public float getPropellerRot() {
        return this.propellerRot;
    }

    public void setPropellerRot(float pPropellerRot) {
        this.propellerRot = pPropellerRot;
    }

    public double getRecoilShake() {
        return this.recoilShake;
    }

    public void setRecoilShake(double pRecoilShake) {
        this.recoilShake = pRecoilShake;
    }

    public float getFlap1LRot() {
        return this.flap1LRot;
    }

    public void setFlap1L2Rot(float pFlap1L2Rot) {
        this.flap1L2Rot = pFlap1L2Rot;
    }

    public float getFlap1R2Rot() {
        return this.flap1R2Rot;
    }

    public void setFlap1R2Rot(float pFlap1R2Rot) {
        this.flap1R2Rot = pFlap1R2Rot;
    }

    public float getFlap1L2Rot() {
        return this.flap1L2Rot;
    }

    public void setFlap1LRot(float pFlap1LRot) {
        this.flap1LRot = pFlap1LRot;
    }

    public float getFlap1RRot() {
        return this.flap1RRot;
    }

    public void setFlap1RRot(float pFlap1RRot) {
        this.flap1RRot = pFlap1RRot;
    }

    public float getFlap2LRot() {
        return this.flap2LRot;
    }

    public void setFlap2LRot(float pFlap2LRot) {
        this.flap2LRot = pFlap2LRot;
    }

    public float getFlap2RRot() {
        return this.flap2RRot;
    }

    public void setFlap2RRot(float pFlap2RRot) {
        this.flap2RRot = pFlap2RRot;
    }

    public float getFlap3Rot() {
        return this.flap3Rot;
    }

    public void setFlap3Rot(float pFlap3Rot) {
        this.flap3Rot = pFlap3Rot;
    }

    // TODO 用数据包定义，直接定义最大数量会不会更好？
    public boolean hasDecoy() {
        return false;
    }

    public boolean engineRunning() {
        return Math.abs(this.entityData.get(POWER)) > 0;
    }

    /**
     * 撬棍shift+右键收回载具时返还的物品
     */
    public @NotNull List<ItemStack> getRetrieveItems() {
        return List.of(ContainerBlockItem.createInstance(this));
    }

    public int getHudColor() {
        return data().get(VehicleProp.HUD_COLOR).get();
    }

    public float getPower() {
        return entityData.get(POWER);
    }

    public int getDecoyCount() {
        return entityData.get(DECOY_COUNT);
    }

    @NotNull
    public SoundEvent getHornSound() {
        return SoundEvents.EMPTY;
    }

    @NotNull
    public SoundEvent getInCarMusicSound() {
        var passenger = this.getFirstPassenger();
        if (passenger instanceof Player player) {
            var stack = player.getOffhandItem();

            var playableData = stack.get(DataComponents.JUKEBOX_PLAYABLE);
            if (playableData == null) return SoundEvents.EMPTY;

            return playableData.song().unwrap(this.level().registryAccess())
                    .map(h -> h.value().soundEvent().value())
                    .orElse(SoundEvents.EMPTY);
        }
        return SoundEvents.EMPTY;
    }

    public void horn() {
        entityData.set(HORN_VOLUME, entityData.get(HORN_VOLUME) + 0.7f);
    }

    public boolean hornWorking() {
        return Math.abs(this.entityData.get(HORN_VOLUME)) > 0.05;
    }

    // TODO 以更好的方式播放车载音乐，现在是读取副手的唱片
    public boolean inCarMusicPlaying() {
        if (!this.level().isClientSide) return false;
        if (!(this.getFirstPassenger() instanceof Player player)) return false;
        var stack = player.getOffhandItem();
        return stack.get(DataComponents.JUKEBOX_PLAYABLE) != null || NetMusicCompatHolder.canPlayMusic(stack);
    }

    public boolean amphibiousVehicle() {
        return getVehicleType() == VehicleType.TANK
                || getVehicleType() == VehicleType.APC
                || getVehicleType() == VehicleType.AA
                || getVehicleType() == VehicleType.CAR
                || getVehicleType() == VehicleType.BOAT;
    }

    public VehicleType getVehicleType() {
        return data().get(VehicleProp.TYPE);
    }

    // TODO 实现正确的OBB碰撞检测
//    /**
//     * @author YWZJ Ranpoes
//     */
//    public void support(Entity entity) {
//        if (!(this instanceof OBBEntity obbEntity)) return;
//
//        Vec3 feetPos = entity.position().subtract(new Vec3(0, 0.1f, 0));
//        for (var obb : obbEntity.getOBBs()) {
//            if (obb.contains(feetPos)) {
//                if (!entity.noPhysics && !this.noPhysics) {
//                    double gravity = Math.max(entity.getDeltaMovement().y, 0);
//                    if (gravity == 0) {
//                        entity.setOnGround(true);
//                    }
//                    double depth = obb.getEmbeddingDepth(feetPos);
//                    entity.setDeltaMovement(this.getDeltaMovement().add(0, gravity + depth < 0.1f ? 0 : depth, 0));
//
//                    continue;
//                }
//            }
//            if (!entity.noPhysics && !this.noPhysics) {
//                var aabb = entity.getBoundingBox();
//                if (OBB.isColliding(obb, aabb)) {
//                    int face = obb.getEmbeddingFace(feetPos);
//                    var axes = obb.getAxes();
//                    var support = axes[Math.abs(face) - 1];
//                    if (face < 0) {
//                        support.negate();
//                    }
//                    if (entity.isPushable()) {
//                        float force = 0.1f;
//                        if (this.getDeltaMovement().length() > 0.01 && face != -2) {
//                            force = 0.2f;
//                        }
//                        entity.setPos(entity.position().add(new Vec3(support).scale(force)));
//                        this.hasImpulse = true;
//                    }
//                }
//            }
//        }
//    }

    // TODO 实现正确的AABB包围箱
//    @Override
//    public EntityDimensions getDimensions(Pose pPose) {
//        var aabb = this.calculateCombinedAABBOptimized();
//        var max = Math.max(aabb.getXsize(), aabb.getZsize());
//        return EntityDimensions.scalable((float) max, (float) aabb.getYsize());
//    }
//
//    public AABB calculateCombinedAABBOptimized() {
//        if (!(this instanceof OBBEntity obbEntity) || obbEntity.getOBBs().isEmpty()) {
//            return this.getBoundingBox();
//        }
//
//        var obbList = obbEntity.getOBBs();
//
//        Vector3f min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
//        Vector3f max = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
//
//        for (OBB obb : obbList) {
//            Vector3f[] vertices = getOBBVertices(obb);
//
//            for (Vector3f vertex : vertices) {
//                min.x = Math.min(min.x, vertex.x);
//                min.y = Math.min(min.y, vertex.y);
//                min.z = Math.min(min.z, vertex.z);
//
//                max.x = Math.max(max.x, vertex.x);
//                max.y = Math.max(max.y, vertex.y);
//                max.z = Math.max(max.z, vertex.z);
//            }
//        }
//
//        return new AABB(new Vec3(min), new Vec3(max));
//    }
//
//    private Vector3f[] getOBBVertices(OBB obb) {
//        Vector3f[] vertices = new Vector3f[8];
//
//        Vector3f[] axes = new Vector3f[3];
//        axes[0] = obb.rotation().transform(new Vector3f(1, 0, 0));
//        axes[1] = obb.rotation().transform(new Vector3f(0, 1, 0));
//        axes[2] = obb.rotation().transform(new Vector3f(0, 0, 1));
//
//        for (int i = 0; i < 8; i++) {
//            float signX = ((i & 1) == 0) ? 1.0f : -1.0f;
//            float signY = ((i & 2) == 0) ? 1.0f : -1.0f;
//            float signZ = ((i & 4) == 0) ? 1.0f : -1.0f;
//
//            Vector3f vertex = new Vector3f(obb.center());
//            vertex.add(new Vector3f(axes[0]).mul(obb.extents().x * signX));
//            vertex.add(new Vector3f(axes[1]).mul(obb.extents().y * signY));
//            vertex.add(new Vector3f(axes[2]).mul(obb.extents().z * signZ));
//
//            vertices[i] = vertex;
//        }
//
//        return vertices;
//    }
}
