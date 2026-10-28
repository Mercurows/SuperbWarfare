package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.config.server.GunConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType

/** 改装界面与服务端共用的实例调整规则；不改变配件定义中的基础 Rotation。 */
object AttachmentAdjustments {
    const val MIN_ROTATION = -180.0
    const val MAX_ROTATION = 180.0
    const val ROTATION_STEP = 1.0
    const val OFFSET_STEP = 0.1

    // 完全自由模式里原本不能偏移的配件使用此范围；导轨槽始终读取 GunProp.RAIL_INFO。
    const val MIN_OFFSET = -10.0
    const val MAX_OFFSET = 10.0

    fun canRotate(gun: GunData, type: AttachmentType): Boolean {
        if (type == AttachmentType.MAGAZINE || type == AttachmentType.CHARM) return false
        if (GunConfig.fullyFreeAttachmentMode) return true
        return type == AttachmentType.MUZZLE &&
            gun.attachment.id(type)?.let(AttachmentDefinition::from)?.canRotate == true
    }

    fun canOffset(type: AttachmentType): Boolean = type != AttachmentType.MAGAZINE &&
        (GunConfig.fullyFreeAttachmentMode || when (type) {
            AttachmentType.SCOPE, AttachmentType.GRIP, AttachmentType.UPPER_RAIL,
            AttachmentType.LOWER_RAIL, AttachmentType.LEFT_RAIL, AttachmentType.RIGHT_RAIL -> true
            else -> false
        })

    fun offsetRange(gun: GunData, type: AttachmentType): ClosedFloatingPointRange<Double> =
        gun.get(GunProp.RAIL_INFO).offsetRange(type) ?: MIN_OFFSET..MAX_OFFSET
}
