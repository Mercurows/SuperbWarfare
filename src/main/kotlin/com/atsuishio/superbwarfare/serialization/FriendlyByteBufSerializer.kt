@file:OptIn(ExperimentalSerializationApi::class)

package com.atsuishio.superbwarfare.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import net.minecraft.network.FriendlyByteBuf

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