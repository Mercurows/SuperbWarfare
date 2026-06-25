package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.serialization.ByteBufDecoder
import com.atsuishio.superbwarfare.serialization.ByteBufEncoder
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedBlockPos
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.registries.ForgeRegistries

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
        Direction.entries[dir],
        BlockPos.of(pos),
        r,
        g,
        b
    )

    override fun getType(): ParticleType<*> {
        return ModParticleTypes.BULLET_DECAL.get()
    }

    override fun writeToNetwork(buffer: FriendlyByteBuf) {
        ByteBufEncoder(buffer).encodeSerializableValue(serializer(), this)
    }

    override fun writeToString(): String {
        return "${ForgeRegistries.PARTICLE_TYPES.getKey(this.type)} ${this.direction.getName()}"
    }

    companion object {
        val CODEC: Codec<BulletDecalOption> =
            RecordCodecBuilder.create { builder ->
                builder.group(
                    Codec.INT.fieldOf("dir").forGetter { it.direction.ordinal },
                    Codec.LONG.fieldOf("pos").forGetter { it.pos.asLong() },
                    Codec.FLOAT.fieldOf("r").forGetter { it.red },
                    Codec.FLOAT.fieldOf("g").forGetter { it.green },
                    Codec.FLOAT.fieldOf("b").forGetter { it.blue }
                ).apply(builder, ::BulletDecalOption)
            }

        @Suppress("DEPRECATION")
        val DESERIALIZER: ParticleOptions.Deserializer<BulletDecalOption> =
            object : ParticleOptions.Deserializer<BulletDecalOption> {
                @Throws(CommandSyntaxException::class)
                override fun fromCommand(
                    particleType: ParticleType<BulletDecalOption>,
                    reader: StringReader
                ): BulletDecalOption {
                    reader.expect(' ')
                    val dir = reader.readInt()
                    reader.expect(' ')
                    val pos = reader.readLong()
                    reader.expect(' ')
                    val r = reader.readFloat()
                    reader.expect(' ')
                    val g = reader.readFloat()
                    reader.expect(' ')
                    val b = reader.readFloat()
                    return BulletDecalOption(dir, pos, r, g, b)
                }

                override fun fromNetwork(
                    particleType: ParticleType<BulletDecalOption>,
                    buffer: FriendlyByteBuf
                ) = ByteBufDecoder(buffer).decodeSerializableValue(serializer<BulletDecalOption>())
            }
    }
}
