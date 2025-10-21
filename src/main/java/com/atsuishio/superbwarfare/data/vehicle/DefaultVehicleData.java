package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ModColor;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.atsuishio.superbwarfare.data.vehicle.subdata.*;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModify;
import com.google.gson.annotations.SerializedName;
import net.minecraftforge.common.ForgeConfigSpec;

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
    public int repairCooldown = getConfigOrDefault(VehicleConfig.REPAIR_COOLDOWN);

    @ServerOnly
    @SerializedName("RepairAmount")
    public float repairAmount = getConfigOrDefault(VehicleConfig.REPAIR_AMOUNT).floatValue();

    private static <T> T getConfigOrDefault(ForgeConfigSpec.ConfigValue<T> config) {
        try {
            return config.get();
        } catch (Exception exception) {
            return config.getDefault();
        }
    }

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
    @SerializedName("DestroyInfo")
    public DestroyInfo destroyInfo = new DestroyInfo();

    @SerializedName("VehicleContainerType")
    public VehicleContainerType vehicleContainerType = VehicleContainerType.MEDIUM;

    @SerializedName("HasUpgradeSlots")
    public boolean hasUpgradeSlots = false;

    // TODO 能不能挪assets里
    @SerializedName("Icon")
    public VehicleIconInfo icon = new VehicleIconInfo(Mod.loc("textures/gun_icon/default_icon.png").toString(), null);
    @SerializedName("HUDColor")
    public ModColor hudColor = new ModColor(0x66FF00);

    @SerializedName("Type")
    public VehicleType type = VehicleType.EMPTY;

    @SerializedName("Engine")
    public EngineInfo engine = new EngineInfo();
}
