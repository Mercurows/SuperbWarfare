package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.SeekType
import com.atsuishio.superbwarfare.entity.projectile.AerialBombEntity
import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity
import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity
import com.atsuishio.superbwarfare.entity.projectile.MelonBombEntity
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketStandardEntity
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketTBGEntity
import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity
import com.atsuishio.superbwarfare.entity.projectile.SmallRocketEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.receive.DistantVehiclesMessage
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage
import com.atsuishio.superbwarfare.network.message.receive.ProjectileSnapshot
import com.atsuishio.superbwarfare.network.message.receive.VehicleSnapshot
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.VectorTool
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.neoforged.bus.api.SubscribeEvent
import java.util.UUID
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
            entity is SmallCannonShellEntity ||
            entity is RpgRocketStandardEntity || entity is RpgRocketTBGEntity ||
            entity is SmallRocketEntity || entity is MediumRocketEntity

    // Последний известный чанк каждого синхронизируемого снаряда: быстрый снаряд
    // обгоняет генерацию и выгружается ВМЕСТЕ с чанком — из getAllEntities он
    // пропадает, и обычный реаниматор его не видит. Спасатель тикетит последний
    // чанк пропавшего, чанк грузится, снаряд оживает и летит дальше
    private class LastSeen(val dim: ResourceKey<Level>, val chunkPos: ChunkPos, val tick: Int)

    private val lastSeenProjectiles = HashMap<UUID, LastSeen>()
    private const val RESCUE_TIMEOUT_TICKS = 200

    // Сколько тиков подряд снаряд ведётся баллистической симуляцией
    private val simulatedTicks = HashMap<UUID, Int>()
    private const val MAX_SIMULATED_TICKS = 600

    private fun simulateStalledProjectiles(server: net.minecraft.server.MinecraftServer) {
        for (level in server.allLevels) {
            for (entity in level.getAllEntities()) {
                if (entity !is FastThrowableProjectile || !isDistantSyncProjectile(entity)) continue
                if (level.isPositionEntityTicking(entity.blockPosition())) {
                    simulatedTicks.remove(entity.uuid)
                    continue
                }

                // Ограничитель: снаряд, слишком долго летящий по несуществующим
                // чанкам (например, над бесконечным океаном), убираем — иначе он
                // вечно тянул бы за собой генерацию мира
                val ticks = (simulatedTicks[entity.uuid] ?: 0) + 1
                if (ticks > MAX_SIMULATED_TICKS || entity.y < level.minBuildHeight - 64) {
                    simulatedTicks.remove(entity.uuid)
                    entity.discard()
                    continue
                }
                simulatedTicks[entity.uuid] = ticks

                val motion = entity.deltaMovement
                entity.setPos(entity.x + motion.x, entity.y + motion.y, entity.z + motion.z)
                if (!entity.isNoGravity) {
                    entity.setDeltaMovement(motion.subtract(0.0, entity.getCustomGravity().toDouble(), 0.0))
                }
                // Держим текущий чанк загруженным (не выгрузится из getAllEntities)
                // и заказываем прогрузку по курсу — как в обычном tick()
                entity.keepChunkLoaded(entity.position())
            }
        }
        // Чистка ключей уничтоженных снарядов
        if (simulatedTicks.size > 256) {
            simulatedTicks.keys.retainAll(
                server.allLevels.flatMap { lvl -> lvl.getAllEntities().map { it.uuid } }.toSet(),
            )
        }
    }

    // «Ручной радар» для ПЗРК/ПТРК (Игла, Javelin): пока игрок целится из оружия
    // с захватом (SeekType != NONE, CanGuidedByRadar), сервер шлёт ему вражескую
    // технику в радиусе SeekRange через EntitySyncMessage — тот же канал, что у
    // радара FuMO25 и VehicleEntity.vehicleRadar, поэтому клиентский SeekTool
    // подхватывает такие цели за пределами прогруза без доработок
    private fun handheldSeekRadar(server: net.minecraft.server.MinecraftServer) {
        if (!MiscConfig.SYNC_ENTITY_OVER_RANGE.get()) return
        if (server.tickCount % MiscConfig.SYNC_ENTITY_INTERVAL.get() != 0) return

        for (level in server.allLevels) {
            for (player in level.players()) {
                val stack = player.mainHandItem
                if (stack.item !is GunItem) continue
                val data = GunData.from(stack)
                if (!data.zooming.get()) continue
                if (data.get(GunProp.SEEK_TYPE) == SeekType.NONE) continue
                if (!data.get(GunProp.CAN_GUIDED_BY_RADAR)) continue

                val seekRange = data.get(GunProp.SEEK_RANGE)
                val minTargetHeight = data.get(GunProp.MIN_TARGET_HEIGHT)
                val maxTargetHeight = data.get(GunProp.MAX_TARGET_HEIGHT)

                val hostileList = level.allEntities
                    .asSequence()
                    .mapNotNull {
                        val flag = (it is VehicleEntity || VehicleConfig.inScanList(it.type))
                                && SeekTool.NOT_IN_SMOKE.test(it)
                                && it.distanceToSqr(player) <= seekRange * seekRange
                                && SeekTool.IN_HEIGHT_RANGE.test(it, minTargetHeight, maxTargetHeight)
                                && !SeekTool.IS_FRIENDLY.test(player, it)
                                && VectorTool.checkNoClip(player.eyePosition, it.eyePosition, level)
                        if (!flag) return@mapNotNull null
                        EntitySyncMessage.SyncedEntity(
                            it.id,
                            BuiltInRegistries.ENTITY_TYPE.getKey(it.type),
                            it.position(),
                            it.deltaMovement,
                            CompoundTag().also { tag -> it.saveWithoutId(tag) }
                        )
                    }.toList()
                sendPacketTo(player, EntitySyncMessage(level.dimension().location(), hostileList, false))
            }
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        handheldSeekRadar(event.server)

        val radius = VehicleConfig.DISTANT_VEHICLE_SYNC_RADIUS.get()
        if (radius <= 0) return
        val server = event.server

        // КОРЕНЬ рваного полёта: тик снаряда привязан к готовности чанков —
        // быстрый снаряд обгоняет генерацию и летит рывками (замер/рывок).
        // Ведём его баллистически каждый тик, пока чанк не дотикал до
        // entity-ticking: без коллизий (проверять всё равно не по чему),
        // обычная физика подхватит, как только чанк догрузится
        simulateStalledProjectiles(server)

        val interval = VehicleConfig.DISTANT_VEHICLE_SYNC_INTERVAL.get()
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

            // Учёт последних чанков снарядов: отличает честную гибель (чанк
            // тикает, а снаряда нет — взорвался, клиентам шлём removed) от
            // выгрузки с недогенерированным чанком (спасаем тикетом)
            val now = server.tickCount
            val presentUuids = HashSet<UUID>(projectiles.size)
            for (projectile in projectiles) {
                presentUuids += projectile.uuid
                lastSeenProjectiles[projectile.uuid] =
                    LastSeen(level.dimension(), ChunkPos(projectile.blockPosition()), now)
            }
            val removedProjectiles = ArrayList<UUID>()
            val rescueIterator = lastSeenProjectiles.entries.iterator()
            while (rescueIterator.hasNext()) {
                val entry = rescueIterator.next()
                val seen = entry.value
                if (now - seen.tick > RESCUE_TIMEOUT_TICKS) {
                    rescueIterator.remove()
                    continue
                }
                if (seen.dim != level.dimension() || entry.key in presentUuids) continue
                if (level.isPositionEntityTicking(seen.chunkPos.getMiddleBlockPosition(0))) {
                    // Чанк тикает, а снаряда нет — честно уничтожен
                    removedProjectiles += entry.key
                    rescueIterator.remove()
                } else if (ProjectileConfig.PROJECTILE_CHUNK_LOADING.get()) {
                    level.chunkSource.addRegionTicket(
                        FastThrowableProjectile.PROJECTILE_TICKET, seen.chunkPos, 3, seen.chunkPos,
                    )
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
                            uuid = vehicle.uuid,
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
                            isWreck = vehicle.isWreck,
                            sympatheticDetonated = vehicle.sympatheticDetonated,
                        )
                    }
                    .toList()

                val projectileSnapshots = projectiles.asSequence()
                    .filter(::inRadius)
                    .map { projectile ->
                        val motion = projectile.deltaMovement
                        ProjectileSnapshot(
                            uuid = projectile.uuid,
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
                sendPacketTo(
                    player,
                    DistantVehiclesMessage(interval, vehicleSnapshots, projectileSnapshots, removedProjectiles),
                )
            }
        }
    }
}
