// 自动生成文件，请勿手动更改

package com.atsuishio.superbwarfare.client.particle

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder

val CustomCloudOption.Companion.CODEC: MapCodec<CustomCloudOption> get() = RecordCodecBuilder.mapCodec { builder ->
    builder.group(
        com.mojang.serialization.Codec.INT.fieldOf("color").forGetter { it.color },
        com.mojang.serialization.Codec.INT.fieldOf("life").forGetter { it.life },
        com.mojang.serialization.Codec.FLOAT.fieldOf("size").forGetter { it.size },
        com.mojang.serialization.Codec.FLOAT.fieldOf("gravity").forGetter { it.gravity },
        com.mojang.serialization.Codec.BOOL.fieldOf("cooldown").forGetter { it.cooldown },
        com.mojang.serialization.Codec.BOOL.fieldOf("light").forGetter { it.light }
    ).apply(builder, ::CustomCloudOption)
}
