package com.atsuishio.superbwarfare.tools

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Derives a Mojang [MapCodec] from a kotlinx.serialization [KSerializer].
 *
 * Encoding: serializer -> kotlinx [JsonObject] -> Gson (JsonOps' element type) -> target [com.mojang.serialization.DynamicOps].
 * Decoding is the reverse. Because JsonOps is used purely as an intermediate
 * representation, the resulting codec works with any DynamicOps (JsonOps, NbtOps, ...).
 */
private val SERIALIZER_JSON = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun <T : Any> serializerToMapCodec(serializer: KSerializer<T>): MapCodec<T> {
    val encoder = object : Encoder<T> {
        override fun <U> encode(input: T, ops: DynamicOps<U>, prefix: U): DataResult<U> {
            val gson = SERIALIZER_JSON.encodeToJsonElement(serializer, input).toGson()
            if (gson !is GsonObject) {
                return DataResult.error { "serializerToMapCodec: expected JSON object, got $gson" }
            }

            val map = gson.entrySet().associate { (key, value) ->
                ops.createString(key) to JsonOps.INSTANCE.convertTo(ops, value)
            }
            return ops.mergeToMap(prefix, map)
        }
    }

    val decoder = object : Decoder<T> {
        override fun <U> decode(ops: DynamicOps<U>, input: U): DataResult<Pair<T, U>> {
            return ops.getMap(input).flatMap { mapLike ->
                val gson = GsonObject()
                mapLike.entries().forEach { entry ->
                    val key = entry.first
                    val value = entry.second
                    val gsonKey = ops.convertTo(JsonOps.INSTANCE, key)
                    if (gsonKey.isJsonPrimitive && gsonKey.asJsonPrimitive.isString) {
                        gson.add(gsonKey.asString, ops.convertTo(JsonOps.INSTANCE, value))
                    }
                }
                try {
                    DataResult.success(
                        Pair(SERIALIZER_JSON.decodeFromJsonElement(serializer, gson.toKxJson()), ops.empty())
                    )
                } catch (e: Exception) {
                    DataResult.error { "serializerToMapCodec: ${e.message}" }
                }
            }
        }
    }

    return MapCodec.assumeMapUnsafe(Codec.of(encoder, decoder))
}
