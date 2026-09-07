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
 * Only object-shaped roots are supported, which matches NeoForge data attachments and
 * codec-based registries. [serializersModule] is forwarded so that `@Contextual` fields can
 * resolve custom serializers.
 */
fun <T> encodeToCompoundTag(
    serializer: SerializationStrategy<T>,
    value: T,
    serializersModule: SerializersModule = SerializersModule {}
): CompoundTag {
    lateinit var result: CompoundTag
    NbtObjectEncoder(serializersModule) { tag ->
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
    private val nodeConsumer: (Tag) -> Unit
) : NamedValueEncoder() {

    final override fun composeName(parentName: String, childName: String): String = childName

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
            StructureKind.LIST -> NbtListEncoder(serializersModule, consumer)
            StructureKind.MAP -> throw SerializationException(
                "NBT serialization does not support maps (${descriptor.serialName})"
            )

            is PolymorphicKind -> throw SerializationException(
                "NBT serialization does not support polymorphic types (${descriptor.serialName})"
            )

            else -> NbtObjectEncoder(serializersModule, consumer)
        }
    }

    override fun endEncode(descriptor: SerialDescriptor) {
        nodeConsumer(getCurrent())
    }
}

private class NbtObjectEncoder(
    serializersModule: SerializersModule,
    nodeConsumer: (Tag) -> Unit
) : AbstractNbtEncoder(serializersModule, nodeConsumer) {

    private val content = CompoundTag()

    override fun putElement(key: String, element: Tag) {
        content.put(key, element)
    }

    override fun getCurrent(): Tag = content
}

private class NbtListEncoder(
    serializersModule: SerializersModule,
    nodeConsumer: (Tag) -> Unit
) : AbstractNbtEncoder(serializersModule, nodeConsumer) {

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
) : NamedValueDecoder() {

    protected abstract val value: Tag

    protected abstract fun currentElement(tag: String): Tag?

    final override fun composeName(parentName: String, childName: String): String = childName

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val current = currentTagOrNull?.let { currentElement(it) } ?: value

        return when (descriptor.kind) {
            StructureKind.LIST -> NbtListDecoder(
                serializersModule,
                current as? ListTag
                    ?: throw SerializationException(
                        "Expected ListTag for ${descriptor.serialName}, got ${current.type.name}"
                    )
            )

            StructureKind.MAP -> throw SerializationException(
                "NBT serialization does not support maps (${descriptor.serialName})"
            )

            is PolymorphicKind -> throw SerializationException(
                "NBT serialization does not support polymorphic types (${descriptor.serialName})"
            )

            else -> NbtObjectDecoder(
                serializersModule,
                current as? CompoundTag
                    ?: throw SerializationException(
                        "Expected CompoundTag for ${descriptor.serialName}, got ${current.type.name}"
                    )
            )
        }
    }

    private fun numericTag(tag: String): NumericTag =
        currentElement(tag) as? NumericTag
            ?: throw SerializationException(
                "Expected a numeric NBT tag at '$tag', got ${currentElement(tag)?.type?.name ?: "nothing"}"
            )

    private fun stringTag(tag: String): StringTag =
        currentElement(tag) as? StringTag
            ?: throw SerializationException(
                "Expected a StringTag at '$tag', got ${currentElement(tag)?.type?.name ?: "nothing"}"
            )

    private fun byteInRange(value: Long, type: String, tag: String): Byte {
        if (value !in Byte.MIN_VALUE..Byte.MAX_VALUE) {
            throw SerializationException("Value $value at '$tag' is out of $type range")
        }
        return value.toByte()
    }

    private fun shortInRange(value: Long, type: String, tag: String): Short {
        if (value !in Short.MIN_VALUE..Short.MAX_VALUE) {
            throw SerializationException("Value $value at '$tag' is out of $type range")
        }
        return value.toShort()
    }

    private fun intInRange(value: Long, type: String, tag: String): Int {
        if (value !in Int.MIN_VALUE..Int.MAX_VALUE) {
            throw SerializationException("Value $value at '$tag' is out of $type range")
        }
        return value.toInt()
    }

    override fun decodeTaggedBoolean(tag: String): Boolean =
        numericTag(tag).asByte != 0.toByte()

    override fun decodeTaggedByte(tag: String): Byte =
        byteInRange(numericTag(tag).asLong, "byte", tag)

    override fun decodeTaggedShort(tag: String): Short =
        shortInRange(numericTag(tag).asLong, "short", tag)

    override fun decodeTaggedInt(tag: String): Int =
        intInRange(numericTag(tag).asLong, "int", tag)

    override fun decodeTaggedLong(tag: String): Long = numericTag(tag).asLong

    override fun decodeTaggedFloat(tag: String): Float = numericTag(tag).asFloat

    override fun decodeTaggedDouble(tag: String): Double = numericTag(tag).asDouble

    override fun decodeTaggedChar(tag: String): Char {
        val content = stringTag(tag).asString
        if (content.length != 1) {
            throw SerializationException("Expected single-char StringTag at '$tag', got \"$content\"")
        }
        return content[0]
    }

    override fun decodeTaggedString(tag: String): String = stringTag(tag).asString

    override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int {
        val name = stringTag(tag).asString
        for (i in 0 until enumDescriptor.elementsCount) {
            if (enumDescriptor.getElementName(i) == name) return i
        }
        throw SerializationException(
            "Enum ${enumDescriptor.serialName} has no element '$name' at '$tag'"
        )
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

            if (value.contains(name) || forceNull) return index
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

    override fun currentElement(tag: String): Tag? =
        value[tag.toIntOrNull() ?: throw SerializationException("Invalid list index tag '$tag'")]

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (position < value.size - 1) {
            position++
            return position
        }
        return CompositeDecoder.DECODE_DONE
    }
}
