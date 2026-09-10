package com.atsuishio.superbwarfare.serialization.structured

import com.atsuishio.superbwarfare.serialization.NbtRawTagDecoder
import com.atsuishio.superbwarfare.serialization.NbtRawTagEncoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.StringTag
import java.util.*

/**
 * Serializers for *structured* formats — NBT and JSON — as opposed to the streaming ones in
 * `serialization.kserializer`, which are written for `FriendlyByteBuf`-style output (a byte + a byte
 * array, two raw longs, ...) and therefore do not survive a structured round trip.
 *
 * The pattern for a member of this set:
 *
 *  1. encode the format-native representation when the encoder exposes it (here: an `IntArrayTag`, the
 *     native NBT UUID, via [NbtRawTagEncoder]);
 *  2. fall back to a portable representation otherwise (here: the canonical string), so the same
 *     serializer also works for JSON and any other structured format.
 *
 * A `typealias` plus a `@Serializable(with = ...)` annotation keeps field declarations terse:
 * `@SerialName("UUID") val uuid: StructuredUUID? = null`.
 */
typealias StructuredUUID = @Serializable(StructuredUUIDSerializer::class) UUID

/**
 * UUID as native NBT ([NbtUtils.createUUID], an `IntArrayTag` of four ints — the same representation
 * `CompoundTag.putUUID`/`getUUID` use) and as the canonical string for other structured formats.
 *
 * Using the native form matters for compatibility: gun tags written before this serializer existed
 * store `UUID` as an `IntArrayTag`, and it is also `CompoundTag`'s own representation, so nothing has
 * to migrate.
 */
object StructuredUUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("superbwarfare:structured.UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        val nbt = encoder as? NbtRawTagEncoder
        if (nbt != null) {
            nbt.writeRawTag(NbtUtils.createUUID(value))
            return
        }

        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        val nbt = decoder as? NbtRawTagDecoder
        if (nbt != null) {
            // The raw tag is consumed here, so the portable form has to be read straight out of it as
            // well — asking the decoder for a string afterwards would pop the tag stack twice.
            val raw = nbt.readRawTag()

            // Check the type first: NbtUtils.loadUUID throws on anything that is not an IntArrayTag.
            if (raw is IntArrayTag) return NbtUtils.loadUUID(raw)
            if (raw is StringTag) return UUID.fromString(raw.asString)

            throw SerializationException("Expected an NBT UUID (IntArrayTag) or a string, got ${raw?.type?.name}")
        }

        return UUID.fromString(decoder.decodeString())
    }
}
