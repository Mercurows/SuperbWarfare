package com.atsuishio.superbwarfare.api.event

import com.google.gson.JsonParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import net.minecraftforge.eventbus.api.Cancelable
import net.minecraftforge.eventbus.api.Event
import org.jetbrains.annotations.ApiStatus

typealias GsonObject = com.google.gson.JsonObject

@Cancelable
@ApiStatus.AvailableSince("0.8.9")
open class LoadingJsonEvent(
    val id: String,
    var jsonStr: String
) : Event() {
    val asGsonObject: GsonObject
        get() = JsonParser.parseString(jsonStr).asJsonObject

    val asJsonObject: JsonObject
        get() = Json.parseToJsonElement(jsonStr).jsonObject
}