package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ModColor;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.vehicle.subdata.*;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModify;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class DefaultVehicleData implements IDBasedData<DefaultVehicleData> {
    @SerializedName("ID")
    public String id = "";

    public transient boolean isDefaultData = true;

    @Override
    public String getId() {
        return this.id;
    }

    @SerializedName("MaxHealth")
    public float maxHealth = 50;

    @ServerOnly
    @SerializedName("RepairCooldown")
    public int repairCooldown = getConfigOrDefault(VehicleConfig.REPAIR_COOLDOWN);

    @ServerOnly
    @SerializedName("RepairAmount")
    public float repairAmount = getConfigOrDefault(VehicleConfig.REPAIR_AMOUNT).floatValue();

    private static <T> T getConfigOrDefault(ModConfigSpec.ConfigValue<T> config) {
        try {
            return config.get();
        } catch (Exception exception) {
            return config.getDefault();
        }
    }

    /**
     * 开始自动扣血时的血量比例
     */
    @ServerOnly
    @SerializedName("SelfHurtPercent")
    public float selfHurtPercent = 0.1F;

    /**
     * 自动扣血每tick扣血量
     */
    @ServerOnly
    @SerializedName("SelfHurtAmount")
    public float selfHurtAmount = 0.1F;

    @SerializedName("MaxEnergy")
    public int maxEnergy = 100000;

    @SerializedName("Seats")
    protected ObjectToList<SeatInfo> seats = new ObjectToList<>();

    public List<SeatInfo> seats() {
        if (seats == null) return List.of();
        return Collections.unmodifiableList(seats.list);
    }

    @SerializedName("UpStep")
    public float upStep = 0;

    @SerializedName("AllowFreeCam")
    public boolean allowFreeCam = false;

    @SerializedName("HasDecoy")
    public boolean hasDecoy = false;

    @ServerOnly
    @SerializedName("ApplyDefaultDamageModifiers")
    public boolean applyDefaultDamageModifiers = true;

    @ServerOnly
    @SerializedName("SendHitParticles")
    public boolean sendHitParticles = true;

    @ServerOnly
    @SerializedName("DamageModifiers")
    public ObjectToList<StringToObject<DamageModify>> damageModifiers = new ObjectToList<>();

    @ServerOnly
    @SerializedName("Mass")
    public float mass = 1;

    @ServerOnly
    @SerializedName("DestroyInfo")
    public DestroyInfo destroyInfo = new DestroyInfo();

    @SerializedName("VehicleContainerType")
    public VehicleContainerType vehicleContainerType = VehicleContainerType.MEDIUM;

    @SerializedName("HasUpgradeSlots")
    public boolean hasUpgradeSlots = false;

    @SerializedName("VehicleIcon")
    public ResourceLocation vehicleIcon = Mod.loc("textures/gun_icon/default_icon.png");

    @SerializedName("ContainerIcon")
    public ResourceLocation containerIcon = null;

    @SerializedName("HUDColor")
    public ModColor hudColor = new ModColor(0x66FF00);

    @SerializedName("Type")
    public VehicleType type = VehicleType.EMPTY;

    @SerializedName("EngineType")
    public EngineType engineType = EngineType.EMPTY;
    @SerializedName("EngineInfo")
    public JsonObject engineInfo = new JsonObject();
    // 引擎音效
    @SerializedName("EngineSound")
    public SoundEvent engineSound = SoundEvents.EMPTY;
    // 喇叭音效
    @SerializedName("HornSound")
    public SoundEvent hornSound = SoundEvents.EMPTY;

    @SerializedName("RotateOffsetHeight")
    public float rotateOffsetHeight = 0;

    @SerializedName("Weapons")
    public Map<String, DefaultGunData> weapons = Map.of();

    /**
     * 碰撞等级，范围是0~4
     * 0 - 无法撞坏方块
     * 1 - 允许撞坏软方块
     * 2 - 允许撞坏普通方块
     * 3 - 允许撞坏硬方块
     * 4 - 允许野兽撞击模式
     */
    @SerializedName("CollisionLevel")
    public CollisionLevel collisionLevel = new CollisionLevel();

    // 主武器位
    @ServerOnly
    @SerializedName("MainWeaponPos")
    public Vec3 mainWeaponPos = null;
    @ServerOnly
    @SerializedName("MainWeaponTurnSpeed")
    public Vec2 mainWeaponTurnSpeed = new Vec2(5, 5);
    @ServerOnly
    @SerializedName("MainWeaponYawRange")
    public Vec2 mainWeaponYawRange = new Vec2(-514, 514);
    @ServerOnly
    @SerializedName("MainWeaponPitchRange")
    public Vec2 mainWeaponPitchRange = new Vec2(-10, 30);
    @SerializedName("MainWeaponControllerIndex")
    public int mainWeaponControllerIndex = 0;
    @SerializedName("MainWeaponHudType")
    public String mainWeaponHudType = "@Empty";

    @ServerOnly
    @SerializedName("BarrelPos")
    public Vec3 barrelPos = Vec3.ZERO;

    @ServerOnly
    @SerializedName("PassengerWeaponStationPos")
    public Vec3 passengerWeaponStationPos = null;
    @ServerOnly
    @SerializedName("PassengerWeaponStationBarrelPos")
    public Vec3 passengerWeaponStationBarrelPos = Vec3.ZERO;
    @ServerOnly
    @SerializedName("PassengerWeaponStationTurnSpeed")
    public Vec2 passengerWeaponStationTurnSpeed = new Vec2(5, 5);
    @ServerOnly
    @SerializedName("PassengerWeaponStationYawRange")
    public Vec2 passengerWeaponStationYawRange = new Vec2(-514, 514);
    @ServerOnly
    @SerializedName("PassengerWeaponStationPitchRange")
    public Vec2 passengerWeaponStationPitchRange = new Vec2(-10, 30);
    @ServerOnly
    @SerializedName("PassengerWeaponStationControllerIndex")
    public int passengerWeaponStationControllerIndex = 1;

    @SerializedName("Gravity")
    public double gravity = 0.06;
    @SerializedName("TerrainCompat")
    public Vec2 terrainCompat = Vec2.ZERO;
    // 受惯性影响的旋转幅度
    @SerializedName("InertiaRotateRate")
    public float inertiaRotateRate = 0f;

    @Override
    public void limit() {
        this.maxHealth = Math.max(this.maxHealth, 0);
        this.repairCooldown = Math.max(this.repairCooldown, 0);
        this.maxEnergy = Math.max(this.maxEnergy, 0);
        this.weapons = weapons == null ? Map.of() : weapons;

        this.collisionLevel = this.collisionLevel == null ? new CollisionLevel() : this.collisionLevel;
        this.collisionLevel.level = Mth.clamp(this.collisionLevel.level, 0, 4);
        this.collisionLevel.powerLimits = this.collisionLevel.powerLimits == null ? List.of() : this.collisionLevel.powerLimits;
    }
}
