package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.receive.DistantVehiclesMessage
import com.atsuishio.superbwarfare.network.message.receive.ProjectileSnapshot
import com.atsuishio.superbwarfare.network.message.receive.VehicleSnapshot
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent

/**
 * Держит клиентские "призраки" дальней техники (за пределами ванильного
 * tracking range). Инстансы НЕ добавляются в ClientLevel — только рендер.
 */
@EventBusSubscriber(Dist.CLIENT)
object DistantVehicleManager {

    class Ghost(val serverId: Int, val entity: VehicleEntity) {
        var targetX = entity.x
        var targetY = entity.y
        var targetZ = entity.z
        var targetYaw = entity.yRot
        var targetPitch = entity.xRot
        var targetTurretY = entity.turretYRot
        var targetTurretX = entity.turretXRot
        var lerpSteps = 0
        var lastUpdate = 0L
        var interval = 10
    }

    class ProjectileGhost(val serverId: Int, val entity: Entity) {
        var vx = 0.0
        var vy = 0.0
        var vz = 0.0
        var gravity = 0f
        var lastUpdate = 0L
        var interval = 10
        var lastSnapshotX = Double.NaN
        var lastSnapshotY = Double.NaN
        var lastSnapshotZ = Double.NaN
        // Ошибка предсказания досыпается порциями по тикам (аналог lerpSteps),
        // жёсткий снап на каждом пакете трясёт быстрые снаряды на ±скорость
        var correctionX = 0.0
        var correctionY = 0.0
        var correctionZ = 0.0
        var correctionSteps = 0
    }

    private val ghostMap = LinkedHashMap<Int, Ghost>()
    private val projectileMap = LinkedHashMap<Int, ProjectileGhost>()
    private var cachedLevel: ClientLevel? = null
    private var tickCounter = 0L

    fun ghosts(): Collection<Ghost> = ghostMap.values

    fun projectileGhosts(): Collection<ProjectileGhost> = projectileMap.values

    fun handleMessage(msg: DistantVehiclesMessage) {
        val level = mc.level ?: return
        if (level !== cachedLevel) {
            clearAll()
            cachedLevel = level
        }

        val seen = HashSet<Int>(msg.vehicles.size)
        for (snapshot in msg.vehicles) {
            seen += snapshot.entityId
            val ghost = ghostMap[snapshot.entityId]
                ?: createGhost(level, snapshot)?.also { ghostMap[snapshot.entityId] = it }
                ?: continue

            ghost.interval = msg.interval
            ghost.lastUpdate = tickCounter
            ghost.targetX = snapshot.x
            ghost.targetY = snapshot.y
            ghost.targetZ = snapshot.z
            ghost.targetYaw = snapshot.yaw
            ghost.targetPitch = snapshot.pitch
            ghost.targetTurretY = snapshot.turretYRot
            ghost.targetTurretX = snapshot.turretXRot
            ghost.lerpSteps = msg.interval
            ghost.entity.skinId = snapshot.skinId
        }
        // Пакет авторитетный: чего нет в списке — того больше нет в радиусе
        ghostMap.keys.retainAll(seen)

        val seenProjectiles = HashSet<Int>(msg.projectiles.size)
        for (snapshot in msg.projectiles) {
            seenProjectiles += snapshot.entityId
            val ghost = projectileMap[snapshot.entityId]
                ?: createProjectileGhost(level, snapshot)?.also { projectileMap[snapshot.entityId] = it }
                ?: continue

            ghost.interval = msg.interval
            ghost.lastUpdate = tickCounter

            // Сервер не сдвинул снаряд с прошлого снапшота (чанк ещё грузится,
            // снаряд там заморожен, но deltaMovement у него ненулевой) — глушим
            // счисление, иначе призрак ездит вперёд-назад между пакетами
            val serverFrozen = !ghost.lastSnapshotX.isNaN() &&
                Math.abs(snapshot.x - ghost.lastSnapshotX) < 0.01 &&
                Math.abs(snapshot.y - ghost.lastSnapshotY) < 0.01 &&
                Math.abs(snapshot.z - ghost.lastSnapshotZ) < 0.01
            if (serverFrozen) {
                ghost.vx = 0.0
                ghost.vy = 0.0
                ghost.vz = 0.0
                ghost.gravity = 0f
            } else {
                ghost.vx = snapshot.vx
                ghost.vy = snapshot.vy
                ghost.vz = snapshot.vz
                ghost.gravity = snapshot.gravity
            }
            ghost.lastSnapshotX = snapshot.x
            ghost.lastSnapshotY = snapshot.y
            ghost.lastSnapshotZ = snapshot.z

            // Ошибка между счислением и серверной позицией: маленькую размазываем
            // по следующему интервалу, большую (телепорт/рассинхрон) — снапим
            val errorX = snapshot.x - ghost.entity.x
            val errorY = snapshot.y - ghost.entity.y
            val errorZ = snapshot.z - ghost.entity.z
            if (errorX * errorX + errorY * errorY + errorZ * errorZ > 64.0 * 64.0) {
                ghost.entity.setPos(snapshot.x, snapshot.y, snapshot.z)
                ghost.correctionSteps = 0
            } else {
                val steps = msg.interval.coerceAtLeast(1)
                ghost.correctionX = errorX / steps
                ghost.correctionY = errorY / steps
                ghost.correctionZ = errorZ / steps
                ghost.correctionSteps = steps
            }
        }
        // Снаряд пропал из списка — взорвался, убираем сразу
        projectileMap.keys.retainAll(seenProjectiles)
    }

    private fun clearAll() {
        ghostMap.clear()
        projectileMap.clear()
    }

    private fun createGhost(level: ClientLevel, snapshot: VehicleSnapshot): Ghost? {
        val typeId = ResourceLocation.tryParse(snapshot.type) ?: return null
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(typeId)) return null
        val entity = BuiltInRegistries.ENTITY_TYPE.get(typeId).create(level) as? VehicleEntity ?: return null

        entity.moveTo(snapshot.x, snapshot.y, snapshot.z, snapshot.yaw, snapshot.pitch)
        entity.yRotO = snapshot.yaw
        entity.xRotO = snapshot.pitch
        entity.turretYRot = snapshot.turretYRot
        entity.turretYRotO = snapshot.turretYRot
        entity.turretXRot = snapshot.turretXRot
        entity.turretXRotO = snapshot.turretXRot
        entity.skinId = snapshot.skinId
        return Ghost(snapshot.entityId, entity)
    }

    private fun createProjectileGhost(level: ClientLevel, snapshot: ProjectileSnapshot): ProjectileGhost? {
        val typeId = ResourceLocation.tryParse(snapshot.type) ?: return null
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(typeId)) return null
        val entity = BuiltInRegistries.ENTITY_TYPE.get(typeId).create(level) ?: return null

        entity.moveTo(snapshot.x, snapshot.y, snapshot.z, 0f, 0f)
        entity.setDeltaMovement(snapshot.vx, snapshot.vy, snapshot.vz)
        updateProjectileRotation(entity)
        entity.yRotO = entity.yRot
        entity.xRotO = entity.xRot
        return ProjectileGhost(snapshot.entityId, entity)
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val level = mc.level
        if (level == null || level !== cachedLevel) {
            clearAll()
            cachedLevel = level
            return
        }
        tickCounter++

        val iterator = ghostMap.values.iterator()
        while (iterator.hasNext()) {
            val ghost = iterator.next()
            if (tickCounter - ghost.lastUpdate > ghost.interval * 3L) {
                iterator.remove()
                continue
            }
            tickGhost(ghost)
        }

        val projectileIterator = projectileMap.values.iterator()
        while (projectileIterator.hasNext()) {
            val ghost = projectileIterator.next()
            if (tickCounter - ghost.lastUpdate > ghost.interval * 3L) {
                projectileIterator.remove()
                continue
            }
            tickProjectileGhost(ghost)
        }
    }

    private fun tickGhost(ghost: Ghost) {
        val entity = ghost.entity
        entity.xo = entity.x
        entity.yo = entity.y
        entity.zo = entity.z
        entity.yRotO = entity.yRot
        entity.xRotO = entity.xRot
        entity.turretYRotO = entity.turretYRot
        entity.turretXRotO = entity.turretXRot
        entity.tickCount++

        if (ghost.lerpSteps > 0) {
            val steps = ghost.lerpSteps.toDouble()
            entity.setPos(
                entity.x + (ghost.targetX - entity.x) / steps,
                entity.y + (ghost.targetY - entity.y) / steps,
                entity.z + (ghost.targetZ - entity.z) / steps,
            )
            entity.yRot += Mth.wrapDegrees(ghost.targetYaw - entity.yRot) / ghost.lerpSteps
            entity.xRot += (ghost.targetPitch - entity.xRot) / ghost.lerpSteps
            entity.turretYRot += Mth.wrapDegrees(ghost.targetTurretY - entity.turretYRot) / ghost.lerpSteps
            entity.turretXRot += (ghost.targetTurretX - entity.turretXRot) / ghost.lerpSteps
            ghost.lerpSteps--
        }
    }

    // Счисление пути: клиент сам ведёт снаряд по скорости и гравитации,
    // сервер лишь корректирует позицию раз в interval тиков
    private fun tickProjectileGhost(ghost: ProjectileGhost) {
        val entity = ghost.entity
        entity.xo = entity.x
        entity.yo = entity.y
        entity.zo = entity.z
        entity.yRotO = entity.yRot
        entity.xRotO = entity.xRot
        entity.tickCount++

        ghost.vy -= ghost.gravity
        var dx = ghost.vx
        var dy = ghost.vy
        var dz = ghost.vz
        if (ghost.correctionSteps > 0) {
            dx += ghost.correctionX
            dy += ghost.correctionY
            dz += ghost.correctionZ
            ghost.correctionSteps--
        }
        entity.setPos(entity.x + dx, entity.y + dy, entity.z + dz)
        entity.setDeltaMovement(ghost.vx, ghost.vy, ghost.vz)
        updateProjectileRotation(entity)
    }

    // Копия FastThrowableProjectile.updateRotation: BasicProjectileRenderer
    // ориентирует модель по xRot/yRot, у знаков та же инверсия
    private fun updateProjectileRotation(entity: Entity) {
        val motion = entity.deltaMovement
        if (motion.lengthSqr() < 1.0e-6) return
        val horizontal = motion.horizontalDistance()
        entity.xRot = lerpRotation(entity.xRotO, -(Mth.atan2(motion.y, horizontal) * (180.0 / Math.PI)).toFloat())
        entity.yRot = lerpRotation(entity.yRotO, -(Mth.atan2(motion.x, motion.z) * (180.0 / Math.PI)).toFloat())
    }

    private fun lerpRotation(currentRot: Float, targetRot: Float): Float {
        var current = currentRot
        while (targetRot - current < -180f) current -= 360f
        while (targetRot - current >= 180f) current += 360f
        return Mth.lerp(0.2f, current, targetRot)
    }

    @SubscribeEvent
    fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        clearAll()
        cachedLevel = null
    }
}
