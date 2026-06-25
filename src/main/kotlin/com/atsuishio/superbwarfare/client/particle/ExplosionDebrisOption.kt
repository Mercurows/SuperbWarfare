package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.tools.createStreamCodec
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import kotlin.math.roundToInt

@Serializable
class ExplosionDebrisOption(
    private val color: Int,
    val life: Int,
    val fade: Float,
    val size: Float,
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
        sizeAdd: Float,
        size: Float = 1f
    ) : this(
        (r * 255).roundToInt() shl 16 or ((g * 255).roundToInt() shl 8) or (b * 255).roundToInt(),
        life,
        fade,
        size,
        animationSpeed,
        sizeAdd
    )

    val red: Float
        get() = (this.color shr 16 and 255) / 255f

    val green: Float
        get() = (this.color shr 8 and 255) / 255f

    val blue: Float
        get() = (this.color and 255) / 255f

    override fun getType(): ParticleType<*> {
        return ModParticleTypes.EXPLOSION_DEBRIS.get()
    }

    companion object {
        val CODEC: MapCodec<ExplosionDebrisOption> =
            RecordCodecBuilder.mapCodec { builder: RecordCodecBuilder.Instance<ExplosionDebrisOption> ->
                builder.group(
                    Codec.INT.fieldOf("color").forGetter { it.color },
                    Codec.INT.fieldOf("life").forGetter { it.life },
                    Codec.FLOAT.fieldOf("fade").forGetter { it.fade },
                    Codec.FLOAT.fieldOf("size").forGetter { it.size },
                    Codec.INT.fieldOf("animationSpeed").forGetter { it.animationSpeed },
                    Codec.FLOAT.fieldOf("sizeAdd").forGetter { it.sizeAdd }
                ).apply(builder, ::ExplosionDebrisOption)
            }

        val STREAM_CODEC = createStreamCodec<ExplosionDebrisOption>()
    }
}
