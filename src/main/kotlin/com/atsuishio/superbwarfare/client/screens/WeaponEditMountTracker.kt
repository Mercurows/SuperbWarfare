package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.client.screens.WeaponEditMountTracker.guiHeight
import com.atsuishio.superbwarfare.client.screens.WeaponEditMountTracker.guiWidth
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.tools.BoneScreenPoint
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

/**
 * 改装界面用：本帧各配件槽位挂点骨骼在 GUI 坐标下的屏幕位置。
 *
 * 位置只能由 [com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer] 在第一人称渲染
 * 枪械模型时写入 —— 那里才拿得到"模型几何原点"的 PoseStack 与当时的投影矩阵
 * （转换本身见 [com.atsuishio.superbwarfare.tools.BedrockBoneCoordinateTool]）。
 *
 * 写入与读取在同一帧完成：关卡渲染（枪在其中）先于 GUI 渲染（改装界面在其中）。
 * [guiWidth] / [guiHeight] 也一起记下来，界面据此把面板钳制在屏幕内。
 */
@OnlyIn(Dist.CLIENT)
object WeaponEditMountTracker {

    private var positions: Map<AttachmentType, BoneScreenPoint> = emptyMap()

    /** 弹种按钮的位置。弹种不是槽位，骨骼由渲染器按模型挑（`ammo_pos`，没有则退回 `magazine_pos`）。 */
    var ammoPosition: BoneScreenPoint? = null
        private set

    /** 瞄准镜叠装时下一件会装到的位置（顶层配件的 `scope_pos`）；顶层不可叠装时为 `null`。 */
    var scopeStackPosition: BoneScreenPoint? = null
        private set

    var guiWidth: Int = 0
        private set

    var guiHeight: Int = 0
        private set

    fun update(
        positions: Map<AttachmentType, BoneScreenPoint>,
        ammoPosition: BoneScreenPoint?,
        scopeStackPosition: BoneScreenPoint?,
        guiWidth: Int,
        guiHeight: Int,
    ) {
        this.positions = positions
        this.ammoPosition = ammoPosition
        this.scopeStackPosition = scopeStackPosition
        this.guiWidth = guiWidth
        this.guiHeight = guiHeight
    }

    /** [type] 槽位挂点骨骼的屏幕位置；骨骼不存在时为 `null`。 */
    fun positionOf(type: AttachmentType): BoneScreenPoint? = positions[type]

    /** 退出改装时清空，避免下次打开界面时先用到上一轮的残留位置。 */
    fun clear() {
        positions = emptyMap()
        ammoPosition = null
        scopeStackPosition = null
    }
}
