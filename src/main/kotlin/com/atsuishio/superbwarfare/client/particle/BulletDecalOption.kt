package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedBlockPos
import com.atsuishio.superbwarfare.tools.asCodecField
import com.atsuishio.superbwarfare.tools.createStreamCodec
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType

@Serializable
class BulletDecalOption @JvmOverloads constructor(
    val direction: Direction,
    val pos: SerializedBlockPos,
    val red: Float = 0.9f,
    val green: Float = 0f,
    val blue: Float = 0f
) : ParticleOptions {
    constructor(dir: Int, pos: Long) : this(Direction.entries[dir], BlockPos.of(pos), 0.9f, 0f, 0f)

    constructor(dir: Int, pos: Long, r: Float, g: Float, b: Float) : this(
        Direction.entries[dir], BlockPos.of(pos), r, g, b
    )

    override fun getType(): ParticleType<BulletDecalOption> = ModParticleTypes.BULLET_DECAL.get()

    companion object {
        val CODEC: MapCodec<BulletDecalOption> = RecordCodecBuilder.mapCodec { builder ->
            builder.group(
                Codec.INT.fieldOf("dir").forGetter { it.direction.ordinal },
                Codec.LONG.fieldOf("pos").forGetter { it.pos.asLong() },
                BulletDecalOption::red.asCodecField(),
                BulletDecalOption::green.asCodecField(),
                BulletDecalOption::blue.asCodecField(),
            ).apply(builder, ::BulletDecalOption)
        }

        val STREAM_CODEC = createStreamCodec<BulletDecalOption>()
    }
}