package com.atsuishio.superbwarfare.api.event

import com.google.gson.JsonParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import net.neoforged.bus.api.Event
import net.neoforged.bus.api.ICancellableEvent
import org.jetbrains.annotations.ApiStatus

typealias GsonObject = com.google.gson.JsonObject

@ApiStatus.AvailableSince("0.8.9")
open class LoadingJsonEvent(
    val id: String,
    var jsonStr: String
) : Event(), ICancellableEvent {
    val asGsonObject: GsonObject
        get() = JsonParser.parseString(jsonStr).asJsonObject

    val asJsonObject: JsonObject
        get() = Json.parseToJsonElement(jsonStr).jsonObject
}