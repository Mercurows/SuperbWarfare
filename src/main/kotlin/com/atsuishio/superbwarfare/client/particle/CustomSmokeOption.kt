package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.tools.asCodecField
import com.atsuishio.superbwarfare.tools.createStreamCodec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType

@Serializable
class CustomSmokeOption(val red: Float, val green: Float, val blue: Float) : ParticleOptions {
    override fun getType(): ParticleType<*> = ModParticleTypes.CUSTOM_SMOKE.get()

    companion object {
        val CODEC: MapCodec<CustomSmokeOption> = RecordCodecBuilder.mapCodec { builder ->
            builder.group(
                CustomSmokeOption::red.asCodecField("r"),
                CustomSmokeOption::green.asCodecField("g"),
                CustomSmokeOption::blue.asCodecField("b"),
            ).apply(builder, ::CustomSmokeOption)
        }

        val STREAM_CODEC = createStreamCodec<CustomSmokeOption>()
    }
}