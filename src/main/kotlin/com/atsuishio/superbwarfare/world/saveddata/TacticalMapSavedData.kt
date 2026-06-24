package com.atsuishio.superbwarfare.world.saveddata

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.saveddata.SavedData

/**
 * 战术地图持久化数据。
 *
 * 存储每个维度下已渲染的 chunk 颜色 + 高度数据（压缩后的字节），
 * 由 [com.atsuishio.superbwarfare.client.map.TacticalMapCache] 在客户端（单人集成服务器）
 * 读写，替代不安全的原始文件 IO。
 *
 * NBT 结构:
 *   Dimensions (ListTag<CompoundTag>)
 *     └ Dimension (String): e.g. "minecraft:overworld"
 *         Chunks (ListTag<CompoundTag>)
 *           └ Key (Long): chunkPosKey
 *              Data (ByteArray): Deflater 压缩后的 1536 字节 chunk 数据
 */
class TacticalMapSavedData : SavedData() {

    /** dimension → (chunkKey → compressed data) */
    val data: MutableMap<String, MutableMap<Long, ByteArray>> = linkedMapOf()

    fun putChunk(dimension: String, chunkKey: Long, compressed: ByteArray) {
        data.getOrPut(dimension) { linkedMapOf() }[chunkKey] = compressed
        setDirty()
    }

    fun getChunk(dimension: String, chunkKey: Long): ByteArray? {
        return data[dimension]?.get(chunkKey)
    }

    fun getChunksForDimension(dimension: String): Map<Long, ByteArray> {
        return data[dimension] ?: emptyMap()
    }

    fun clearDimension(dimension: String) {
        data.remove(dimension)
        setDirty()
    }

    fun clearAll() {
        data.clear()
        setDirty()
    }

    override fun save(tag: CompoundTag): CompoundTag {
        val dims = ListTag()
        for ((dim, chunks) in data) {
            val dimTag = CompoundTag()
            dimTag.putString("Dimension", dim)
            val chunkList = ListTag()
            for ((key, bytes) in chunks) {
                val entry = CompoundTag()
                entry.putLong("Key", key)
                entry.putByteArray("Data", bytes)
                chunkList.add(entry)
            }
            dimTag.put("Chunks", chunkList)
            dims.add(dimTag)
        }
        tag.put("Dimensions", dims)
        return tag
    }

    companion object {
        const val FILE_ID: String = "superbwarfare_tactical_map"

        fun load(tag: CompoundTag): TacticalMapSavedData {
            val savedData = TacticalMapSavedData()
            if (tag.contains("Dimensions", Tag.TAG_LIST.toInt())) {
                val dims = tag.getList("Dimensions", Tag.TAG_COMPOUND.toInt())
                for (i in dims.indices) {
                    val dimTag = dims.getCompound(i)
                    val dim = dimTag.getString("Dimension")
                    val chunkMap: MutableMap<Long, ByteArray> = linkedMapOf()
                    val chunks = dimTag.getList("Chunks", Tag.TAG_COMPOUND.toInt())
                    for (j in chunks.indices) {
                        val entry = chunks.getCompound(j)
                        chunkMap[entry.getLong("Key")] = entry.getByteArray("Data")
                    }
                    savedData.data[dim] = chunkMap
                }
            }
            return savedData
        }
    }
}
