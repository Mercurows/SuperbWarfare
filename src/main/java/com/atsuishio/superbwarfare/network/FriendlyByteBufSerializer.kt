@file:OptIn(ExperimentalSerializationApi::class)

package com.atsuishio.superbwarfare.network

import io.netty.buffer.Unpooled
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import net.minecraft.network.FriendlyByteBuf


class ByteBufEncoder(private val buf: FriendlyByteBuf) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

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

    override val serializersModule: SerializersModule = EmptySerializersModule()

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

@Serializable
data class D(
    val a: Int = 114,
    val b: String = "514",
    val c: Float? = null
)

inline fun <reified T> encodeTo(output: FriendlyByteBuf, value: T) {
    ByteBufEncoder(output).encodeSerializableValue(serializer(), value)
}

inline fun <reified T> decodeFrom(input: FriendlyByteBuf): T {
    return ByteBufDecoder(input).decodeSerializableValue(serializer())
}

fun test() {
    val buf = FriendlyByteBuf(Unpooled.buffer())
    encodeTo(buf, D())
    val d = decodeFrom<D>(FriendlyByteBuf(buf.copy()))
    println(d)

    val buf2 = FriendlyByteBuf(Unpooled.buffer())
    encodeTo(buf2, D(114514, "1919810", 114.514F))
    val d2 = decodeFrom<D>(FriendlyByteBuf(buf2.copy()))
    println(d2)
}