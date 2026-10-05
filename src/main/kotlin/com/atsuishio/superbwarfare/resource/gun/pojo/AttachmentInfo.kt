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

    /**
     * **四面导轨（下 / 上 / 左 / 右）任一件**或**下挂副武器**装了东西时，是否换成带导轨的护木
     * （`custom_hand_guard` / `oem_hand_guard`，见 `GeoGunRenderer.shouldShowCustomHandGuard`）。
     *
     * 与 [gripHandGuard] 分开，是因为有的枪**握把并不长在那支护木上**（Vector）：靠"装握把"永远
     * 换不出它，只能看导轨上有没有挂东西。两个开关互不影响，都设成 true 就是"握把或导轨任一个都换"。
     */
    @JvmField
    @SerialName("RailHandGuard")
    var railHandGuard: Boolean = false

    // 装备瞄准镜的时候是否需要渲染新的护木
    @JvmField
    @SerialName("ScopeHandGuard")
    var scopeHandGuard: Boolean = false

    // 装备瞄准镜的时候是否需要渲染桥架
    @JvmField
    @SerialName("ScopeMount")
    var scopeMount: Boolean = false
}