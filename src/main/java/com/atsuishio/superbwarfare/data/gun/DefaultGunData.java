package com.atsuishio.superbwarfare.data.gun;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Set;

public class DefaultGunData {
    @SerializedName("ID")
    public String id = "";

    @SerializedName("RecoilX")
    public double recoilX;
    @SerializedName("RecoilY")
    public double recoilY;

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

    @SerializedName("Projectile")
    public ProjectileInfo projectile = new ProjectileInfo();

    @SerializedName("ProjectileAmount")
    public int projectileAmount = 1;
    @SerializedName("Weight")
    public double weight;

    @SerializedName("DefaultFireMode")
    public FireMode defaultFireMode = FireMode.SEMI;
    @SerializedName("AvailableFireModes")
    public Set<FireMode> availableFireModes = Set.of(FireMode.SEMI);

    @SerializedName("ReloadTypes")
    public Set<ReloadType> reloadTypes = Set.of(ReloadType.MAGAZINE);

    @SerializedName("BurstAmount")
    public int burstAmount;
    @SerializedName("BypassesArmor")
    public double bypassArmor;

    @SerializedName("AmmoType")
    public String ammoType = "";

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

    @SerializedName("ShootDelay")
    public int shootDelay = 0;

    @SerializedName("HeatPerShoot")
    public double heatPerShoot = 0;

    @SerializedName("AvailablePerks")
    public List<String> availablePerks = List.of(
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

    @SerializedName("DamageReduce")
    public DamageReduce damageReduce = new DamageReduce();
}
