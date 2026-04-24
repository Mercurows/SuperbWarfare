package com.atsuishio.superbwarfare.api.event

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.ShootParameters
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraftforge.eventbus.api.Event
import org.jetbrains.annotations.ApiStatus

@ApiStatus.AvailableSince("0.8.9")
open class ShootEvent private constructor(val parameters: ShootParameters) : Event() {
    val shooter: Entity? = parameters.shooter
    val level: ServerLevel = parameters.level
    val data: GunData = parameters.data
    val spread: Double = parameters.spread
    val zoom: Boolean = parameters.zoom

    class Pre(parameters: ShootParameters) : ShootEvent(parameters)

    class Post(parameters: ShootParameters) : ShootEvent(parameters)
}
