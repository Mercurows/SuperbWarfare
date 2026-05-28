package com.atsuishio.superbwarfare.perk.js

import com.atsuishio.superbwarfare.perk.Perk
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.ResourceLocation
import java.util.*

class PerkDescriptor(
    val type: String,
    val script: ResourceLocation?,
    val bypassArmorRate: Double,
    val damageRate: Double,
    val speedRate: Double,
    val slug: Boolean,
    val rgb: List<Int>?,
    val mobEffects: List<String>?,
    val hideParticle: Boolean,
) {
    val perkType: Perk.Type by lazy {
        when (type.lowercase(Locale.ROOT)) {
            "ammo" -> Perk.Type.AMMO
            "functional" -> Perk.Type.FUNCTIONAL
            "damage" -> Perk.Type.DAMAGE
            else -> throw IllegalArgumentException("Unknown perk type: '$type'. Valid: ammo, functional, damage")
        }
    }

    companion object {
        val CODEC: Codec<PerkDescriptor> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("Type").forGetter { it.type },
                ResourceLocation.CODEC.optionalFieldOf("Script").forGetter { it.script?.let { s -> Optional.of(s) } ?: Optional.empty() },
                Codec.DOUBLE.optionalFieldOf("BypassArmorRate", 0.0).forGetter { it.bypassArmorRate },
                Codec.DOUBLE.optionalFieldOf("DamageRate", 1.0).forGetter { it.damageRate },
                Codec.DOUBLE.optionalFieldOf("SpeedRate", 1.0).forGetter { it.speedRate },
                Codec.BOOL.optionalFieldOf("Slug", false).forGetter { it.slug },
                Codec.INT.listOf().optionalFieldOf("RGB").forGetter { it.rgb?.let { rgb -> Optional.of(rgb) } ?: Optional.empty() },
                Codec.STRING.listOf().optionalFieldOf("MobEffects").forGetter { it.mobEffects?.let { m -> Optional.of(m) } ?: Optional.empty() },
                Codec.BOOL.optionalFieldOf("HideParticle", false).forGetter { it.hideParticle },
            ).apply(instance) { type, script, bypassArmorRate, damageRate, speedRate, slug, rgb, mobEffects, hideParticle ->
                PerkDescriptor(
                    type = type,
                    script = script.orElse(null),
                    bypassArmorRate = bypassArmorRate,
                    damageRate = damageRate,
                    speedRate = speedRate,
                    slug = slug,
                    rgb = rgb.orElse(null),
                    mobEffects = mobEffects.orElse(null),
                    hideParticle = hideParticle,
                )
            }
        }
    }
}
