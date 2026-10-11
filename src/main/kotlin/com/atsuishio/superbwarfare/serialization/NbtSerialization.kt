@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)

package com.atsuishio.superbwarfare.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.internal.NamedValueDecoder
import kotlinx.serialization.internal.NamedValueEncoder
import kotlinx.serialization.modules.SerializersModule
import net.minecraft.nbt.*

/**
 * Serializes [value] through [serializer] directly into an NBT [CompoundTag], without any
 * intermediate JSON/Gson representation.
 *
 * Only object-shaped roots are supported, which matches capability serialization.
 * [serializersModule] is forwarded so that `@Contextual` fields can resolve custom serializers.
 *
 * When [encodeDefaults] is `false`, properties equal to their declared default are left out of the
 * tag; a missing key decodes back to that default, so the round trip is unchanged while the payload
 * stays as small as the hand-written `if (value == default) remove(key)` idiom. Elements without a
 * default are always written.
 */
fun <T> encodeToCompoundTag(
    serializer: SerializationStrategy<T>,
    value: T,
    serializersModule: SerializersModule = SerializersModule {},
    encodeDefaults: Boolean = true
): CompoundTag {
    lateinit var result: CompoundTag
    NbtObjectEncoder(serializersModule, encodeDefaults) { tag ->
        result = tag as? CompoundTag
            ?: throw SerializationException("NBT encoder root must be a CompoundTag, got ${tag.type.name}")
    }.encodeSerializableValue(serializer, value)
    return result
}

/** Deserializes [tag] back into an object using the mirror of [encodeToCompoundTag]. */
fun <T> decodeFromCompoundTag(
    deserializer: DeserializationStrategy<T>,
    tag: CompoundTag,
    serializersModule: SerializersModule = SerializersModule {}
): T = NbtObjectDecoder(serializersModule, tag).decodeSerializableValue(deserializer)

/**
 * Tree encoder that materializes each structure into its own NBT [Tag] and hands it to the
 * parent through [nodeConsumer] when the structure is finished. Mirrors the approach used by
 * kotlinx's internal JSON tree encoder, but writes native NBT tags.
 */
private abstract class AbstractNbtEncoder(
    final override val serializersModule: SerializersModule,
    private val encodeDefaults: Boolean,
    private val nodeConsumer: (Tag) -> Unit
) : NamedValueEncoder(), NbtRawTagEncoder {

    final override fun composeName(parentName: String, childName: String): String = childName

    /**
     * Optional (defaulted) properties are skipped when [encodeDefaults] is `false`: NBT is
     * self-describing here, since a missing key decodes back to the property's default.
     */
    final override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        encodeDefaults

    /** Consumes the pending element name, exactly like the built-in primitive encoders do. */
    final override fun writeRawTag(tag: Tag) {
        putElement(popTag(), tag)
    }

    protected abstract fun putElement(key: String, element: Tag)

    protected abstract fun getCurrent(): Tag

    override fun encodeTaggedBoolean(tag: String, value: Boolean) =
        putElement(tag, ByteTag.valueOf(value))

    override fun encodeTaggedByte(tag: String, value: Byte) =
        putElement(tag, ByteTag.valueOf(value))

    override fun encodeTaggedShort(tag: String, value: Short) =
        putElement(tag, ShortTag.valueOf(value))

    override fun encodeTaggedInt(tag: String, value: Int) =
        putElement(tag, IntTag.valueOf(value))

    override fun encodeTaggedLong(tag: String, value: Long) =
        putElement(tag, LongTag.valueOf(value))

    override fun encodeTaggedFloat(tag: String, value: Float) =
        putElement(tag, FloatTag.valueOf(value))

    override fun encodeTaggedDouble(tag: String, value: Double) =
        putElement(tag, DoubleTag.valueOf(value))

    override fun encodeTaggedChar(tag: String, value: Char) =
        putElement(tag, StringTag.valueOf(value.toString()))

    override fun encodeTaggedString(tag: String, value: String) =
        putElement(tag, StringTag.valueOf(value))

    override fun encodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor, ordinal: Int) =
        putElement(tag, StringTag.valueOf(enumDescriptor.getElementName(ordinal)))

    /** NBT has no null type; nullable values are encoded by omitting the key entirely. */
    override fun encodeTaggedNull(tag: String) = Unit

    override fun encodeTaggedValue(tag: String, value: Any): Unit =
        throw SerializationException("NBT serialization does not support ${value::class} at '$tag'")

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val consumer: (Tag) -> Unit =
            if (currentTagOrNull == null) nodeConsumer
            else { tag -> putElement(currentTag, tag) }

        return when (descriptor.kind) {
            StructureKind.LIST -> NbtListEncoder(serializersModule, encodeDefaults, consumer)
            StructureKind.MAP -> throw SerializationException(
                "NBT serialization does not support maps (${descriptor.serialName})"
            )

            is PolymorphicKind -> throw SerializationException(
                "NBT serialization does not support polymorphic types (${descriptor.serialName})"
            )

            else -> NbtObjectEncoder(serializersModule, encodeDefaults, consumer)
        }
    }

    override fun endEncode(descriptor: SerialDescriptor) {
        nodeConsumer(getCurrent())
    }
}

private class NbtObjectEncoder(
    serializersModule: SerializersModule,
    encodeDefaults: Boolean,
    nodeConsumer: (Tag) -> Unit
) : AbstractNbtEncoder(serializersModule, encodeDefaults, nodeConsumer) {

    private val content = CompoundTag()

    override fun putElement(key: String, element: Tag) {
        content.put(key, element)
    }

    override fun getCurrent(): Tag = content
}

private class NbtListEncoder(
    serializersModule: SerializersModule,
    encodeDefaults: Boolean,
    nodeConsumer: (Tag) -> Unit
) : AbstractNbtEncoder(serializersModule, encodeDefaults, nodeConsumer) {

    private val content = ArrayList<Tag>()

    override fun putElement(key: String, element: Tag) {
        content.add(element)
    }

    override fun getCurrent(): Tag {
        val list = ListTag()
        for (element in content) {
            list.add(element)
        }
        return list
    }
}

/**
 * Tree decoder that reads a serialized value from its own NBT subtree. Missing fields that are
 * nullable and have no default value are reported as `null` (NBT omits them during encoding).
 */
private abstract class AbstractNbtDecoder(
    final override val serializersModule: SerializersModule
) : NamedValueDecoder(), NbtRawTagDecoder {

    protected abstract val value: Tag

    protected abstract fun currentElement(tag: String): Tag?

    final override fun composeName(parentName: String, childName: String): String = childName

    /** Consumes the pending element name, exactly like the built-in primitive decoders do. */
    final override fun readRawTag(): Tag? = currentElement(runCatching { popTag() }.getOrElse { return null })

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val current = currentTagOrNull?.let { currentElement(it) } ?: value

        return when (descriptor.kind) {
            StructureKind.LIST -> NbtListDecoder(serializersModule, current as? ListTag ?: ListTag())

            StructureKind.MAP, is PolymorphicKind -> NbtObjectDecoder(serializersModule, CompoundTag())

            else -> NbtObjectDecoder(serializersModule, current as? CompoundTag ?: CompoundTag())
        }
    }

    /** A tag of the wrong kind is read as zero, which is what the property decodes to anyway. */
    private fun numericTag(tag: String): NumericTag =
        currentElement(tag) as? NumericTag ?: ByteTag.valueOf(0)

    private fun stringTag(tag: String): StringTag =
        currentElement(tag) as? StringTag ?: StringTag.valueOf("")

    override fun decodeTaggedBoolean(tag: String): Boolean =
        numericTag(tag).asByte != 0.toByte()

    /** Numeric kinds truncate exactly like the casts they replace, so an out-of-range value is read too. */
    override fun decodeTaggedByte(tag: String): Byte = numericTag(tag).asByte

    override fun decodeTaggedShort(tag: String): Short = numericTag(tag).asShort

    override fun decodeTaggedInt(tag: String): Int = numericTag(tag).asInt

    override fun decodeTaggedLong(tag: String): Long = numericTag(tag).asLong

    override fun decodeTaggedFloat(tag: String): Float = numericTag(tag).asFloat

    override fun decodeTaggedDouble(tag: String): Double = numericTag(tag).asDouble

    override fun decodeTaggedChar(tag: String): Char = stringTag(tag).asString.singleOrNull() ?: '\u0000'

    override fun decodeTaggedString(tag: String): String = stringTag(tag).asString

    override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int {
        val name = stringTag(tag).asString
        for (i in 0 until enumDescriptor.elementsCount) {
            if (enumDescriptor.getElementName(i) == name) return i
        }

        // An unknown name would be out of bounds for the generated `values()[...]`, so take the first.
        return 0
    }

    override fun decodeTaggedNotNullMark(tag: String): Boolean =
        currentElement(tag) != null
}

private class NbtObjectDecoder(
    serializersModule: SerializersModule,
    override val value: CompoundTag
) : AbstractNbtDecoder(serializersModule) {

    private var position = 0
    private var forceNull = false

    override fun currentElement(tag: String): Tag? = value.get(tag)

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (position < descriptor.elementsCount) {
            val index = position++
            val name = elementName(descriptor, index)
            forceNull = !value.contains(name)
                    && !descriptor.isElementOptional(index)
                    && descriptor.getElementDescriptor(index).isNullable

            if (value.contains(name) || forceNull || !descriptor.isElementOptional(index)) return index
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun decodeNotNullMark(): Boolean =
        !forceNull && super.decodeNotNullMark()
}

private class NbtListDecoder(
    serializersModule: SerializersModule,
    override val value: ListTag
) : AbstractNbtDecoder(serializersModule) {

    private var position = -1

    override fun elementName(descriptor: SerialDescriptor, index: Int): String = index.toString()

    override fun currentElement(tag: String): Tag? = value.get(tag.toIntOrNull() ?: -1)

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (position < value.size - 1) {
            position++
            return position
        }
        return CompositeDecoder.DECODE_DONE
    }
}
