package com.atsuishio.superbwarfare.api.event

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.Event
import org.jetbrains.annotations.ApiStatus

@ApiStatus.AvailableSince("0.8.9.1")
class ClientVehicleFireEvent(val entity: VehicleEntity, val shooter: Entity) : Event()