package com.atsuishio.superbwarfare.api.event

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.Entity
import net.minecraftforge.eventbus.api.Event
import org.jetbrains.annotations.ApiStatus

@ApiStatus.AvailableSince("0.8.9.1")
open class ClientVehicleFireEvent(
    val vehicle: VehicleEntity,
    val shooter: Entity,
    val index: Int,
    val weaponName: String? = null
) : Event()
