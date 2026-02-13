@file:JvmName("JsonUtil")

package com.atsuishio.superbwarfare.tools

import com.google.gson.Gson
import kotlinx.serialization.json.*

typealias GsonElement = com.google.gson.JsonElement
typealias GsonObject = com.google.gson.JsonObject
typealias GsonArray = com.google.gson.JsonArray
typealias GsonPrimitive = com.google.gson.JsonPrimitive
typealias GsonNull = com.google.gson.JsonNull

fun convertKxJsonToGson(kxJson: JsonElement): GsonElement {
    return when (kxJson) {
        is JsonNull -> GsonNull.INSTANCE
        is JsonObject -> {
            val gsonObj = GsonObject()
            kxJson.forEach { (key, value) ->
                gsonObj.add(key, convertKxJsonToGson(value))
            }
            gsonObj
        }

        is JsonArray -> {
            val gsonArray = GsonArray()
            kxJson.forEach { gsonArray.add(convertKxJsonToGson(it)) }
            gsonArray
        }

        is JsonPrimitive -> {
            when {
                kxJson.isString -> GsonPrimitive(kxJson.content)
                kxJson.booleanOrNull != null -> GsonPrimitive(kxJson.boolean)
                kxJson.intOrNull != null -> GsonPrimitive(kxJson.int)
                kxJson.longOrNull != null -> GsonPrimitive(kxJson.long)
                kxJson.doubleOrNull != null -> GsonPrimitive(kxJson.double)
                kxJson.floatOrNull != null -> GsonPrimitive(kxJson.float)
                else -> GsonPrimitive(kxJson.content) // fallback
            }
        }
    }
}

fun JsonElement.toGson() = convertKxJsonToGson(this)

fun convertGsonToKxJson(gson: GsonElement): JsonElement {
    val str = Gson().toJson(gson)
    return Json.parseToJsonElement(str)
}

fun GsonElement.toKxJson() = convertGsonToKxJson(this)
