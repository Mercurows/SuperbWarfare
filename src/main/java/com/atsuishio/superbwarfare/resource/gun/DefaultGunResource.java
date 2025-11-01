package com.atsuishio.superbwarfare.resource.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ModColor;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.resource.ModelResource;
import com.google.gson.annotations.SerializedName;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

public class DefaultGunResource implements IDBasedData {

    @SerializedName("ID")
    public String id = "";

    public transient boolean isDefaultResource = true;

    @Override
    public String getId() {
        return this.id;
    }

    @SerializedName("GunIcon")
    public String gunIcon = Mod.loc("textures/gun_icon/default_icon.png").toString();

    @SerializedName("Model")
    public ModelResource model = new ModelResource();

    public ModelResource getModel() {
        return model == null ? new ModelResource() : model;
    }

    @SerializedName("Animation")
    public GunAnimation animation = new GunAnimation();

    @SerializedName("UseOldHandRenderer")
    public boolean useOldHandRenderer = false;

    @SerializedName("FlarePosition")
    public Vec3 flarePosition = null;

    @SerializedName("FlareSize")
    public float flareSize = 1;

    /*
     * 准星类型
     * 预制的字段有：
     * @Custom - 自定义
     * @GunDefault - 默认枪械准星
     * @VehicleDefault - 默认载具准星
     */
    // TODO 支持override之后把二次灾变的霰弹模式的普通准星加上
    @SerializedName("Crosshair")
    public String crosshair = "@GunDefault";
    @SerializedName("CrosshairColor")
    public ModColor crosshairColor = new ModColor();

    @SerializedName("Name")
    public String name = "superbwarfare.gun.default";
    @SerializedName("HideCrosshairWhenZoom")
    public boolean hideCrosshairWhenZoom = true;

    @SerializedName("EnergyBarColor")
    public ModColor energyBarColor = new ModColor(0x95E9FF);

    @SerializedName("TriggerSound")
    public SoundEvent triggerSound = ModSounds.TRIGGER_CLICK.get();
    @SerializedName("DischargeSound")
    public SoundEvent dischargeSound = null;

    @SerializedName("EjectShell")
    public boolean ejectShell = false;
    @SerializedName("CanZoom")
    public boolean canZoom = true;
}
