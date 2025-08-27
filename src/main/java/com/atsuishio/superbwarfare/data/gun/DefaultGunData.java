package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class DefaultGunData implements IDBasedData {
    @SerializedName("ID")
    public String id = "";

    public transient boolean isDefaultData = true;

    @Override
    public String getId() {
        return this.id;
    }

    // 不要动态修改这玩意，很容易出问题
    @SerializedName("MaxDurability")
    public int maxDurability = 0;

    @SerializedName("DurabilityPerShoot")
    public int durabilityPerShoot = 1;

    @SerializedName("RecoilX")
    public double recoilX;
    @SerializedName("RecoilY")
    public double recoilY;
    @SerializedName("Recoil")
    public double recoil;

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

    @SerializedName("MeleeDamage")
    public double meleeDamage;
    @SerializedName("MeleeDuration")
    public int meleeDuration = 16;
    @SerializedName("MeleeDamageTime")
    public int meleeDamageTime = 6;

    @ServerOnly
    @SerializedName("Projectile")
    public StringToObject<ProjectileInfo> projectile = new StringToObject<>(new ProjectileInfo());

    @SerializedName("AmmoCostPerShoot")
    public int ammoCostPerShoot = 1;
    @SerializedName("ProjectileAmount")
    public int projectileAmount = 1;
    @SerializedName("Weight")
    public double weight = 1;

    @SerializedName("DefaultFireMode")
    public FireMode defaultFireMode = FireMode.SEMI;
    @SerializedName("AvailableFireModes")
    public Set<FireMode> availableFireModes = Set.of(FireMode.SEMI);

    @SerializedName("ReloadTypes")
    public Set<ReloadType> reloadTypes = Set.of(ReloadType.MAGAZINE);

    @SerializedName("AutoReload")
    public boolean autoReload = false;

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

    // 单发装填时的上弹时间
    @SerializedName("IterativeAmmoLoadTime")
    public int iterativeAmmoLoadTime = 1;

    // 单次单发装填上弹数量
    @SerializedName("IterativeLoadAmount")
    public int iterativeLoadAmount = 1;

    @SerializedName("FinishTime")
    public int finishTime;

    @SerializedName("SoundRadius")
    public double soundRadius;
    @SerializedName("RPM")
    public int rpm = 600;

    @SerializedName("ExplosionDamage")
    public double explosionDamage;
    @SerializedName("ExplosionRadius")
    public double explosionRadius;
    @SerializedName("Gravity")
    public double gravity = Double.NaN;

    @SerializedName("ShootDelay")
    public int shootDelay = 0;

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

    @ServerOnly
    @SerializedName("DamageReduce")
    public DamageReduce damageReduce = new DamageReduce();
}
