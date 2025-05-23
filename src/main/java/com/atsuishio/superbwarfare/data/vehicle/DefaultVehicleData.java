package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModify;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DefaultVehicleData {
    @SerializedName("ID")
    public String id = "";

    @SerializedName("MaxHealth")
    public float maxHealth = 50;

    @ServerOnly
    @SerializedName("RepairCooldown")
    public int repairCooldown = VehicleConfig.REPAIR_COOLDOWN.get();

    @ServerOnly
    @SerializedName("RepairAmount")
    public float repairAmount = VehicleConfig.REPAIR_AMOUNT.get().floatValue();

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
    public List<DamageModify> damageModifiers = List.of();

    @ServerOnly
    @SerializedName("Mass")
    public float mass = 1;
}
