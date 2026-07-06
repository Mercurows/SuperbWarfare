package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.receive.DistantVehiclesMessage
import com.atsuishio.superbwarfare.network.message.receive.VehicleSnapshot
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
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

    private val ghostMap = LinkedHashMap<Int, Ghost>()
    private var cachedLevel: ClientLevel? = null
    private var tickCounter = 0L

    fun ghosts(): Collection<Ghost> = ghostMap.values

    fun handleMessage(msg: DistantVehiclesMessage) {
        val level = mc.level ?: return
        if (level !== cachedLevel) {
            ghostMap.clear()
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

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val level = mc.level
        if (level == null || level !== cachedLevel) {
            ghostMap.clear()
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

    @SubscribeEvent
    fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        ghostMap.clear()
        cachedLevel = null
    }
}
