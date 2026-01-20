package com.atsuishio.superbwarfare.world.phys

import net.minecraft.world.phys.EntityHitResult

class ExtendedEntityRayTraceResult(result: EntityResult) : EntityHitResult(result.entity, result.hitVec) {
    @get:JvmName("isHeadshot")
    val headshot: Boolean = result.headshot

    @get:JvmName("isLegShot")
    val legShot: Boolean = result.legShot
}