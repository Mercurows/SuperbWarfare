package com.atsuishio.superbwarfare.client.map

/**
 * 战术地图坐标转换工具。
 * 纯函数，无状态 —— 在 Screen 中替代所有内联 world↔screen 坐标计算。
 */
object CoordinateConverter {

    /** 将缩放级别 (zoom) 转换为地图比例尺 (pixels per block)。等价于 zoom / 5.0。 */
    fun scaleFromZoom(zoom: Double): Double = zoom / 5.0

    /** 世界 X → 屏幕 X */
    fun worldToScreenX(worldX: Double, mapCenterX: Float, viewBlockX: Double, scale: Double): Double =
        mapCenterX + (worldX - viewBlockX) * scale

    /** 世界 Z → 屏幕 Y（注意：Z 映射到 Y 轴） */
    fun worldToScreenY(worldZ: Double, mapCenterY: Float, viewBlockZ: Double, scale: Double): Double =
        mapCenterY + (worldZ - viewBlockZ) * scale

    /** 屏幕 X → 世界 X */
    fun screenToWorldX(screenX: Double, mapCenterX: Float, viewBlockX: Double, scale: Double): Double =
        viewBlockX + (screenX - mapCenterX) / scale

    /** 屏幕 Y → 世界 Z */
    fun screenToWorldY(screenY: Double, mapCenterY: Float, viewBlockZ: Double, scale: Double): Double =
        viewBlockZ + (screenY - mapCenterY) / scale

    /** 将屏幕坐标夹到地图可视区域内，返回 (clampedX, clampedY)。 */
    fun clampToMapArea(
        sx: Double, sy: Double,
        mapLeft: Int, mapTop: Int, mapAreaW: Int, mapAreaH: Int,
        inset: Int = 4
    ): Pair<Float, Float> {
        val cx = sx.coerceIn((mapLeft + inset).toDouble(), (mapLeft + mapAreaW - inset).toDouble()).toFloat()
        val cy = sy.coerceIn((mapTop + inset).toDouble(), (mapTop + mapAreaH - inset).toDouble()).toFloat()
        return cx to cy
    }
}
