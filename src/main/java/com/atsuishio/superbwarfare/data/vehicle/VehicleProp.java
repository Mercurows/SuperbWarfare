package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.data.Prop;
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleContainerType;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModify;
import com.atsuishio.superbwarfare.tools.ParticleTool;

import java.util.List;

public class VehicleProp<T> extends Prop<VehicleData, DefaultVehicleData, T> {

    public static final VehicleProp<Float> MAX_HEALTH = new VehicleProp<Float>("MaxHealth")
            .withLimiter(v -> Math.max(0, v));
    public static final VehicleProp<Integer> REPAIR_COOLDOWN = new VehicleProp<Integer>("RepairCooldown")
            .withLimiter(v -> Math.max(0, v));
    public static final VehicleProp<Float> REPAIR_AMOUNT = new VehicleProp<>("RepairAmount");
    public static final VehicleProp<Float> SELF_HURT_PERCENT = new VehicleProp<>("SelfHurtPercent");
    public static final VehicleProp<Float> SELF_HURT_AMOUNT = new VehicleProp<>("SelfHurtAmount");

    public static final VehicleProp<Integer> MAX_ENERGY = new VehicleProp<Integer>("MaxEnergy")
            .withLimiter(v -> Math.max(0, v));

    public static final VehicleProp<List<SeatInfo>> SEATS = new VehicleProp<List<SeatInfo>>("Seats")
            .withLimiter(v -> v == null ? List.of() : v);

    public static final VehicleProp<Float> UP_STEP = new VehicleProp<>("UpStep");
    public static final VehicleProp<Boolean> ALLOW_FREE_CAM = new VehicleProp<>("AllowFreeCam");
    public static final VehicleProp<Boolean> APPLY_DEFAULT_DAMAGE_MODIFIERS = new VehicleProp<>("ApplyDefaultDamageModifiers");

    public static final VehicleProp<List<DamageModify>> DAMAGE_MODIFIERS = new VehicleProp<>("DamageModifiers");

    public static final VehicleProp<Float> MASS = new VehicleProp<>("Mass");

    public static final VehicleProp<Boolean> CRASH_PASSENGERS_ON_DESTROY = new VehicleProp<>("CrashPassengersOnDestroy");
    public static final VehicleProp<Boolean> EXPLODE_PASSENGERS_ON_DESTROY = new VehicleProp<>("ExplodePassengersOnDestroy");
    public static final VehicleProp<Boolean> EXPLOSION_DESTROY_BLOCK_ON_DESTROY = new VehicleProp<>("ExplosionDestroyBlockOnDestroy");

    public static final VehicleProp<Float> EXPLOSION_DAMAGE = new VehicleProp<>("ExplosionDamage");
    public static final VehicleProp<Float> EXPLOSION_RADIUS = new VehicleProp<>("ExplosionRadius");

    public static final VehicleProp<ParticleTool.ParticleType> EXPLOSION_PARTICLE_TYPE = new VehicleProp<>("ExplosionParticleType");
    public static final VehicleProp<VehicleContainerType> VEHICLE_CONTAINER_TYPE = new VehicleProp<>("VehicleContainerType");
    public static final VehicleProp<Boolean> HAS_UPGRADE_SLOTS = new VehicleProp<>("HasUpgradeSlots");

    private VehicleProp(String name) {
        super(DefaultVehicleData.class, name);
    }
}
