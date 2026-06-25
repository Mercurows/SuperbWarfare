package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.serialization.ByteBufDecoder
import com.atsuishio.superbwarfare.serialization.ByteBufEncoder
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.registries.ForgeRegistries
import kotlin.math.roundToInt

@Serializable
class CustomCloudOption(
    private val color: Int,
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

    override fun writeToNetwork(buffer: FriendlyByteBuf) {
        ByteBufEncoder(buffer).encodeSerializableValue(serializer(), this)
    }

    override fun writeToString(): String {
        return "${ForgeRegistries.PARTICLE_TYPES.getKey(this.type)} [$color, $life, $size, $gravity, $cooldown, $light]"
    }

    companion object {
        val CODEC: Codec<CustomCloudOption> =
            RecordCodecBuilder.create { builder: RecordCodecBuilder.Instance<CustomCloudOption> ->
                builder.group(
                    Codec.INT.fieldOf("color").forGetter { it.color },
                    Codec.INT.fieldOf("life").forGetter { it.life },
                    Codec.FLOAT.fieldOf("size").forGetter { it.size },
                    Codec.FLOAT.fieldOf("gravity").forGetter { it.gravity },
                    Codec.BOOL.fieldOf("cooldown").forGetter { it.cooldown },
                    Codec.BOOL.fieldOf("light").forGetter { it.light }
                ).apply(builder, ::CustomCloudOption)
            }

        @Suppress("DEPRECATION")
        val DESERIALIZER: ParticleOptions.Deserializer<CustomCloudOption> =
            object : ParticleOptions.Deserializer<CustomCloudOption> {
                @Throws(CommandSyntaxException::class)
                override fun fromCommand(
                    particleType: ParticleType<CustomCloudOption>,
                    reader: StringReader
                ): CustomCloudOption {
                    reader.expect(' ')
                    val color = reader.readInt()
                    reader.expect(' ')
                    val life = reader.readInt()
                    reader.expect(' ')
                    val size = reader.readFloat()
                    reader.expect(' ')
                    val gravity = reader.readFloat()
                    reader.expect(' ')
                    val cooldown = reader.readBoolean()
                    reader.expect(' ')
                    val light = reader.readBoolean()
                    return CustomCloudOption(color, life, size, gravity, cooldown, light)
                }

                override fun fromNetwork(
                    particleType: ParticleType<CustomCloudOption>,
                    buffer: FriendlyByteBuf
                ) = ByteBufDecoder(buffer).decodeSerializableValue(serializer<CustomCloudOption>())
            }
    }
}
