package com.atsuishio.superbwarfare.client.lighting

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongIterator
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.*

/**
 * High-performance, zero-allocation registry for client-side dynamic block light sources.
 *
 * Data Layout (64-bit Long bitfield per spark):
 * Bits 63..48 : maxLevel   (8 bits, clamped 1..15)
 * Bits 47..32 : minLevel   (8 bits, clamped 1..maxLevel)
 * Bits 31..16 : startTick  (16 bits, unsigned tick timestamp)
 * Bits 15..0  : expiryTick (16 bits, unsigned tick timestamp)
 *
 * @author paralax034
 * @since 0.8.9.1
 */
@OnlyIn(Dist.CLIENT)
object LightPositionRegistry {

    private const val MAX_ACTIVE_LIGHTS = 2048

    private val sparks = Long2LongOpenHashMap(512).also { it.defaultReturnValue(0L) }
    private val active = LongOpenHashSet(512)
    private val expiredBuf = LongArrayList(128)

    private var currentTick = 0L

    /**
     * Registers or refreshes a dynamic light source.
     *
     * Resets startTick on each invocation so continuous fire or persistent burning
     * always refreshes back to peak maxLevel without decaying to zero over time.
     *
     * @param packedPos  [BlockPos.asLong] of the light source
     * @param maxLevel   peak light level (0–15)
     * @param minLevel   minimum level before expiry (≥1)
     * @param ttlTicks   lifetime in client ticks
     */
    @JvmStatic
    fun putSpark(packedPos: Long, maxLevel: Int, minLevel: Int, ttlTicks: Int) {
        if (maxLevel <= 0 || ttlTicks <= 0) return
        if (!active.contains(packedPos) && active.size >= MAX_ACTIVE_LIGHTS) return

        val clampedMax = maxLevel.coerceIn(1, 15)
        val clampedMin = minLevel.coerceIn(1, clampedMax)
        val expiry = currentTick + ttlTicks

        // Reset start to currentTick so continuous refresh maintains peak brightness
        val packed = (clampedMax.toLong() shl 48) or
                (clampedMin.toLong() shl 32) or
                ((currentTick and 0xFFFFL) shl 16) or
                (expiry and 0xFFFFL)

        sparks.put(packedPos, packed)
        active.add(packedPos)
    }

    /**
     * Registers a dynamic light source with radial distance attenuation.
     *
     * @param centerPos  center block pos
     * @param maxLevel   peak light level at center (1–15)
     * @param minLevel   minimum level before expiry (≥1)
     * @param ttlTicks   lifetime in client ticks
     * @param radius     attenuation radius in blocks
     */
    @JvmStatic
    fun putSparkRadius(centerPos: BlockPos, maxLevel: Int, minLevel: Int, ttlTicks: Int, radius: Int = 3) {
        if (maxLevel <= 0 || ttlTicks <= 0) return
        val clampedMax = maxLevel.coerceIn(1, 15)
        val clampedMin = minLevel.coerceIn(1, clampedMax)
        val r = radius.coerceIn(0, 5)

        if (r == 0) {
            putSpark(centerPos.asLong(), clampedMax, clampedMin, ttlTicks)
            return
        }

        val cx = centerPos.x
        val cy = centerPos.y
        val cz = centerPos.z

        for (dx in -r..r) {
            for (dy in -r..r) {
                for (dz in -r..r) {
                    val dist = kotlin.math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toInt()
                    if (dist <= r) {
                        val level = (clampedMax - dist).coerceAtLeast(clampedMin)
                        putSpark(BlockPos.asLong(cx + dx, cy + dy, cz + dz), level, clampedMin, ttlTicks)
                    }
                }
            }
        }
    }

    /**
     * Returns the current computed light level for a packed position.
     *
     * @param packedPos packed 64-bit Long coordinate
     * @return dynamic light level (1–15), or -1 if no active spark exists
     */
    @JvmStatic
    fun getLevel(packedPos: Long): Int {
        val packed = sparks.get(packedPos)
        if (packed == 0L) return -1

        val maxLevel = (packed ushr 48).toInt()
        val minLevel = ((packed ushr 32) and 0xFFL).toInt()
        val startLow = (packed ushr 16) and 0xFFFFL
        val expiryLow = packed and 0xFFFFL
        val curLow = currentTick and 0xFFFFL

        val total = (expiryLow - startLow) and 0xFFFFL
        val elapsed = (curLow - startLow) and 0xFFFFL

        if (elapsed > total) return -1
        if (total == 0L) return maxLevel

        val progress = elapsed.toDouble() / total.toDouble()
        val decay = 1.0 - progress * progress
        val base = minLevel + ((maxLevel - minLevel) * decay).toInt()

        return base.coerceIn(1, 15)
    }

    @JvmStatic
    fun activeIterator(): LongIterator = active.iterator()

    @JvmStatic
    fun isEmpty(): Boolean = active.isEmpty()

    /**
     * Advances internal tick counter and purges expired sparks.
     */
    @JvmStatic
    fun tick() {
        currentTick++
        if (sparks.isEmpty()) return

        val level = Minecraft.getInstance().level ?: return
        val engine = level.lightEngine
        expiredBuf.clear()
        val curLow = currentTick and 0xFFFFL

        for (entry in sparks.long2LongEntrySet()) {
            val packed = entry.longValue
            val startLow = (packed ushr 16) and 0xFFFFL
            val expiryLow = packed and 0xFFFFL
            val total = (expiryLow - startLow) and 0xFFFFL
            val elapsed = (curLow - startLow) and 0xFFFFL

            if (elapsed > total) {
                expiredBuf.add(entry.longKey)
            } else {
                // Re-queue a lighting check every tick while the spark is alive.
                // Without this, the engine may serve a cached value from before
                // the spark was registered, making the flash invisible.
                engine.checkBlock(BlockPos.of(entry.longKey))
            }
        }

        if (!expiredBuf.isEmpty) {
            for (i in expiredBuf.indices) {
                val key = expiredBuf.getLong(i)
                sparks.remove(key)
                active.remove(key)
                // Final check forces the engine to clear the cached emission value
                engine.checkBlock(BlockPos.of(key))
            }
            expiredBuf.clear()
        }
    }

    @JvmStatic
    fun clear() {
        sparks.clear()
        active.clear()
        expiredBuf.clear()
        currentTick = 0L
    }
}