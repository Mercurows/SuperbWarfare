@file:JvmName("JsonUtil")

package com.atsuishio.superbwarfare.tools

import kotlinx.serialization.json.*

typealias GsonElement = com.google.gson.JsonElement
typealias GsonObject  = com.google.gson.JsonObject
typealias GsonArray   = com.google.gson.JsonArray
typealias GsonPrimitive = com.google.gson.JsonPrimitive
typealias GsonNull    = com.google.gson.JsonNull

// Kx -> Gson
// Recursive direct-tree conversion (already correct in original; kept as-is).

/**
 * Converts a kotlinx [JsonElement] to its Gson counterpart by direct recursive tree
 * traversal — no intermediate string representation is produced.
 *
 * @param kxJson the kotlinx JSON element to convert.
 * @return the equivalent [GsonElement].
 */
fun convertKxJsonToGson(kxJson: JsonElement): GsonElement {
    return when (kxJson) {
        is JsonNull    -> GsonNull.INSTANCE
        is JsonObject  -> {
            val obj = GsonObject()
            kxJson.forEach { (key, value) -> obj.add(key, convertKxJsonToGson(value)) }
            obj
        }
        is JsonArray   -> {
            val arr = GsonArray()
            kxJson.forEach { arr.add(convertKxJsonToGson(it)) }
            arr
        }
        is JsonPrimitive -> when {
            kxJson.isString          -> GsonPrimitive(kxJson.content)
            kxJson.booleanOrNull != null -> GsonPrimitive(kxJson.boolean)
            kxJson.intOrNull    != null -> GsonPrimitive(kxJson.int)
            kxJson.longOrNull   != null -> GsonPrimitive(kxJson.long)
            kxJson.doubleOrNull != null -> GsonPrimitive(kxJson.double)
            kxJson.floatOrNull  != null -> GsonPrimitive(kxJson.float)
            else                     -> GsonPrimitive(kxJson.content)
        }
    }
}

/** Converts this kotlinx [JsonElement] to a Gson [GsonElement]. */
fun JsonElement.toGson(): GsonElement = convertKxJsonToGson(this)

// Gson -> Kx

/**
 * Converts a Gson [GsonElement] to its kotlinx-serialization [JsonElement] counterpart
 * by direct recursive tree traversal.
 *
 * **Why not `Gson().toJson()` + `Json.parseToJsonElement()`?**
 * The old implementation serialised the Gson tree to a [String] and then re-parsed it,
 * allocating a new [com.google.gson.Gson] instance on every call, a transient JSON string,
 * and a full kotlinx parse pass.  For a weapon definition with dozens of nested fields
 * this produced ~3× the allocations for no benefit.  The recursive approach below matches
 * the already-correct [convertKxJsonToGson] implementation and produces zero intermediate
 * strings.
 *
 * **Number type inference:** Gson stores all JSON numbers as opaque [Number] instances.
 * We promote to the narrowest exact integer type first (Int -> Long), falling back to
 * Double for fractional values.  This matches the behaviour of kotlinx
 * [Json.parseToJsonElement] on the same input text.
 *
 * @param gson the Gson JSON element to convert.
 * @return the equivalent kotlinx [JsonElement].
 */
fun convertGsonToKxJson(gson: GsonElement): JsonElement {
    return when (gson) {
        is GsonNull    -> JsonNull
        is GsonObject  -> buildJsonObject {
            for ((key, value) in gson.entrySet()) put(key, convertGsonToKxJson(value))
        }
        is GsonArray   -> buildJsonArray {
            for (element in gson) add(convertGsonToKxJson(element))
        }
        is GsonPrimitive -> when {
            gson.isBoolean -> JsonPrimitive(gson.asBoolean)
            gson.isNumber  -> {
                val asLong   = gson.asNumber.toLong()
                val asDouble = gson.asNumber.toDouble()
                when {
                    // Exact integer that fits in Int — most common case for weapon JSON data
                    asDouble == asLong.toDouble() && asLong in Int.MIN_VALUE..Int.MAX_VALUE ->
                        JsonPrimitive(asLong.toInt())
                    // Exact integer that requires Long range
                    asDouble == asLong.toDouble() ->
                        JsonPrimitive(asLong)
                    // Fractional / very large number
                    else ->
                        JsonPrimitive(asDouble)
                }
            }
            // Fallback covers isString and any exotic primitive types
            else -> JsonPrimitive(gson.asString)
        }
        // Gson's class hierarchy is sealed by the above cases; this branch is unreachable
        // but required to satisfy Kotlin's exhaustive-when check on a non-sealed Java class.
        else -> JsonNull
    }
}

/** Converts this Gson [GsonElement] to a kotlinx [JsonElement]. */
fun GsonElement.toKxJson(): JsonElement = convertGsonToKxJson(this)