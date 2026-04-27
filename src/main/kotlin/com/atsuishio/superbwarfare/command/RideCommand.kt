package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

val RIDE_COMMAND = buildCommand("ride") {
    requirePermission(2)

    entityArg("passenger") passenger@{
        entityArg("vehicle") vehicle@{
            execute {
                val res = ride(getArg(this@passenger), getArg(this@vehicle))
                if (res.success) success { res.message } else fail { res.message }
                return@execute res.index
            }

            intArg("seatIndex", min = 1) {
                execute {
                    val res = ride(getArg(this@passenger), getArg(this@vehicle), intArg)
                    if (res.success) success { res.message } else fail { res.message }
                    return@execute res.index
                }

                boolArg("forceRide") {
                    execute {
                        val res = ride(
                            getArg(this@passenger),
                            getArg(this@vehicle),
                            intArg,
                            boolArg
                        )
                        if (res.success) success { res.message } else fail { res.message }
                        return@execute res.index
                    }
                }
            }
        }
    }
}

private fun ride(passenger: Entity, vehicle: Entity, seatIndex: Int = 0, forceRide: Boolean = false): RideCommandResult {
    val passenger =
        passenger as? LivingEntity ?: return RideCommandResult(
            false,
            Component.translatable("commands.superbwarfare.ride.fail.passenger"),
            0
        )
    val vehicle = vehicle as? VehicleEntity ?: return RideCommandResult(
        false,
        Component.translatable("commands.superbwarfare.ride.fail.vehicle"),
        0
    )

    val seatSize = vehicle.computed().seats().size
    val passengerCount = vehicle.passengers.size
    val index = seatIndex - 1

    if (seatSize <= passengerCount || index >= seatSize) return RideCommandResult(
        false,
        Component.translatable("commands.superbwarfare.ride.fail.full"),
        0
    )

    if (seatIndex == 0) {
        val flag = passenger.startRiding(vehicle, true)
        return if (flag) RideCommandResult(
            true,
            Component.translatable(
                "commands.superbwarfare.ride.success",
                passenger.displayName,
                vehicle.displayName,
                vehicle.getSeatIndex(passenger)
            ),
            seatIndex
        ) else RideCommandResult(
            false,
            Component.translatable("commands.superbwarfare.ride.fail.exist"),
            0
        )
    }

    if (!forceRide) {
        val existedPassenger = vehicle.getOrderedPassengers()[index]
        if (existedPassenger != null) return RideCommandResult(
            false,
            Component.translatable("commands.superbwarfare.ride.fail.exist"),
            0
        )

        passenger.startRiding(vehicle, true)
        vehicle.changeSeat(passenger, index)

        return RideCommandResult(
            true,
            Component.translatable(
                "commands.superbwarfare.ride.success",
                passenger.displayName,
                vehicle.displayName,
                seatIndex
            ),
            seatIndex
        )
    } else {
        val existedPassenger = vehicle.getOrderedPassengers()[index]
        existedPassenger?.stopRiding()

        passenger.startRiding(vehicle, true)
        vehicle.changeSeat(passenger, index)

        return RideCommandResult(
            true,
            Component.translatable(
                "commands.superbwarfare.ride.success",
                passenger.displayName,
                vehicle.displayName,
                seatIndex
            ),
            seatIndex
        )
    }
}

private data class RideCommandResult(
    val success: Boolean,
    val message: Component,
    val index: Int
)
