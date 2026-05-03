package com.atsuishio.superbwarfare.item.ammo

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

@JvmRecord
data class AmmoBoxInfo(val type: String, val isDrop: Boolean) {
    companion object {
        val CODEC: Codec<AmmoBoxInfo> =
            RecordCodecBuilder.create {
                it.group(
                    Codec.STRING.fieldOf("type").forGetter(AmmoBoxInfo::type),
                    Codec.BOOL.fieldOf("is_drop").forGetter(AmmoBoxInfo::isDrop)
                ).apply(it, ::AmmoBoxInfo)
            }
    }
}