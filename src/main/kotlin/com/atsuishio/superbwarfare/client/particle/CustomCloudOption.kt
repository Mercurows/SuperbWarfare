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
class CustomCloudOption(
    val color: Int,
    val life: Int,
    val size: Float,
    val gravity: Float,
    val cooldown: Boolean,
    val light: Boolean
) : ParticleOptions {
    constructor(
        r: Float,
        g: Float,
        b: Float,
        life: Int,
        size: Float,
        gravity: Float,
        cooldown: Boolean,
        light: Boolean
    ) : this(
        (r * 255).roundToInt() shl 16 or ((g * 255).roundToInt() shl 8) or (b * 255).roundToInt(),
        life,
        size,
        gravity,
        cooldown,
        light
    )

    val red: Float
        get() = (this.color shr 16 and 255) / 255f

    val green: Float
        get() = (this.color shr 8 and 255) / 255f

    val blue: Float
        get() = (this.color and 255) / 255f

    override fun getType(): ParticleType<*> {
        return ModParticleTypes.CUSTOM_CLOUD.get()
    }

    companion object {
        val STREAM_CODEC = createStreamCodec<CustomCloudOption>()
    }
}