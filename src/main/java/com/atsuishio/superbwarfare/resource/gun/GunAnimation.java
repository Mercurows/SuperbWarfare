package com.atsuishio.superbwarfare.resource.gun;

import com.google.gson.annotations.SerializedName;

public class GunAnimation {

    @SerializedName("TransitionTickTime")
    public int transitionTickTime = 1;

    // This should NOT be null or empty!
    @SerializedName("Idle")
    public String idle;

    @SerializedName("Fire")
    public String fire;

    // Reload > ReloadNormal | ReloadEmpty
    @SerializedName("Reload")
    public String reload;
    @SerializedName("ReloadNormal")
    public String reloadNormal;
    @SerializedName("ReloadEmpty")
    public String reloadEmpty;

    @SerializedName("Prepare")
    public String prepare;
    @SerializedName("Iterative")
    public String iterative;
    @SerializedName("Finish")
    public String finish;

    @SerializedName("Edit")
    public String edit;

    @SerializedName("Bolt")
    public String bolt;

    @SerializedName("Run")
    public String run;

    @SerializedName("Melee")
    public String melee;
}
