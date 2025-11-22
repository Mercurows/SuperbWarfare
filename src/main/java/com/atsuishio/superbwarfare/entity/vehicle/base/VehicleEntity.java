package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.energy.SyncedEntityEnergyStorage;
import com.atsuishio.superbwarfare.capability.energy.VehicleEnergyStorage;
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.compat.netmusic.NetMusicCompatHolder;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.DataLoader;
import com.atsuishio.superbwarfare.data.StringOrVec3;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData;
import com.atsuishio.superbwarfare.data.vehicle.VehicleData;
import com.atsuishio.superbwarfare.data.vehicle.VehiclePropertyModifier;
import com.atsuishio.superbwarfare.data.vehicle.subdata.*;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.utils.*;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.ProjectileWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.atsuishio.superbwarfare.menu.VehicleMenu;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.resource.vehicle.VehicleResource;
import com.atsuishio.superbwarfare.tools.*;
import com.atsuishio.superbwarfare.world.TDMSavedData;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraPitch;
import static com.atsuishio.superbwarfare.event.ClientMouseHandler.freeCameraYaw;
import static com.atsuishio.superbwarfare.tools.TraceTool.pickNew;

public abstract class VehicleEntity extends Entity implements VehiclePropertyModifier, HasCustomInventoryScreen, ContainerEntity {
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
    public static final EntityDataAccessor<IntList> SELECTED_WEAPON = SynchedEntityData.defineId(VehicleEntity.class, ModSerializers.INT_LIST_SERIALIZER.get());

    public static final EntityDataAccessor<Float> TURRET_HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> L_WHEEL_HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> R_WHEEL_HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> MAIN_ENGINE_HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> SUB_ENGINE_HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Boolean> TURRET_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> L_WHEEL_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> R_WHEEL_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> MAIN_ENGINE_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> SUB_ENGINE_DAMAGED = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);

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
    public static Consumer<VehicleEntity> fireSound = vehicle -> {
    };

    public static boolean IGNORE_ENTITY_GROUND_CHECK_STEPPING = false;

    public static final EntityDataAccessor<Float> SERVER_YAW = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> SERVER_PITCH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Integer> CANNON_RECOIL_TIME = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> CANNON_RECOIL_FORCE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW_WHILE_SHOOT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Integer> FIRE_TIME = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Integer> AMMO = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DECOY_READY = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Float> PROPELLER_ROT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> GEAR_ROT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
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
    public static final EntityDataAccessor<Float> PLANE_BREAK = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Integer> ENERGY = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> LASER_LENGTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> LASER_SCALE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> LASER_SCALE_O = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> CHARGE_PROGRESS = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    // Map SeatIndex -> GunData
    protected static final EntityDataAccessor<Map<String, GunData>> GUN_DATA_MAP = SynchedEntityData.defineId(VehicleEntity.class, ModSerializers.VEHICLE_GUN_DATA_MAP_SERIALIZER.get());

    public Map<String, GunData> getGunDataMap() {
        var rawMap = entityData.get(GUN_DATA_MAP);
        var newMap = new HashMap<String, GunData>();
        var weapons = computed().weapons();

        for (var kv : weapons.entrySet()) {
            var data = rawMap.get(kv.getKey());

            if (data == null) {
                data = GunData.from(new ItemStack(ModItems.VEHICLE_GUN.get()));
            }

            data.defaultDataSupplier = kv::getValue;
            newMap.put(kv.getKey(), data);
        }

        return newMap;
    }

    public @Nullable SeatInfo getSeat(int seatIndex) {
        if (seatIndex < 0) return null;

        var seats = computed().seats();
        if (seatIndex >= seats.size()) return null;
        return seats.get(seatIndex);
    }

    public @Nullable SeatInfo getSeat(Entity passenger) {
        return getSeat(getSeatIndex(passenger));
    }

    /**
     * 获取载具座位上选中的武器
     *
     * @param seatIndex 座位号
     * @return 武器数据
     */
    public @Nullable GunData getGunData(int seatIndex) {
        if (seatIndex < 0) return null;
        var selectedWeapon = this.entityData.get(SELECTED_WEAPON);
        if (seatIndex >= selectedWeapon.size()) return null;
        return getGunData(seatIndex, selectedWeapon.getInt(seatIndex));
    }

    /**
     * 获取载具座位上指定编号的武器
     *
     * @param seatIndex   座位号
     * @param weaponIndex 武器号
     * @return 武器数据
     */
    public @Nullable GunData getGunData(int seatIndex, int weaponIndex) {
        var seat = getSeat(seatIndex);
        if (seat == null) return null;

        var weapons = seat.weapons();
        if (weaponIndex < 0 || weaponIndex >= weapons.size()) return null;

        return getGunData(weapons.get(weaponIndex));
    }

    /**
     * 获取载具的乘客座位上指定编号的武器
     *
     * @param passenger   乘客
     * @param weaponIndex 武器号
     * @return 武器数据
     */
    public @Nullable GunData getGunData(Entity passenger, int weaponIndex) {
        return getGunData(getSeatIndex(passenger), weaponIndex);
    }

    /**
     * 获取载具的乘客座位上选中的武器
     *
     * @param passenger 乘客
     * @return 武器数据
     */
    public @Nullable GunData getGunData(Entity passenger) {
        return getGunData(passenger, this.getSelectedWeapon(this.getSeatIndex(passenger)));
    }

    /**
     * 根据名称获取武器
     *
     * @param name 武器名称
     * @return 武器数据
     */
    public @Nullable GunData getGunData(String name) {
        return getGunDataMap().get(name);
    }

    public @Nullable String getGunName(int seatIndex) {
        if (seatIndex < 0) return null;
        var seat = getSeat(seatIndex);
        if (seat == null) return null;

        var selectedWeapon = this.entityData.get(SELECTED_WEAPON);
        if (seatIndex >= selectedWeapon.size()) return null;

        var weaponIndex = selectedWeapon.getInt(seatIndex);
        if (weaponIndex < 0) return null;

        var weapons = seat.weapons();
        if (weaponIndex >= weapons.size()) return null;

        return getGunName(seatIndex, weaponIndex);
    }

    public @Nullable String getGunName(int seatIndex, int weaponIndex) {
        if (seatIndex < 0) return null;
        var seat = getSeat(seatIndex);
        if (seat == null) return null;

        var selectedWeapon = this.entityData.get(SELECTED_WEAPON);
        if (seatIndex >= selectedWeapon.size()) return null;

        var weapons = seat.weapons();
        if (weaponIndex >= weapons.size()) return null;

        return weapons.get(weaponIndex);
    }

    public void modifyGunData(int seatIndex, int weaponIndex, @NotNull Consumer<GunData> consumer) {
        modifyGunData(getGunName(seatIndex, weaponIndex), consumer);
    }

    public void modifyGunData(int seatIndex, @NotNull Consumer<GunData> consumer) {
        modifyGunData(getGunName(seatIndex), consumer);
    }

    public void modifyGunData(@Nullable String name, @NotNull Consumer<GunData> consumer) {
        if (name == null) return;

        var map = getGunDataMap();
        var data = getGunData(name);
        if (data == null) return;

        data = data.copy();
        consumer.accept(data);
        data.save();
        map.put(name, data);

        entityData.set(GUN_DATA_MAP, map);
    }

    public VehicleWeapon[][] availableWeapons;

    protected int interpolationSteps;
    protected double x;
    protected double y;
    protected double z;

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
    private boolean wasFiring = false;
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

    public float propellerRot;
    public float propellerRotO;

    public double recoilShake;
    public double recoilShakeO;

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
    public float gearRot;
    public Vec3 lerpBombHitPosO;
    public Vec3 lerpBombHitPos;
    public boolean engineStart;
    public boolean engineStartOver;

    public double bombHitPosX;
    public double bombHitPosY;
    public double bombHitPosZ;

    public int holdTick;
    public int holdPowerTick;
    public float destroyRot;

    public int jumpCoolDown;

    public VehicleEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        registerTransforms();
        initOBB();

        if (this.hasEnergyStorage()) {
            this.energyStorage = new VehicleEnergyStorage(this);
        }
        this.isInitialized = true;

        this.setHealth(this.getMaxHealth());
    }

    private List<OBBInfo> obbCache = new ArrayList<>();

    private void initOBB() {
        if (!(this instanceof OBBEntity)) return;

        this.obbCache = data().getDefault().copy().obb.stream().filter(Objects::nonNull).toList();
    }

    public List<OBBInfo> getOBB() {
        return obbCache;
    }

    @Override
    public void onSyncedDataUpdated(@NotNull List<SynchedEntityData.DataValue<?>> dataValues) {
        super.onSyncedDataUpdated(dataValues);

        data().update();
    }

    public void processInput(short keys) {
        setLeftInputDown((keys & 0b000000001) > 0);
        setRightInputDown((keys & 0b000000010) > 0);
        setForwardInputDown((keys & 0b000000100) > 0);
        setBackInputDown((keys & 0b000001000) > 0);
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

    public void setFireInputDown(boolean set) {
        entityData.set(FIRE_INPUT_DOWN, set);
    }

    public void setDecoyInputDown(boolean set) {
        entityData.set(DECOY_INPUT_DOWN, set);
    }

    public void setSprintInputDown(boolean set) {
        entityData.set(SPRINT_INPUT_DOWN, set);
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
    private LazyOptional<?> itemHandler = LazyOptional.of(() -> new InvWrapper(this));
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

    // TODO 0.8.9重置物品栏
    @Override
    public int getContainerSize() {
        var type = computed().vehicleContainerType;
        if (type == null) return 0;
        if (type.hasMenu()) return 102;
        return computed().vehicleContainerType.getSize();
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
        return this.hasContainer() && !this.isRemoved() && this.position().closerThan(pPlayer.position(), 8);
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

    @Nullable
    @Override
    public ResourceLocation getLootTable() {
        return null;
    }

    @Override
    public void setLootTable(@Nullable ResourceLocation pLootTable) {
    }

    @Override
    public long getLootTableSeed() {
        return 0;
    }

    @Override
    public void setLootTableSeed(long pLootTableSeed) {
    }

    public boolean hasMenu() {
        return computed().vehicleContainerType.hasMenu();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, Player pPlayer) {
        if (!pPlayer.isSpectator() && this.hasMenu()) {
            var computed = computed();
            var type = computed.vehicleContainerType;
            if (type == null || !type.hasMenu()) return null;

//            var upgrade = computed.hasUpgradeSlots;
//            var menu = switch (type) {
//                case MINI ->
//                        upgrade ? ModMenuTypes.VEHICLE_MENU_MINI_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_MINI.get();
//                case SMALL ->
//                        upgrade ? ModMenuTypes.VEHICLE_MENU_SMALL_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_SMALL.get();
//                case MEDIUM ->
//                        upgrade ? ModMenuTypes.VEHICLE_MENU_MEDIUM_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_MEDIUM.get();
//                case LARGE ->
//                        upgrade ? ModMenuTypes.VEHICLE_MENU_LARGE_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_LARGE.get();
//                case HUGE ->
//                        upgrade ? ModMenuTypes.VEHICLE_MENU_HUGE_UPGRADE.get() : ModMenuTypes.VEHICLE_MENU_HUGE.get();
//                default -> null;
//            };
//            if (menu == null) return null;
//
//            return new VehicleMenu(menu, pContainerId, pPlayerInventory, this, type.getRow(), type.getCol(), upgrade);
            return new VehicleMenu(ModMenuTypes.VEHICLE_MENU_HUGE.get(), pContainerId, pPlayerInventory, this, 6, 17, false);
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
    }

    protected <T> void padList(@NotNull List<T> list, int targetSize, T defaultValue, @Nullable Consumer<T> onRemove) {
        while (targetSize != list.size()) {
            if (targetSize > list.size()) {
                list.add(defaultValue);
            } else {
                var last = list.remove(list.size() - 1);
                if (last != null && onRemove != null) {
                    onRemove.accept(last);
                }
            }
        }
    }

    protected void checkSeatsSize() {
        int targetSize = computed().seats().size();
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

    public DefaultVehicleData computed() {
        return VehicleData.compute(this);
    }

    @Override
    public float getStepHeight() {
        return computed().upStep;
    }

    @Override
    public @Nullable Entity getFirstPassenger() {
        checkSeatsSize();
        if (orderedPassengers.isEmpty()) {
            return null;
        }
        return orderedPassengers.get(0);
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

    public Vec3 getThirdPersonCameraPosition() {
        var pos = computed().thirdPersonCameraPos;
        if (pos == null) {
            pos = new Vec3(0, 1, 3);
        }
        return new Vec3(pos.z + ClientMouseHandler.custom3pDistanceLerp, pos.y, pos.x);
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
        if (this instanceof MortarEntity) return;
        if (level().isClientSide && (Math.abs(diffY) > 0.5 || Math.abs(diffX) > 0.5)) {
            level().playLocalSound(this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), ModSounds.TURRET_TURN.get(), this.getSoundSource(), (float) java.lang.Math.min(0.15 * (java.lang.Math.max(Mth.abs(diffX), Mth.abs(diffY))), 0.75), (random.nextFloat() * 0.05f + pitch), false);
        }
    }

    /**
     * 受击时是否出现粒子效果
     */
    public boolean shouldSendHitParticles() {
        return computed().sendHitParticles;
    }

    /**
     * 受击时是否出现音效
     */
    public boolean shouldSendHitSounds() {
        return true;
    }

    protected SyncedEntityEnergyStorage energyStorage = null;
    protected LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> energyStorage);

    protected boolean isInitialized;

    public boolean isInitialized() {
        return this.isInitialized;
    }

    public EntityDataAccessor<Integer> getEnergyDataAccessor() {
        return ENERGY;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OVERRIDE, "");
        this.entityData.define(HEALTH, this.getMaxHealth());
        this.entityData.define(LAST_ATTACKER_UUID, "undefined");
        this.entityData.define(LAST_DRIVER_UUID, "undefined");
        this.entityData.define(GUN_DATA_MAP, new HashMap<>());

        this.entityData.define(AI_TURRET_TARGET_UUID, "undefined");
        this.entityData.define(AI_PASSENGER_WEAPON_TARGET_UUID, "undefined");

        this.entityData.define(DELTA_ROT, 0f);
        this.entityData.define(MOUSE_SPEED_X, 0f);
        this.entityData.define(MOUSE_SPEED_Y, 0f);

        this.entityData.define(TURRET_HEALTH, getTurretMaxHealth());
        this.entityData.define(L_WHEEL_HEALTH, getWheelMaxHealth());
        this.entityData.define(R_WHEEL_HEALTH, getWheelMaxHealth());
        this.entityData.define(MAIN_ENGINE_HEALTH, getEngineMaxHealth());
        this.entityData.define(SUB_ENGINE_HEALTH, getEngineMaxHealth());

        this.entityData.define(TURRET_DAMAGED, false);
        this.entityData.define(L_WHEEL_DAMAGED, false);
        this.entityData.define(R_WHEEL_DAMAGED, false);
        this.entityData.define(MAIN_ENGINE_DAMAGED, false);
        this.entityData.define(SUB_ENGINE_DAMAGED, false);

        this.entityData.define(CANNON_RECOIL_TIME, 0);
        this.entityData.define(CANNON_RECOIL_FORCE, 0f);
        this.entityData.define(POWER, 0f);
        this.entityData.define(YAW_WHILE_SHOOT, 0f);
        this.entityData.define(SERVER_YAW, getYRot());
        this.entityData.define(SERVER_PITCH, getXRot());
        this.entityData.define(AMMO, 0);
        this.entityData.define(FIRE_TIME, 0);
        this.entityData.define(DECOY_READY, false);
        this.entityData.define(GEAR_ROT, 0f);
        this.entityData.define(GEAR_UP, false);
        this.entityData.define(FORWARD_INPUT_DOWN, false);
        this.entityData.define(BACK_INPUT_DOWN, false);
        this.entityData.define(LEFT_INPUT_DOWN, false);
        this.entityData.define(RIGHT_INPUT_DOWN, false);
        this.entityData.define(UP_INPUT_DOWN, false);
        this.entityData.define(DOWN_INPUT_DOWN, false);
        this.entityData.define(FIRE_INPUT_DOWN, false);
        this.entityData.define(DECOY_INPUT_DOWN, false);
        this.entityData.define(SPRINT_INPUT_DOWN, false);

        this.entityData.define(PLANE_BREAK, 0f);
        this.entityData.define(SELECTED_WEAPON, IntList.of(new int[this.getMaxPassengers()]));
        this.entityData.define(ENERGY, 0);
        this.entityData.define(PROPELLER_ROT, 0f);

        this.entityData.define(HORN_VOLUME, 0f);
        this.entityData.define(LASER_LENGTH, 0f);
        this.entityData.define(LASER_SCALE, 0f);
        this.entityData.define(LASER_SCALE_O, 0f);
        this.entityData.define(CHARGE_PROGRESS, 0f);
    }

    // energy start

    /**
     * 消耗指定电量
     *
     * @param amount 要消耗的电量
     */
    public void consumeEnergy(int amount) {
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
        return computed().maxEnergy;
    }

    public boolean hasEnergyStorage() {
        return this.computed().maxEnergy > 0;
    }

    // energy end

    // TODO 正确实现武器信息
    public List<VehicleWeapon> getAvailableWeapons(int seatIndex) {
        var weapons = getAllWeapons();
        if (seatIndex < 0 || seatIndex >= weapons.length) return List.of();

        return List.of(weapons[seatIndex]);
    }

    protected VehicleWeapon[][] getAllWeapons() {
        return computed().seats().stream().map(seat -> {
            if (seat == null || seat.weapons().isEmpty()) return new ProjectileWeapon[0];

            return seat.weapons().stream().map(name -> {
                var data = getGunData(name);
                if (data == null) return new ProjectileWeapon();

                var sound = data.compute().soundInfo;
                var icon = data.compute().icon;
                return new ProjectileWeapon()
                        .zoom(false)
                        .sound(sound.change)
                        .icon(icon);
            }).toArray(VehicleWeapon[]::new);
        }).toArray(VehicleWeapon[][]::new);
    }

    /**
     * 当前情况载具是否可以开火
     *
     * @param living 玩家
     * @return 是否可以开火
     */
    public boolean canShoot(LivingEntity living) {
        var gunData = getGunData(getSeatIndex(living));
        return gunData != null && gunData.canShoot(getAmmoSupplier());
    }

    /**
     * 主武器射速
     *
     * @return 射速
     */
    public int vehicleWeaponRpm(LivingEntity living) {
        var data = getGunData(getSeatIndex(living));
        if (data == null) return 0;
        return data.compute().rpm;
    }

    public int vehicleWeaponRpm(int seatIndex) {
        var data = getGunData(seatIndex);
        if (data == null) return 0;
        return data.compute().rpm;
    }

    public int vehicleWeaponRpm(String weaponName) {
        var data = getGunData(weaponName);
        if (data == null) return 0;
        return data.compute().rpm;
    }

    public int getWeaponHeat(LivingEntity living) {
        var gunData = getGunData(getSeatIndex(living));
        if (gunData == null) return 0;
        return java.lang.Math.toIntExact(java.lang.Math.round(gunData.heat.get()));
    }

    public int getWeaponHeat(int seatIndex) {
        var gunData = getGunData(seatIndex);
        if (gunData == null) return 0;
        return java.lang.Math.toIntExact(java.lang.Math.round(gunData.heat.get()));
    }

    public int getWeaponHeat(String weaponName) {
        var gunData = getGunData(weaponName);
        if (gunData == null) return 0;
        return java.lang.Math.toIntExact(java.lang.Math.round(gunData.heat.get()));
    }

    public int getWeaponHeat(int seatIndex, int weaponIndex) {
        var gunData = getGunData(seatIndex, weaponIndex);
        if (gunData == null) return 0;
        return java.lang.Math.toIntExact(java.lang.Math.round(gunData.heat.get()));
    }

    public int getShootAnimationTimer(String weaponName) {
        var gunData = getGunData(weaponName);
        if (gunData == null) return 0;
        return gunData.shootAnimationTimer.get();
    }

    public int getShootAnimationTimer(int seatIndex, int weaponIndex) {
        var gunData = getGunData(seatIndex, weaponIndex);
        if (gunData == null) return 0;
        return gunData.shootAnimationTimer.get();
    }

    public void vehicleShoot(LivingEntity living, String weaponName) {
        modifyGunData(weaponName, data -> {
            if (!data.canShoot(getAmmoSupplier())) return;
            data.shoot(new ShootParameters(getAmmoSupplier(), living, (ServerLevel) this.level(), getShootPos(weaponName, 1), getShootVec(weaponName, 1), data, data.compute().spread, true, null, null));
        });

        var gunData = getGunData(weaponName);
        afterShoot(gunData, getShootVec(weaponName, 1));
        playShootSound3p(living, weaponName);
    }

    public void vehicleShoot(LivingEntity living, @Nullable UUID uuid, @Nullable Vec3 targetPos) {
        int seatIndex = getSeatIndex(living);
        modifyGunData(seatIndex, data -> {
            if (!data.canShoot(getAmmoSupplier())) return;
            data.shoot(new ShootParameters(getAmmoSupplier(), living, (ServerLevel) this.level(), getShootPos(living, 1), getShootVec(living, 1),
                    data, data.compute().spread, true, uuid, targetPos));
        });

        var gunData = getGunData(seatIndex);
        afterShoot(gunData, getShootVec(living, 1));
        playShootSound3p(living, seatIndex);
    }

    public void afterShoot(GunData gunData, Vec3 shootVec) {
        if (gunData != null) {
            var computedGunData = gunData.compute();
            if (computedGunData.recoilTime > 0) {
                if (computedGunData.recoilTime > entityData.get(CANNON_RECOIL_TIME)) {
                    entityData.set(CANNON_RECOIL_TIME, computedGunData.recoilTime);
                }

                float angle = (float) Mth.wrapDegrees(-VehicleVecUtils.getYRotFromVector(getViewVector(1)) + VehicleVecUtils.getYRotFromVector(shootVec));

                Vec3 vo = new Vec3(0, 0, 1);
                double f = 0.3 * entityData.get(CANNON_RECOIL_FORCE) * (double) (entityData.get(CANNON_RECOIL_TIME) / computedGunData.recoilTime);
                Vec3 v1 = vo.yRot(entityData.get(YAW_WHILE_SHOOT) * Mth.DEG_TO_RAD).scale(f);
                Vec3 v2 = vo.yRot(angle * Mth.DEG_TO_RAD).scale(computedGunData.recoilForce);
                Vec3 v3 = v1.add(v2);

                entityData.set(YAW_WHILE_SHOOT, (float) Mth.wrapDegrees(-VehicleVecUtils.getYRotFromVector(vo) + VehicleVecUtils.getYRotFromVector(v3)));
                entityData.set(CANNON_RECOIL_FORCE, (float) v3.length());

                gunData.shakePlayers(this);
            }
        }

        entityData.set(FIRE_TIME, Math.min(entityData.get(FIRE_TIME) + 3, 5));

    }

    public float shootingVolume() {
        return entityData.get(FIRE_TIME) * 0.25f;
    }

    public float shootingPitch() {
        var gunData = getGunData(0);
        if (gunData != null) {
            return (float) (0.98f + entityData.get(FIRE_TIME) * 0.01f - (gunData.heat.get() > 80 ? (gunData.heat.get() - 80) * 0.01 : 0));
        } else {
            return 1;
        }
    }

    public void playShootSound3p(LivingEntity living, String weaponName) {
        var gunData = this.getGunData(weaponName);
        if (gunData == null) return;
        var pos = getShootPos(weaponName, 1);

        playShootSound3p(living, gunData, pos);
    }

    public void playShootSound3p(LivingEntity living, int seatIndex) {
        var gunData = this.getGunData(seatIndex);
        if (gunData == null) return;
        var pos = getShootPos(living, 1);

        playShootSound3p(living, gunData, pos);
    }

    public void playShootSound3p(LivingEntity living, GunData gunData, Vec3 pos) {
        if (!(living.level() instanceof ServerLevel serverLevel)) return;

        if (gunData == null) return;

        var computedGunData = gunData.compute();
        var soundInfo = computedGunData.soundInfo;
        float pitch = getWeaponHeat(living) <= 60 ? 1 : (float) (1 - 0.011 * java.lang.Math.abs(60 - getWeaponHeat(living)));

        Entity listener = living.getVehicle() == this ? living : null;

        if (soundInfo.fire3P != null) {
            SoundTool.playDistantSound(serverLevel, soundInfo.fire3P, pos, (float) (computedGunData.soundRadius * 0.4f), pitch, listener);
        }

        if (soundInfo.fire3PFar != null) {
            SoundTool.playDistantSound(serverLevel, soundInfo.fire3PFar, pos, (float) (computedGunData.soundRadius * 0.7f), pitch, listener);
        }

        if (soundInfo.fire3PVeryFar != null) {
            SoundTool.playDistantSound(serverLevel, soundInfo.fire3PVeryFar, pos, (float) computedGunData.soundRadius, pitch, listener);
        }
    }


    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.entityData.set(OVERRIDE, compound.getString("Override"));

        // GunData
        var state = compound.getCompound("WeaponState");
        var gunDataMap = new HashMap<String, GunData>();
        for (var key : state.getAllKeys()) {
            var tag = state.getCompound(key);

            tag = tag.copy();
            tag.putString("id", "superbwarfare:vehicle_gun");
            tag.putInt("Count", 1);

            gunDataMap.put(key, GunData.from(ItemStack.of(tag)));
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
        this.entityData.set(MAIN_ENGINE_HEALTH, compound.getFloat("MainEngineHealth"));
        this.entityData.set(SUB_ENGINE_HEALTH, compound.getFloat("SubEngineHealth"));

        this.entityData.set(TURRET_DAMAGED, compound.getBoolean("TurretDamaged"));
        this.entityData.set(L_WHEEL_DAMAGED, compound.getBoolean("LeftWheelDamaged"));
        this.entityData.set(R_WHEEL_DAMAGED, compound.getBoolean("RightWheelDamaged"));
        this.entityData.set(MAIN_ENGINE_DAMAGED, compound.getBoolean("MainEngineDamaged"));
        this.entityData.set(SUB_ENGINE_DAMAGED, compound.getBoolean("SubEngineDamaged"));

        this.entityData.set(POWER, compound.getFloat("Power"));
        this.entityData.set(DECOY_READY, compound.getBoolean("DecoyReady"));
        this.entityData.set(GEAR_ROT, compound.getFloat("GearRot"));
        this.entityData.set(GEAR_UP, compound.getBoolean("GearUp"));
        this.entityData.set(PROPELLER_ROT, compound.getFloat("PropellerRot"));
        this.entityData.set(CHARGE_PROGRESS, compound.getFloat("ChargeProgress"));
        this.entityData.set(LAST_ATTACKER_UUID, compound.getString("LastAttacker"));
        this.entityData.set(LAST_DRIVER_UUID, compound.getString("LastDriver"));

        this.entityData.set(SERVER_YAW, compound.getFloat("ServerYaw"));
        this.entityData.set(SERVER_PITCH, compound.getFloat("ServerPitch"));


        var selectedWeaponTag = compound.get("SelectedWeapon");
        int[] selected;
        if (selectedWeaponTag instanceof IntArrayTag arrayTag) {
            selected = arrayTag.getAsIntArray();
        } else {
            selected = new int[this.getMaxPassengers()];
        }

        if (selected.length != this.getMaxPassengers()) {
            // 数量不符时（可能是更新或遇到损坏数据），重新初始化已选择武器
            this.entityData.set(SELECTED_WEAPON, IntList.of(new int[this.getMaxPassengers()]));
        } else {
            this.entityData.set(SELECTED_WEAPON, IntList.of(selected));
        }

        if (this.hasEnergyStorage() && compound.get("Energy") instanceof IntTag energyNBT) {
            energyStorage.deserializeNBT(energyNBT);
        }

        this.resizeItems();
        ContainerHelper.loadAllItems(compound, this.getItemStacks());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        checkSeatsSize();

        compound.putFloat("Health", this.entityData.get(HEALTH));

        var overrideString = this.entityData.get(OVERRIDE);
        if (!overrideString.isBlank()) {
            compound.putString("Override", overrideString);
        }

        compound.putString("LastAttacker", this.entityData.get(LAST_ATTACKER_UUID));
        compound.putString("LastDriver", this.entityData.get(LAST_DRIVER_UUID));

        var gunDataMap = entityData.get(GUN_DATA_MAP);

        var tag = new CompoundTag();
        for (var kv : gunDataMap.entrySet()) {
            var data = GunData.from(kv.getValue().stack.copy());
            data.backupAmmoCount.reset();
            data.save();

            var stackTag = data.stack.save(new CompoundTag());
            stackTag.remove("id");
            stackTag.remove("Count");
            if (stackTag.isEmpty()) continue;

            tag.put(String.valueOf(kv.getKey()), stackTag);
        }

        if (!tag.isEmpty()) {
            compound.put("WeaponState", tag);
        }

        compound.putFloat("TurretHealth", this.entityData.get(TURRET_HEALTH));
        compound.putFloat("LeftWheelHealth", this.entityData.get(L_WHEEL_HEALTH));
        compound.putFloat("RightWheelHealth", this.entityData.get(R_WHEEL_HEALTH));
        compound.putFloat("MainEngineHealth", this.entityData.get(MAIN_ENGINE_HEALTH));
        compound.putFloat("SubEngineHealth", this.entityData.get(SUB_ENGINE_HEALTH));

        compound.putBoolean("TurretDamaged", this.entityData.get(TURRET_DAMAGED));
        compound.putBoolean("LeftWheelDamaged", this.entityData.get(L_WHEEL_DAMAGED));
        compound.putBoolean("RightWheelDamaged", this.entityData.get(R_WHEEL_DAMAGED));
        compound.putBoolean("MainEngineDamaged", this.entityData.get(MAIN_ENGINE_DAMAGED));
        compound.putBoolean("SubEngineDamaged", this.entityData.get(SUB_ENGINE_DAMAGED));

        compound.putFloat("Power", this.entityData.get(POWER));
        compound.putBoolean("DecoyReady", this.entityData.get(DECOY_READY));
        compound.putFloat("GearRot", this.entityData.get(GEAR_ROT));
        compound.putBoolean("GearUp", this.entityData.get(GEAR_UP));
        compound.putFloat("PropellerRot", this.entityData.get(PROPELLER_ROT));
        compound.putFloat("ChargeProgress", this.entityData.get(CHARGE_PROGRESS));

        compound.putFloat("ServerYaw", this.entityData.get(SERVER_YAW));
        compound.putFloat("ServerPitch", this.entityData.get(SERVER_PITCH));

        if (this.getMaxPassengers() > 0) {
            compound.putIntArray("SelectedWeapon", this.entityData.get(SELECTED_WEAPON).toIntArray());
        }

        if (this.hasEnergyStorage()) {
            compound.put("Energy", energyStorage.serializeNBT());
        }

        this.resizeItems();
        ContainerHelper.saveAllItems(compound, this.getItemStacks());
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
                VehicleVecUtils.setDriverAngle(this, player);
                player.setSprinting(false);
                return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
            } else if (!(this.getFirstPassenger() instanceof Player)) {
                if (player instanceof FakePlayer) return InteractionResult.PASS;
                this.getFirstPassenger().stopRiding();
                VehicleVecUtils.setDriverAngle(this, player);
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

    @Deprecated(forRemoval = true)
    public void setDriverAngle(Player player) {
        VehicleVecUtils.setDriverAngle(this, player);
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
            computedAmount = this.getDamageModifier().compute(this, source, amount);
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
                    case MAIN_ENGINE ->
                            entityData.set(MAIN_ENGINE_HEALTH, entityData.get(MAIN_ENGINE_HEALTH) - computedAmount);
                    case SUB_ENGINE ->
                            entityData.set(SUB_ENGINE_HEALTH, entityData.get(SUB_ENGINE_HEALTH) - computedAmount);
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

    public float getSourceAngle(DamageSource source, float multiplier) {
        return VehicleVecUtils.getDamageSourceAngle(this, source, multiplier);
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
                NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(3, 5));
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
        this.entityData.set(HEALTH, Mth.clamp(pHealth, 0F, this.getMaxHealth()));
    }

    public float getMaxHealth() {
        return computed().maxHealth;
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
    @ParametersAreNonnullByDefault
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(ModSounds.WHEEL_VEHICLE_STEP.get(), (float) (getDeltaMovement().length() * 0.1), random.nextFloat() * 0.15f + 1.05f);
    }

    @Override
    public boolean canBeCollidedWith() {
        return !(this instanceof OBBEntity obbEntity) || obbEntity.getOBBs().isEmpty();
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
        return computed().seats().size();
    }

    /**
     * 呼吸回血冷却时长(单位:tick)，设为小于0的值以禁用呼吸回血
     */
    public int maxRepairCoolDown() {
        return computed().repairCooldown;
    }

    /**
     * 呼吸回血回血量
     */
    public float repairAmount() {
        return computed().repairAmount;
    }

    @Override
    public void baseTick() {
        var computed = computed();
        if (this.level().isClientSide) {
            if (!this.wasEngineRunning && this.engineRunning()) {
                engineSound.accept(this);
                swimSound.accept(this);
                if (computed.engineType == EngineType.TRACK) {
                    trackSound.accept(this);
                }
            }

            if (!this.wasHornWorking && this.hornWorking()) {
                hornSound.accept(this);
            }

            if (!this.wasInCarMusicPlaying && this.inCarMusicPlaying()) {
                inCarMusic.accept(this);
            }

            if (fireSound != null && !this.wasFiring && this.isFiring() && this.level().isClientSide()) {
                fireSound.accept(this);
            }
            this.wasFiring = this.isFiring();
        }

        // 枪数据处理
        if (!this.level().isClientSide) {
            var newMap = new HashMap<String, GunData>();

            for (var kv : entityData.get(GUN_DATA_MAP).entrySet()) {
                var newData = kv.getValue().copy();
                newData.tick(this, true);
                newMap.put(kv.getKey(), newData);
            }
            entityData.set(GUN_DATA_MAP, newMap);
        }

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

        rudderRotO = this.getRudderRot();

        propellerRotO = this.getPropellerRot();

        recoilShakeO = this.getRecoilShake();

        velocityO = this.getVelocity();
        if (jumpCoolDown > 0 && onGround()) {
            jumpCoolDown--;
        }

        lastTickSpeed = new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y + 0.06, this.getDeltaMovement().z).length();
        lastTickVerticalSpeed = this.getDeltaMovement().y + 0.06;
        if (collisionCoolDown > 0) {
            collisionCoolDown--;
        }

        this.entityData.set(LASER_SCALE_O, this.entityData.get(LASER_SCALE));

        flap1LRotO = this.getFlap1LRot();
        flap1RRotO = this.getFlap1RRot();
        flap1L2RotO = this.getFlap1L2Rot();
        flap1R2RotO = this.getFlap1R2Rot();
        flap2LRotO = this.getFlap2LRot();
        flap2RRotO = this.getFlap2RRot();
        flap3RotO = this.getFlap3Rot();
        gearRotO = this.getGearRot();
        lerpBombHitPosO = lerpBombHitPos;

        super.baseTick();

        if (this.entityData.get(LASER_SCALE) > 0) {
            this.entityData.set(LASER_SCALE, Math.max(this.entityData.get(LASER_SCALE) - 0.1f, 0));
            this.entityData.set(LASER_SCALE, this.entityData.get(LASER_SCALE) * 0.9f);
        }

        if (this.entityData.get(LASER_SCALE) == 0) {
            this.entityData.set(LASER_LENGTH, 0f);
        }

        if (repairCoolDown > 0) {
            repairCoolDown--;
        }

        if (getHealth() >= getMaxHealth()) {
            repairCoolDown = maxRepairCoolDown();
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

        if (this.getHealth() <= computed.selfHurtPercent * this.getMaxHealth()) {
            // 血量过低时自动扣血
            this.onHurt(computed.selfHurtAmount, attacker, false);
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
            if (getNthEntity(getTurretControllerIndex()) instanceof Player) {
                this.adjustTurretAngle();
            } else if (getNthEntity(getTurretControllerIndex()) instanceof Mob mob) {
                this.turretAutoAimFromUuid(entityData.get(AI_TURRET_TARGET_UUID), mob);
            }
        }

        if (hasPassengerWeaponStation()) {
            if (getNthEntity(getPassengerWeaponStationControllerIndex()) instanceof Player || getNthEntity(getPassengerWeaponStationControllerIndex()) == null) {
                this.gunnerAngle();
            } else if (getNthEntity(getPassengerWeaponStationControllerIndex()) instanceof Mob mob) {
                this.passengerWeaponAutoAimFormUuid(entityData.get(AI_PASSENGER_WEAPON_TARGET_UUID), mob);
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

        double direct = (90 - VehicleVecUtils.calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90;
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

        if (decoyReloadCoolDown > 0) {
            decoyReloadCoolDown--;
        }

        if (this.entityData.get(CANNON_RECOIL_TIME) > 0) {
            this.entityData.set(CANNON_RECOIL_TIME, this.entityData.get(CANNON_RECOIL_TIME) - 1);
        }

        this.setRecoilShake(Mth.abs(entityData.get(CANNON_RECOIL_FORCE)) * 0.0000007 * java.lang.Math.pow(entityData.get(CANNON_RECOIL_TIME), 4) * java.lang.Math.sin(0.2 * java.lang.Math.PI * (entityData.get(CANNON_RECOIL_TIME) - 2.5)));
        entityData.set(CANNON_RECOIL_FORCE, entityData.get(CANNON_RECOIL_FORCE) * 0.93f);

        this.preventStacking();
        this.supportEntities();
        this.crushEntities();

        this.setDeltaMovement(this.getDeltaMovement().add(0, -this.computed().gravity, 0));

        this.move(MoverType.SELF, this.getDeltaMovement());

        this.collideBlocks();
        this.moveOnDragonTeeth();

        if (this.hasEnergyStorage() && this.tickCount % 20 == 0) {
            for (var stack : this.getItemStacks()) {
                int neededEnergy = this.getMaxEnergy() - this.getEnergy();
                if (neededEnergy <= 0) break;

                var energyCap = stack.getCapability(ForgeCapabilities.ENERGY).resolve();
                if (energyCap.isEmpty()) continue;

                var energyStorage = energyCap.get();
                var stored = energyStorage.getEnergyStored();
                if (stored <= 0) continue;

                int energyToExtract = Math.min(stored, neededEnergy);
                energyStorage.extractEnergy(energyToExtract, false);
                this.setEnergy(this.getEnergy() + energyToExtract);
            }
        }

        if (this.level() instanceof ServerLevel) {
            updateBackupAmmoCount();
        }

        entityData.set(HORN_VOLUME, entityData.get(HORN_VOLUME) * 0.5f);
        if (entityData.get(FIRE_TIME) > 0) {
            entityData.set(FIRE_TIME, entityData.get(FIRE_TIME) - 1);
        }

        if (hasDecoy()) {
            if (getVehicleType() == VehicleType.AIRPLANE || getVehicleType() == VehicleType.HELICOPTER) {
                releaseDecoy();
            } else {
                releaseSmokeDecoy(getTurretVector(1));
            }
        }

        var terrainCompat = this.computed().terrainCompat;
        if (terrainCompat.x > 0 && terrainCompat.y > 0) {
            this.terrainCompact(terrainCompat.x, terrainCompat.y);
        }
        this.inertiaRotate(this.computed().inertiaRotateRate);

        if (getLeftTrack() < 0) {
            setLeftTrack(getTrackAnimationLength());
        }

        if (getLeftTrack() > getTrackAnimationLength()) {
            setLeftTrack(0);
        }

        if (getRightTrack() < 0) {
            setRightTrack(getTrackAnimationLength());
        }

        if (getRightTrack() > getTrackAnimationLength()) {
            setRightTrack(0);
        }

        lowHealthWarning();
        this.refreshDimensions();

        if (this instanceof OBBEntity obbEntity) {
            obbEntity.updateOBB();
        }
    }

    public SoundEvent getShootSoundInstance() {
        // TODO why 0?
        var gunData = getGunData(0);
        if (gunData != null) {
            var instance = gunData.compute().soundInfo.fireSoundInstances;
            if (instance != null) return instance;
        } else {
            return getShootSoundInstance("Main");
        }
        return SoundEvents.EMPTY;
    }

    public SoundEvent getShootSoundInstance(String weaponName) {
        var gunData = getGunData(weaponName);
        if (gunData != null) {
            var instance = gunData.compute().soundInfo.fireSoundInstances;
            if (instance != null) return instance;
        }

        return SoundEvents.EMPTY;
    }

    public boolean isFiring() {
        return this.entityData.get(FIRE_TIME) > 0;
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

    /**
     * 获取开火用AmmoSupplier实体
     */
    public Entity getAmmoSupplier() {
        return this;
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
                case MAIN_ENGINE -> {
                    if (entityData.get(MAIN_ENGINE_DAMAGED)) {
                        this.onEngine1Damaged(pos);
                    }
                }
                case SUB_ENGINE -> {
                    if (entityData.get(SUB_ENGINE_DAMAGED)) {
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

        if (entityData.get(MAIN_ENGINE_HEALTH) < 0) {
            entityData.set(MAIN_ENGINE_DAMAGED, true);
        } else if (entityData.get(MAIN_ENGINE_HEALTH) > 0.95 * getEngineMaxHealth()) {
            entityData.set(MAIN_ENGINE_DAMAGED, false);
        }

        if (entityData.get(SUB_ENGINE_HEALTH) < 0) {
            entityData.set(SUB_ENGINE_DAMAGED, true);
        } else if (entityData.get(SUB_ENGINE_HEALTH) > 0.95 * getEngineMaxHealth()) {
            entityData.set(SUB_ENGINE_DAMAGED, false);
        }

        entityData.set(TURRET_HEALTH, Math.min(entityData.get(TURRET_HEALTH) + 0.0025f * getTurretMaxHealth(), getTurretMaxHealth()));
        entityData.set(L_WHEEL_HEALTH, Math.min(entityData.get(L_WHEEL_HEALTH) + 0.0025f * getWheelMaxHealth(), getWheelMaxHealth()));
        entityData.set(R_WHEEL_HEALTH, Math.min(entityData.get(R_WHEEL_HEALTH) + 0.0025f * getWheelMaxHealth(), getWheelMaxHealth()));
        entityData.set(MAIN_ENGINE_HEALTH, Math.min(entityData.get(MAIN_ENGINE_HEALTH) + 0.0025f * getEngineMaxHealth(), getEngineMaxHealth()));
        entityData.set(SUB_ENGINE_HEALTH, Math.min(entityData.get(SUB_ENGINE_HEALTH) + 0.0025f * getEngineMaxHealth(), getEngineMaxHealth()));
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
        List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0F, 0.5F, 0F), e -> e instanceof AbstractArrow);
        list.forEach(Entity::discard);
    }

    public void lowHealthWarning() {
        if (!data().compute().hasLowHealthWarning) return;
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

    public void adjustTurretAngle() {
        VehicleWeaponUtils.adjustTurretAngle(this);
    }

    public void aiTurretShoot(LivingEntity living, UUID uuid, Vec3 targetPos) {
        if (this instanceof WeaponVehicleEntity && aiTurretDiff < 2 && canShoot(living) && living.level() instanceof ServerLevel) {
            vehicleShoot(living, uuid, targetPos);
        }
    }

    public void aiPassengerWeaponShoot(LivingEntity living, UUID uuid, Vec3 targetPos) {
        if (this instanceof WeaponVehicleEntity && aiPassengerDiff < 2 && canShoot(living) && living.level() instanceof ServerLevel) {
            vehicleShoot(living, uuid, targetPos);
        }
    }

    public int getSelectedWeapon(int seatIndex) {
        var selectedWeapon = this.entityData.get(SELECTED_WEAPON);
        if (seatIndex < 0 || seatIndex >= selectedWeapon.size()) return -1;

        return selectedWeapon.getInt(seatIndex);
    }

    public void turretAutoAimFromVector(Vec3 shootVec) {
        VehicleWeaponUtils.turretAutoAimFromVector(this, shootVec);
    }

    public void turretAutoAimFromUuid(String uuid, LivingEntity pLiving) {
        VehicleWeaponUtils.turretAutoAimFromUuid(this, uuid, pLiving);
    }

    public void passengerPitch(Entity entity, float minPitch, float maxPitch, float passengerRot) {
        VehicleVecUtils.setPassengerPitch(this, entity, minPitch, maxPitch, passengerRot);
    }

    public void passengerYaw(Entity entity, float minYaw, float maxYaw, float passengerRot) {
        VehicleVecUtils.setPassengerYaw(this, entity, minYaw, maxYaw, passengerRot);
    }

    public void passengerPitchOnTurret(Entity entity, float turretMinPitch, float turretMaxPitch) {
        VehicleVecUtils.setPassengerPitchOnTurret(this, entity, turretMinPitch, turretMaxPitch);
    }

    public void passengerYawOnTurret(Entity entity, float minYaw, float maxYaw, float passengerRot, boolean rotateWithTurret) {
        VehicleVecUtils.setPassengerYawOnTurret(this, entity, minYaw, maxYaw, passengerRot, rotateWithTurret);
    }

    @Override
    public void onPassengerTurned(@NotNull Entity entity) {
        this.clampRotation(entity);
    }

    protected void clampRotation(Entity entity) {
        int index = getSeatIndex(entity);
        var seats = computed().seats();
        if (index < 0 || index >= seats.size()) return;
        var seat = seats.get(index);

        if (seat.transform.equals("Vehicle")
                || seat.transform.equals("VehicleFlat")
                || (seat.transform.equals("Turret") && seat.canRotateBody)
        ) {
            if (!seat.canRotateBody) {
                passengerYaw(entity, seat.minYaw, seat.maxYaw, seat.orientation);
            }

            if (hasTurret() && index == getTurretControllerIndex()) {
                if (seat.transform.equals("Vehicle") || seat.transform.equals("VehicleFlat")) {
                    float diffY = Mth.wrapDegrees(entity.getYRot() - this.getYRot());
                    passengerPitch(entity, seat.minPitch, seat.maxPitch, diffY);
                } else {
                    passengerPitchOnTurret(entity, seat.minPitch, seat.maxPitch);
                    passengerYawOnTurret(entity, seat.minYaw, seat.maxYaw, seat.orientation, true);
                }
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
        var seat = computed().seats().get(index);

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
        var seats = computed().seats();
        if (index < 0 || index >= seats.size()) return;

        var seat = seats.get(index);
        passengerPos(passenger, callback, seat.position, seat.transform);
    }

    public void passengerPos(Entity passenger, @NotNull MoveFunction callback, Vec3 vec3, String string) {
        Vector4f worldPosition = transformPosition(getTransformFromString(string), (float) vec3.x, (float) vec3.y, (float) vec3.z);
        passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z);
        copyEntityData(passenger);
    }

    protected Map<String, Function<Float, Matrix4f>> positionTransform = new HashMap<>();
    protected Map<String, Function<Float, Vec3>> vectorTransform = new HashMap<>();
    protected Map<String, Function<Float, Quaternionf>> rotationTransform = new HashMap<>();

    protected void registerTransforms() {
        positionTransform.put("VehicleFlat", this::getVehicleFlatTransform);
        positionTransform.put("Turret", this::getTurretTransform);
        positionTransform.put("Barrel", this::getBarrelTransform);
        positionTransform.put("WeaponStation", this::getGunTransform);
        positionTransform.put("WeaponStationBarrel", this::getPassengerWeaponStationBarrelTransform);
        positionTransform.put("Default", this::getVehicleTransform);

        vectorTransform.put("Turret", this::getTurretVector);
        vectorTransform.put("Barrel", this::getBarrelVector);
        vectorTransform.put("WeaponStationBarrel", this::getPassengerWeaponStationVector);
        vectorTransform.put("DeltaMovement", tick -> getDeltaMovement().normalize());
        vectorTransform.put("Up", this::getUpVec);
        vectorTransform.put("Default", this::getViewVector);

        rotationTransform.put("Turret", tick -> VectorTool.combineRotationsTurret(tick, this));
        rotationTransform.put("Barrel", tick -> VectorTool.combineRotationsBarrel(tick, this));
        rotationTransform.put("RotationsYaw", tick -> VectorTool.combineRotationsYaw(tick, this));
        rotationTransform.put("Default", tick -> VectorTool.combineRotations(tick, this));
    }

    public @NotNull Matrix4f getTransformFromString(String string) {
        return getTransformFromString(string, 1);
    }

    public @NotNull Matrix4f getTransformFromString(String string, float ticks) {
        return positionTransform
                .getOrDefault(string, positionTransform.get("Default"))
                .apply(ticks);
    }

    public @NotNull Vec3 getVectorFromString(String string) {
        return getVectorFromString(string, 0);
    }

    public @NotNull Vec3 getVectorFromString(String string, float ticks) {
        return vectorTransform
                .getOrDefault(string, vectorTransform.get("Default"))
                .apply(ticks);
    }

    public @NotNull Vec3 getVectorFromString(String string, float ticks, int seatIndex) {
        var entity = getNthEntity(seatIndex);
        return switch (string) {
            case "Bomb" ->
                    bombHitPos(getNthEntity(seatIndex), ticks).subtract(getShootPosForHud(getNthEntity(seatIndex), ticks));
            case "Passenger" -> entity != null ? entity.getViewVector(ticks) : getViewVector(ticks);
            default -> getVectorFromString(string, ticks);
        };
    }

    public @NotNull Quaternionf getRotationFromString(String string) {
        return getRotationFromString(string, 0);
    }

    public @NotNull Quaternionf getRotationFromString(String string, float ticks) {
        return rotationTransform
                .getOrDefault(string, rotationTransform.get("Default"))
                .apply(ticks);
    }

    /**
     * @return 炮弹发射位置
     */
    public Vec3 getShootPos(int seatIndex, float ticks) {
        return getShootPos(getNthEntity(seatIndex), ticks);
    }

    public Vec3 bombHitPos(Entity entity, float ticks) {
        var gunData = getGunData(entity);
        if (gunData != null) {
            Vec3 bombHitPos = ProjectileCalculator.calculatePreciseImpactPoint(level(), getShootPosForHud(entity, ticks), getShootVec(entity, ticks), getDeltaMovement().length() * gunData.compute().velocity, -projectileGravity(entity));
            bombHitPosX = Mth.lerp(0.2 * ticks, bombHitPosX, bombHitPos.x);
            bombHitPosY = Mth.lerp(0.2 * ticks, bombHitPosY, bombHitPos.y);
            bombHitPosZ = Mth.lerp(0.2 * ticks, bombHitPosZ, bombHitPos.z);
            return new Vec3(bombHitPosX, bombHitPosY, bombHitPosZ);
        } else {
            return Vec3.ZERO;
        }
    }

    /**
     * @param entity 操控载具的实体
     * @return 炮弹发射位置
     */
    public Vec3 getShootPos(Entity entity, float ticks) {
        var data = getGunData(getSeatIndex(entity));
        if (data != null) {
            var vec3 = data.firePosition();

            var worldPosition = transformPosition(
                    this.getTransformFromString(data.compute().shootPos.transform, ticks),
                    (float) vec3.x, (float) vec3.y, (float) vec3.z);

            return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
        }
        return getEyePosition(ticks);
    }

    public Vec3 getShootPos(String weaponName, float ticks) {
        var data = getGunData(weaponName);
        if (data != null) {
            var vec3 = data.firePosition();

            var worldPosition = transformPosition(
                    this.getTransformFromString(data.compute().shootPos.transform, ticks),
                    (float) vec3.x, (float) vec3.y, (float) vec3.z);

            return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
        }
        return getEyePosition(ticks);
    }

    /**
     * @param entity 操控载具的实体
     * @return 所有炮弹发射位置的中心点，用于HUD瞄准
     */
    public Vec3 getShootPosForHud(Entity entity, float ticks) {
        var data = getGunData(getSeatIndex(entity));
        if (data != null) {
            var vec3 = data.firePositionForHud();

            var worldPosition = transformPosition(
                    this.getTransformFromString(data.compute().shootPos.transform, ticks),
                    (float) vec3.x, (float) vec3.y, (float) vec3.z);

            return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
        }
        return getEyePosition(ticks);
    }

    /**
     * @param entity 操控载具的实体
     * @return 所有炮弹发射位置的方向，用于HUD瞄准
     */
    public Vec3 getShootDirectionForHud(Entity entity, float partialTicks) {
        var data = getGunData(getSeatIndex(entity));
        if (data == null) {
            return getViewVector(partialTicks);
        }

        StringOrVec3 stringOrVec3 = data.fireDirectionForHud();

        if (stringOrVec3 == null) {
            return getViewVec(entity, partialTicks);
        } else if (stringOrVec3.isString()) {
            return getVectorFromString(stringOrVec3.string, partialTicks, getSeatIndex(entity));
        } else {
            var vec3 = stringOrVec3.vec3;
            Vector4f worldPosition = transformPosition(
                    getTransformFromString(data.compute().shootPos.transform, partialTicks),
                    (float) vec3.x + (float) stringOrVec3.vec3.x,
                    (float) vec3.y + (float) stringOrVec3.vec3.y,
                    (float) vec3.z + (float) stringOrVec3.vec3.z);

            Vector4f worldPositionO = transformPosition(
                    getTransformFromString(data.compute().shootPos.transform, partialTicks),
                    (float) vec3.x,
                    (float) vec3.y,
                    (float) vec3.z);

            Vec3 startPos = new Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z);
            Vec3 endPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
            return startPos.vectorTo(endPos).normalize();
        }
    }

    public Vec3 getShootVec(int seatIndex, float ticks) {
        return getShootVec(getNthEntity(seatIndex), ticks);
    }

    public Vec3 getShootVec(Entity entity, float partialTicks) {
        return VehicleVecUtils.getShootVec(this, entity, partialTicks);
    }

    public Vec3 getShootVec(String weaponName, float partialTicks) {
        return VehicleVecUtils.getShootVec(this, weaponName, partialTicks);
    }

    public Vec3 getViewVec(Entity entity, float partialTicks) {
        return VehicleVecUtils.getViewVec(this, entity, partialTicks);
    }

    public Vec3 getViewPos(Entity entity, float partialTicks) {
        return VehicleVecUtils.getViewPos(this, entity, partialTicks);
    }

    public Vec3 getSeekVec(Entity entity, float partialTicks) {
        return VehicleVecUtils.getSeekVec(this, entity, partialTicks);
    }

    public Vec3 getSeekVec(int seatIndex, float partialTicks) {
        return VehicleVecUtils.getSeekVec(this, getNthEntity(seatIndex), partialTicks);
    }

    public Entity getPlayerLookAtEntityOnVehicle(Entity shooter, double entityReach, float partialTick) {
        Vec3 eye = getShootPosForHud(shooter, partialTick);
        double distance = entityReach * entityReach;
        HitResult hitResult = pickNew(eye, 512, this);

        Vec3 viewVec = getViewVec(shooter, partialTick);
        Vec3 toVec = eye.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(this, eye, toVec, aabb,
                p -> !p.isSpectator() && p.isAlive() && SeekTool.BASIC_FILTER.test(p) && !p.getType().is(ModTags.EntityTypes.DECOY) && SeekTool.NOT_IN_SMOKE.test(p) && p != shooter && !(p instanceof Projectile), distance);
        if (entityhitresult != null) {
            hitResult = entityhitresult;
        }
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            if (entityhitresult != null) {
                return entityhitresult.getEntity();
            }
        }
        return null;
    }

    /**
     * @param entity 操控载具的实体
     * @return 炮弹发射时的初始速度
     */

    public float projectileVelocity(Entity entity) {
        var gunData = getGunData(getSeatIndex(entity));
        if (gunData == null) return 25;
        if (gunData.compute().addShooterDeltaMovement) {
            return (float) (getDeltaMovement().length() * gunData.compute().velocity);
        }

        return (float) gunData.compute().velocity;
    }

    public float projectileVelocity(int seatIndex) {
        var gunData = getGunData(seatIndex);
        if (gunData == null) return 25;
        if (gunData.compute().addShooterDeltaMovement) {
            return (float) (getDeltaMovement().length() * gunData.compute().velocity);
        }

        return (float) gunData.compute().velocity;
    }

    public float projectileVelocity(String weaponName) {
        var gunData = getGunData(weaponName);
        if (gunData == null) return 25;
        if (gunData.compute().addShooterDeltaMovement) {
            return (float) (getDeltaMovement().length() * gunData.compute().velocity);
        }

        return (float) gunData.compute().velocity;
    }

    public float projectileVelocity(GunData gunData) {
        if (gunData == null) return 25;
        if (gunData.compute().addShooterDeltaMovement) {
            return (float) (getDeltaMovement().length() * gunData.compute().velocity);
        }
        return (float) gunData.compute().velocity;
    }

    /**
     * @param entity 操控载具的实体
     * @return 炮弹重力
     */

    public float projectileGravity(Entity entity) {
        var gunData = getGunData(getSeatIndex(entity));
        if (gunData == null) return 0;

        return (float) gunData.compute().gravity;
    }

    public float projectileGravity(int seatIndex) {
        var gunData = getGunData(seatIndex);
        if (gunData == null) return 0;

        return (float) gunData.compute().gravity;
    }

    public float projectileGravity(String weaponName) {
        var gunData = getGunData(weaponName);
        if (gunData == null) return 0;

        return (float) gunData.compute().gravity;
    }

    public float projectileGravity(GunData gunData) {
        if (gunData == null) return 0;
        return (float) gunData.compute().gravity;
    }

    /**
     * @param entity 操控载具的实体
     * @return 炮弹发射时的散布
     */

    public float projectileSpread(Entity entity) {
        var gunData = getGunData(getSeatIndex(entity));
        if (gunData == null) return 0.5f;

        return (float) gunData.compute().spread;
    }

    public float projectileSpread(int seatIndex) {
        var gunData = getGunData(seatIndex);
        if (gunData == null) return 0.5f;

        return (float) gunData.compute().spread;
    }

    public float projectileSpread(String weaponName) {
        var gunData = getGunData(weaponName);
        if (gunData == null) return 0.5f;

        return (float) gunData.compute().spread;
    }

    public float projectileSpread(GunData gunData) {
        if (gunData == null) return 0.5f;
        return (float) gunData.compute().spread;
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
                double gravity = living.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get());
                targetVel = targetVel.add(0, gravity, 0);
            }

            if (target instanceof Player) {
                targetVel = targetVel.multiply(2, 1, 2);
            }

            Vec3 targetVec = RangeTool.calculateFiringSolution(getShootPos(pLiving, 1), targetPos, targetVel, projectileVelocity(pLiving), projectileGravity(pLiving));
            passengerWeaponAutoAimFormVector(targetVec);

            int rpm = 20 / Mth.clamp((vehicleWeaponRpm(pLiving) / 60), 1, 2147483647);
            if (tickCount % rpm == 0) {
                aiPassengerWeaponShoot(pLiving, UUID.fromString(uuid), null);
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
        float diffY = (float) Mth.wrapDegrees(-VehicleVecUtils.getYRotFromVector(shootVec) + VehicleVecUtils.getYRotFromVector(getPassengerWeaponStationVector(1)));
        float diffX = (float) Mth.wrapDegrees(-VehicleVecUtils.getXRotFromVector(shootVec) + VehicleVecUtils.getXRotFromVector(getPassengerWeaponStationVector(1)));

        this.turretTurnSound(diffX, diffY, 0.95f);

        this.setGunXRot(Mth.clamp(this.getGunXRot() + Mth.clamp(0.5f * diffX, -xSpeed, xSpeed), -passengerWeaponMaxPitch(), -passengerWeaponMinPitch()));
        this.setGunYRot(Mth.clamp(this.getGunYRot() - Mth.clamp(0.5f * diffY, -ySpeed, ySpeed), -passengerWeaponMaxYaw(), -passengerWeaponMinYaw()));

        this.aiPassengerDiff = VectorTool.calculateAngle(shootVec, getPassengerWeaponStationVector(1));
    }

    public void gunnerAngle() {
        float ySpeed = passengerWeaponYSpeed();
        float xSpeed = passengerWeaponXSpeed();

        Entity gunner = this.getNthEntity(getPassengerWeaponStationControllerIndex());

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
        var destroyInfo = computed().destroyInfo;

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
        var computed = computed();

        var engineType = computed.engineType;
        if (engineType == EngineType.EMPTY) return;

        var engineInfo = computed.engineInfo;
        try {
            switch (engineType) {
                case FIXED -> this.fixedEngine();
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
                case SHIP -> {
                    var info = DataLoader.GSON.fromJson(engineInfo, EngineInfo.Ship.class);
                    this.shipEngine(info);
                }
                case AIRCRAFT -> {
                    var info = DataLoader.GSON.fromJson(engineInfo, EngineInfo.AirCraft.class);
                    this.airCraftEngine(info);
                }
                case WHEELCHAIR -> {
                    var info = DataLoader.GSON.fromJson(engineInfo, EngineInfo.WheelChair.class);
                    this.wheelChairEngine(info);
                }
            }
        } catch (Exception e) {
            Mod.LOGGER.error("Failed to parse engine info for vehicle {}, {}", this, e);
        }
    }

    public float getEngineSoundVolume() {
        var computed = computed();

        var engineType = computed.engineType;
        if (engineType == EngineType.EMPTY) return 0;

        var engineInfo = computed.engineInfo;
        var info = DataLoader.GSON.fromJson(engineInfo, EngineInfo.class);
        return switch (engineType) {
            case FIXED -> 0;
            case TRACK ->
                    Math.max(Mth.abs(entityData.get(POWER)), Mth.abs(1.4f * this.entityData.get(DELTA_ROT))) * info.engineSoundVolume;
            case HELICOPTER -> entityData.get(PROPELLER_ROT) * info.engineSoundVolume;
            default -> Mth.abs(entityData.get(POWER)) * info.engineSoundVolume;
        };
    }

    public Matrix4f getVehicleTransform(float ticks) {
        Matrix4f transformV = this.getVehicleYOffsetTransform(ticks);
        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0, -rotateOffsetHeight(), 0);
        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        return transformV;
    }

    // From Immersive_Aircraft
    public Matrix4f getVehicleYOffsetTransform(float partialTicks) {
        return VehicleVecUtils.getVehicleYOffsetTransform(this, partialTicks);
    }

    public float rotateOffsetHeight() {
        return computed().rotateOffsetHeight;
    }

    public Matrix4f getVehicleFlatTransform(float partialTicks) {
        return VehicleVecUtils.getVehicleFlatTransform(this, partialTicks);
    }

    public Matrix4f getClientVehicleTransform(float partialTicks) {
        return VehicleVecUtils.getClientVehicleTransform(this, partialTicks);
    }

    public boolean hasTurret() {
        return getTurretPos() != null;
    }

    public Vec3 getTurretPos() {
        return computed().turretPos;
    }

    public int getTurretControllerIndex() {
        return computed().turretControllerIndex;
    }

    /**
     * @return 炮塔最大俯仰速度
     */
    public float getTurretTurnXSpeed() {
        return computed().turretTurnSpeed.x;
    }

    /**
     * @return 炮塔最大偏航速度
     */
    public float getTurretTurnYSpeed() {
        return computed().turretTurnSpeed.y;
    }

    /**
     * @return 炮塔最小偏航
     */
    public float getTurretMinYaw() {
        return computed().turretYawRange.x;
    }

    /**
     * @return 炮塔最大偏航
     */
    public float getTurretMaxYaw() {
        return computed().turretYawRange.y;
    }

    /**
     * @return 炮塔最小俯角
     */
    public float getTurretMinPitch() {
        return computed().turretPitchRange.x;
    }

    /**
     * @return 炮塔最大仰角
     */
    public float getTurretMaxPitch() {
        return computed().turretPitchRange.y;
    }

    public Vec3 getBarrelPosition() {
        return computed().barrelPos;
    }

    public boolean hasPassengerWeaponStation() {
        return getPassengerWeaponStationPosition() != null;
    }

    public Vec3 getPassengerWeaponStationPosition() {
        return computed().passengerWeaponStationPos;
    }

    public Vec3 getPassengerWeaponStationBarrelPosition() {
        return computed().passengerWeaponStationBarrelPos;
    }

    public int getPassengerWeaponStationControllerIndex() {
        return computed().passengerWeaponStationControllerIndex;
    }

    /**
     * @return 乘客武器站最大偏航速度
     */
    public float passengerWeaponYSpeed() {
        return computed().passengerWeaponStationTurnSpeed.y;
    }

    /**
     * @return 乘客武器站最大俯仰速度
     */
    public float passengerWeaponXSpeed() {
        return computed().passengerWeaponStationTurnSpeed.x;
    }

    /**
     * @return 乘客武器站最小仰角
     */
    public float passengerWeaponMinPitch() {
        return computed().passengerWeaponStationPitchRange.x;
    }

    /**
     * @return 乘客武器站最大仰角
     */
    public float passengerWeaponMaxPitch() {
        return computed().passengerWeaponStationPitchRange.y;
    }

    /**
     * @return 炮塔最小偏航
     */
    public float passengerWeaponMinYaw() {
        return computed().passengerWeaponStationYawRange.x;
    }

    /**
     * @return 炮塔最大偏航
     */
    public float passengerWeaponMaxYaw() {
        return computed().passengerWeaponStationYawRange.y;
    }

    public Matrix4f getTurretTransform(float partialTicks) {
        return VehicleVecUtils.getTurretTransform(this, partialTicks);
    }

    public Vec3 getTurretVector(float pPartialTicks) {
        return VehicleVecUtils.getTurretVector(this, pPartialTicks);
    }

    public Matrix4f getBarrelTransform(float partialTicks) {
        return VehicleVecUtils.getBarrelTransform(this, partialTicks);
    }

    public Matrix4f getGunTransform(float partialTicks) {
        return VehicleVecUtils.getGunTransform(this, partialTicks);
    }

    public Matrix4f getPassengerWeaponStationBarrelTransform(float partialTicks) {
        return VehicleVecUtils.getPassengerWeaponStationBarrelTransform(this, partialTicks);
    }

    public Vec3 getPassengerWeaponStationVector(float partialTicks) {
        return VehicleVecUtils.getPassengerWeaponStationVector(this, partialTicks);
    }

    public Vector4f transformPosition(Matrix4f transform, float x, float y, float z) {
        return transform.transform(new Vector4f(x, y, z, 1));
    }

    public void handleClientSync() {
        if (level() instanceof ServerLevel && tickCount % 2 == 0) {
            entityData.set(SERVER_YAW, getYRot());
            entityData.set(SERVER_PITCH, getXRot());
        }
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

        float diffY = Mth.wrapDegrees(entityData.get(SERVER_YAW) - this.getYRot());
        float diffX = Mth.wrapDegrees(entityData.get(SERVER_PITCH) - this.getXRot());

        this.setYRot(this.getYRot() + 0.1f * diffY);
        this.setXRot(this.getXRot() + 0.1f * diffX);

        setPos(interpolatedX, interpolatedY, interpolatedZ);

        --interpolationSteps;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.interpolationSteps = 10;
    }

    @Deprecated(forRemoval = true, since = "0.8.9")
    protected Vec3 getDismountOffset(double vehicleWidth, double passengerWidth) {
        return VehicleMiscUtils.getDismountOffset(this, vehicleWidth, passengerWidth);
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
        var dismountInfo = this.computed().seats().get(index).dismountInfo;
        if (dismountInfo != null) {
            var vec3 = dismountInfo.position;
            if (vec3 != null) {
                var worldPosition = transformPosition(
                        this.getTransformFromString(dismountInfo.transform),
                        (float) vec3.x, (float) vec3.y, (float) vec3.z);
                return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
            } else {
                return dismount(passenger);
            }
        } else {
            return dismount(passenger);
        }
    }

    public @NotNull Vec3 dismount(LivingEntity passenger) {
        Vec3 vec3d = VehicleMiscUtils.getDismountOffset(this, getBbWidth() * Mth.SQRT_OF_TWO, passenger.getBbWidth() * Mth.SQRT_OF_TWO);
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

    public @NotNull Vec3 getEjectionPosition(LivingEntity passenger, int index) {
        var dismountInfo = this.computed().seats().get(index).dismountInfo;
        if (dismountInfo != null) {
            var vec3 = dismountInfo.ejectPosition;
            if (vec3 == null) {
                return passenger.position();
            }
            var worldPosition = transformPosition(
                    this.getTransformFromString(dismountInfo.transform),
                    (float) vec3.x, (float) vec3.y, (float) vec3.z);

            return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
        }
        return passenger.position();
    }

    public boolean allowEjection(int seatIndex) {
        var dismountInfo = this.computed().seats().get(seatIndex).dismountInfo;
        if (dismountInfo != null) {
            return this.computed().seats().get(seatIndex).dismountInfo.canEject;
        } else {
            return false;
        }
    }

    public void removeSeatIndexTag(Entity entity) {
        entity.getPersistentData().remove(TAG_SEAT_INDEX);
    }

    public @NotNull Vec3 getEjectionMovement(LivingEntity entity, int index) {
        var dismountInfo = this.computed().seats().get(index).dismountInfo;
        if (dismountInfo == null) return getDeltaMovement();

        double force = dismountInfo.ejectForce;
        StringOrVec3 stringOrVec3 = dismountInfo.ejectDirection;

        if (stringOrVec3 == null) {
            return getDeltaMovement().add(getUpVec(1).scale(force));
        } else if (stringOrVec3.isString()) {
            return getDeltaMovement().add(getVectorFromString(stringOrVec3.string, 1, getSeatIndex(entity)).scale(force));
        } else {
            var vec3 = stringOrVec3.vec3;
            Vector4f worldPosition = transformPosition(
                    getTransformFromString(dismountInfo.transform),
                    (float) vec3.x + (float) stringOrVec3.vec3.x,
                    (float) vec3.y + (float) stringOrVec3.vec3.y,
                    (float) vec3.z + (float) stringOrVec3.vec3.z);

            Vector4f worldPositionO = transformPosition(
                    getTransformFromString(dismountInfo.transform),
                    (float) vec3.x,
                    (float) vec3.y,
                    (float) vec3.z);

            Vec3 startPos = new Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z);
            Vec3 endPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
            return getDeltaMovement().add(startPos.vectorTo(endPos).normalize().scale(force));
        }
    }

    public ResourceLocation getVehicleIcon() {
        return computed().vehicleIcon;
    }

    public boolean allowFreeCam() {
        return computed().allowFreeCam;
    }

    public Vec3 getUpVec(float ticks) {
        Matrix4f transform = getVehicleTransform(ticks);

        Vector4f force0 = transformPosition(transform, 0, 0, 0);
        Vector4f force1 = transformPosition(transform, 0, 1, 0);

        return new Vec3(force0.x, force0.y, force0.z).vectorTo(new Vec3(force1.x, force1.y, force1.z));
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

    public Vec3 getCameraPos(Entity entity, float partialTicks) {
        return VehicleVecUtils.getCameraPos(this, entity, partialTicks);
    }

    public Vec3 cameraDirection(Entity entity, float partialTicks) {
        return VehicleVecUtils.getCameraDirection(this, entity, partialTicks);
    }

    public Vec3 getZoomPos(Entity entity, float partialTicks) {
        return VehicleVecUtils.getZoomPos(this, entity, partialTicks);
    }

    public Vec3 getZoomDirection(Entity entity, float partialTicks) {
        return VehicleVecUtils.getZoomDirection(this, entity, partialTicks);
    }

    public double getMouseSensitivity() {
        return 0.1;
    }

    public Vec2 getMouseSpeed() {
        return VehicleResource.compute(this).mouseSpeed;
    }

    public float gearRot(float tickDelta) {
        return Mth.lerp(tickDelta, gearRotO, getGearRot());
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public float getMass() {
        return computed().mass;
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
        var seat = computed().seats().get(seatIndex);
        Vec3 sensitivity = seat.sensitivity;
        return zoom ? sensitivity.x * original : Minecraft.getInstance().options.getCameraType().isFirstPerson() ? sensitivity.y * original : sensitivity.z * original;
    }

    /**
     * 载具在集装箱物品上显示的贴图
     */
    @Nullable
    public ResourceLocation getVehicleItemIcon() {
        return computed().containerIcon;
    }

    /**
     * 判断一个座位是否是封闭的（封闭载具座位具有免疫负面效果等功能）
     * 默认认为隐藏乘客的座位均为封闭座位
     *
     * @param index 位置
     */
    public boolean isEnclosed(int index) {
        var seats = computed().seats();
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
     * 是否禁用玩家手臂
     *
     * @param entity 玩家
     */
    public boolean banHand(LivingEntity entity) {
        int index = getSeatIndex(entity);
        if (index == -1) return false;

        var gunData = getGunData(index);
        var seats = computed().seats();
        if (index >= seats.size()) return false;

        var seat = seats.get(index);
        return gunData != null || seat.banHand;
    }

    /**
     * 是否隐藏载具上的玩家
     *
     * @return 是否隐藏
     */
    public boolean hidePassenger(int index) {
        var seats = computed().seats();
        if (index < 0 || index >= seats.size()) return false;

        var seat = seats.get(index);
        return seat.hidePassenger;
    }

    public boolean hidePassenger(Entity passenger) {
        return hidePassenger(getSeatIndex(passenger));
    }

    public int getAmmoCount(LivingEntity living) {
        var data = getGunData(getSeatIndex(living));
        if (data == null) return 0;
        return data.useBackpackAmmo() ? data.backupAmmoCount.get() : data.ammo.get();
    }

    public int getAmmoCount(int seatIndex) {
        var data = getGunData(seatIndex);
        if (data == null) return 0;
        return data.useBackpackAmmo() ? data.backupAmmoCount.get() : data.ammo.get();
    }

    public int getAmmoCount(String weaponName) {
        var data = getGunData(weaponName);
        if (data == null) return 0;
        return data.useBackpackAmmo() ? data.backupAmmoCount.get() : data.ammo.get();
    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        if (!getRetrieveItems().isEmpty()) {
            return getRetrieveItems().get(0);
        }
        return ContainerBlockItem.createInstance(this.getType());
    }

    /**
     * 获取视角旋转
     *
     * @param zoom          是否在载具上瞄准
     * @param isFirstPerson 是否是第一人称视角
     */
    @OnlyIn(Dist.CLIENT)
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        int index = this.getSeatIndex(player);
        var seat = computed().seats().get(index);
        var gunData = getGunData(player);
        if (seat != null) {
            var data = seat.cameraPos;
            if (data != null) {
                if (zoom && gunData != null && gunData.compute().shootPos.viewDirection != null) {
                    return new Vec2((float) -VehicleVecUtils.getYRotFromVector(getViewVec(player, partialTicks)), (float) -VehicleVecUtils.getXRotFromVector(getViewVec(player, partialTicks)));
                }
                if (data.aircraftCamera) {
                    return new Vec2((float) (getYaw(partialTicks) - freeCameraYaw), (float) (getPitch(partialTicks) + freeCameraPitch));
                }
                if (zoom || isFirstPerson) {
                    return new Vec2((float) -VehicleVecUtils.getYRotFromVector(cameraDirection(player, partialTicks)), (float) -VehicleVecUtils.getXRotFromVector(cameraDirection(player, partialTicks)));
                }
            } else {
                return null;
            }
        }
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
        int index = this.getSeatIndex(player);
        var seat = computed().seats().get(index);
        if (seat != null) {
            var data = seat.cameraPos;
            var gunData = getGunData(player);
            if (data != null) {
                if (zoom || isFirstPerson) {
                    if (zoom) {
                        if (gunData != null && gunData.compute().shootPos.viewPosition != null) {
                            return getViewPos(player, partialTicks);
                        } else {
                            return getZoomPos(player, partialTicks);
                        }
                    } else {
                        return getCameraPos(player, partialTicks);
                    }
                } else if (data.aircraftCamera) {
                    Matrix4f transform = getClientVehicleTransform(partialTicks);
                    Vector4f maxCameraPosition = transformPosition(transform, (float) data.aircraftCameraPos.x, (float) data.aircraftCameraPos.y + 0.1f * (float) ClientMouseHandler.custom3pDistanceLerp, (float) data.aircraftCameraPos.z - (float) ClientMouseHandler.custom3pDistanceLerp);
                    return CameraTool.getMaxZoom(transform, maxCameraPosition);
                }
            }
            return null;
        }
        return null;
    }

    /**
     * 是否使用载具固定视角
     */
    @OnlyIn(Dist.CLIENT)
    public boolean useFixedCameraPos(Entity entity) {
        int index = this.getSeatIndex(entity);
        var seat = computed().seats().get(index);
        if (seat != null) {
            var data = seat.cameraPos;
            if (data != null) {
                return data.useFixedCameraPos;
            }
        }
        return false;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && this.hasEnergyStorage()) {
            return energy.cast();
        } else if (cap == ForgeCapabilities.ITEM_HANDLER && this.hasContainer()) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        return this.getCapability(cap, null);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (this.hasContainer()) {
            itemHandler.invalidate();
        }
        if (this.hasEnergyStorage()) {
            energy.invalidate();
        }
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        if (this.hasContainer()) {
            itemHandler = LazyOptional.of(() -> new InvWrapper(this));
        }
        if (this.hasEnergyStorage()) {
            energy = LazyOptional.of(() -> new VehicleEnergyStorage(this));
        }
    }

    /**
     * 瞄准时的放大倍率
     *
     * @return 放大倍率
     */
    public double getDefaultZoom(Entity entity) {
        var gunData = getGunData(getSeatIndex(entity));
        if (gunData != null) {
            return gunData.compute().defaultZoom;
        } else {
            return 1;
        }
    }

    public boolean canCrushEntities() {
        return true;
    }

    public void fixedEngine() {
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(new Vec3(0, this.getDeltaMovement().y, 0));
        }
    }

    public void trackEngine(EngineInfo.Track engineInfo) {
        VehicleEngineUtils.trackEngine(this, engineInfo);
    }

    public void wheelEngine(EngineInfo.Wheel engineInfo) {
        VehicleEngineUtils.wheelEngine(this, engineInfo);
    }

    public void helicopterEngine(EngineInfo.Helicopter engineInfo) {
        VehicleEngineUtils.helicopterEngine(this, engineInfo);
    }

    public void shipEngine(EngineInfo.Ship engineInfo) {
        VehicleEngineUtils.shipEngine(this, engineInfo);
    }

    public void airCraftEngine(EngineInfo.AirCraft engineInfo) {
        VehicleEngineUtils.aircraftEngine(this, engineInfo);
    }

    public void wheelChairEngine(EngineInfo.WheelChair engineInfo) {
        VehicleEngineUtils.wheelChairEngine(this, engineInfo);
    }

    public void releaseSmokeDecoy(Vec3 vec3) {
        VehicleWeaponUtils.releaseSmokeDecoy(this, vec3);
    }

    public void releaseDecoy() {
        VehicleWeaponUtils.releaseDecoy(this);
    }

    // 惯性倾斜
    public void inertiaRotate(float multiplier) {
        this.setXRot(this.getXRot() - 0.5f * (float) (this.getAcceleration() * multiplier));
    }

    public void terrainCompact(float width, float length) {
        VehicleMotionUtils.terrainCompact(this, width, length);
    }

    public Matrix4f getWheelsTransform(float partialTicks) {
        return VehicleMotionUtils.getWheelsTransform(this, partialTicks);
    }

    public double traceBlockY(Vec3 pos, double maxLength) {
        return VehicleMotionUtils.traceBlockYPos(this, pos, maxLength);
    }

    public void moveOnDragonTeeth() {
        VehicleMotionUtils.handleVehicleMoveOnDragonTeeth(this);
    }

    public void collideBlocks() {
        VehicleMotionUtils.collideBlocks(this);
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
            if (this.getVehicleType() == VehicleType.AIRPLANE && ((entityData.get(GEAR_ROT) > 0.15 && !(this instanceof Tom6Entity)) || Mth.abs(getRoll()) > 20 || Mth.abs(getXRot()) > 30)) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) ((8 + Mth.abs(getRoll() * 0.2f)) * (lastTickSpeed - 0.3) * (lastTickSpeed - 0.3)));
                if (!this.level().isClientSide) {
                    this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                }
                this.bounceVertical(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
            } else if (this.getVehicleType() == VehicleType.HELICOPTER) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) (60 * ((lastTickSpeed - 0.5) * (lastTickSpeed - 0.5))));
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
        VehicleMotionUtils.bounceHorizontal(this, direction);
    }

    public void bounceVertical(Direction direction) {
        VehicleMotionUtils.bounceVertical(this, direction);
    }

    public void preventStacking() {
        VehicleMotionUtils.preventStacking(this);
    }

    public void pushNew(double pX, double pY, double pZ) {
        this.setDeltaMovement(this.getDeltaMovement().add(pX, pY, pZ));
    }

    public void supportEntities() {
        VehicleMotionUtils.supportEntities(this);
    }

    public @NotNull RandomSource getRandom() {
        return this.random;
    }

    public void crushEntities() {
        VehicleMotionUtils.crushEntities(this);
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
        return this.computed().engineSound;
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

    public int getTrackAnimationLength() {
        return 100;
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

    public float getGearRot() {
        return this.gearRot;
    }

    public void setGearRot(float pGearRot) {
        this.gearRot = pGearRot;
    }

    public boolean hasDecoy() {
        return computed().hasDecoy;
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
        return computed().hudColor.get();
    }

    public float getPower() {
        return entityData.get(POWER);
    }

    public String getDecoyState() {
        return entityData.get(DECOY_READY) ? "READY" : "RELOADING";
    }

    @NotNull
    public SoundEvent getHornSound() {
        return this.computed().hornSound;
    }

    @NotNull
    public SoundEvent getInCarMusicSound() {
        var passenger = this.getFirstPassenger();
        if (passenger instanceof Player player) {
            var stack = player.getOffhandItem();
            if (stack.getItem() instanceof RecordItem recordItem) {
                return recordItem.getSound();
            }
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
        return stack.getItem() instanceof RecordItem || NetMusicCompatHolder.canPlayMusic(stack);
    }

    public VehicleType getVehicleType() {
        return computed().type;
    }

    /**
     * @author YWZJ Ranpoes
     */
    public void support(Entity entity) {
        VehicleMotionUtils.support(this, entity);
    }

    public boolean isAmphibious() {
        return VehicleMiscUtils.isAmphibious(this);
    }

    @OnlyIn(Dist.CLIENT)
    public Component firstPersonAmmoComponent(GunData data, Player player) {
        var name = data.compute().name;
        if (name == null || name.isBlank()) return Component.empty();

        int ammoCount = this.getAmmoCount(player);
        return Component.translatable(name, ammoCount == Integer.MAX_VALUE ? "∞" : ammoCount);
    }

    @OnlyIn(Dist.CLIENT)
    public Component thirdPersonAmmoComponent(GunData data, Player player) {
        return firstPersonAmmoComponent(data, player);
    }
}
