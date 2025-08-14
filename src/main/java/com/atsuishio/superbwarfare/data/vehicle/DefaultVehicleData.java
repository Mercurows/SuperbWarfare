package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModify;
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

    @ServerOnly
    @SerializedName("RepairMaterial")
    public String repairMaterial = "minecraft:iron_ingot";

    @ServerOnly
    @SerializedName("RepairMaterialHealAmount")
    public float repairMaterialHealAmount = 50;

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
}
