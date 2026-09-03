package com.atsuishio.superbwarfare.resource.gun.pojo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AttachmentInfo {
    // 装备握把的时候是否需要渲染新的护木
    @JvmField
    @SerialName("GripHandGuard")
    var gripHandGuard: Boolean = false

    // 装备瞄准镜的时候是否需要渲染新的护木
    @JvmField
    @SerialName("ScopeHandGuard")
    var scopeHandGuard: Boolean = false

    // 装备瞄准镜的时候是否需要渲染桥架
    @JvmField
    @SerialName("ScopeMount")
    var scopeMount: Boolean = false
}