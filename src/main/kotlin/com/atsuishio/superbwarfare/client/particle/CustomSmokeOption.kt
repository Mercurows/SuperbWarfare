package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.ksp.annotation.GenerateMapCodec
import com.atsuishio.superbwarfare.tools.createStreamCodec
import kotlinx.serialization.Serializable
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType

@GenerateMapCodec
@Serializable
class CustomSmokeOption(val red: Float, val green: Float, val blue: Float) : ParticleOptions {
    override fun getType(): ParticleType<*> = ModParticleTypes.CUSTOM_SMOKE.get()

    companion object {
        val STREAM_CODEC = createStreamCodec<CustomSmokeOption>()
    }
}