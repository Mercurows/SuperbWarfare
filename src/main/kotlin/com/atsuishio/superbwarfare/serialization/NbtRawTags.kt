package com.atsuishio.superbwarfare.serialization

import net.minecraft.nbt.Tag

/**
 * Escape hatch for *structured* NBT serializers that must read or write a native NBT tag which
 * kotlinx's data model cannot express — an `IntArrayTag` UUID, a `LongArrayTag`, a byte array, ...
 *
 * Implemented by the NBT format in `NbtSerialization`. A structured serializer tests for these
 * interfaces and falls back to a portable representation (usually a string) when the format does not
 * expose raw tags, so the very same serializer also works for JSON. See
 * `com.atsuishio.superbwarfare.serialization.structured` for the serializers built on top.
 *
 * Both methods consume exactly one pending element name, mirroring what the built-in primitive
 * encoders do, so the `TaggedEncoder`/`TaggedDecoder` tag stacks stay balanced.
 */
interface NbtRawTagEncoder {
    /** Writes [tag] for the element currently being encoded. */
    fun writeRawTag(tag: Tag)
}

/** Reader counterpart of [NbtRawTagEncoder]. */
interface NbtRawTagDecoder {
    /** Reads the raw tag of the element currently being decoded, or `null` when it is absent. */
    fun readRawTag(): Tag?
}
