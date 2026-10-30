package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.level.PhysicsLevel
import cn.solarmoon.spark_core.physics.terrain.TerrainRegionSnapshot
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Game-thread driver for each active tracked vehicle's local terrain mesh.
 *
 * The physics thread publishes only a plain chassis centre. This handler asks the
 * terrain cache for a detached region, hands the pure geometry build to a small
 * per-level worker, and lets the runtime queue the native swap. Nothing here reads
 * the Minecraft level block by block: the expensive world scan already happens once
 * per section inside `PhysicsChunkManager`, and every tracked vehicle reuses it.
 *
 * Two behaviours are deliberate:
 *
 * - **Per-level queues.** A global cursor is wrong: runtimes appear and disappear,
 *   so a shared index points at a different vehicle on every pass and starves the
 *   ones at the end. Each level carries its own first-mesh and refresh cursors.
 * - **One in-flight build per runtime.** A vehicle whose mesh is still being built
 *   is skipped, so a slow build cannot queue a second one behind it. That is the
 *   same `rebuildQueued` discipline the reference implementation uses for belt runs.
 */
object TrackedTerrainAutoSampler {

    /** Vehicles with no mesh at all. They must become drivable quickly. */
    private const val FIRST_MESH_PER_TICK = 2

    /** Vehicles that already have a mesh; only the window moved or a block changed. */
    private const val REFRESH_PER_TICK = 1

    /** Ticks a level with no tracked vehicles keeps its builder thread alive. */
    private const val BUILDER_IDLE_TICKS = 200L

    private class LevelQueue {
        var firstCursor = 0
        var refreshCursor = 0
        var idleTicks = 0L
    }

    private val queues = ConcurrentHashMap<Level, LevelQueue>()
    private val builders = ConcurrentHashMap<Level, ExecutorService>()

    /**
     * Build [snapshot] off the game thread and deliver the plan to [sink].
     *
     * The build is pure data: it reads only the detached snapshot, so it cannot
     * touch the level, and it cannot race a block edit — a stale result carries an
     * old revision token and is discarded on install.
     */
    internal fun buildAsync(
        level: PhysicsLevel,
        snapshot: TerrainRegionSnapshot,
        terrain: CompiledTerrain,
        requestId: Long,
        runtimeGeneration: Long,
        rampGate: RampGate? = null,
        gear: List<GearProbe> = emptyList(),
        sink: (TrackedTerrainSampleResult) -> Unit,
    ) {
        val executor = builders.computeIfAbsent(level.mcLevel) { key ->
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "SBW-TrackedTerrain-${level.name}").apply { isDaemon = true }
            }
        }
        val gearCopy = java.util.List.copyOf(gear)
        try {
            executor.execute {
                val result = try {
                    TrackedTerrainSampler.sample(
                        snapshot, terrain, requestId, runtimeGeneration, rampGate, gearCopy,
                    )
                } catch (error: Throwable) {
                    TrackedTerrainSampleResult.Missing("mesh build failed: ${error.message}")
                }
                sink(result)
            }
        } catch (error: Throwable) {
            // Never perform geometry planning on the tick thread after rejection.
            sink(TrackedTerrainSampleResult.Missing("mesh executor unavailable: ${error.message}"))
        }
    }

    @SubscribeEvent
    fun onLevelTick(event: TickEvent.LevelTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val world = event.level as? ServerLevel ?: return
        val runtimes = TrackedVehicleRuntime.ACTIVE_RUNTIMES.filter { it.belongsTo(world) }
        if (runtimes.isEmpty()) {
            val idle = queues[world] ?: return
            idle.firstCursor = 0
            idle.refreshCursor = 0
            idle.idleTicks += 1
            if (idle.idleTicks > BUILDER_IDLE_TICKS) {
                queues.remove(world)
                builders.remove(world)?.shutdown()
            }
            return
        }
        val queue = queues.computeIfAbsent(world) { LevelQueue() }
        queue.idleTicks = 0

        // Every runtime must renew its region request every tick, even when it is
        // not picked for a rebuild: the cache retires sections nobody asked for,
        // and a vehicle whose sections were retired would wait for terrain that
        // will never be built. Only the *rebuild* is budgeted.
        runtimes.forEach { it.maintainTerrainRequest() }

        val waiting = runtimes.filter { !it.terrainReady() }
        if (waiting.isNotEmpty()) {
            val count = FIRST_MESH_PER_TICK.coerceAtMost(waiting.size)
            queue.firstCursor = Math.floorMod(queue.firstCursor, waiting.size)
            repeat(count) { offset ->
                waiting[(queue.firstCursor + offset) % waiting.size].samplePublishedTerrain(world)
            }
            queue.firstCursor = Math.floorMod(queue.firstCursor + count, waiting.size)
        }

        val ready = runtimes.filter { it.terrainReady() }
        if (ready.isEmpty()) return
        val count = REFRESH_PER_TICK.coerceAtMost(ready.size)
        queue.refreshCursor = Math.floorMod(queue.refreshCursor, ready.size)
        repeat(count) { offset ->
            ready[(queue.refreshCursor + offset) % ready.size].samplePublishedTerrain(world)
        }
        queue.refreshCursor = Math.floorMod(queue.refreshCursor + count, ready.size)
    }

    /** Drop a level's builder thread; called when the level closes. */
    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        (event.level as? Level)?.let(::shutdownLevel)
    }

    fun shutdownLevel(world: Level) {
        queues.remove(world)
        builders.remove(world)?.shutdown()
    }

    /** Diagnostics: builder threads currently alive. */
    fun activeBuilderCount(): Int = builders.size
}
