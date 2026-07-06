package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.client.particle.CustomFlareOption
import com.atsuishio.superbwarfare.entity.projectile.Agm65Entity
import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity
import com.atsuishio.superbwarfare.entity.projectile.Ru3m14MissileEntity
import com.atsuishio.superbwarfare.entity.projectile.Ru9m100MissileEntity
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketStandardEntity
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketTBGEntity
import com.atsuishio.superbwarfare.entity.projectile.SmallRocketEntity
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
import java.util.UUID

/**
 * Держит клиентские "призраки" дальней техники (за пределами ванильного
 * tracking range). Инстансы НЕ добавляются в ClientLevel — только рендер.
 */
@EventBusSubscriber(Dist.CLIENT)
object DistantVehicleManager {

    class Ghost(var serverId: Int, val entity: VehicleEntity) {
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

    // Та же схема, что у техники: лерп к последней серверной позиции.
    // Экстраполяция по скорости давала пилу «вперёд-назад», когда снаряд
    // на сервере притормаживал на подгрузке чанков: клиент улетал вперёд,
    // а коррекция тянула назад. Лерп к цели реверсов не даёт в принципе.
    class ProjectileGhost(var serverId: Int, val entity: Entity) {
        var targetX = entity.x
        var targetY = entity.y
        var targetZ = entity.z
        var lerpSteps = 0
        var lastUpdate = 0L
        var interval = 10
        // Прошлый снапшот — для эффективной скорости по наблюдаемым позициям
        var prevSnapX = Double.NaN
        var prevSnapY = 0.0
        var prevSnapZ = 0.0
        var prevSnapTick = 0L
    }

    // Ключ — UUID энтити: сетевой entityId меняется при выгрузке/загрузке
    // на границах прогруза, и ключевание по id плодило призраков-клонов
    private val ghostMap = LinkedHashMap<UUID, Ghost>()
    private val projectileMap = LinkedHashMap<UUID, ProjectileGhost>()
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

        val seen = HashSet<UUID>(msg.vehicles.size)
        for (snapshot in msg.vehicles) {
            seen += snapshot.uuid
            val ghost = ghostMap[snapshot.uuid]
                ?: createGhost(level, snapshot)?.also { ghostMap[snapshot.uuid] = it }
                ?: continue

            ghost.serverId = snapshot.entityId
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

        for (snapshot in msg.projectiles) {
            val ghost = projectileMap[snapshot.uuid]
                ?: createProjectileGhost(level, snapshot)?.also { projectileMap[snapshot.uuid] = it }
                ?: continue

            ghost.serverId = snapshot.entityId
            ghost.interval = msg.interval
            ghost.lastUpdate = tickCounter

            val errorX = snapshot.x - ghost.entity.x
            val errorY = snapshot.y - ghost.entity.y
            val errorZ = snapshot.z - ghost.entity.z
            if (errorX * errorX + errorY * errorY + errorZ * errorZ > 192.0 * 192.0) {
                // Большой рассинхрон (телепорт) — снапим сразу
                ghost.entity.setPos(snapshot.x, snapshot.y, snapshot.z)
                ghost.lerpSteps = 0
            } else {
                // Проективная цель: снапшот отстаёт от реального времени на
                // интервал, целимся на интервал вперёд. Скорость берём НЕ из
                // snapshot.v (deltaMovement врёт: у ускоряющихся ракет и при
                // троттлинге тиков на подгрузке чанков он сильно больше
                // фактического сдвига — проекция перелетала и срабатывал SNAP
                // назад каждые ~20 тиков), а по наблюдаемым позициям снапшотов
                if (!ghost.prevSnapX.isNaN()) {
                    val dt = (tickCounter - ghost.prevSnapTick).coerceIn(1L, 60L).toDouble()
                    ghost.targetX = snapshot.x + (snapshot.x - ghost.prevSnapX) / dt * msg.interval
                    ghost.targetY = snapshot.y + (snapshot.y - ghost.prevSnapY) / dt * msg.interval
                    ghost.targetZ = snapshot.z + (snapshot.z - ghost.prevSnapZ) / dt * msg.interval
                } else {
                    ghost.targetX = snapshot.x
                    ghost.targetY = snapshot.y
                    ghost.targetZ = snapshot.z
                }
                ghost.lerpSteps = msg.interval
            }
            ghost.prevSnapX = snapshot.x
            ghost.prevSnapY = snapshot.y
            ghost.prevSnapZ = snapshot.z
            ghost.prevSnapTick = tickCounter
        }
        // Снаряды НЕ чистим по отсутствию в одном пакете: на переходах чанков
        // снаряд может на пакет выпасть из серверного списка энтити, и удаление
        // с пересозданием давало видимый скачок. Взорвавшиеся уберёт таймаут
        // (2 интервала, ~1 с) в onClientTick.
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
            tickProjectileGhost(ghost, level)
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

    private fun tickProjectileGhost(ghost: ProjectileGhost, level: ClientLevel) {
        val entity = ghost.entity
        entity.xo = entity.x
        entity.yo = entity.y
        entity.zo = entity.z
        entity.yRotO = entity.yRot
        entity.xRotO = entity.xRot
        entity.tickCount++

        // Пока снаряд ведёт ваниль (в tracking range) — призрак скрыт рендером,
        // но продолжает лерпить в фоне: с проективной целью он идёт в реальном
        // времени, и на передаче нет ни разрыва позиции, ни рывка скорости.
        // (Жёсткая приколка к реальной энтити давала протухшую цель лерпа и
        // скачок скорости сразу после отцепления.) След при ванильном рендере
        // не спавним — у настоящего снаряда работает штатный
        val vanillaVisible = level.getEntity(ghost.serverId) != null

        if (ghost.lerpSteps > 0) {
            val steps = ghost.lerpSteps.toDouble()
            val dx = (ghost.targetX - entity.x) / steps
            val dy = (ghost.targetY - entity.y) / steps
            val dz = (ghost.targetZ - entity.z) / steps
            entity.setPos(entity.x + dx, entity.y + dy, entity.z + dz)
            ghost.lerpSteps--
            // Ориентация модели — по фактическому направлению движения
            entity.setDeltaMovement(dx, dy, dz)
            updateProjectileRotation(entity)
            if (!vanillaVisible) {
                spawnGhostTrail(entity)
            }
        }
    }

    // Дымовой/огненный след призраков. Оригинальные *Trail() снаряда спавнят
    // частицы без force-флага, а ваниль отсекает такие дальше 32 блоков от
    // камеры (LevelRenderer.addParticleInternal) — поэтому спавним сами с force
    private fun spawnGhostTrail(entity: Entity) {
        if (entity.tickCount <= 2) return
        val level = entity.level() as? ClientLevel ?: return

        if (entity is CannonShellEntity) {
            spawnShellTrail(level, entity)
            return
        }
        val flareSize = when (entity) {
            is Ru3m14MissileEntity -> 1.0f
            is Ru9m100MissileEntity, is Agm65Entity, is MediumRocketEntity -> 0.75f // Kh39 наследует Agm65
            is SmallRocketEntity -> 0.25f
            is MissileProjectile, is MortarShellEntity,
            is RpgRocketStandardEntity, is RpgRocketTBGEntity -> 0.4f
            else -> return // авиабомбы и прочие — без следа, как в оригинале
        }

        val motion = entity.deltaMovement
        val speed = motion.length()
        if (speed < 1.0e-4) return
        val direction = motion.normalize()
        val startX = entity.xo
        val startY = entity.yo + entity.bbHeight / 2
        val startZ = entity.zo
        var i = 0.0
        while (i < speed) {
            val random = 2 * (level.random.nextFloat() - 0.5f)
            level.addParticle(
                CustomFlareOption(
                    0.5f, 0.43f, 0.36f, 160, 0.93f,
                    (10 + 8 * random).toInt(), 0.03f, size = flareSize,
                ),
                true,
                startX - direction.x * i + random * 0.2,
                startY - direction.y * i + random * 0.2,
                startZ - direction.z * i + random * 0.2,
                0.0, 0.0, 0.0,
            )
            i += 1.5
        }
    }

    private fun spawnShellTrail(level: ClientLevel, entity: Entity) {
        val motion = entity.deltaMovement
        val speed = motion.length()
        if (speed < 1.0e-4) return
        val direction = motion.normalize()
        var i = 0.0
        while (i < speed) {
            val random = level.random.nextFloat()
            level.addParticle(
                CustomCloudOption(
                    0.6f, 0.58f, 0.57f, (120 + 40 * random).toInt(),
                    1.5f + 0.5f * random, 0f, cooldown = false, light = false,
                ),
                true,
                entity.xo - direction.x * i + 0.25f * random,
                entity.yo + entity.bbHeight / 2 - direction.y * i + 0.25f * random,
                entity.zo - direction.z * i + 0.25f * random,
                0.0, 0.0, 0.0,
            )
            i += 2.0
        }
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
