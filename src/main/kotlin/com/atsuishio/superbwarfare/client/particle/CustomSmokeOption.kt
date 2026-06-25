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

@Serializable
class CustomSmokeOption(val red: Float, val green: Float, val blue: Float) : ParticleOptions {
    override fun getType(): ParticleType<*> {
        return ModParticleTypes.CUSTOM_SMOKE.get()
    }

    override fun writeToNetwork(buffer: FriendlyByteBuf) {
        ByteBufEncoder(buffer).encodeSerializableValue(serializer(), this)
    }

    override fun writeToString(): String {
        return "${ForgeRegistries.PARTICLE_TYPES.getKey(this.type)} [$red, $green, $blue]"
    }

    companion object {
        val CODEC: Codec<CustomSmokeOption> =
            RecordCodecBuilder.create { builder ->
                builder.group(
                    Codec.FLOAT.fieldOf("r").forGetter { it.red },
                    Codec.FLOAT.fieldOf("g").forGetter { it.green },
                    Codec.FLOAT.fieldOf("b").forGetter { it.blue }
                ).apply(builder, ::CustomSmokeOption)
            }

        @Suppress("DEPRECATION")
        val DESERIALIZER: ParticleOptions.Deserializer<CustomSmokeOption> =
            object : ParticleOptions.Deserializer<CustomSmokeOption> {
                @Throws(CommandSyntaxException::class)
                override fun fromCommand(
                    particleType: ParticleType<CustomSmokeOption>,
                    reader: StringReader
                ): CustomSmokeOption {
                    reader.expect(' ')
                    val r = reader.readFloat()
                    reader.expect(' ')
                    val g = reader.readFloat()
                    reader.expect(' ')
                    val b = reader.readFloat()
                    return CustomSmokeOption(r, g, b)
                }

                override fun fromNetwork(
                    particleType: ParticleType<CustomSmokeOption>,
                    buffer: FriendlyByteBuf
                ) = ByteBufDecoder(buffer).decodeSerializableValue(serializer<CustomSmokeOption>())
            }
    }
}
