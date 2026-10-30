package com.atsuishio.superbwarfare.physics.tracked

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.receive.TrackedCollisionDebugMessage
import com.atsuishio.superbwarfare.network.message.receive.TrackedTerrainDebugMeshMessage
import com.atsuishio.superbwarfare.physics.VehiclePhysics
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.server.ServerLifecycleHooks
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Server-side subscription for the Bullet debug view.
 *
 * One press shows every tracked vehicle already loaded in the player's
 * dimension. It does not load chunks, does not require the player to be
 * riding, and does not rebuild terrain. The triangles are the mesh installed
 * in Bullet. The extra edges are the hull OBBs, wheel cylinders and run boxes
 * published by the physics thread.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
object TrackedTerrainDebugService {
    private class Watch {
        val meshEpoch = ConcurrentHashMap<Int, Long>()
        val shapeEpoch = ConcurrentHashMap<Int, Long>()
        val known = ConcurrentHashMap.newKeySet<Int>()
    }

    private val watches = ConcurrentHashMap<UUID, Watch>()

    /** True while any player is subscribed. Physics-thread readers use this. */
    fun watchingAnyone(): Boolean = watches.isNotEmpty()

    fun show(player: ServerPlayer): String {
        val watch = Watch()
        watches[player.uuid] = watch
        val shown = publishAll(player, watch, force = true)
        return "已显示已加载区块内的履带车：地形网格 $shown 辆，黄色是车体 OBB，白色是轮圆柱，绿色是履带运行段。/sbwtest bt hide 关闭。"
    }

    fun hide(player: ServerPlayer) {
        val watch = watches.remove(player.uuid) ?: return
        for (entityId in watch.known) {
            send(player, TrackedTerrainDebugMeshMessage.clear(entityId))
            sendShape(player, TrackedCollisionDebugMessage.clear(entityId))
        }
    }

    fun watching(player: ServerPlayer): Boolean = watches.containsKey(player.uuid)

    fun status(player: ServerPlayer, ridden: VehicleEntity?): List<String> {
        val lines = ArrayList<String>()
        val vehicles = loadedTracked(player)
        lines += if (watching(player)) {
            "线框：开启，已加载区块内 ${vehicles.size} 辆履带车"
        } else {
            "线框：关闭"
        }
        val waiting = vehicles.count { VehiclePhysics.trackedRuntime(it) == null }
        if (waiting > 0) lines += "$waiting 辆履带车还没有接入 Bullet，不拿伤害盒冒充碰撞体"
        val vehicle = ridden ?: vehicles.firstOrNull()
        val runtime = vehicle?.let { VehiclePhysics.trackedRuntime(it) }
        if (runtime == null) {
            if (vehicles.isEmpty()) lines += "当前维度没有已加载的履带车"
            return lines
        }
        val mesh = runtime.terrainDebugMesh()
        lines += "mesh hash=${runtime.terrainInstalledHash()} epoch=${runtime.terrainDebugEpoch()} " +
            "triangles=${mesh?.triangleCount ?: 0} ramps=${runtime.terrainRampCount()} ready=${runtime.terrainReady()}"
        lines += "shapes epoch=${runtime.collisionDebugSnapshot()?.epoch ?: 0} " +
            "segments=${runtime.collisionDebugSnapshot()?.segmentCount ?: 0}"
        lines += "support ${runtime.lastSupportContacts()}"
        return lines
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END || watches.isEmpty()) return
        val server = ServerLifecycleHooks.getCurrentServer() ?: return
        val stale = ArrayList<UUID>()
        for ((playerId, watch) in watches) {
            val player = server.playerList.getPlayer(playerId)
            if (player == null) {
                stale += playerId
                continue
            }
            publishAll(player, watch, force = false)
        }
        stale.forEach { watches.remove(it) }
    }

    @SubscribeEvent
    fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        watches.remove(event.entity.uuid)
    }

    private fun publishAll(player: ServerPlayer, watch: Watch, force: Boolean): Int {
        val vehicles = loadedTracked(player)
        val live = HashSet<Int>()
        var shown = 0
        for (vehicle in vehicles) {
            live += vehicle.id
            watch.known += vehicle.id
            val runtime = VehiclePhysics.trackedRuntime(vehicle) ?: continue
            publishMesh(player, watch, vehicle, runtime, force)
            publishShapes(player, watch, vehicle, runtime, force)
            shown++
        }
        val gone = watch.known.filter { it !in live }
        for (entityId in gone) {
            watch.known.remove(entityId)
            watch.meshEpoch.remove(entityId)
            watch.shapeEpoch.remove(entityId)
            send(player, TrackedTerrainDebugMeshMessage.clear(entityId))
            sendShape(player, TrackedCollisionDebugMessage.clear(entityId))
        }
        return shown
    }

    private fun publishMesh(
        player: ServerPlayer,
        watch: Watch,
        vehicle: VehicleEntity,
        runtime: TrackedVehicleRuntime,
        force: Boolean,
    ) {
        val epoch = runtime.terrainDebugEpoch()
        if (!force && watch.meshEpoch[vehicle.id] == epoch) return
        val mesh = runtime.terrainDebugMesh()
        if (mesh == null || mesh.triangleCount == 0) {
            send(player, TrackedTerrainDebugMeshMessage.clear(vehicle.id))
            watch.meshEpoch[vehicle.id] = epoch
            return
        }
        val selection = TrackedTerrainDebugSelection.of(mesh, vehicle.x, vehicle.y, vehicle.z)
        for (packet in TrackedTerrainDebugMeshMessage.chunks(
            vehicle.id, mesh.requestId, mesh.contentHash, selection,
        )) {
            send(player, packet)
        }
        watch.meshEpoch[vehicle.id] = epoch
    }

    private fun publishShapes(
        player: ServerPlayer,
        watch: Watch,
        vehicle: VehicleEntity,
        runtime: TrackedVehicleRuntime,
        force: Boolean,
    ) {
        val snapshot = runtime.collisionDebugSnapshot() ?: return
        if (!force && watch.shapeEpoch[vehicle.id] == snapshot.epoch) return
        for (packet in TrackedCollisionDebugMessage.chunks(vehicle.id, snapshot)) {
            sendShape(player, packet)
        }
        watch.shapeEpoch[vehicle.id] = snapshot.epoch
    }

    /**
     * Tracked vehicles already present in the player's dimension.
     *
     * `allEntities` is the loaded set. This does not ask the chunk source for
     * anything new, so opening the view cannot load terrain.
     */
    private fun loadedTracked(player: ServerPlayer): List<VehicleEntity> {
        val out = ArrayList<VehicleEntity>()
        for (entity in player.serverLevel().allEntities) {
            val vehicle = entity as? VehicleEntity ?: continue
            if (vehicle.isRemoved) continue
            if (VehiclePhysics.trackedRuntime(vehicle) == null &&
                vehicle.computed().engineType != EngineType.TRACK
            ) {
                continue
            }
            out += vehicle
        }
        return out
    }

    private fun send(player: ServerPlayer, packet: TrackedTerrainDebugMeshMessage) {
        sendPacketTo(player, packet)
    }

    private fun sendShape(player: ServerPlayer, packet: TrackedCollisionDebugMessage) {
        sendPacketTo(player, packet)
    }
}
