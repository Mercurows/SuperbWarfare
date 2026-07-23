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
 * High-performance registry for client-side dynamic block light sources.
 *
 * Each light source is stored as a packed long containing:
 * maxLevel[8] | minLevel[8] | startTick[16] | expiryTick[16]
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
    private val random = Random()

    /**
     * Registers or refreshes a dynamic light source.
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

        val packed = (clampedMax.toLong() shl 48) or
                (clampedMin.toLong() shl 32) or
                ((currentTick and 0xFFFFL) shl 16) or
                (expiry and 0xFFFFL)

        sparks.put(packedPos, packed)
        active.add(packedPos)
    }

    /**
     * Returns the current computed light level for a packed position.
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

        return (base + random.nextInt(3) - 1).coerceIn(1, 15)
    }

    @JvmStatic
    fun activeIterator(): LongIterator = active.iterator()

    @JvmStatic
    fun isEmpty(): Boolean = active.isEmpty()

    /**
     * Advances the internal tick counter and removes expired sources.
     * Forcefully updates the lighting engine on expired block positions to prevent ghost lighting.
     */
    @JvmStatic
    fun tick() {
        currentTick++
        if (sparks.isEmpty()) return

        expiredBuf.clear()
        val curLow = currentTick and 0xFFFFL

        for (entry in sparks.long2LongEntrySet()) {
            val packed = entry.longValue
            val startLow = (packed ushr 16) and 0xFFFFL
            val expiryLow = packed and 0xFFFFL
            val total = (expiryLow - startLow) and 0xFFFFL
            val elapsed = (curLow - startLow) and 0xFFFFL
            if (elapsed > total) expiredBuf.add(entry.longKey)
        }

        if (!expiredBuf.isEmpty) {
            val level = Minecraft.getInstance().level
            val engine = level?.lightEngine
            for (i in expiredBuf.indices) {
                val key = expiredBuf.getLong(i)
                sparks.remove(key)
                active.remove(key)
                // Force a final lighting recalculation on the expired block to clean up the dynamic light
                engine?.checkBlock(BlockPos.of(key))
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