package com.atsuishio.superbwarfare.client.map.context

import com.atsuishio.superbwarfare.client.map.TacticalMapCache
import net.minecraft.client.Minecraft
import java.io.File
import java.util.*

/**
 * 战术地图标记点的持久化读写。
 * 纯函数，无状态 —— 标记数据（markers 列表 / connections 映射）由 Screen 持有。
 */
object MarkerPersistence {

    private fun getMarkerDir(minecraft: Minecraft): File {
        val worldId = TacticalMapCache.getWorldIdentifier()
        val dim = (minecraft.level?.dimension()?.location()?.toString() ?: "unknown").replace(":", "_")
        val dir = File(minecraft.gameDirectory, "superbwarfare/tactical_markers/$worldId/$dim")
        dir.mkdirs()
        return dir
    }

    private fun markerFile(minecraft: Minecraft, uuid: UUID): File =
        File(getMarkerDir(minecraft), "$uuid.txt")

    fun loadMarkers(
        minecraft: Minecraft,
        markers: MutableList<MapMarker>,
        connections: MutableMap<UUID, MutableSet<UUID>>
    ) {
        val dir = getMarkerDir(minecraft)
        val files = dir.listFiles() ?: return
        markers.clear()
        for (file in files) {
            if (!file.isFile || !file.name.endsWith(".txt")) continue
            try {
                val uuid = UUID.fromString(file.nameWithoutExtension)
                val p = file.readText().trim().split("|", limit = 6)
                if (p.size >= 5) {
                    markers.add(
                        MapMarker(
                            id = uuid,
                            name = p[0],
                            x = p[1].toInt(),
                            y = p[2].toInt(),
                            z = p[3].toInt(),
                            colorIndex = p[4].toInt()
                        )
                    )
                    if (p.size >= 6 && p[5].isNotEmpty()) {
                        connections[uuid] = p[5].split(",").mapNotNullTo(mutableSetOf()) { s ->
                            try {
                                UUID.fromString(s)
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun saveMarker(
        minecraft: Minecraft,
        marker: MapMarker,
        connections: Map<UUID, Set<UUID>>
    ) {
        try {
            val conns = connections[marker.id]?.joinToString(",") ?: ""
            val content = "${marker.name}|${marker.x}|${marker.y}|${marker.z}|${marker.colorIndex}|$conns"
            val newFile = markerFile(minecraft, marker.id)
            val tmp = File(newFile.parentFile, "${newFile.name}.tmp")
            newFile.delete()
            tmp.writeText(content)
            tmp.renameTo(newFile)
        } catch (_: Exception) {
        }
    }

    fun deleteMarkerFile(minecraft: Minecraft, marker: MapMarker) {
        try {
            markerFile(minecraft, marker.id).delete()
        } catch (_: Exception) {
        }
    }
}
