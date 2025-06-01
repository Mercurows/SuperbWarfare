package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.vehicle.VehicleData;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.ContainerBlockItem;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
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
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public abstract class VehicleEntity extends Entity {

    public static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> LAST_ATTACKER_UUID = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> LAST_DRIVER_UUID = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> DELTA_ROT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> MOUSE_SPEED_X = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> MOUSE_SPEED_Y = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<IntList> SELECTED_WEAPON = SynchedEntityData.defineId(VehicleEntity.class, ModSerializers.INT_LIST_SERIALIZER.get());
    public static final EntityDataAccessor<Integer> HEAT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);

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


    public void mouseInput(double x, double y) {
        entityData.set(MOUSE_SPEED_X, (float) x);
        entityData.set(MOUSE_SPEED_Y, (float) y);
    }

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
    protected void addPassenger(@NotNull Entity newPassenger) {
        if (newPassenger.getVehicle() != this) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        }

        int index;

        if (entityIndexOverride != null && entityIndexOverride.apply(newPassenger) != -1) {
            index = entityIndexOverride.apply(newPassenger);
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

        orderedPassengers.set(index, newPassenger);
        this.passengers = ImmutableList.copyOf(orderedPassengers.stream().filter(Objects::nonNull).toList());
        this.gameEvent(GameEvent.ENTITY_MOUNT, newPassenger);
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
        return data().upStep();
    }

    @Nullable
    @Override
    public Entity getFirstPassenger() {
        return orderedPassengers.get(0);
    }

    /**
     * 获取第index个乘客
     *
     * @param index 目标座位
     * @return 目标座位的乘客
     */
    public Entity getNthEntity(int index) {
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

    public VehicleEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setHealth(this.getMaxHealth());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(HEALTH, this.getMaxHealth());
        this.entityData.define(LAST_ATTACKER_UUID, "undefined");
        this.entityData.define(LAST_DRIVER_UUID, "undefined");
        this.entityData.define(DELTA_ROT, 0f);
        this.entityData.define(MOUSE_SPEED_X, 0f);
        this.entityData.define(MOUSE_SPEED_Y, 0f);
        this.entityData.define(HEAT, 0);

        if (this instanceof WeaponVehicleEntity weaponVehicle && weaponVehicle.getAllWeapons().length > 0) {
            this.entityData.define(SELECTED_WEAPON, IntList.of(initSelectedWeaponArray(weaponVehicle)));
        }
    }

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
        this.entityData.set(HEALTH, compound.getFloat("Health"));

        if (this instanceof WeaponVehicleEntity weaponVehicle && weaponVehicle.getAllWeapons().length > 0) {
            var selected = compound.getIntArray("SelectedWeapon");

            if (selected.length != this.getMaxPassengers()) {
                // 数量不符时（可能是更新或遇到损坏数据），重新初始化已选择武器
                this.entityData.set(SELECTED_WEAPON, IntList.of(initSelectedWeaponArray(weaponVehicle)));
            } else {
                this.entityData.set(SELECTED_WEAPON, IntList.of(selected));
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        compound.putFloat("Health", this.entityData.get(HEALTH));
        compound.putString("LastAttacker", this.entityData.get(LAST_ATTACKER_UUID));
        compound.putString("LastDriver", this.entityData.get(LAST_DRIVER_UUID));

        if (this instanceof WeaponVehicleEntity weaponVehicle && weaponVehicle.getAllWeapons().length > 0) {
            compound.putIntArray("SelectedWeapon", this.entityData.get(SELECTED_WEAPON).toIntArray());
        }
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        if (player.getVehicle() == this) return InteractionResult.PASS;
        var data = data();

        ItemStack stack = player.getMainHandItem();
        if (player.isShiftKeyDown() && stack.is(ModItems.CROWBAR.get()) && this.getPassengers().isEmpty()) {
            ItemStack container = ContainerBlockItem.createInstance(this);
            if (!player.addItem(container)) {
                player.drop(container, false);
            }
            this.remove(RemovalReason.DISCARDED);
            this.discard();
            return InteractionResult.SUCCESS;
        } else if (this.getHealth() < this.getMaxHealth()
                && data.canRepairManually()
                && data.isRepairMaterial(stack)
        ) {
            this.heal(Math.min(data.repairMaterialHealAmount(), this.getMaxHealth()));
            stack.shrink(1);
            if (!this.level().isClientSide) {
                this.level().playSound(null, this, SoundEvents.IRON_GOLEM_REPAIR, this.getSoundSource(), 0.5f, 1);
            }
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {
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

        this.onHurt(computedAmount, source.getEntity(), true);

        // 显示火花粒子效果
        if (this.sendFireStarParticleOnHurt() && this.level() instanceof ServerLevel serverLevel) {
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), this.getX(), this.getY() + 0.5 * getBbHeight(), this.getZ(), 2, 0.4, 0.4, 0.4, 0.2, false);
        }
        // 播放受击音效
        if (this.playHitSoundOnHurt()) {
            this.level().playSound(null, this.getOnPos(), ModSounds.HIT.get(), SoundSource.PLAYERS, 1, 1);
        }

        return super.hurt(source, computedAmount);
    }

    /**
     * 受击时是否显示火花粒子效果
     */
    public boolean sendFireStarParticleOnHurt() {
        return true;
    }

    /**
     * 受击时是否播放受击音效
     */
    public boolean playHitSoundOnHurt() {
        return true;
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
            float angle = (float) java.lang.Math.abs(VectorTool.calculateAngle(this.position().vectorTo(attacker.position()), this.getViewVector(1)));
            return (float) java.lang.Math.max(1f - multiply * toVec.dot(getViewVector(1)), 0.5f);
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
        return data().maxHealth();
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

    public double getSubmergedHeight(Entity entity) {
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
        return data().repairCooldown();
    }

    /**
     * 呼吸回血回血量
     */
    public float repairAmount() {
        return data().repairAmount();
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

        handleClientSync();

        if (this.level() instanceof ServerLevel && this.getHealth() <= 0) {
            destroy();
        }

        travel();

        Entity attacker = EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_ATTACKER_UUID));

        var data = data();
        if (this.getHealth() <= data.selfHurtPercent() * this.getMaxHealth()) {
            // 血量过低时自动扣血
            this.onHurt(data.selfHurtAmount(), attacker, false);
        } else {
            // 呼吸回血
            if (repairCoolDown == 0) {
                this.heal(repairAmount());
            }
        }

        if (getFirstPassenger() != null) {
            this.entityData.set(LAST_DRIVER_UUID, getFirstPassenger().getStringUUID());
        }

        clearArrow();
        this.refreshDimensions();
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

            float min = -ySpeed + (float) (isInWater() && !onGround() ? 2.5 : 6) * entityData.get(DELTA_ROT);
            float max = ySpeed + (float) (isInWater() && !onGround() ? 2.5 : 6) * entityData.get(DELTA_ROT);

            this.setTurretXRot(this.getTurretXRot() + Mth.clamp(0.95f * diffX, -xSpeed, xSpeed));
            this.setTurretYRot(this.getTurretYRot() + Mth.clamp(0.9f * diffY, min, max));
            turretYRotLock = Mth.clamp(0.9f * diffY, min, max);
        } else {
            turretYRotLock = 0;
        }
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
        this.discard();
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
    public Matrix4f getVehicleTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo, getY()), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        transform.rotate(Axis.XP.rotationDegrees(Mth.lerp(ticks, xRotO, getXRot())));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, prevRoll, getRoll())));
        return transform;
    }

    public Matrix4f getVehicleFlatTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo, getY()), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        transform.rotate(Axis.ZP.rotationDegrees(Mth.lerp(ticks, prevRoll, getRoll())));
        return transform;
    }

    public Vector4f transformPosition(Matrix4f transform, float x, float y, float z) {
        return transform.transform(new Vector4f(x, y, z, 1));
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
    public @NotNull Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
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

    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/gun_icon/default_icon.png");
    }

    public boolean allowFreeCam() {
        return data().allowFreeCam();
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

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public float getMass() {
        return data().mass();
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
     * 渲染载具的第一人称UI
     * 务必标记 @OnlyIn(Dist.CLIENT) !
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
}
