package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.*;
import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.vehicle.subdata.*;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModify;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

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

    private static <T> T getConfigOrDefault(ForgeConfigSpec.ConfigValue<T> config) {
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
    public int maxEnergy = Integer.MAX_VALUE;

    @SerializedName("OBB")
    public List<OBBInfo> obb = List.of();

    @SerializedName("Seats")
    protected ObjectToList<SeatInfo> seats = new ObjectToList<>();

    public List<SeatInfo> seats() {
        if (seats == null) return List.of();
        return Collections.unmodifiableList(seats.list);
    }

    @SerializedName("UpStep")
    public float upStep = 0;

    @SerializedName("MouseSpeed")
    public Vec2 mouseSpeed = new Vec2(0.4f, 0.4f);

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

    @SerializedName("SeekInfo")
    public SeekInfo seekInfo = null;

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
    // 第三人称视角
    @SerializedName("ThirdPersonCameraPos")
    public Vec3 thirdPersonCameraPos = new Vec3(0, 1, 3);

    @SerializedName("HasLowHealthWarning")
    public boolean hasLowHealthWarning = true;

    @SerializedName("RotateOffsetHeight")
    public float rotateOffsetHeight = 0;

    @SerializedName("Weapons")
    protected Map<String, JsonObject> weapons = Map.of();

    private transient Map<String, DefaultGunData> processedWeapons;

    public Map<String, DefaultGunData> weapons() {
        if (processedWeapons != null) return processedWeapons;

        var map = new HashMap<String, DefaultGunData>();

        for (var entry : weapons.entrySet()) {
            var value = entry.getValue();
            if (value == null) continue;
            value = value.deepCopy();

            if (value.get("Template") instanceof JsonPrimitive primitive && primitive.isString()) {
                value.remove("Template");
                var templateValue = weapons.get(primitive.getAsString());
                if (templateValue != null) {
                    var newValue = templateValue.deepCopy();
                    for (var kv : value.entrySet()) {
                        newValue.add(kv.getKey(), kv.getValue());
                    }
                    value = newValue;
                }
            }

            map.put(entry.getKey(), DataLoader.GSON.fromJson(value, DefaultGunData.class));
        }

        processedWeapons = Collections.unmodifiableMap(map);
        return processedWeapons;
    }

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
    @SerializedName("TurretPos")
    public Vec3 turretPos = null;
    @SerializedName("TurretTurnSpeed")
    public Vec2 turretTurnSpeed = new Vec2(5, 5);
    @SerializedName("TurretYawRange")
    public Vec2 turretYawRange = new Vec2(-514, 514);
    @SerializedName("TurretPitchRange")
    public Vec2 turretPitchRange = new Vec2(-10, 30);
    @SerializedName("TurretControllerIndex")
    public int turretControllerIndex = 0;
    
    @SerializedName("HudType")
    public String hudType = "@Empty";
    @SerializedName("BarrelPos")
    public Vec3 barrelPos = Vec3.ZERO;

    // 乘客位武器
    @SerializedName("PassengerWeaponStationPos")
    public Vec3 passengerWeaponStationPos = null;
    @SerializedName("PassengerWeaponStationBarrelPos")
    public Vec3 passengerWeaponStationBarrelPos = Vec3.ZERO;
    @SerializedName("PassengerWeaponStationTurnSpeed")
    public Vec2 passengerWeaponStationTurnSpeed = new Vec2(5, 5);
    @SerializedName("PassengerWeaponStationYawRange")
    public Vec2 passengerWeaponStationYawRange = new Vec2(-514, 514);
    @SerializedName("PassengerWeaponStationPitchRange")
    public Vec2 passengerWeaponStationPitchRange = new Vec2(-10, 30);
    @SerializedName("PassengerWeaponStationControllerIndex")
    public int passengerWeaponStationControllerIndex = 1;

    @SerializedName("UsePassengerCreativeAmmoBox")
    public boolean usePassengerCreativeAmmoBox = true;

    @SerializedName("Gravity")
    public double gravity = 0.06;
    @SerializedName("TerrainCompat")
    public List<Vec3> terrainCompat = null;
    @SerializedName("TerrainCompatRotateRate")
    public float terrainCompatRotateRate = 1;
    // 受惯性影响的旋转幅度
    @SerializedName("InertiaRotateRate")
    public float inertiaRotateRate = 0f;

    @Override
    public void limit() {
        this.maxHealth = Math.max(this.maxHealth, 0);
        this.repairCooldown = Math.max(this.repairCooldown, 0);
        this.maxEnergy = Math.max(this.maxEnergy, 0);
        this.weapons = weapons == null ? Map.of() : weapons;

        this.obb = this.obb == null ? List.of() : this.obb;
        this.obb = this.obb.stream().filter(Objects::nonNull).peek(OBBInfo::limit).toList();

        this.collisionLevel = this.collisionLevel == null ? new CollisionLevel() : this.collisionLevel;
        this.collisionLevel.level = Mth.clamp(this.collisionLevel.level, 0, 4);
        this.collisionLevel.powerLimits = this.collisionLevel.powerLimits == null ? List.of() : this.collisionLevel.powerLimits;
    }
}
