package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.google.gson.annotations.SerializedName;

public class DestroyInfo {

    @SerializedName("CrashPassengers")
    public boolean crashPassengers = false;

    @SerializedName("ExplodePassengers")
    public boolean explodePassengers = true;

    @SerializedName("ExplodeBlocks")
    public boolean explodeBlocks = true;

    @SerializedName("ExplosionDamage")
    public float explosionDamage = 0;

    @SerializedName("ExplosionRadius")
    public float explosionRadius = 0;

    @SerializedName("ParticleType")
    public ParticleTool.ParticleType particleType = ParticleTool.ParticleType.MINI;

    @SerializedName("SympatheticDetonation")
    public boolean sympatheticDetonation = false;

    @SerializedName("SympatheticDetonationForce")
    public float sympatheticDetonationForce = 1.5f;

    @SerializedName("SympatheticDetonationChance")
    public float sympatheticDetonationChance = 0.5f;

    @SerializedName("NoWreck")
    public boolean NoWreck = false;

    public DestroyInfo(boolean crashPassengers, boolean explodePassengers, boolean explodeBlocks, float explosionDamage, float explosionRadius, ParticleTool.ParticleType particleType) {
        this.crashPassengers = crashPassengers;
        this.explodePassengers = explodePassengers;
        this.explodeBlocks = explodeBlocks;
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
        this.particleType = particleType;
    }

    public DestroyInfo() {
    }
}
