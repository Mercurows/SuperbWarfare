package com.atsuishio.superbwarfare.serialization.kserializer

import com.google.common.io.ByteStreams
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.Tag
import net.minecraft.nbt.TagTypes

typealias SerializedTag = @Serializable(TagSerializer::class) Tag

object TagSerializer : KSerializer<Tag> {
    override val descriptor = buildClassSerialDescriptor("Tag") {
        element<Byte>("type")
        element<ByteArray>("data")
    }

    private val byteArraySerializer = ByteArraySerializer()

    override fun serialize(encoder: Encoder, value: Tag) {
        val byteArray = ByteStreams.newDataOutput()
            .apply { value.write(this) }
            .toByteArray()

        encoder.encodeByte(value.id)
        encoder.encodeSerializableValue(byteArraySerializer, byteArray)
    }

    override fun deserialize(decoder: Decoder): Tag {
        val type = decoder.decodeByte().toInt()
        val byteArray = decoder.decodeSerializableValue(byteArraySerializer)

        return TagTypes.getType(type).load(ByteStreams.newDataInput(byteArray), 0, NbtAccounter.UNLIMITED)
    }
}