package com.atsuishio.superbwarfare.resource.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ModColor;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.resource.ModelResource;
import com.google.gson.annotations.SerializedName;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class DefaultGunResource implements IDBasedData<DefaultGunResource> {

    private transient String id = "";

    @Override
    public @NotNull String getId() {
        return this.id;
    }

    @Override
    public void setId(@NotNull String id) {
        this.id = id;
    }

    @SerializedName("Icon")
    public String icon = Mod.loc("textures/gun_icon/default_icon.png").toString();

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
