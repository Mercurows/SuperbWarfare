// 自动生成文件，请勿手动更改

package com.atsuishio.superbwarfare.client.particle

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder

val CannonMuzzleFlareOption.Companion.CODEC: MapCodec<CannonMuzzleFlareOption> get() = RecordCodecBuilder.mapCodec { builder ->
    builder.group(
        com.mojang.serialization.Codec.INT.fieldOf("color").forGetter { it.color },
        com.mojang.serialization.Codec.INT.fieldOf("life").forGetter { it.life },
        com.mojang.serialization.Codec.FLOAT.fieldOf("fade").forGetter { it.fade },
        com.mojang.serialization.Codec.INT.fieldOf("animationSpeed").forGetter { it.animationSpeed },
        com.mojang.serialization.Codec.FLOAT.fieldOf("sizeAdd").forGetter { it.sizeAdd }
    ).apply(builder, ::CannonMuzzleFlareOption)
}
