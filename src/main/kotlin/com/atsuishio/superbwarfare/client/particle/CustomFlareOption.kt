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
class CustomFlareOption(
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
        return ModParticleTypes.CUSTOM_FLARE.get()
    }

    override fun writeToNetwork(buffer: FriendlyByteBuf) {
        ByteBufEncoder(buffer).encodeSerializableValue(serializer(), this)
    }

    override fun writeToString(): String {
        return "${ForgeRegistries.PARTICLE_TYPES.getKey(this.type)} [$color, $life, $fade, $size, $animationSpeed, $sizeAdd]"
    }

    companion object {
        val CODEC: Codec<CustomFlareOption> =
            RecordCodecBuilder.create { builder: RecordCodecBuilder.Instance<CustomFlareOption> ->
                builder.group(
                    Codec.INT.fieldOf("color").forGetter { it.color },
                    Codec.INT.fieldOf("life").forGetter { it.life },
                    Codec.FLOAT.fieldOf("fade").forGetter { it.fade },
                    Codec.FLOAT.fieldOf("size").forGetter { it.size },
                    Codec.INT.fieldOf("animationSpeed").forGetter { it.animationSpeed },
                    Codec.FLOAT.fieldOf("sizeAdd").forGetter { it.sizeAdd }
                ).apply(builder, ::CustomFlareOption)
            }

        @Suppress("DEPRECATION")
        val DESERIALIZER: ParticleOptions.Deserializer<CustomFlareOption> =
            object : ParticleOptions.Deserializer<CustomFlareOption> {
                @Throws(CommandSyntaxException::class)
                override fun fromCommand(
                    particleType: ParticleType<CustomFlareOption>,
                    reader: StringReader
                ): CustomFlareOption {
                    reader.expect(' ')
                    val color = reader.readInt()
                    reader.expect(' ')
                    val life = reader.readInt()
                    reader.expect(' ')
                    val fade = reader.readFloat()
                    reader.expect(' ')
                    val size = reader.readFloat()
                    reader.expect(' ')
                    val animationSpeed = reader.readInt()
                    reader.expect(' ')
                    val sizeAdd = reader.readFloat()
                    return CustomFlareOption(color, life, fade, size, animationSpeed, sizeAdd)
                }

                override fun fromNetwork(
                    particleType: ParticleType<CustomFlareOption>,
                    buffer: FriendlyByteBuf
                ) = ByteBufDecoder(buffer).decodeSerializableValue(serializer<CustomFlareOption>())
            }
    }
}
