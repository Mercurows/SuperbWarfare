package com.atsuishio.superbwarfare.client.map.context

import net.minecraft.nbt.CompoundTag
import java.util.*

/**
 * 战术地图上的位置标记点。
 *
 * @param id 唯一标识，用于区分不同的标记点
 * @param name 标记点名称（用户输入）
 * @param x 世界坐标 X（block 单位）
 * @param y 世界坐标 Y（创建时的地表高度）
 * @param z 世界坐标 Z（block 单位）
 * @param colorIndex 0-15，对应 Minecraft 16 种经典聊天颜色
 */
data class MapMarker(
    val id: UUID = UUID.randomUUID(),
    var name: String,
    var x: Int,
    var y: Int,
    var z: Int,
    var colorIndex: Int
) {
    /** 序列化为 NBT */
    fun toNBT(): CompoundTag = CompoundTag().apply {
        putString("id", id.toString())
        putString("name", name)
        putInt("x", x)
        putInt("y", y)
        putInt("z", z)
        putInt("colorIndex", colorIndex)
    }

    companion object {
        /** 16 种原版染料颜色（DyeColor）的 RGB 值 */
        val COLORS: IntArray = intArrayOf(
            0xF9FFFE, // 0  WHITE
            0xF9801D, // 1  ORANGE
            0xC74EBD, // 2  MAGENTA
            0x3AB3DA, // 3  LIGHT_BLUE
            0xFED83D, // 4  YELLOW
            0x80C71F, // 5  LIME
            0xF38BAA, // 6  PINK
            0x474F52, // 7  GRAY
            0x9D9D97, // 8  LIGHT_GRAY
            0x169C9C, // 9  CYAN
            0x8932B8, // 10 PURPLE
            0x3C44AA, // 11 BLUE
            0x835432, // 12 BROWN
            0x5E7C16, // 13 GREEN
            0xB02E26, // 14 RED
            0x1D1D21, // 15 BLACK
        )

        fun getColorRGB(colorIndex: Int): Int {
            return COLORS[colorIndex.coerceIn(0, 15)]
        }

        /** 将 ARGB int 拆分为 r, g, b 三元组 */
        fun rgbToFloat3(rgb: Int): Triple<Float, Float, Float> {
            return Triple(
                ((rgb shr 16) and 0xFF) / 255f,
                ((rgb shr 8) and 0xFF) / 255f,
                (rgb and 0xFF) / 255f
            )
        }

        /** 从 NBT 反序列化，失败返回 null */
        fun fromNBT(tag: CompoundTag): MapMarker? {
            val idStr = tag.getString("id")
            if (idStr.isEmpty()) return null
            return try {
                MapMarker(
                    id = UUID.fromString(idStr),
                    name = tag.getString("name"),
                    x = tag.getInt("x"),
                    y = tag.getInt("y"),
                    z = tag.getInt("z"),
                    colorIndex = tag.getInt("colorIndex")
                )
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
