// 自动生成文件，请勿手动更改

package com.atsuishio.superbwarfare.client.particle

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder

val CustomSmokeOption.Companion.CODEC: MapCodec<CustomSmokeOption> get() = RecordCodecBuilder.mapCodec { builder ->
    builder.group(
        com.mojang.serialization.Codec.FLOAT.fieldOf("red").forGetter { it.red },
        com.mojang.serialization.Codec.FLOAT.fieldOf("green").forGetter { it.green },
        com.mojang.serialization.Codec.FLOAT.fieldOf("blue").forGetter { it.blue }
    ).apply(builder, ::CustomSmokeOption)
}
