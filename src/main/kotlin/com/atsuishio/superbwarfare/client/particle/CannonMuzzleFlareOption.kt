package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.ksp.annotation.GenerateMapCodec
import com.atsuishio.superbwarfare.tools.createStreamCodec
import kotlinx.serialization.Serializable
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import kotlin.math.roundToInt

@GenerateMapCodec
@Serializable
data class CannonMuzzleFlareOption(
    val color: Int,
    val life: Int,
    val fade: Float,
    val animationSpeed: Int,
    val sizeAdd: Float
) : ParticleOptions {
    constructor(
        r: Float,
        g: Float,
        b: Float,
        life: Int,
        fade: Float,
        animationSpeed: Int,
        sizeAdd: Float
    ) : this(
        (r * 255).roundToInt() shl 16 or ((g * 255).roundToInt() shl 8) or (b * 255).roundToInt(),
        life,
        fade,
        animationSpeed,
        sizeAdd
    )

    val red get() = (this.color shr 16 and 255) / 255f
    val green get() = (this.color shr 8 and 255) / 255f
    val blue get() = (this.color and 255) / 255f

    override fun getType(): ParticleType<*> = ModParticleTypes.CANNON_MUZZLE_FLARE.get()

    companion object {
        val STREAM_CODEC = createStreamCodec<CannonMuzzleFlareOption>()
    }
}