package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.energy.SyncedEntityEnergyStorage;
import com.atsuishio.superbwarfare.capability.energy.VehicleEnergyStorage;
import com.atsuishio.superbwarfare.data.Prop;
import com.atsuishio.superbwarfare.data.vehicle.VehicleData;
import com.atsuishio.superbwarfare.data.vehicle.VehicleProp;
import com.atsuishio.superbwarfare.data.vehicle.VehiclePropertyModifier;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.*;
import java.util.function.Function;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;
import static com.atsuishio.superbwarfare.tools.SeekTool.teamFilter;

public abstract class VehicleEntity extends Entity implements Container, VehiclePropertyModifier {

    public static final String TAG_SEAT_INDEX = "SBWSeatIndex";

    public static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> OVERRIDE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> LAST_ATTACKER_UUID = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> LAST_DRIVER_UUID = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> DELTA_ROT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> MOUSE_SPEED_X = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> MOUSE_SPEED_Y = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<IntList> SELECTED_WEAPON = SynchedEntityData.defineId(VehicleEntity.class, ModSerializers.INT_LIST_SERIALIZER.get());
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

    public VehicleWeapon[][] availableWeapons;

    protected int interpolationSteps;
    protected double x;
    protected double y;
    protected double z;
    protected double serverYRot;
    protected double serverXRot;

    public float roll;
    public float prevRoll;
    public int lastHurtTick;
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

    public @Nullable Player damageDebugResultReceiver = null;

    public VehicleEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.isInitialized = true;

        this.setHealth(this.getMaxHealth());

        if (this.hasEnergyStorage()) {
            this.energyStorage = new VehicleEnergyStorage(this);
        }
    }

    protected final Map<VehicleProp<?>, Prop.PropModifyContext<VehicleData, ?>> propertyModifiers = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Map<VehicleProp<?>, Prop.PropModifyContext<VehicleData, ?>> getPropModifiers() {
        return this.propertyModifiers;
    }

    public void mouseInput(double x, double y) {
        entityData.set(MOUSE_SPEED_X, (float) x);
        entityData.set(MOUSE_SPEED_Y, (float) y);
    }

    // container start

    private LazyOptional<?> itemHandler = LazyOptional.of(() -> new InvWrapper(this));
    protected final NonNullList<ItemStack> items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);

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
        return 0;
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

    public boolean hasMenu() {
        return false;
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

        return Container.super.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItem(@NotNull Container target, int slot, @NotNull ItemStack stack) {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) return false;
        return Container.super.canTakeItem(target, slot, stack);
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

    /**
     * 获取按顺序排列的成员列表
     *
     * @return 按顺序排列的成员列表
     */
    public List<Entity> getOrderedPassengers() {
        return orderedPassengers;
    }

    // 仅在客户端存在的实体顺序获取，用于在客户端正确同步实体座位顺序
    public Function<Entity, Integer> entityIndexOverride = null;

    @Override
    protected void addPassenger(@NotNull Entity pPassenger) {
        if (pPassenger.getVehicle() != this) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        }

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
    public float getStepHeight() {
        return data().get(VehicleProp.UP_STEP);
    }

    @Override
    public @Nullable Entity getFirstPassenger() {
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
        this.entityData.define(HEALTH, this.getMaxHealth());
        this.entityData.define(OVERRIDE, "");
        this.entityData.define(LAST_ATTACKER_UUID, "undefined");
        this.entityData.define(LAST_DRIVER_UUID, "undefined");
        this.entityData.define(DELTA_ROT, 0f);
        this.entityData.define(MOUSE_SPEED_X, 0f);
        this.entityData.define(MOUSE_SPEED_Y, 0f);
        this.entityData.define(HEAT, 0);

        this.entityData.define(TURRET_HEALTH, getTurretMaxHealth());
        this.entityData.define(L_WHEEL_HEALTH, getWheelMaxHealth());
        this.entityData.define(R_WHEEL_HEALTH, getWheelMaxHealth());
        this.entityData.define(ENGINE_HEALTH, getEngineMaxHealth());
        this.entityData.define(L_ENGINE_HEALTH, getEngineMaxHealth());

        this.entityData.define(TURRET_DAMAGED, false);
        this.entityData.define(L_WHEEL_DAMAGED, false);
        this.entityData.define(R_WHEEL_DAMAGED, false);
        this.entityData.define(ENGINE1_DAMAGED, false);
        this.entityData.define(ENGINE2_DAMAGED, false);

        if (this instanceof WeaponVehicleEntity weaponVehicle && weaponVehicle.getAllWeapons().length > 0) {
            this.entityData.define(SELECTED_WEAPON, IntList.of(initSelectedWeaponArray(weaponVehicle)));
        }
        this.entityData.define(ENERGY, 0);
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
        this.energyStorage.extractEnergy(amount, false);
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
        return false;
    }

    // energy end

    private int[] initSelectedWeaponArray(WeaponVehicleEntity weaponVehicle) {
        // 初始化武器数组
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
            energyStorage.deserializeNBT(energyNBT);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        compound.putFloat("Health", this.entityData.get(HEALTH));
        compound.putString("Override", this.entityData.get(OVERRIDE));
        compound.putString("LastAttacker", this.entityData.get(LAST_ATTACKER_UUID));
        compound.putString("LastDriver", this.entityData.get(LAST_DRIVER_UUID));

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

        if (this instanceof WeaponVehicleEntity weaponVehicle && weaponVehicle.getAllWeapons().length > 0) {
            compound.putIntArray("SelectedWeapon", this.entityData.get(SELECTED_WEAPON).toIntArray());
        }

        if (this.hasEnergyStorage()) {
            compound.put("Energy", energyStorage.serializeNBT());
        }
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        if (player.getVehicle() == this) return InteractionResult.PASS;
        var data = data();

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

        if (player.isShiftKeyDown() && stack.is(ModTags.Items.CROWBAR) && this.getPassengers().isEmpty()) {
            for (var item : getRetrieveItems()) {
                ItemHandlerHelper.giveItemToPlayer(player, item);
            }
            this.remove(RemovalReason.DISCARDED);
            this.discard();
            return InteractionResult.SUCCESS;
        } else if (this.getHealth() < this.getMaxHealth()
                && data.canRepairManually()
                && data.isRepairMaterial(stack)
        ) {
            this.heal(Math.min(data.get(VehicleProp.REPAIR_MATERIAL_HEAL_AMOUNT), this.getMaxHealth()));
            stack.shrink(1);
            if (!this.level().isClientSide) {
                this.level().playSound(null, this, SoundEvents.IRON_GOLEM_REPAIR, this.getSoundSource(), 0.5f, 1);
            }
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown() && this.getMaxPassengers() > 0) {

            List<Entity> entities = getPassengers();
            for (var passenger : entities) {
                if (passenger.getTeam() != null && (passenger.getTeam().getName().equals("TDM") || passenger.getTeam() != player.getTeam())) {
                    return InteractionResult.PASS;
                }
            }

            Entity lastDriver = EntityFindUtil.findEntity(level(), entityData.get(LAST_DRIVER_UUID));
            if (lastDriver != null && !teamFilter(player, lastDriver) && lastDriver.getTeam() != null) {
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

    // 将有炮塔的载具驾驶员设置为炮塔角度
    public void setDriverAngle(Player player) {
        if (this instanceof LandArmorEntity landArmorEntity) {
            player.xRotO = -(float) getXRotFromVector(landArmorEntity.getBarrelVec(1));
            player.setXRot(-(float) getXRotFromVector(landArmorEntity.getBarrelVec(1)));
            player.yRotO = -(float) getYRotFromVector(landArmorEntity.getBarrelVec(1));
            player.setYRot(-(float) getYRotFromVector(landArmorEntity.getBarrelVec(1)));
            player.setYHeadRot(-(float) getYRotFromVector(landArmorEntity.getBarrelVec(1)));
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

        if (this.damageDebugResultReceiver != null) {
            this.damageDebugResultReceiver.sendSystemMessage(DamageHandler.getDamageInfo(this, source, amount));
        }

        // 计算减伤后的伤害
        float computedAmount = getDamageModifier().compute(source, amount);
        this.crash = source.is(ModDamageTypes.VEHICLE_STRIKE);

        if (source.getEntity() != null) {
            this.entityData.set(LAST_ATTACKER_UUID, source.getEntity().getStringUUID());
        }

        // 受伤打断呼吸回血
        if (computedAmount > 0) {
            lastHurtTick = 0;
            repairCoolDown = maxRepairCoolDown();
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
                Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(3, 5));
            }

            if (pHealAmount > 0 && this.getHealth() > 0 && send) {
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
        return 1;
    }

    public static double getSubmergedHeight(Entity entity) {
        for (FluidType fluidType : ForgeRegistries.FLUID_TYPES.get().getValues()) {
            if (entity.level().getFluidState(entity.blockPosition()).getFluidType() == fluidType)
                return entity.getFluidTypeHeight(fluidType);
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
        super.baseTick();

        this.lastHurtTick++;

        if (repairCoolDown > 0) {
            repairCoolDown--;
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
            if (this.level() instanceof ServerLevel serverLevel) {
                this.handlePartDamaged(obbEntity, serverLevel);
            }

            // 处理部件血量
            this.handlePartHealth();
        }

        entityData.set(MOUSE_SPEED_X, entityData.get(MOUSE_SPEED_X) * 0.95f);
        entityData.set(MOUSE_SPEED_Y, entityData.get(MOUSE_SPEED_Y) * 0.95f);

        this.refreshDimensions();
    }

    public void handlePartDamaged(OBBEntity obbEntity, ServerLevel serverLevel) {
        var obbList = obbEntity.getOBBs();
        for (var obb : obbList) {
            Vec3 pos = new Vec3(obb.center());
            switch (obb.part()) {
                case TURRET -> {
                    if (entityData.get(TURRET_DAMAGED)) {
                        this.onTurretDamaged(pos, serverLevel);
                    }
                }
                case WHEEL_LEFT -> {
                    if (entityData.get(L_WHEEL_DAMAGED)) {
                        this.onLeftWheelDamaged(pos, serverLevel);
                    }
                }
                case WHEEL_RIGHT -> {
                    if (entityData.get(R_WHEEL_DAMAGED)) {
                        this.onRightWheelDamaged(pos, serverLevel);
                    }
                }
                case ENGINE1 -> {
                    if (entityData.get(ENGINE1_DAMAGED)) {
                        this.onEngine1Damaged(pos, serverLevel);
                    }
                }
                case ENGINE2 -> {
                    if (entityData.get(ENGINE2_DAMAGED)) {
                        this.onEngine2Damaged(pos, serverLevel);
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

    public void defaultPartDamageEffect(Vec3 pos, ServerLevel serverLevel) {
        sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), pos.x, pos.y, pos.z, 5, 0.25, 0.25, 0.25, 0.25, true);
        sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 1, 1, 0.5, 1, 0.01, true);
    }

    public void onTurretDamaged(Vec3 pos, ServerLevel serverLevel) {
        this.defaultPartDamageEffect(pos, serverLevel);
    }

    public void onLeftWheelDamaged(Vec3 pos, ServerLevel serverLevel) {
        this.defaultPartDamageEffect(pos, serverLevel);
    }

    public void onRightWheelDamaged(Vec3 pos, ServerLevel serverLevel) {
        this.defaultPartDamageEffect(pos, serverLevel);
    }

    public void onEngine1Damaged(Vec3 pos, ServerLevel serverLevel) {
        this.defaultPartDamageEffect(pos, serverLevel);
    }

    public void onEngine2Damaged(Vec3 pos, ServerLevel serverLevel) {
        this.defaultPartDamageEffect(pos, serverLevel);
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
            if (this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ(), 2, 0.35 * this.getBbWidth(), 0.15 * this.getBbHeight(), 0.35 * this.getBbWidth(), 0.01, true);
            }
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.getHealth() <= 0.25 * this.getMaxHealth()) {
                playLowHealthParticle(serverLevel);
            }
            if (this.getHealth() <= 0.15 * this.getMaxHealth()) {
                playLowHealthParticle(serverLevel);
            }
        }

        if (this.getHealth() <= 0.1 * this.getMaxHealth()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ(), 2, 0.35 * this.getBbWidth(), 0.15 * this.getBbHeight(), 0.35 * this.getBbWidth(), 0.01, true);
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ(), 2, 0.35 * this.getBbWidth(), 0.15 * this.getBbHeight(), 0.35 * this.getBbWidth(), 0.01, true);
                ParticleTool.sendParticle(serverLevel, ParticleTypes.FLAME, this.getX(), this.getY() + 0.85f * getBbHeight(), this.getZ(), 4, 0.35 * this.getBbWidth(), 0.12 * this.getBbHeight(), 0.35 * this.getBbWidth(), 0.05, true);
                ParticleTool.sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), this.getX(), this.getY() + 0.85f * getBbHeight(), this.getZ(), 4, 0.1 * this.getBbWidth(), 0.05 * this.getBbHeight(), 0.1 * this.getBbWidth(), 0.4, true);
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

    public void playLowHealthParticle(ServerLevel serverLevel) {
        ParticleTool.sendParticle(serverLevel, ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ(), 1, 0.35 * this.getBbWidth(), 0.15 * this.getBbHeight(), 0.35 * this.getBbWidth(), 0.01, true);
        ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.7f * getBbHeight(), this.getZ(), 1, 0.35 * this.getBbWidth(), 0.15 * this.getBbHeight(), 0.35 * this.getBbWidth(), 0.01, true);
    }

    public void turretAngle(float ySpeed, float xSpeed) {
        Entity driver = this.getFirstPassenger();
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

            this.setTurretXRot(this.getTurretXRot() + Mth.clamp(0.95f * diffX, -xSpeed, xSpeed));
            this.setTurretYRot(this.getTurretYRot() + Mth.clamp(0.9f * diffY, min, max));
            turretYRotLock = Mth.clamp(0.9f * diffY, min, max);
        } else {
            turretYRotLock = 0;
        }
    }

    public void turretAutoAimFormVector(float ySpeed, float xSpeed, float minXAngle, float maxXAngle, Vec3 shootVec) {
        //shootVec是需要让炮塔以这个角度发射的向量
        float diffY = (float) Mth.wrapDegrees(-getYRotFromVector(shootVec) + getYRotFromVector(getBarrelVec(1)));
        float diffX = (float) Mth.wrapDegrees(-getXRotFromVector(shootVec) + getXRotFromVector(getBarrelVec(1)));

        this.turretTurnSound(diffX, diffY, 0.95f);

        if (entityData.get(TURRET_DAMAGED)) {
            ySpeed *= 0.2f;
            xSpeed *= 0.2f;
        }

        float min = -ySpeed + (float) (isInWater() && !onGround() ? 2.5 : 6) * entityData.get(DELTA_ROT);
        float max = ySpeed + (float) (isInWater() && !onGround() ? 2.5 : 6) * entityData.get(DELTA_ROT);

        this.setTurretXRot(Mth.clamp(this.getTurretXRot() + Mth.clamp(0.5f * diffX, -xSpeed, xSpeed), -maxXAngle, -minXAngle));
        this.setTurretYRot(this.getTurretYRot() - Mth.clamp(0.5f * diffY, min, max));
        turretYRotLock = Mth.clamp(0.9f * diffY, min, max);
    }

    public void passengerWeaponAutoAimFormVector(float ySpeed, float xSpeed, float minXAngle, float maxXAngle, Vec3 shootVec) {
        //shootVec是需要让武器站以这个角度发射的向量
        float diffY = (float) Mth.wrapDegrees(-getYRotFromVector(shootVec) + getYRotFromVector(getGunnerVector(1)));
        float diffX = (float) Mth.wrapDegrees(-getXRotFromVector(shootVec) + getXRotFromVector(getGunnerVector(1)));

        turretTurnSound(diffX, diffY, 0.95f);

        this.setGunXRot(Mth.clamp(this.getGunXRot() + Mth.clamp(0.5f * diffX, -xSpeed, xSpeed), -maxXAngle, -minXAngle));
        this.setGunYRot(this.getGunYRot() - Mth.clamp(0.5f * diffY, -ySpeed, ySpeed));
    }

    public void gunnerAngle(float ySpeed, float xSpeed) {
        Entity gunner = this.getNthEntity(1);

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

        if (data.get(VehicleProp.EXPLODE_PASSENGERS_ON_DESTROY)) {
            if (this.crash && data.get(VehicleProp.CRASH_PASSENGERS_ON_DESTROY)) {
                crashPassengers();
            } else {
                explodePassengers();
            }
        }

        var radius = data.get(VehicleProp.EXPLOSION_RADIUS);
        if (radius > 0) {
            var damage = data.get(VehicleProp.EXPLOSION_DAMAGE);
            var particleType = data.get(VehicleProp.EXPLOSION_PARTICLE_TYPE);
            var causeVanillaExplosion = data.get(VehicleProp.CAUSE_VANILLA_EXPLOSION);

            var explosion = createCustomExplosion()
                    .radius(radius)
                    .damage(damage)
                    .withParticleType(particleType);

            if (causeVanillaExplosion) {
                explosion.causeVanillaExplosion();
            }
            if (!data.get(VehicleProp.EXPLOSION_DESTROY_BLOCK_ON_DESTROY)) {
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
                var tempAttacker = living == getAttacker() ? null : getAttacker();

                living.hurt(ModDamageTypes.causeAirCrashDamage(this.level().registryAccess(), null, tempAttacker), Integer.MAX_VALUE);
                living.invulnerableTime = 0;
                living.hurt(ModDamageTypes.causeAirCrashDamage(this.level().registryAccess(), null, tempAttacker), Integer.MAX_VALUE);
                living.invulnerableTime = 0;
                living.hurt(ModDamageTypes.causeAirCrashDamage(this.level().registryAccess(), null, tempAttacker), Integer.MAX_VALUE);
                living.invulnerableTime = 0;
                living.hurt(ModDamageTypes.causeAirCrashDamage(this.level().registryAccess(), null, tempAttacker), Integer.MAX_VALUE);
                living.invulnerableTime = 0;
                living.hurt(ModDamageTypes.causeAirCrashDamage(this.level().registryAccess(), null, tempAttacker), Integer.MAX_VALUE);
            }
        }
    }

    protected void explodePassengers() {
        for (var entity : this.getPassengers()) {
            if (entity instanceof LivingEntity living) {
                var tempAttacker = living == getAttacker() ? null : getAttacker();

                living.hurt(ModDamageTypes.causeVehicleExplosionDamage(this.level().registryAccess(), null, tempAttacker), Integer.MAX_VALUE);
                living.invulnerableTime = 0;
                living.hurt(ModDamageTypes.causeVehicleExplosionDamage(this.level().registryAccess(), null, tempAttacker), Integer.MAX_VALUE);
                living.invulnerableTime = 0;
                living.hurt(ModDamageTypes.causeVehicleExplosionDamage(this.level().registryAccess(), null, tempAttacker), Integer.MAX_VALUE);
                living.invulnerableTime = 0;
                living.hurt(ModDamageTypes.causeVehicleExplosionDamage(this.level().registryAccess(), null, tempAttacker), Integer.MAX_VALUE);
                living.invulnerableTime = 0;
                living.hurt(ModDamageTypes.causeVehicleExplosionDamage(this.level().registryAccess(), null, tempAttacker), Integer.MAX_VALUE);
            }
        }
    }

    public void travel() {
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
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, prevRoll, getRoll())));
        return transform;
    }

    public Matrix4f getVehicleHorizontalTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo, getY()), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        return transform;
    }

    public Matrix4f getTurretTransform(float ticks) {
        return getVehicleTransform(ticks);
    }

    public Vector4f transformPosition(Matrix4f transform, float x, float y, float z) {
        return transform.transform(new Vector4f(x, y, z, 1));
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
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        this.x = x;
        this.y = y;
        this.z = z;
        serverYRot = yaw;
        serverXRot = pitch;
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
        return Mod.loc("textures/gun_icon/default_icon.png");
    }

    public boolean allowFreeCam() {
        return data().get(VehicleProp.ALLOW_FREE_CAM);
    }

    // 本方法留空
    @Override
    public void push(double pX, double pY, double pZ) {
    }

    public Vec3 getNewEyePos(float pPartialTicks) {
        return getEyePosition();
    }

    public Vec3 getBarrelVector(float pPartialTicks) {
        return this.calculateViewVector(this.getBarrelXRot(pPartialTicks), this.getBarrelYRot(pPartialTicks));
    }

    public Vec3 getGunnerVector(float pPartialTicks) {
        return this.getViewVector(pPartialTicks);
    }

    public float getBarrelXRot(float pPartialTicks) {
        return Mth.lerp(pPartialTicks, turretXRotO - this.xRotO, getTurretXRot() - this.getXRot());
    }

    public float getBarrelYRot(float pPartialTick) {
        return -Mth.lerp(pPartialTick, turretYRotO - this.yRotO, getTurretYRot() - this.getYRot());
    }

    public Vec3 getGunVector(float pPartialTicks) {
        return this.calculateViewVector(this.getGunXRot(pPartialTicks), this.getGunYRot(pPartialTicks));
    }

    public float getGunXRot(float pPartialTicks) {
        return Mth.lerp(pPartialTicks, gunXRotO - this.xRotO, getGunXRot() - this.getXRot());
    }

    public float getGunYRot(float pPartialTick) {
        return -Mth.lerp(pPartialTick, gunYRotO - this.yRotO, getGunYRot() - this.getYRot());
    }

    public float turretYRotO() {
        return turretYRotO;
    }

    public float turretYRot() {
        return turretYRot;
    }

    public float turretXRotO() {
        return turretXRotO;
    }

    public float turretXRot() {
        return turretXRot;
    }

    public Vec3 getBarrelVec(float ticks) {
        return getBarrelVector(ticks);
    }

    public Vec3 getGunVec(float ticks) {
        return getGunVector(ticks);
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

    public Vec3 driverZoomPos(float ticks) {
        return getEyePosition();
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

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
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
        return null;
    }

    /**
     * 判断一个座位是否是封闭的（封闭载具座位具有免疫负面效果等功能）
     * 默认认为隐藏乘客的座位均为封闭座位
     *
     * @param index 位置
     */
    public boolean isEnclosed(int index) {
        return hidePassenger(index);
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
        return false;
    }

    public boolean hidePassenger(Entity passenger) {
        return hidePassenger(getSeatIndex(passenger));
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
     * 渲染载具的第一人称UI
     * 务必标记 @OnlyIn(Dist.CLIENT)
     */
    @OnlyIn(Dist.CLIENT)
    public void renderFirstPersonOverlay(GuiGraphics guiGraphics, Font font, Player player, int screenWidth, int screenHeight, float scale) {
        if (!(this instanceof WeaponVehicleEntity weaponVehicle)) return;
        if (!(player instanceof LocalPlayer)) return;

        float minWH = (float) Math.min(screenWidth, screenHeight);
        float scaledMinWH = Mth.floor(minWH * scale);
        float centerW = ((screenWidth - scaledMinWH) / 2);
        float centerH = ((screenHeight - scaledMinWH) / 2);

        // 默认武器准心渲染
        var texture = Mod.loc(switch (weaponVehicle.getWeaponIndex(0)) {
            case 0 -> "textures/screens/land/lav_cannon_cross.png";
            case 1 -> "textures/screens/land/lav_gun_cross.png";
            case 2 -> "textures/screens/land/lav_missile_cross.png";
            default -> "";
        });
        if (texture.getPath().isEmpty()) return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        preciseBlit(guiGraphics, texture, centerW, centerH, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH);
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

    /**
     * 获取载具上玩家的旋转
     *
     * @return X轴旋转，Z轴旋转
     */
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public Pair<Quaternionf, Quaternionf> getPassengerRotation(Entity entity, float tickDelta) {
        return null;
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
     * 撬棍shift+右键收回载具时返还的物品
     */
    public @NotNull List<ItemStack> getRetrieveItems() {
        return List.of(ContainerBlockItem.createInstance(this));
    }
}
