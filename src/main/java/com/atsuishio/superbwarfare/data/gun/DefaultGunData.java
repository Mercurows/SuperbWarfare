package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ModColor;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class DefaultGunData implements IDBasedData<DefaultGunData> {

    private transient String id = "";
    public transient boolean isDefaultData = true;

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    // 不要动态修改这玩意，很容易出问题
    @SerializedName("MaxDurability")
    public int maxDurability = 0;

    @ServerOnly
    @SerializedName("DurabilityPerShoot")
    public int durabilityPerShoot = 1;

    @SerializedName("MaxEnergy")
    public int maxEnergy = 0;

    @ServerOnly
    @SerializedName("MaxReceiveEnergy")
    public int maxReceiveEnergy = -1;
    @ServerOnly
    @SerializedName("MaxExtractEnergy")
    public int maxExtractEnergy = -1;

    @SerializedName("RecoilX")
    public double recoilX;
    @SerializedName("RecoilY")
    public double recoilY;
    @SerializedName("Recoil")
    public double recoil;
    @SerializedName("RecoilTime")
    public int recoilTime = 0;
    @SerializedName("RecoilForce")
    public float recoilForce = 0f;

    //x:范围，y：振动时长，z：振幅
    @ServerOnly
    @SerializedName("ShootShake")
    public Vec3 shootShake = null;

    @SerializedName("DefaultZoom")
    public double defaultZoom = 1.25;
    @SerializedName("MinZoom")
    public double minZoom = defaultZoom;
    @SerializedName("MaxZoom")
    public double maxZoom = defaultZoom;

    @SerializedName("Spread")
    public double spread;
    @SerializedName("Damage")
    public double damage;
    @SerializedName("Headshot")
    public double headshot = 1.5;
    @SerializedName("Velocity")
    public double velocity;
    @SerializedName("Magazine")
    public int magazine;

    @SerializedName("Range")
    public int range = 128;

    @SerializedName("MeleeDamage")
    public double meleeDamage;
    @SerializedName("MeleeDuration")
    public int meleeDuration = 16;
    @SerializedName("MeleeDamageTime")
    public int meleeDamageTime = 6;

    @ServerOnly
    @SerializedName("Projectile")
    public StringToObject<ProjectileInfo> projectile = new StringToObject<>(new ProjectileInfo());

    public ProjectileInfo projectile() {
        return projectile.value;
    }

    @ServerOnly
    @SerializedName("ShootPos")
    public ShootPos shootPos = new ShootPos();

    @SerializedName("SeekWeaponInfo")
    public SeekWeaponInfo seekWeaponInfo = null;

    @SerializedName("AmmoCostPerShoot")
    public int ammoCostPerShoot = 1;
    @SerializedName("ProjectileAmount")
    public int projectileAmount = 1;
    @SerializedName("Weight")
    public double weight = 1;

    @SerializedName("DefaultFireMode")
    public String defaultFireMode = FireMode.SEMI.name;
    @SerializedName("AvailableFireModes")
    public ObjectToList<StringToObject<FireModeInfo>> availableFireModes = new ObjectToList<>(new StringToObject<>(new FireModeInfo()));

    public List<FireModeInfo> availableFireModes() {
        return availableFireModes.list.stream().map(m -> m.value).toList();
    }

    @SerializedName("ReloadTypes")
    public Set<ReloadType> reloadTypes = Set.of(ReloadType.MAGAZINE);

    @SerializedName("SeekType")
    public SeekType seekType = SeekType.NONE;

    @SerializedName("GunType")
    public GunType gunType = GunType.SPECIAL;

    @SerializedName("AutoReload")
    public Boolean autoReload = null;

    @SerializedName("WithdrawAmmoWhenChangeSlot")
    public boolean withdrawAmmoWhenChangeSlot = false;

    @SerializedName("ZoomReload")
    public boolean zoomReload = true;

    @SerializedName("ClearHoldProgressAfterShoot")
    public boolean clearHoldProgressAfterShoot = false;

    @SerializedName("BurstAmount")
    public int burstAmount;
    @SerializedName("BypassesArmor")
    public double bypassesArmor;

    @SerializedName("AmmoType")
    public ObjectToList<StringToObject<AmmoConsumer>> ammoConsumers = new ObjectToList<>();

    private transient List<AmmoConsumer> ammoConsumersCache;

    public List<AmmoConsumer> getAmmoConsumers() {
        if (ammoConsumersCache == null) {
            this.ammoConsumersCache = this.ammoConsumers.list.stream()
                    .map(c -> {
                        if (!c.value.initialized()) {
                            c.value.init();
                        }
                        return c.value;
                    })
                    .filter(c -> {
                        if (c.type == AmmoConsumer.AmmoConsumeType.INVALID) {
                            Mod.LOGGER.warn("invalid ammo string {} for {}", c.ammo, this.id);
                            return false;
                        }
                        return true;
                    })
                    .toList();
        }

        return this.ammoConsumersCache;
    }

    private transient List<FireModeInfo> fireModesCache;

    public List<FireModeInfo> getFireModes() {
        if (fireModesCache == null) {
            this.fireModesCache = this.availableFireModes.list.stream()
                    .map(c -> {
                        c.value.init();
                        return c.value;
                    })
                    .toList();
        }

        return this.fireModesCache;
    }

    @SerializedName("NormalReloadTime")
    public int normalReloadTime;
    @SerializedName("EmptyReloadTime")
    public int emptyReloadTime;
    @SerializedName("BoltActionTime")
    public int boltActionTime;
    @SerializedName("PrepareTime")
    public int prepareTime;
    @SerializedName("PrepareLoadTime")
    public int prepareLoadTime;

    // 单发装填时的上弹时间
    @SerializedName("PrepareAmmoLoadTime")
    public int prepareAmmoLoadTime = 1;
    @SerializedName("PrepareEmptyTime")
    public int prepareEmptyTime;

    // 每次单发装填用时的
    @SerializedName("IterativeTime")
    public int iterativeTime;

    // 单发装填时的上弹时间，在reload.iterativeLoadTimer等于该值时上弹
    @SerializedName("IterativeAmmoLoadTime")
    public int iterativeAmmoLoadTime = 1;

    // 单次单发装填上弹数量
    @SerializedName("IterativeLoadAmount")
    public int iterativeLoadAmount = 1;

    @SerializedName("FinishTime")
    public int finishTime;

    // 连发模式下的射击间隔时间
    @SerializedName("BurstCooldown")
    public int burstCooldown = 30;

    @ServerOnly
    @SerializedName("SoundRadius")
    public double soundRadius;
    @SerializedName("RPM")
    public int rpm = 600;

    @SerializedName("ExplosionDamage")
    public double explosionDamage;
    @SerializedName("ExplosionRadius")
    public double explosionRadius;
    @SerializedName("Gravity")
    public double gravity = 0.05;

    @SerializedName("ShootDelay")
    public int shootDelay = 0;

    @ServerOnly
    @SerializedName("HeatPerShoot")
    public double heatPerShoot = 0;

    @SerializedName("AvailablePerks")
    public ObjectToList<String> availablePerks = new ObjectToList<>(
            "@Ammo",
            "superbwarfare:field_doctor",
            "superbwarfare:powerful_attraction",
            "superbwarfare:intelligent_chip",
            "superbwarfare:monster_hunter",
            "superbwarfare:vorpal_weapon",
            "!superbwarfare:micro_missile",
            "!superbwarfare:longer_wire",
            "!superbwarfare:cupid_arrow"
    );

    public List<String> availablePerks() {
        return availablePerks.list;
    }

    @ServerOnly
    @SerializedName("DamageReduce")
    public DamageReduce damageReduce = new DamageReduce();

    // 自然情况下每tick减少的热量
    @ServerOnly
    @SerializedName("NaturalCooldown")
    public double naturalCooldown = 0.25;
    // 在水中或雨中时的散热比例
    @ServerOnly
    @SerializedName("InWaterCooldownRate")
    public double inWaterCooldownRate = 1.1;
    // 在细雪中时的散热比例
    @ServerOnly
    @SerializedName("InSnowCooldownRate")
    public double inSnowCooldownRate = 1.5;
    // 在火焰中时的散热比例
    @ServerOnly
    @SerializedName("InFireCooldownRate")
    public double inFireCooldownRate = 0.6;
    // 在岩浆中时的散热比例
    @ServerOnly
    @SerializedName("InLavaCooldownRate")
    public double inLavaCooldownRate = 0.2;

    // 瞄准时的扩散比例
    @SerializedName("ZoomSpreadRate")
    public double zoomSpreadRate = 0.1;

    @SerializedName("SeekTime")
    public int seekTime = 20;
    @SerializedName("SeekAngle")
    public double seekAngle = 10;
    @SerializedName("SeekRange")
    public double seekRange = 384;

    @SerializedName("MinTargetHeight")
    public double minTargetHeight = 0;

    @SerializedName("MaxTargetHeight")
    public double maxTargetHeight = 114514;

    @SerializedName("SoundInfo")
    public SoundInfo soundInfo = new SoundInfo();

    @ServerOnly
    @SerializedName("ShootAnimationTime")
    public int shootAnimationTime = 0;

    @ServerOnly
    @SerializedName("IsAntiAirProjectile")
    public boolean isAntiAirProjectile = false;

    @ServerOnly
    @SerializedName("IsClusterMunitionsProjectile")
    public boolean isClusterMunitionsProjectile = false;

    @ServerOnly
    @SerializedName("SpreadAmount")
    public int spreadAmount = 10;

    @ServerOnly
    @SerializedName("SpreadAngle")
    public int spreadAngle = 15;

    @ServerOnly
    @SerializedName("IsArmorPiercingProjectile")
    public boolean isArmorPiercingProjectile = false;

    @ServerOnly
    @SerializedName("IsHighExplosiveProjectile")
    public boolean isHighExplosiveProjectile = false;

    @ServerOnly
    @SerializedName("IsGrapeShotProjectile")
    public boolean isGrapeShotProjectile = false;

    @SerializedName("AddShooterDeltaMovement")
    public boolean addShooterDeltaMovement = false;

    @SerializedName("Icon")
    public ResourceLocation icon = Mod.loc("textures/gun_icon/default_icon.png");
    /*
     * 准星类型
     * 预制的字段有：
     * @Empty - 空
     * @Custom - 自定义
     * @GunDefault - 默认枪械准星
     * @VehicleDefault - 默认载具准星
     */
    @SerializedName("Crosshair")
    public String crosshair = "@GunDefault";
    // 瞄准时的准星，默认为空，仅用于部分载具
    @SerializedName("CrosshairZooming")
    public String crosshairZooming = "@Empty";
    @SerializedName("CrosshairColor")
    public ModColor crosshairColor = new ModColor();
    @SerializedName("Name")
    public String name;

    @Override
    public void limit() {
        maxDurability = Math.max(0, maxDurability);
        durabilityPerShoot = Math.max(0, durabilityPerShoot);
        maxEnergy = Math.max(0, maxEnergy);

        var temp = Mth.clamp(maxReceiveEnergy, -1, maxEnergy);
        maxReceiveEnergy = temp < 0 ? maxEnergy : temp;

        temp = Mth.clamp(maxExtractEnergy, -1, maxEnergy);
        maxExtractEnergy = temp < 0 ? maxEnergy : temp;

        meleeDuration = Math.max(1, meleeDuration);
        zoomSpreadRate = Mth.clamp(zoomSpreadRate, 0, 1);
        range = Math.max(1, range);

        meleeDamageTime = Math.min(meleeDuration - 1, meleeDamageTime);

        ammoCostPerShoot = Math.max(0, ammoCostPerShoot);
        projectileAmount = Math.max(0, projectileAmount);
        weight = Math.max(1, weight);

        if (projectileAmount == 0 && meleeDamage > 0) {
            magazine = 0;
        } else {
            magazine = Math.max(0, magazine);
        }

        if (reloadTypes == null) {
            reloadTypes = Set.of();
        }

        if (seekType == null) {
            seekType = SeekType.NONE;
        }

        burstAmount = Math.max(0, burstAmount);
        rpm = Mth.clamp(rpm, 1, 114514);
    }
}
