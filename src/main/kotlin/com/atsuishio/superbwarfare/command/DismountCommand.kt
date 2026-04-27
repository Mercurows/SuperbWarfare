package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity

val DISMOUNT_COMMAND = buildCommand("dismount") {
    requirePermission(2)

    entityArg("vehicle") {
        execute {
            val res = dismount(entity)
            if (res.first) success { res.second } else fail { res.second }
            return@execute 0
        }

        "all" {
            execute {
                val res = dismount(entity)
                if (res.first) success { res.second } else fail { res.second }
                return@execute 0
            }
        }

        intArg("seatIndex", min = 1) {
            execute {
                val res = dismount(entity, intArg)
                if (res.first) success { res.second } else fail { res.second }
                return@execute 0
            }
        }
    }
}

private fun dismount(entity: Entity, index: Int = 0): Pair<Boolean, Component> {
    val vehicle = entity as? VehicleEntity
        ?: return false to Component.translatable("commands.superbwarfare.dismount.fail.vehicle")
    if (index == 0) {
        vehicle.passengers.forEach { it.stopRiding() }
        return true to Component.translatable("commands.superbwarfare.dismount.success.all", entity.displayName)
    }

    val seatSize = vehicle.computed().seats().size
    if (index - 1 !in 0..<seatSize) return false to Component.translatable("commands.superbwarfare.dismount.fail.index")

    vehicle.getOrderedPassengers().getOrNull(index - 1)?.stopRiding()
    return true to Component.translatable("commands.superbwarfare.dismount.success.single", entity.displayName, index)
}