@file:OptIn(ExperimentalSerializationApi::class)

package com.atsuishio.superbwarfare.network

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


private val module = SerializersModule {}

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

typealias CompressedString = @Serializable(CompressedStringSerializer::class) String
typealias SerializedUUID = @Serializable(UUIDSerializer::class) UUID
typealias SerializedResourceLocation = @Serializable(ResourceLocationSerializer::class) ResourceLocation
typealias SerializedVec3 = @Serializable(Vec3Serializer::class) Vec3
typealias SerializedVector3f = @Serializable(Vector3fSerializer::class) Vector3f
typealias SerializedBlockPos = @Serializable(BlockPosSerializer::class) BlockPos

object CompressedStringSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("CompressedString", PrimitiveKind.STRING)

    private fun compress(data: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { output ->
            output.write(data)
            output.finish()
        }
        return outputStream.toByteArray()
    }

    private fun decompress(compressedData: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()

        GZIPInputStream(ByteArrayInputStream(compressedData)).use { input ->
            val buffer = ByteArray(1024)
            var len: Int
            while ((input.read(buffer).also { len = it }) != -1) {
                outputStream.write(buffer, 0, len)
            }
        }
        return outputStream.toByteArray()
    }

    private val CACHE = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, ByteArray>() {
            override fun load(str: String): ByteArray {
                return compress(str.toByteArray())
            }
        })

    override fun serialize(encoder: Encoder, value: String) {
        val compressed = CACHE.getUnchecked(value)

        encoder.encodeInt(compressed.size)
        compressed.forEach {
            encoder.encodeByte(it)
        }
    }

    override fun deserialize(decoder: Decoder): String {
        val size = decoder.decodeInt()
        val bytes = ByteArray(size)

        repeat(size) { index ->
            bytes[index] = decoder.decodeByte()
        }

        return String(decompress(bytes))
    }
}

object ResourceLocationSerializer : KSerializer<ResourceLocation> {
    override val descriptor = PrimitiveSerialDescriptor("ResourceLocation", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ResourceLocation) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ResourceLocation {
        return ResourceLocation.parse(decoder.decodeString())
    }
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = buildClassSerialDescriptor("UUID") {
        element<Long>("mostSignificantBits")
        element<Long>("leastSignificantBits")
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeLong(value.mostSignificantBits)
        encoder.encodeLong(value.leastSignificantBits)
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