@file:OptIn(ExperimentalSerializationApi::class)

package com.atsuishio.superbwarfare.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.util.*

private val module = SerializersModule {
    contextual(ResourceLocation::class, ResourceLocationSerializer)
    contextual(UUID::class, UUIDSerializer)
    contextual(Vector3f::class, Vector3fSerializer)
    contextual(Vec3::class, Vec3Serializer)
    contextual(BlockPos::class, BlockPosSerializer)
}

class ByteBufEncoder(private val buf: FriendlyByteBuf) : AbstractEncoder() {
    override val serializersModule = module

    override fun encodeBoolean(value: Boolean) {
        buf.writeBoolean(value)
    }

    override fun encodeByte(value: Byte) {
        buf.writeByte(value.toInt())
    }

    override fun encodeShort(value: Short) {
        buf.writeShort(value.toInt())
    }

    override fun encodeInt(value: Int) {
        buf.writeVarInt(value)
    }

    override fun encodeLong(value: Long) {
        buf.writeLong(value)
    }

    override fun encodeFloat(value: Float) {
        buf.writeFloat(value)
    }

    override fun encodeDouble(value: Double) {
        buf.writeDouble(value)
    }

    override fun encodeChar(value: Char) {
        buf.writeChar(value.code)
    }

    override fun encodeString(value: String) {
        buf.writeUtf(value)
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        buf.writeVarInt(index)
    }

    override fun encodeNull() {
        buf.writeBoolean(false)
    }

    override fun encodeNotNullMark() {
        buf.writeBoolean(true)
    }

    override fun beginCollection(
        descriptor: SerialDescriptor,
        collectionSize: Int
    ): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }
}

class ByteBufDecoder(private val buf: FriendlyByteBuf, var elementIndex: Int = 0) : AbstractDecoder() {
    private var elementsCount = 0

    override val serializersModule = module

    override fun decodeBoolean() = buf.readBoolean()
    override fun decodeByte() = buf.readByte()
    override fun decodeShort() = buf.readShort()
    override fun decodeInt() = buf.readVarInt()
    override fun decodeLong() = buf.readLong()
    override fun decodeFloat() = buf.readFloat()
    override fun decodeDouble() = buf.readDouble()
    override fun decodeChar() = buf.readChar()
    override fun decodeString(): String = buf.readUtf()
    override fun decodeEnum(enumDescriptor: SerialDescriptor) = decodeInt()

    override fun decodeNotNullMark() = decodeBoolean()
    override fun decodeCollectionSize(descriptor: SerialDescriptor) = decodeInt().also { elementsCount = it }

    override fun beginStructure(descriptor: SerialDescriptor) = ByteBufDecoder(buf, descriptor.elementsCount)

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun decodeSequentially() = true
}

object ResourceLocationSerializer : KSerializer<ResourceLocation> {

    private val delegateSerializer = String.serializer()
    override val descriptor = SerialDescriptor("ResourceLocation", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: ResourceLocation) {
        encoder.encodeSerializableValue(delegateSerializer, value.toString())
    }

    override fun deserialize(decoder: Decoder): ResourceLocation {
        return ResourceLocation.tryParse(decoder.decodeSerializableValue(delegateSerializer))!!
    }
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = buildClassSerialDescriptor("UUID") {
        element<Long>("leastSignificantBits")
        element<Long>("mostSignificantBits")
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeLong(value.leastSignificantBits)
        encoder.encodeLong(value.mostSignificantBits)
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID(decoder.decodeLong(), decoder.decodeLong())
    }
}

object Vector3fSerializer : KSerializer<Vector3f> {
    override val descriptor = buildClassSerialDescriptor("Vector3f") {
        element<Float>("x")
        element<Float>("y")
        element<Float>("z")
    }

    override fun serialize(encoder: Encoder, value: Vector3f) {
        encoder.encodeFloat(value.x)
        encoder.encodeFloat(value.y)
        encoder.encodeFloat(value.z)
    }

    override fun deserialize(decoder: Decoder): Vector3f {
        return Vector3f(decoder.decodeFloat(), decoder.decodeFloat(), decoder.decodeFloat())
    }
}

object Vec3Serializer : KSerializer<Vec3> {
    override val descriptor = buildClassSerialDescriptor("Vec3") {
        element<Double>("x")
        element<Double>("y")
        element<Double>("z")
    }

    override fun serialize(encoder: Encoder, value: Vec3) {
        encoder.encodeDouble(value.x)
        encoder.encodeDouble(value.y)
        encoder.encodeDouble(value.z)
    }

    override fun deserialize(decoder: Decoder): Vec3 {
        return Vec3(decoder.decodeDouble(), decoder.decodeDouble(), decoder.decodeDouble())
    }
}

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