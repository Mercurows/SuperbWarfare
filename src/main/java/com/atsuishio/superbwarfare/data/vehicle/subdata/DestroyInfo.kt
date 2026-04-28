package com.atsuishio.superbwarfare.data.vehicle.subdata

import com.atsuishio.superbwarfare.tools.ParticleTool
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DestroyInfo {
    @SerialName("CrashPassengers")
    var crashPassengers: Boolean = false

    @SerialName("ExplodePassengers")
    var explodePassengers: Boolean = true

    @SerialName("ExplodeBlocks")
    var explodeBlocks: Boolean = true

    @SerialName("ExplosionDamage")
    var explosionDamage: Float = 0f

    @SerialName("ExplosionRadius")
    var explosionRadius: Float = 0f

    @SerialName("ParticleType")
    var particleType: ParticleTool.ParticleType = ParticleTool.ParticleType.MINI

    @SerialName("SympatheticDetonation")
    var sympatheticDetonation: Boolean = false

    @SerialName("SympatheticDetonationForce")
    var sympatheticDetonationForce: Float = 1.5f

    @SerialName("SympatheticDetonationChance")
    var sympatheticDetonationChance: Float = 0.5f

    @SerialName("NoWreck")
    var noWreck: Boolean = false

    constructor(
        crashPassengers: Boolean,
        explodePassengers: Boolean,
        explodeBlocks: Boolean,
        explosionDamage: Float,
        explosionRadius: Float,
        particleType: ParticleTool.ParticleType
    ) {
        this.crashPassengers = crashPassengers
        this.explodePassengers = explodePassengers
        this.explodeBlocks = explodeBlocks
        this.explosionDamage = explosionDamage
        this.explosionRadius = explosionRadius
        this.particleType = particleType
    }

    constructor()
}
