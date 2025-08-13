package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.data.Prop;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModify;

import java.util.List;

public class VehicleProp<T> extends Prop<VehicleData, DefaultVehicleData, T> {

    public static final VehicleProp<Float> MAX_HEALTH = new VehicleProp<Float>("MaxHealth")
            .withLimiter(v -> Math.max(0, v));
    public static final VehicleProp<Integer> REPAIR_COOLDOWN = new VehicleProp<Integer>("RepairCooldown")
            .withLimiter(v -> Math.max(0, v));
    public static final VehicleProp<Float> REPAIR_AMOUNT = new VehicleProp<>("RepairAmount");
    public static final VehicleProp<String> REPAIR_MATERIAL = new VehicleProp<>("RepairMaterial");
    public static final VehicleProp<Float> REPAIR_MATERIAL_HEAL_AMOUNT = new VehicleProp<>("RepairMaterialHealAmount");

    public static final VehicleProp<Float> SELF_HURT_PERCENT = new VehicleProp<>("SelfHurtPercent");
    public static final VehicleProp<Float> SELF_HURT_AMOUNT = new VehicleProp<>("SelfHurtAmount");

    public static final VehicleProp<Integer> MAX_ENERGY = new VehicleProp<Integer>("MaxEnergy")
            .withLimiter(v -> Math.max(0, v));
    public static final VehicleProp<Float> UP_STEP = new VehicleProp<>("UpStep");
    public static final VehicleProp<Boolean> ALLOW_FREE_CAM = new VehicleProp<>("AllowFreeCam");
    public static final VehicleProp<Boolean> APPLY_DEFAULT_DAMAGE_MODIFIERS = new VehicleProp<>("ApplyDefaultDamageModifiers");

    public static final VehicleProp<List<DamageModify>> DAMAGE_MODIFIERS = new VehicleProp<>("DamageModifiers");

    public static final VehicleProp<Float> MASS = new VehicleProp<>("Mass");


    private VehicleProp(String name) {
        super(DefaultVehicleData.class, name);
    }
}
