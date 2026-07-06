package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.projectile.AerialBombEntity
import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity
import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity
import com.atsuishio.superbwarfare.entity.projectile.MelonBombEntity
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketStandardEntity
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketTBGEntity
import com.atsuishio.superbwarfare.entity.projectile.SmallRocketEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.receive.DistantVehiclesMessage
import com.atsuishio.superbwarfare.network.message.receive.ProjectileSnapshot
import com.atsuishio.superbwarfare.network.message.receive.VehicleSnapshot
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.ServerTickEvent

/**
 * Раз в distant_vehicle_sync_interval тиков шлёт каждому игроку авторитетный
 * список техники и крупных снарядов в радиусе distant_vehicle_sync_radius.
 * Дедупликация с ванильным трекингом — на клиенте (см. DistantVehicleRenderer).
 */
@EventBusSubscriber
object DistantVehicleTracker {

    // Только визуально заметные издалека снаряды: артиллерия, РПГ/ракеты,
    // все ПТУР/ЗРК/крылатые (MissileProjectile) и авиабомбы (AerialBombEntity).
    // Пули и гранаты не синхронизируем.
    private fun isDistantSyncProjectile(entity: Entity): Boolean =
        entity is MissileProjectile || entity is AerialBombEntity || entity is MelonBombEntity ||
            entity is MortarShellEntity || entity is CannonShellEntity ||
            entity is RpgRocketStandardEntity || entity is RpgRocketTBGEntity ||
            entity is SmallRocketEntity || entity is MediumRocketEntity

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

            val vehicles = ArrayList<VehicleEntity>()
            val projectiles = ArrayList<Entity>()
            for (entity in level.getAllEntities()) {
                when {
                    entity is VehicleEntity -> vehicles += entity
                    isDistantSyncProjectile(entity) -> projectiles += entity
                }
            }

            // Реанимация зависших снарядов: снаряд, влетевший в ещё не дотикавший
            // до entity-ticking чанк, перестаёт тикать и сам себе чанки больше не
            // грузит — подкидываем тикет извне, пока он не оживёт
            if (ProjectileConfig.PROJECTILE_CHUNK_LOADING.get()) {
                for (projectile in projectiles) {
                    if (projectile is FastThrowableProjectile &&
                        !level.isPositionEntityTicking(projectile.blockPosition())
                    ) {
                        projectile.keepChunkLoaded(projectile.position())
                    }
                }
            }

            for (player in players) {
                fun inRadius(entity: Entity): Boolean {
                    val dx = entity.x - player.x
                    val dz = entity.z - player.z
                    return dx * dx + dz * dz <= radiusSq
                }

                val vehicleSnapshots = vehicles.asSequence()
                    .filter(::inRadius)
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

                val projectileSnapshots = projectiles.asSequence()
                    .filter(::inRadius)
                    .map { projectile ->
                        val motion = projectile.deltaMovement
                        ProjectileSnapshot(
                            entityId = projectile.id,
                            type = BuiltInRegistries.ENTITY_TYPE.getKey(projectile.type).toString(),
                            x = projectile.x,
                            y = projectile.y,
                            z = projectile.z,
                            vx = motion.x,
                            vy = motion.y,
                            vz = motion.z,
                            gravity = if (projectile.isNoGravity) 0f
                            else (projectile as? FastThrowableProjectile)?.getCustomGravity() ?: 0f,
                        )
                    }
                    .toList()

                // Пустой список тоже шлём: он авторитетно чистит призраков на клиенте
                sendPacketTo(player, DistantVehiclesMessage(interval, vehicleSnapshots, projectileSnapshots))
            }
        }
    }
}
