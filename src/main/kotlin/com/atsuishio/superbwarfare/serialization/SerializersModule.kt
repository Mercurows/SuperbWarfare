package com.atsuishio.superbwarfare.serialization

import com.atsuishio.superbwarfare.serialization.kserializer.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

val serializersModule = SerializersModule {
    contextual(BlockPosSerializer)
    contextual(ResourceLocationSerializer)
    contextual(GsonObjectSerializer)
    contextual(SoundEventSerializer)
    contextual(TagSerializer)
    contextual(UUIDSerializer)
    contextual(Vec3Serializer)
    contextual(Vector3fSerializer)
}