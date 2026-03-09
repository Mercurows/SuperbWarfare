package com.atsuishio.superbwarfare.serialization.kserializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.core.BlockPos

typealias SerializedBlockPos = @Serializable(BlockPosSerializer::class) BlockPos

object BlockPosSerializer : KSerializer<BlockPos> {
    override val descriptor = buildClassSerialDescriptor("BlockPos") {
        element<Int>("x")
        element<Int>("y")
        element<Int>("z")
    }

    override fun serialize(encoder: Encoder, value: BlockPos) {
        encoder.encodeInt(value.x)
        encoder.encodeInt(value.y)
        encoder.encodeInt(value.z)
    }

    override fun deserialize(decoder: Decoder): BlockPos {
        return BlockPos(decoder.decodeInt(), decoder.decodeInt(), decoder.decodeInt())
    }
}