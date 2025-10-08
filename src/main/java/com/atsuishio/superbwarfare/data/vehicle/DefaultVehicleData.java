package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleContainerType;
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

    @ServerOnly
    @SerializedName("RepairCooldown")
    public int repairCooldown = VehicleConfig.REPAIR_COOLDOWN.get();

    @ServerOnly
    @SerializedName("RepairAmount")
    public float repairAmount = VehicleConfig.REPAIR_AMOUNT.get().floatValue();

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
}
