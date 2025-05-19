package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.google.gson.annotations.SerializedName;

public class DefaultVehicleData {
    @SerializedName("ID")
    public String id = "";

    @SerializedName("MaxHealth")
    public float maxHealth = 50;

    @SerializedName("RepairCooldown")
    public int repairCooldown = VehicleConfig.REPAIR_COOLDOWN.get();

    @SerializedName("RepairAmount")
    public float repairAmount = VehicleConfig.REPAIR_AMOUNT.get().floatValue();

    @SerializedName("MaxEnergy")
    public int maxEnergy = 100000;

    @SerializedName("UpStep")
    public float upStep = 0;

    // TODO damage modifier
//    @SerializedName("DamageModifier")
//    private List<DamageModify> damageModifier;
}
