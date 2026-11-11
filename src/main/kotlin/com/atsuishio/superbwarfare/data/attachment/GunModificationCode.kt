package com.atsuishio.superbwarfare.data.attachment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

/** Portable configuration only. Never serialize arbitrary item NBT or registry ordinals. */
@Serializable
data class GunModificationPreset(
    @SerialName("g") val gun: String,
    @SerialName("a") val attachments: Map<String, List<GunModificationEntry>>,
    /** Export-time hint; the receiving server always checks its own rules. */
    @SerialName("m") val minimumMode: Int = 0,
)

@Serializable
data class GunModificationEntry(
    @SerialName("i") val id: String,
    @SerialName("o") val offset: Double = 0.0,
    @SerialName("r") val rotation: Double = 0.0,
    @SerialName("c") val color: String? = null,
    @SerialName("s") val skin: String = "",
    @SerialName("m") val scopeMode: Int = 0,
    @SerialName("z") val zoom: Double? = null,
)

class GunModificationException(val reason: String, vararg val details: String) : IllegalArgumentException(reason)

/** SBW1.<unpadded Base64URL(UTF-8 JSON)>. No compression wrapper or item registry table. */
object GunModificationCode {
    const val MAX_CODE_LENGTH = 16384
    const val MAX_INPUT_LENGTH = MAX_CODE_LENGTH + 257
    private const val MAX_JSON_BYTES = (MAX_CODE_LENGTH - 5) * 3 / 4
    // Protocol-owned coverage: missing slots mean empty, future slots are left untouched by SBW1.
    private val slotKeys = mapOf("Scope" to "s", "Magazine" to "m", "Muzzle" to "z", "Stock" to "t",
        "Grip" to "g", "SubWeapon" to "w", "Charm" to "c", "LowerRail" to "b", "UpperRail" to "u",
        "LeftRail" to "l", "RightRail" to "r")
    private val slotNames = slotKeys.entries.associate { (name, key) -> key to name }
    val SLOTS: List<String> = slotKeys.keys.toList()
    private val json = Json { encodeDefaults = false }
    private val idPattern = Regex("(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+")
    private val colorPattern = Regex("[0-9A-Fa-f]{6}")

    fun shortId(id: String): String = id.removePrefix("superbwarfare:")
    fun fullId(id: String): String = if (':' in id) id else "superbwarfare:$id"

    /** Localized label is for humans only; gun validation always uses the encoded registry ID. */
    fun withLabel(code: String, name: String): String = name.replace('#', '＃')
        .replace('\n', ' ').replace('\r', ' ').take(256) + "#" + code

    fun encode(preset: GunModificationPreset): String {
        validate(preset)
        val normalized = preset.copy(gun = shortId(preset.gun), attachments = preset.attachments
            .filterValues { it.isNotEmpty() }.mapKeys { (name, _) -> slotKeys.getValue(name) }.toSortedMap()
            .mapValues { (_, entries) -> entries.map {
                it.copy(id = shortId(it.id), color = it.color?.uppercase(Locale.ROOT))
            } })
        val bytes = json.encodeToString(normalized).toByteArray(Charsets.UTF_8)
        checkCode(bytes.size <= MAX_JSON_BYTES)
        val body = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val result = "SBW1.$body"
        checkCode(result.length <= MAX_CODE_LENGTH)
        return result
    }

    fun decode(code: String): GunModificationPreset {
        try {
            checkCode(code.length <= MAX_INPUT_LENGTH)
            val input = code.trim()
            val separator = input.indexOf('#')
            checkCode(separator <= 256)
            val encoded = if (separator >= 0) input.substring(separator + 1).trim() else input
            checkCode(encoded.length <= MAX_CODE_LENGTH)
            val parts = encoded.split('.')
            checkCode(parts.size == 2)
            if (parts[0] != "SBW1") throw GunModificationException("version")
            checkCode(parts[1].matches(Regex("[A-Za-z0-9_-]+")))
            val bytes = Base64.getUrlDecoder().decode(parts[1])
            checkCode(bytes.size <= MAX_JSON_BYTES)
            checkCode(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) == parts[1])
            val compact = json.decodeFromString<GunModificationPreset>(bytes.decodeToString(throwOnInvalidSequence = true))
            return compact.copy(attachments = compact.attachments.mapKeys { (key, _) ->
                slotNames[key] ?: throw GunModificationException("invalid")
            }).also(::validate)
        } catch (e: GunModificationException) {
            throw e
        } catch (_: Exception) {
            throw GunModificationException("invalid")
        }
    }

    private fun validate(preset: GunModificationPreset) {
        checkCode(validId(preset.gun) && preset.minimumMode in 0..2)
        checkCode(preset.attachments.keys.all { it in SLOTS })
        for ((slot, entries) in preset.attachments) {
            checkCode(entries.size <= if (slot == "Scope") 32 else 1)
            for (entry in entries) {
                checkCode(validId(entry.id) && entry.skin.length <= 256)
                checkCode(entry.offset.isFinite() && entry.rotation.isFinite() && entry.rotation in -180.0..180.0)
                checkCode(entry.color == null || colorPattern.matches(entry.color))
                checkCode(entry.scopeMode >= 0 && (entry.zoom == null || entry.zoom.isFinite() && entry.zoom > 0))
            }
        }
    }

    private fun validId(id: String) = id.length in 1..256 && idPattern.matches(id)
    private fun checkCode(condition: Boolean) {
        if (!condition) throw GunModificationException("invalid")
    }
}
