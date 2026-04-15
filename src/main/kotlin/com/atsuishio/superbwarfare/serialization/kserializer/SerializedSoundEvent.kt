package com.atsuishio.superbwarfare.serialization.kserializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import java.util.regex.Pattern

typealias SerializedSoundEvent = @Serializable(SoundEventSerializer::class) SoundEvent

object SoundEventSerializer : KSerializer<SoundEvent> {
    override val descriptor = PrimitiveSerialDescriptor("net.minecraft.sounds.SoundEvent", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SoundEvent) {
//        val str = "${value.location} ${value.getRange()}"
        encoder.encodeString(value.location.toString())
    }

    val PATTERN: Pattern =
        Pattern.compile("""^(?<location>\S+)( (?<range>-?\d*(\.(?=\d))?\d*))?$""", Pattern.CASE_INSENSITIVE)

    override fun deserialize(decoder: Decoder): SoundEvent {
        val str = decoder.decodeString().trim { it <= ' ' }
        val matcher = PATTERN.matcher(str)

        require(matcher.matches()) { "invalid SoundEvent $str!" }

        var locationGroup = matcher.group("location")
        if (!locationGroup.contains(":")) {
            locationGroup = "minecraft:$locationGroup"
        }

        val location = ResourceLocation.tryParse(locationGroup)
        requireNotNull(location) { "invalid resource location for SoundEvent $str!" }

        val rangeGroup = matcher.group("range")
        if (rangeGroup != null) {
            val range = rangeGroup.toFloatOrNull()
            requireNotNull(range) { "invalid range for SoundEvent $str!" }

            return SoundEvent.createFixedRangeEvent(location, range)
        }

        return SoundEvent.createVariableRangeEvent(location)
    }
}