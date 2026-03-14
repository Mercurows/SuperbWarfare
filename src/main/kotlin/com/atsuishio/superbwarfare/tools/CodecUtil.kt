package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.network.decodeFrom
import com.atsuishio.superbwarfare.network.encodeTo
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import kotlin.reflect.KProperty1

inline fun <reified T : Any> createStreamCodec(): StreamCodec<FriendlyByteBuf, T> {
    val instance = T::class.objectInstance

    val codec: StreamCodec<FriendlyByteBuf, T> = if (instance != null) {
        StreamCodec.unit(instance)
    } else {
        StreamCodec.of(
            { buf, value -> encodeTo(buf, value) },
            { buf -> decodeFrom(buf) },
        )
    }

    return codec
}

// TODO 能不能彻底干掉MapCodec全自动生成

fun <O> KProperty1<O, Int>.asCodecField(name: String? = null) = createCodecField(this, name)
fun <O> createCodecField(prop: KProperty1<O, Int>, name: String? = null): RecordCodecBuilder<O, Int> {
    return Codec.INT.fieldOf(name ?: prop.name).forGetter { prop.get(it) }
}

fun <O> KProperty1<O, Float>.asCodecField(name: String? = null) = createCodecField(this, name)
fun <O> createCodecField(prop: KProperty1<O, Float>, name: String? = null): RecordCodecBuilder<O, Float> {
    return Codec.FLOAT.fieldOf(name ?: prop.name).forGetter { prop.get(it) }
}

fun <O> KProperty1<O, Boolean>.asCodecField(name: String? = null) = createCodecField(this, name)
fun <O> createCodecField(prop: KProperty1<O, Boolean>, name: String? = null): RecordCodecBuilder<O, Boolean> {
    return Codec.BOOL.fieldOf(name ?: prop.name).forGetter { prop.get(it) }
}