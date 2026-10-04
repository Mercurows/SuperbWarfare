package com.atsuishio.superbwarfare.resource.gun.pojo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AttachmentInfo {
    /**
     * 导轨（前护木）被占用时是否需要渲染带导轨的那一支护木，并隐藏原厂护木
     * （`custom_hand_guard` / `oem_hand_guard`，见 `GeoGunRenderer.renderGripHandGuard`）。
     *
     * "占用导轨"指装了**握把**、**下导轨配件**或**下挂副武器**中的任一个 —— 三者都挂在这一段导轨上，
     * 所以共用这一个开关：它问的是"这把枪有没有带导轨的护木可选"，与装的是哪一种导轨件无关。
     *
     * 字段名是历史遗留（最初只有握把会换护木），没有跟着改名是为了不动现有 json。
     */
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