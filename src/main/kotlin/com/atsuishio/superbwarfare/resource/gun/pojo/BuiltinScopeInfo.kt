package com.atsuishio.superbwarfare.resource.gun.pojo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 「枪自带的瞄具」开关与手感参数。
 *
 * 写在**枪的 assets 侧 json**（`assets/superbwarfare/sbw/guns/<id>.json`）里，与服务端无关：
 * 模板（stencil）窗口纯属客户端渲染，服务端连这份资源都加载不到。
 *
 * 判定完全靠**骨骼名**，与配件瞄准镜同一套约定（见 `ScopeMode` 的 `ocularBone()` / `divisionBone()`）：
 * geo 里只要有 `ocular`（或 `ocular_sight` / `ocular_scope`）骨骼，这把枪就自动获得镜筒窗口；
 * 有 `ocular_ring` 就框一圈镜圈；有 `division` 就在窗口里画准星。
 * 换句话说 [BuiltinScopeInfo] 只负责"**开不开、多大、拉多近**"这三个旋钮，形状全在 geo 里。
 *
 * **不复用 `ScopeInfo` / `ScopeMode`**：那是配件的 schema，带模式切换（`Modes`）、弹药读数
 * （`AmmoBar` / `TextShow`）与 `Zoom`，内置瞄具一个都用不上 —— 枪自己已有 `AmmoBar` / `TextShow`，
 * 而放大倍率来自数据侧的 `GunProp.DefaultZoom`（`ScopeMode.Zoom` 在这里会是永不生效的死字段），
 * 写进来只会误导后来者。
 *
 * ## 尺寸是怎么定的（动任何一个之前先读这段）
 *
 * - **窗口**的角半径 = `atan(80 × ViewRadiusModifier / 90)`，1.0 时约 **41.6°**。
 * - **准星**的角尺寸**不在这个文件里**，由 geo 里 `division` 那块板决定：贴图铺满整块板面，
 *   所以准星在贴图里占多大比例、看上去就有多大。板放得越远、或 UV 区开得越大，准星就越小。
 *   仓库里现成的 7 个瞄准镜板角半径为 24.5°–60.6°，且准星都铺满整块 UV —— 换板尺寸时照这个量级走。
 * - 板**不必**盖住窗口。板外没有像素，窗口那一圈照旧透出世界（`scope_pso_1` 就是板比窗口小）。
 *   也就是说 [viewRadiusModifier] 调大只会放大窗口，不会连带放大准星。
 */
@Serializable
class BuiltinScopeInfo {
    /**
     * 圆窗半径倍率。窗口半径 = `80 × 本值 × 开镜进度`，1.0 就是配件瞄准镜当前的配方。
     * 窗口偏大/偏小改这里，**不要**去动渲染器里那个 80。
     */
    @JvmField
    @SerialName("ViewRadiusModifier")
    var viewRadiusModifier: Float = 1.0f

    /**
     * 开镜时 Z 轴压缩量（把整把枪往眼前拉），与 `ScopeMode.zoomLengthScale` 同一个语义。
     *
     * 默认 `0.75` 正是 `GeoGunRenderer` 里原先的硬编码兜底值 —— 也就是说**声明它等于什么都不改**；
     * 想让镜筒更贴近眼睛（像 `scope_ranger` / `scope_sniper` 的 0.3）再往小调。
     */
    @JvmField
    @SerialName("ZoomLengthScale")
    var zoomLengthScale: Float = 0.75f
}
