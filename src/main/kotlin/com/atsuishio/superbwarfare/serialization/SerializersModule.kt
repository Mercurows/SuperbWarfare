package com.atsuishio.superbwarfare.serialization

import com.atsuishio.superbwarfare.data.ModColorSerializer
import com.atsuishio.superbwarfare.data.vehicle.subdata.CollisionLevel
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
    contextual(Vec2Serializer)
    contextual(CollisionLevel.LimitSerializer)
    contextual(ModColorSerializer)
}