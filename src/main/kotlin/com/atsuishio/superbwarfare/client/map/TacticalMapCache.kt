package com.atsuishio.superbwarfare.client.map

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.map.TacticalMapCache.drawnChunks
import com.atsuishio.superbwarfare.client.map.TacticalMapCache.processPendingChunks
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.material.MapColor
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * 战术地图缓存引擎。
 *
 * 内存层: 256x256 tile (NativeImage) -> DynamicTexture -> GPU
 * 持久层: 16x16 chunk ABGR 颜色 + 高度数据 (1536 bytes raw, Deflater 压缩),
 *   以 tile 为单位归并存储（每 256x256 一个 .bin 文件），
 *   路径: `<gameDir>/superbwarfare/tactical_map_cache/<worldId>/<dim>/`，
 *   每个存档独立缓存，单人/多人均可用。
 */
@OnlyIn(Dist.CLIENT)
object TacticalMapCache {
    const val TILE_SIZE = 256
    private const val TILE_SIZE_BITS = 8
    private const val CHUNK_SIZE = 16
    private const val CHUNK_BYTES = CHUNK_SIZE * CHUNK_SIZE * 4 // 1024

    private val BRIGHTNESS_MODIFIERS = intArrayOf(180, 220, 255, 135)

    // In-memory tiles
    private val tileImages = ConcurrentHashMap<RegionPos, NativeImage>()
    private val tileTextures = ConcurrentHashMap<RegionPos, DynamicTexture>()
    private val dirtyTiles = mutableSetOf<RegionPos>()

    // Track how many chunks have been loaded into each tile.
    // A tile is 256×256 blocks = 16×16 = 256 chunks.
    // GPU upload is deferred until the tile reaches its expected chunk count
    // (or the very first chunk for immediate visual feedback).
    private val tileLoadedChunks = ConcurrentHashMap<RegionPos, Int>()     // chunks loaded so far
    private val tileExpectedChunks = ConcurrentHashMap<RegionPos, Int>()   // total chunks expected (from disk)

    // LOD tile storage — lazily downsampled from base tiles at low zoom
    private val lodTileImages = ConcurrentHashMap<LodTileKey, NativeImage>()
    private val lodTileTextures = ConcurrentHashMap<LodTileKey, DynamicTexture>()

    // Chunk update queue
    private val chunkUpdateQueue = ConcurrentHashMap<Long, LevelChunk>()

    // Chunk surface heights: chunkPos -> 256 shorts (16x16 Y values)
    val chunkHeights = ConcurrentHashMap<Long, ShortArray>()

    private const val MAX_UPDATES_PER_FRAME = 16

    // Track which chunks have already been drawn this session
    private val drawnChunks = mutableSetOf<Long>()

    // Chunk keys loaded from disk, waiting to be drawn in distance order.
    // Drained batch-by-batch each tick, nearest to the player first.
    private val pendingChunkQueue = mutableSetOf<Long>()

    // Pre-loaded chunk data (decompressed) populated by loadAllChunks().
    // Eliminates redundant file reads during processPendingChunks().
    // chunkKey -> raw 1536-byte pixel+height data
    private val preloadedChunkData = ConcurrentHashMap<Long, ByteArray>()

    // Periodic rescan
    private var lastRescanTick = 0L
    private var lastCloseRefresh = 0L       // timestamp of last 3x3 rapid refresh
    private var refreshWaveIndex = 0        // progress through spiral offsets, wraps around
    private var cachedViewDist = -1
    private var cachedOffsets: List<Pair<Int, Int>> = emptyList()
    private const val RESCAN_INTERVAL = 5L
    private const val RESCAN_PER_BATCH = 64

    // Local file persistence per world per dimension
    private var currentWorldId: String? = null
    private var currentDimension: String? = null
    private var cacheDir: File? = null

    /** Build a world-unique identifier for cache separation. */
    fun getWorldIdentifier(): String {
        val mc = mc
        // Single player: use integrated server's world data level name
        if (mc.hasSingleplayerServer() && mc.singleplayerServer != null) {
            return mc.singleplayerServer!!.worldData.levelName
        }
        // Multiplayer: hash server address (SHA-256, truncated to 8 bytes hex)
        val server = mc.currentServer
        if (server != null) {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(server.ip.toByteArray(Charsets.UTF_8))
            return hash.take(8).joinToString("") { "%02x".format(it) }
        }
        return "unknown"
    }

    /**
     * Compute the LOD merge factor for the current zoom level.
     * Returns a power of 2: 1 = native tiles, 2 = merge 2x2, 4 = merge 4x4, etc.
     * Capped at 64 to keep LOD tile creation cost bounded.
     */
    fun computeLodMergeFactor(zoom: Double): Int {
        if (zoom >= 2.0) return 1
        val tileScreenPixels = TILE_SIZE * zoom / 5.0
        if (tileScreenPixels >= 128.0) return 1
        var factor = 1
        while (factor * tileScreenPixels < 128.0 && factor < 64) factor = factor shl 1
        return factor
    }

    // ========================
    //  Lifecycle
    // ========================

    fun initForDimension(dimension: String, worldId: String) {
        if (worldId == currentWorldId && dimension == currentDimension) return
        currentWorldId = worldId
        currentDimension = dimension
        cacheDir = null // recomputed on next access

        loadAllChunks()
    }

    fun clear() {
        tileImages.clear()
        tileTextures.clear()
        dirtyTiles.clear()
        chunkUpdateQueue.clear()
        chunkHeights.clear()
        drawnChunks.clear()
        pendingChunkQueue.clear()
        preloadedChunkData.clear()
        tileLoadedChunks.clear()
        tileExpectedChunks.clear()
        lastRescanTick = 0L
        lastCloseRefresh = 0L
        refreshWaveIndex = 0
        cachedViewDist = -1
        cachedOffsets = emptyList()
        currentWorldId = null
        currentDimension = null
        cacheDir = null

        // Release LOD textures from GPU and clear LOD caches
        for ((key, _) in lodTileTextures) {
            try {
                val loc = loc("map_lod_${key.factor}_${key.rx}_${key.rz}")
                mc.textureManager.release(loc)
            } catch (_: Exception) {
            }
        }
        lodTileImages.clear()
        lodTileTextures.clear()
    }

    private fun ensureInit(level: Level) {
        if (currentWorldId == null) {
            val worldId = getWorldIdentifier()
            initForDimension(level.dimension().location().toString(), worldId)
        }
    }

    // ========================
    //  Chunk queue
    // ========================

    fun queueChunkUpdate(chunk: LevelChunk) {
        chunkUpdateQueue[chunk.pos.toLong()] = chunk
    }

    /**
     * Two-phase processing, completely independent:
     *   Phase A - Redraw: drain the update queue directly, closest chunks first.
     *   Phase B - Initial draw: ring scan for chunks not yet in [drawnChunks].
     */
    fun processChunkUpdates(level: Level, playerX: Double, playerZ: Double) {
        ensureInit(level)
        if (level !is ClientLevel) return

        val pcx = (playerX / CHUNK_SIZE).toInt()
        val pcz = (playerZ / CHUNK_SIZE).toInt()
        var processed = 0

        // ============================================================
        // Phase A - REDRAW: drain the queue directly
        // Every entry in the queue was put there by an explicit request
        // (block change, periodic refresh, etc.).  No ring scan needed.
        // ============================================================
        if (chunkUpdateQueue.isNotEmpty()) {
            val sorted = chunkUpdateQueue.entries.sortedBy { (key, _) ->
                val cx = (key shr 32).toInt()
                val cz = key.toInt()
                maxOf(kotlin.math.abs(cx - pcx), kotlin.math.abs(cz - pcz))
            }
            for ((key, chunk) in sorted) {
                if (processed >= MAX_UPDATES_PER_FRAME) break
                chunkUpdateQueue.remove(key)
                updateChunk(chunk, level)
                drawnChunks.add(key)
                processed++
            }
        }

        // ============================================================
        // Phase B - INITIAL DRAW: ring scan for chunks never drawn
        // ============================================================
        if (processed < MAX_UPDATES_PER_FRAME) {
            val viewDist = mc.options.renderDistance().get()
            ringLoop@ for (r in 0..viewDist) {
                for (dx in -r..r) {
                    for (dz in -r..r) {
                        if (dx != r && dx != -r && dz != r && dz != -r) continue
                        val cx = pcx + dx
                        val cz = pcz + dz
                        val key = chunkPosKey(cx, cz)
                        if (drawnChunks.contains(key)) continue
                        if (level.hasChunk(cx, cz)) {
                            val chunk = level.getChunk(cx, cz)
                            if (chunk is LevelChunk && !chunk.isEmpty) {
                                updateChunk(chunk, level)
                                drawnChunks.add(key)
                                processed++
                                if (processed >= MAX_UPDATES_PER_FRAME) break@ringLoop
                            }
                        }
                    }
                }
            }
        }
    }

    fun getPendingUpdateCount() = chunkUpdateQueue.size

    // ========================
    //  Periodic rescan
    // ========================

    fun periodicRescan(level: Level, playerX: Double, playerZ: Double) {
        if (level !is ClientLevel) return
        ensureInit(level)
        val now = System.currentTimeMillis()
        if (now - lastRescanTick < RESCAN_INTERVAL * 50) return
        lastRescanTick = now

        val viewDist = mc.options.renderDistance().get()
        val pcx = (playerX / CHUNK_SIZE).toInt()
        val pcz = (playerZ / CHUNK_SIZE).toInt()

        // Rebuild spiral offsets only when render distance changes
        if (viewDist != cachedViewDist || cachedOffsets.isEmpty()) {
            cachedViewDist = viewDist
            cachedOffsets = buildList {
                for (r in 0..viewDist) {
                    for (dx in -r..r) for (dz in -r..r)
                        if (dx == r || dx == -r || dz == r || dz == -r)
                            add(dx to dz)
                }
            }
        }
        val offsets = cachedOffsets
        if (offsets.isEmpty()) return

        // Phase 1: Find new chunks (not yet drawn) - closest first
        var scanned = 0
        val newChunkBudget = RESCAN_PER_BATCH / 2
        for ((dx, dz) in offsets) {
            if (scanned >= newChunkBudget) break
            val cx = pcx + dx
            val cz = pcz + dz
            val key = chunkPosKey(cx, cz)
            if (chunkUpdateQueue.containsKey(key) || drawnChunks.contains(key)) continue
            if (level.hasChunk(cx, cz)) {
                val chunk = level.getChunk(cx, cz)
                if (chunk is LevelChunk && !chunk.isEmpty) {
                    queueChunkUpdate(chunk)
                    scanned++
                }
            }
        }

        // Phase 2: Sequential refresh through the spiral, wrapping around.
        // Does NOT use a counter that grows across frames - avoids integer overflow.
        var refreshed = 0
        val refreshBudget = RESCAN_PER_BATCH - scanned
        val n = offsets.size
        if (n == 0) return
        // Defensive clamp (handles any residual corruption, e.g. from serialization)
        if (refreshWaveIndex !in 0..<n) refreshWaveIndex = 0
        val steps = minOf(refreshBudget, n)
        for (s in 0 until steps) {
            val (dx, dz) = offsets[refreshWaveIndex]
            val cx = pcx + dx
            val cz = pcz + dz
            val key = chunkPosKey(cx, cz)
            if (drawnChunks.contains(key) && !chunkUpdateQueue.containsKey(key)) {
                if (level.hasChunk(cx, cz)) {
                    val chunk = level.getChunk(cx, cz)
                    if (chunk is LevelChunk && !chunk.isEmpty) {
                        queueChunkUpdate(chunk)
                        refreshed++
                    }
                }
            }
            // Advance with manual wrap - cannot overflow
            refreshWaveIndex++
            if (refreshWaveIndex >= n) refreshWaveIndex = 0
        }

        // Phase 3: Rapid 1s refresh for the 3x3 chunks immediately around the player.
        // Guarantees block changes right under the player are visible almost instantly.
        if (now - lastCloseRefresh >= 1000L) {
            lastCloseRefresh = now
            for (dx in -1..1) {
                for (dz in -1..1) {
                    val cx = pcx + dx
                    val cz = pcz + dz
                    val key = chunkPosKey(cx, cz)
                    if (drawnChunks.contains(key) && !chunkUpdateQueue.containsKey(key)) {
                        if (level.hasChunk(cx, cz)) {
                            val chunk = level.getChunk(cx, cz)
                            if (chunk is LevelChunk && !chunk.isEmpty) {
                                queueChunkUpdate(chunk)
                            }
                        }
                    }
                }
            }
        }
    }

    // ========================
    //  Core: block sampling
    // ========================

    private fun updateChunk(chunk: LevelChunk, level: Level) {
        ensureInit(level)
        val minX = chunk.pos.minBlockX
        val minZ = chunk.pos.minBlockZ

        for (z in 0..15) {
            var prevHeight = 0
            var prevSet = false
            for (x in 0..15) {
                val worldX = minX + x
                val worldZ = minZ + z

                val surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)
                var y = surfaceY
                if (y <= level.minBuildHeight) {
                    prevHeight = level.minBuildHeight
                    prevSet = true
                    continue
                }

                val mutablePos = BlockPos.MutableBlockPos(worldX, y, worldZ)
                var state = chunk.getBlockState(mutablePos)

                while (y > level.minBuildHeight && state.getMapColor(level, mutablePos) == MapColor.NONE) {
                    y--
                    mutablePos.setY(y)
                    state = chunk.getBlockState(mutablePos)
                }

                val mapColor = state.getMapColor(level, mutablePos)
                if (mapColor == MapColor.NONE) {
                    prevHeight = y
                    prevSet = true
                    continue
                }

                var actualColor = mapColor
                var actualY = y
                if (!state.fluidState.isEmpty) {
                    actualColor =
                        if (state.fluidState.`is`(FluidTags.LAVA)) MapColor.COLOR_ORANGE else MapColor.WATER
                    actualY = surfaceY
                }

                val brightness = if (!prevSet) MapColor.Brightness.NORMAL
                else computeBrightness(actualY, prevHeight, worldX, worldZ)

                prevHeight = actualY
                prevSet = true

                if (actualColor != MapColor.NONE) {
                    val abgr = calculateABGR(actualColor, brightness)
                    val tileRX = worldX shr TILE_SIZE_BITS
                    val tileRZ = worldZ shr TILE_SIZE_BITS
                    val tileX = worldX and (TILE_SIZE - 1)
                    val tileZ = worldZ and (TILE_SIZE - 1)
                    getOrCreateTile(tileRX, tileRZ).setPixelRGBA(tileX, tileZ, abgr)
                    dirtyTiles.add(RegionPos(tileRX, tileRZ))
                    invalidateLodTilesForBaseTile(tileRX, tileRZ)
                }
            }
        }

        // Store heights for this chunk
        val heights = ShortArray(CHUNK_SIZE * CHUNK_SIZE)
        for (z in 0..15) for (x in 0..15)
            heights[z * CHUNK_SIZE + x] = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z).toShort()
        chunkHeights[chunkPosKey(chunk.pos.x, chunk.pos.z)] = heights

        // Save to disk
        saveChunkToDisk(chunk.pos.x, chunk.pos.z)
    }

    private fun computeBrightness(currentY: Int, prevY: Int, worldX: Int, worldZ: Int): MapColor.Brightness {
        val d3 = (currentY - prevY).toDouble() * 0.8 + (((worldX + worldZ) and 1) - 0.5) * 0.4
        return when {
            d3 > 0.6 -> MapColor.Brightness.HIGH
            d3 < -0.6 -> MapColor.Brightness.LOW
            else -> MapColor.Brightness.NORMAL
        }
    }

    private fun calculateABGR(mapColor: MapColor, brightness: MapColor.Brightness): Int {
        if (mapColor == MapColor.NONE) return 0
        val mod = BRIGHTNESS_MODIFIERS[brightness.id]
        val col = mapColor.col
        val r = ((col shr 16) and 0xFF) * mod / 255
        val g = ((col shr 8) and 0xFF) * mod / 255
        val b = (col and 0xFF) * mod / 255
        return (0xFF shl 24) or (b shl 16) or (g shl 8) or r
    }

    // ========================
    //  Tile-based persistence (local files per world per dimension)
    //
    //  Each 256x256 tile contains up to 16x16 (256) chunks, grouped into one
    //  .bin file to keep file counts low while keeping individual files small.
    //  File format: [int count] + count x (long key, int len, byte[len] data)
    // ========================

    private const val HEIGHT_BYTES = CHUNK_SIZE * CHUNK_SIZE * 2 // 512
    private const val TOTAL_BYTES = CHUNK_BYTES + HEIGHT_BYTES   // 1536

    // Chunks per tile edge: TILE_SIZE / CHUNK_SIZE = 256 / 16 = 16
    private const val CHUNKS_PER_TILE_BITS = 4

    private fun getCacheDir(): File? {
        val dim = currentDimension ?: return null
        val worldId = currentWorldId ?: return null
        // Reuse cached directory if still valid
        cacheDir?.let { if (it.exists()) return it }
        val dir = File(
            mc.gameDirectory,
            "superbwarfare/tactical_map_cache/$worldId/${dim.replace(":", "_")}"
        )
        dir.mkdirs()
        cacheDir = dir
        return dir
    }

    /** Storage tile file: groups all chunks within a 256x256 tile. */
    private fun tileFile(tileRX: Int, tileRZ: Int): File? {
        val dir = getCacheDir() ?: return null
        return File(dir, "${tileRX}_${tileRZ}.bin")
    }

    /** Read all chunk entries from a tile file -> (chunkKey -> compressed data). */
    private fun readTileFile(tileRX: Int, tileRZ: Int): Map<Long, ByteArray> {
        val file = tileFile(tileRX, tileRZ) ?: return emptyMap()
        if (!file.exists()) return emptyMap()
        val result = mutableMapOf<Long, ByteArray>()
        try {
            val bytes = file.readBytes()
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            if (buf.remaining() < 4) return emptyMap()
            val count = buf.int
            for (i in 0 until count) {
                if (buf.remaining() < 12) break
                val key = buf.long
                val len = buf.int
                if (buf.remaining() < len) break
                val data = ByteArray(len)
                buf.get(data)
                result[key] = data
            }
        } catch (_: Exception) {
            // Corrupt file -> treat as empty; will be overwritten on next save
        }
        return result
    }

    /** Write chunk entries to a tile file (atomic via temp file). */
    private fun writeTileFile(tileRX: Int, tileRZ: Int, chunks: Map<Long, ByteArray>) {
        val file = tileFile(tileRX, tileRZ) ?: return
        try {
            val totalSize = 4 + chunks.entries.sumOf { 8 + 4 + it.value.size }
            val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
            buf.putInt(chunks.size)
            for ((key, data) in chunks) {
                buf.putLong(key)
                buf.putInt(data.size)
                buf.put(data)
            }
            // Atomic write via temp file
            val tmp = File(file.parentFile, "${file.name}.tmp")
            file.delete()
            tmp.writeBytes(buf.array())
            tmp.renameTo(file)
        } catch (e: Exception) {
            Mod.LOGGER.warn("Failed to write tile $tileRX,$tileRZ: ${e.message}")
        }
    }

    /**
     * Persist a single chunk's color + height data into its parent tile file.
     * Works for both single-player and dedicated-server clients.
     */
    private fun saveChunkToDisk(cx: Int, cz: Int) {
        val storageRX = cx shr CHUNKS_PER_TILE_BITS
        val storageRZ = cz shr CHUNKS_PER_TILE_BITS

        // Compress pixel + height data for this chunk
        val minBlockX = cx * CHUNK_SIZE
        val minBlockZ = cz * CHUNK_SIZE
        val imgTileRX = minBlockX shr TILE_SIZE_BITS
        val imgTileRZ = minBlockZ shr TILE_SIZE_BITS
        val tile = tileImages[RegionPos(imgTileRX, imgTileRZ)] ?: return

        val tileX = minBlockX and (TILE_SIZE - 1)
        val tileZ = minBlockZ and (TILE_SIZE - 1)
        val raw = ByteArray(TOTAL_BYTES)
        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        for (z in 0 until CHUNK_SIZE)
            for (x in 0 until CHUNK_SIZE)
                buf.putInt(tile.getPixelRGBA(tileX + x, tileZ + z))
        val heights = chunkHeights[chunkPosKey(cx, cz)]
        for (z in 0 until CHUNK_SIZE)
            for (x in 0 until CHUNK_SIZE)
                buf.putShort(heights?.get(z * CHUNK_SIZE + x) ?: 0)

        val compressed: ByteArray
        try {
            val deflater = Deflater(Deflater.BEST_SPEED)
            deflater.setInput(raw)
            deflater.finish()
            val tmp = ByteArray(raw.size)
            val size = deflater.deflate(tmp)
            deflater.end()
            compressed = tmp.copyOf(size)
        } catch (e: Exception) {
            Mod.LOGGER.warn("Failed to compress chunk $cx,$cz: ${e.message}")
            return
        }

        // Read existing tile, update chunk, write back (single read-write per save)
        val chunks = readTileFile(storageRX, storageRZ).toMutableMap()
        chunks[chunkPosKey(cx, cz)] = compressed
        writeTileFile(storageRX, storageRZ, chunks)
    }

    /**
     * Load a single chunk's color + height data into the tile image.
     * Checks the in-memory [preloadedChunkData] cache first (populated by
     * [loadAllChunks]), falling back to disk only for chunks saved after the
     * initial preload.
     */
    private fun loadChunkFromDisk(cx: Int, cz: Int): RegionPos? {
        val key = chunkPosKey(cx, cz)

        // Fast path: data was already decompressed by loadAllChunks()
        val raw = preloadedChunkData.remove(key)
        if (raw != null) {
            return applyChunkData(cx, cz, raw)
        }

        // Slow path: chunk was saved after preload, read from disk
        val storageRX = cx shr CHUNKS_PER_TILE_BITS
        val storageRZ = cz shr CHUNKS_PER_TILE_BITS
        val chunks = readTileFile(storageRX, storageRZ)
        val compressed = chunks[key] ?: return null
        val diskRaw = decompressChunkData(compressed) ?: return null
        return applyChunkData(cx, cz, diskRaw)
    }

    /**
     * Write decompressed pixel + height data into the tile image.
     * Returns the affected tile position for batch LOD invalidation.
     * Caller is responsible for calling [invalidateLodTilesForBaseTile]
     * or [invalidateLodTilesBatch] after all chunks are written.
     */
    private fun applyChunkData(cx: Int, cz: Int, raw: ByteArray): RegionPos {
        val minBlockX = cx * CHUNK_SIZE
        val minBlockZ = cz * CHUNK_SIZE
        val imgTileRX = minBlockX shr TILE_SIZE_BITS
        val imgTileRZ = minBlockZ shr TILE_SIZE_BITS
        val tileX = minBlockX and (TILE_SIZE - 1)
        val tileZ = minBlockZ and (TILE_SIZE - 1)
        val tile = getOrCreateTile(imgTileRX, imgTileRZ)

        val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        // Write heights in the same pass to reuse the buffer
        val heights = ShortArray(CHUNK_SIZE * CHUNK_SIZE)
        for (z in 0 until CHUNK_SIZE) {
            for (x in 0 until CHUNK_SIZE) {
                val abgr = buf.getInt()
                if (abgr != 0) tile.setPixelRGBA(tileX + x, tileZ + z, abgr)
            }
        }
        if (raw.size >= TOTAL_BYTES) {
            for (z in 0 until CHUNK_SIZE)
                for (x in 0 until CHUNK_SIZE)
                    heights[z * CHUNK_SIZE + x] = buf.getShort()
            chunkHeights[chunkPosKey(cx, cz)] = heights
        }
        // Deferred GPU upload: only mark dirty when tile is complete or this is
        // the very first chunk (so the player sees something immediately).
        val tilePos = RegionPos(imgTileRX, imgTileRZ)
        val newCount = (tileLoadedChunks.getOrDefault(tilePos, 0) + 1)
        tileLoadedChunks[tilePos] = newCount
        val expected = tileExpectedChunks.getOrDefault(tilePos, Int.MAX_VALUE)
        if (newCount == 1 || newCount >= expected) {
            dirtyTiles.add(tilePos)
        }

        drawnChunks.add(chunkPosKey(cx, cz))
        return tilePos
    }

    /** Batch-invalidate LOD tiles for a set of unique base tile positions. */
    private fun invalidateLodTilesBatch(affectedTiles: Set<RegionPos>) {
        for (tile in affectedTiles) {
            invalidateLodTilesForBaseTile(tile.rx, tile.rz)
        }
    }

    /**
     * Collect all previously-persisted chunk keys from tile files in the cache
     * directory. Decompress data immediately and stage in [preloadedChunkData] so
     * [processPendingChunks] can apply pixels without any further file I/O.
     */
    private fun loadAllChunks() {
        val dir = getCacheDir() ?: return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (!file.isFile || !file.name.endsWith(".bin")) continue
            val parts = file.nameWithoutExtension.split("_")
            if (parts.size == 2) {
                try {
                    val tileRX = parts[0].toInt()
                    val tileRZ = parts[1].toInt()
                    // Read tile file once; decompress all chunks immediately and
                    // stage them in memory so processPendingChunks skips disk I/O entirely.
                    val chunks = readTileFile(tileRX, tileRZ)
                    // Pre-count expected chunk count per tile for deferred GPU upload
                    tileExpectedChunks[RegionPos(tileRX, tileRZ)] = chunks.size
                    for ((key, compressed) in chunks) {
                        val raw = decompressChunkData(compressed) ?: continue
                        preloadedChunkData[key] = raw
                        pendingChunkQueue.add(key)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    /** Decompress a single chunk's compressed data, or null if corrupted. */
    private fun decompressChunkData(compressed: ByteArray): ByteArray? {
        return try {
            val inflater = Inflater()
            inflater.setInput(compressed)
            val tmp = ByteArray(TOTAL_BYTES)
            val size = inflater.inflate(tmp)
            inflater.end()
            // Support both old (1024) and new (1536) format
            when {
                size == CHUNK_BYTES -> tmp.copyOf(size)
                size >= TOTAL_BYTES -> tmp
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Process a batch of pending chunk keys: select the nearest chunks using a
     * bounded max-heap in O(n log k), load pixel data from the in-memory
     * [preloadedChunkData] cache (no disk I/O), and batch-invalidate LOD tiles.
     *
     * Batch size scales adaptively with queue size so large maps load faster:
     *   - > 2000 queued → 4× base rate  (96/tick)
     *   - >  500 queued → 2× base rate  (48/tick)
     *   - otherwise      → 1× base rate  (24/tick)
     */
    fun processPendingChunks(px: Double, pz: Double, maxCount: Int) {
        if (pendingChunkQueue.isEmpty()) return

        // Adaptive batch size: large backlog → more chunks per tick
        val queueSize = pendingChunkQueue.size
        val adaptiveMax = when {
            queueSize > 2000 -> maxCount * 4
            queueSize > 500  -> maxCount * 2
            else             -> maxCount
        }

        val pcx = (px / CHUNK_SIZE).toInt()
        val pcz = (pz / CHUNK_SIZE).toInt()

        // Bounded max-heap of size adaptiveMax — O(n log k)
        val nearest = PriorityQueue<Pair<Long, Long>>(adaptiveMax, compareByDescending { it.second })
        for (key in pendingChunkQueue) {
            val cx = (key shr 32).toInt()
            val cz = key.toInt()
            val dx = cx - pcx
            val dz = cz - pcz
            val dist = dx.toLong() * dx + dz.toLong() * dz
            nearest.offer(key to dist)
            if (nearest.size > adaptiveMax) nearest.poll()
        }

        // Drain heap sorted by distance, record affected tiles for batch LOD invalidation
        val affectedTiles = mutableSetOf<RegionPos>()
        for ((key, _) in nearest.sortedBy { it.second }) {
            pendingChunkQueue.remove(key)
            val cx = (key shr 32).toInt()
            val cz = key.toInt()
            try {
                loadChunkFromDisk(cx, cz)?.let { affectedTiles.add(it) }
            } catch (_: Exception) {
            }
        }

        // Batch-invalidate LOD once per unique base tile (was per-chunk before)
        invalidateLodTilesBatch(affectedTiles)
    }

    // ========================
    //  LOD tile creation & queries
    // ========================

    /**
     * Key for LOD tiles. [factor] is the merge factor (power of 2, 2 <= factor <= 64).
     * [rx] and [rz] are LOD tile grid coordinates; each LOD tile covers
     * (factor * TILE_SIZE) x (factor * TILE_SIZE) world blocks.
     */
    data class LodTileKey(val factor: Int, val rx: Int, val rz: Int)

    /** Downsample factor x factor base tiles into one 256x256 LOD tile. */
    private fun createLodTile(factor: Int, lodRX: Int, lodRZ: Int): NativeImage {
        val image = NativeImage(TILE_SIZE, TILE_SIZE, true)
        val subStep = TILE_SIZE / factor

        for (py in 0 until TILE_SIZE) {
            val baseRZ = lodRZ * factor + py / subStep
            val srcZ = (py % subStep) * factor + factor / 2

            for (px in 0 until TILE_SIZE) {
                val baseRX = lodRX * factor + px / subStep
                val srcX = (px % subStep) * factor + factor / 2

                val baseTile = tileImages[RegionPos(baseRX, baseRZ)] ?: continue
                val pixel = baseTile.getPixelRGBA(srcX, srcZ)
                if (pixel != 0) {
                    image.setPixelRGBA(px, py, pixel)
                }
            }
        }
        return image
    }

    /** Get or create a GPU texture for an LOD tile. */
    fun getLodTileTexture(factor: Int, lodRX: Int, lodRZ: Int): ResourceLocation {
        val key = LodTileKey(factor, lodRX, lodRZ)
        val loc = loc("map_lod_${factor}_${lodRX}_${lodRZ}")

        // Fast path: already on GPU
        lodTileTextures[key]?.let { return loc }

        // Create the downsampled tile (cached in image map)
        val image = lodTileImages.computeIfAbsent(key) {
            createLodTile(factor, lodRX, lodRZ)
        }

        // Register texture
        try {
            mc.textureManager.release(loc)
        } catch (_: Exception) {
        }
        val texture = DynamicTexture(image)
        mc.textureManager.register(loc, texture)
        lodTileTextures[key] = texture

        return loc
    }

    /**
     * Returns all LOD tile keys visible within the given block radius, sorted by
     * distance from center so inner tiles render first.
     */
    fun getVisibleLodTiles(
        centerBlockX: Int,
        centerBlockZ: Int,
        blockRadius: Int,
        factor: Int
    ): List<LodTileKey> {
        val lodSize = TILE_SIZE * factor
        val halfSize = lodSize / 2
        val minRX = Math.floorDiv(centerBlockX - blockRadius - halfSize, lodSize)
        val maxRX = Math.floorDiv(centerBlockX + blockRadius + halfSize, lodSize)
        val minRZ = Math.floorDiv(centerBlockZ - blockRadius - halfSize, lodSize)
        val maxRZ = Math.floorDiv(centerBlockZ + blockRadius + halfSize, lodSize)

        return buildList {
            for (rx in minRX..maxRX) {
                for (rz in minRZ..maxRZ) {
                    add(LodTileKey(factor, rx, rz))
                }
            }
        }.sortedBy { key ->
            val tcX = key.rx.toLong() * lodSize + halfSize - centerBlockX
            val tcZ = key.rz.toLong() * lodSize + halfSize - centerBlockZ
            tcX * tcX + tcZ * tcZ
        }
    }

    /** Invalidate all LOD tiles that cover the given base tile. */
    private fun invalidateLodTilesForBaseTile(rx: Int, rz: Int) {
        var factor = 2
        while (factor <= 64) {
            val lodRX = Math.floorDiv(rx, factor)
            val lodRZ = Math.floorDiv(rz, factor)
            val key = LodTileKey(factor, lodRX, lodRZ)

            lodTileImages.remove(key)

            lodTileTextures.remove(key)?.let {
                try {
                    val loc = loc("map_lod_${factor}_${lodRX}_${lodRZ}")
                    mc.textureManager.release(loc)
                } catch (_: Exception) {
                }
            }
            factor = factor shl 1
        }
    }

    // ========================
    //  Tile management
    // ========================

    private fun getOrCreateTile(rx: Int, rz: Int): NativeImage {
        return tileImages.computeIfAbsent(RegionPos(rx, rz)) {
            NativeImage(TILE_SIZE, TILE_SIZE, true)
        }
    }

    fun getTileTexture(rx: Int, rz: Int): ResourceLocation? {
        if (!tileImages.containsKey(RegionPos(rx, rz))) return null
        tileTextures.computeIfAbsent(RegionPos(rx, rz)) {
            val loc = loc("map_tile_${rx}_${rz}")
            try {
                mc.textureManager.release(loc)
            } catch (_: Exception) {
            }
            DynamicTexture(tileImages[RegionPos(rx, rz)]!!).also {
                mc.textureManager.register(loc, it)
            }
        }
        return loc("map_tile_${rx}_${rz}")
    }

    fun uploadDirtyTextures() {
        for (pos in dirtyTiles.toList()) {
            tileTextures[pos]?.upload()
        }
        dirtyTiles.clear()
    }

    fun getVisibleTiles(centerBlockX: Int, centerBlockZ: Int, blockRadius: Int): List<RegionPos> {
        val minRX = (centerBlockX - blockRadius) shr TILE_SIZE_BITS
        val maxRX = (centerBlockX + blockRadius) shr TILE_SIZE_BITS
        val minRZ = (centerBlockZ - blockRadius) shr TILE_SIZE_BITS
        val maxRZ = (centerBlockZ + blockRadius) shr TILE_SIZE_BITS
        // Sort tiles by distance from center so inner tiles render first
        return buildList {
            for (rx in minRX..maxRX) for (rz in minRZ..maxRZ) add(RegionPos(rx, rz))
        }.sortedBy { pos ->
            val tileCenterX = pos.rx * TILE_SIZE + TILE_SIZE / 2
            val tileCenterZ = pos.rz * TILE_SIZE + TILE_SIZE / 2
            val dx = tileCenterX - centerBlockX
            val dz = tileCenterZ - centerBlockZ
            dx.toLong() * dx + dz.toLong() * dz
        }
    }

    private fun chunkPosKey(cx: Int, cz: Int) = (cx.toLong() shl 32) or (cz.toLong() and 0xFFFFFFFFL)

    fun getCachedHeight(worldX: Int, worldZ: Int): Short? {
        val cx = worldX shr 4
        val cz = worldZ shr 4
        val h = chunkHeights[chunkPosKey(cx, cz)] ?: return null
        return h[(worldZ and 15) * CHUNK_SIZE + (worldX and 15)]
    }

    data class RegionPos(val rx: Int, val rz: Int)
}
