package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.receive.DistantVehiclesMessage
import com.atsuishio.superbwarfare.network.message.receive.VehicleSnapshot
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.ServerTickEvent

/**
 * Раз в distant_vehicle_sync_interval тиков шлёт каждому игроку авторитетный
 * список техники в радиусе distant_vehicle_sync_radius. Дедупликация с
 * ванильным трекингом — на клиенте (см. DistantVehicleRenderer).
 */
@EventBusSubscriber
object DistantVehicleTracker {

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        val radius = VehicleConfig.DISTANT_VEHICLE_SYNC_RADIUS.get()
        if (radius <= 0) return
        val interval = VehicleConfig.DISTANT_VEHICLE_SYNC_INTERVAL.get()
        val server = event.server
        if (server.tickCount % interval != 0) return

        val radiusSq = radius.toDouble() * radius

        for (level in server.allLevels) {
            val players = level.players()
            if (players.isEmpty()) continue

            val vehicles = level.getAllEntities().filterIsInstance<VehicleEntity>()

            for (player in players) {
                val snapshots = vehicles.asSequence()
                    .filter { vehicle ->
                        val dx = vehicle.x - player.x
                        val dz = vehicle.z - player.z
                        dx * dx + dz * dz <= radiusSq
                    }
                    .map { vehicle ->
                        VehicleSnapshot(
                            entityId = vehicle.id,
                            type = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.type).toString(),
                            x = vehicle.x,
                            y = vehicle.y,
                            z = vehicle.z,
                            yaw = vehicle.yRot,
                            pitch = vehicle.xRot,
                            turretYRot = vehicle.turretYRot,
                            turretXRot = vehicle.turretXRot,
                            skinId = vehicle.skinId,
                        )
                    }
                    .toList()

                // Пустой список тоже шлём: он авторитетно чистит призраков на клиенте
                sendPacketTo(player, DistantVehiclesMessage(interval, snapshots))
            }
        }
    }
}
