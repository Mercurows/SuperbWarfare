package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleContainerType;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleIconInfo;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModify;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class DefaultVehicleData implements IDBasedData {
    @SerializedName("ID")
    public String id = "";

    public transient boolean isDefaultData = true;

    @Override
    public String getId() {
        return this.id;
    }

    @SerializedName("MaxHealth")
    public float maxHealth = 50;

    // TODO 这玩意还能不能用配置？

    @ServerOnly
    @SerializedName("RepairCooldown")
    public int repairCooldown = 200;

    @ServerOnly
    @SerializedName("RepairAmount")
    public float repairAmount = 0.05F;

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

    @SerializedName("Seats")
    public ObjectToList<SeatInfo> seats = new ObjectToList<>();

    @SerializedName("UpStep")
    public float upStep = 0;

    @SerializedName("AllowFreeCam")
    public boolean allowFreeCam = false;

    @SerializedName("ApplyDefaultDamageModifiers")
    public boolean applyDefaultDamageModifiers = true;

    @ServerOnly
    @SerializedName("DamageModifiers")
    public ObjectToList<StringToObject<DamageModify>> damageModifiers = new ObjectToList<>();

    @ServerOnly
    @SerializedName("Mass")
    public float mass = 1;

    @ServerOnly
    @SerializedName("CrashPassengersOnDestroy")
    public boolean crashPassengersOnDestroy = false;

    @ServerOnly
    @SerializedName("ExplodePassengersOnDestroy")
    public boolean explodePassengersOnDestroy = true;

    @ServerOnly
    @SerializedName("ExplosionDamage")
    public float explosionDamage = 0;

    @ServerOnly
    @SerializedName("ExplosionRadius")
    public float explosionRadius = 0;

    @ServerOnly
    @SerializedName("ExplosionDestroyBlockOnDestroy")
    public boolean explosionDestroyBlockOnDestroy = true;

    @ServerOnly
    @SerializedName("ExplosionParticleType")
    public ParticleTool.ParticleType explosionParticleType = ParticleTool.ParticleType.MINI;

    @SerializedName("VehicleContainerType")
    public VehicleContainerType vehicleContainerType = VehicleContainerType.MEDIUM;

    @SerializedName("HasUpgradeSlots")
    public boolean hasUpgradeSlots = false;

    @SerializedName("Icon")
    public VehicleIconInfo icon = new VehicleIconInfo(Mod.loc("textures/gun_icon/default_icon.png").toString(), null);

    @SerializedName("Type")
    public VehicleType type = VehicleType.EMPTY;
}
