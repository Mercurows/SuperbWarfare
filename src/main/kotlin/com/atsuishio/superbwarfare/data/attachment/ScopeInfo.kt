package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.ModColor
import com.atsuishio.superbwarfare.tools.MathTool
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.roundToInt

private const val DEFAULT_SCOPE_VIEW_BONE = "scope_view"
private const val DEFAULT_DIVISION_BONE = "division"
private const val SCOPE_BODY_NODE = "scope_body"
private const val OCULAR_NODE = "ocular"
private const val OCULAR_RING_NODE = "ocular_ring"

private const val AMMO_COLOR_DEFAULT_KEY = "Default"
private const val AMMO_COLOR_WHITE = -0x1
private const val OPAQUE_ALPHA = -0x1000000
private const val GRADIENT_MODE_HSV = 2
private const val AMMO_COUNT_PLACEHOLDER = "%ammo_count%"
private const val RANGE_PLACEHOLDER = "%range%"
private const val DEFAULT_TEXT_SCALE = 0.0625f

@Serializable
enum class ScopeType {
    @SerialName("Sight")
    SIGHT,

    @SerialName("Scope")
    SCOPE,
}

/** 弹药条骨骼被压缩的轴向，写成 `"NONE"` 表示只染色、不改缩放 */
@Serializable
enum class AmmoBarAxis {
    @SerialName("X")
    X,

    @SerialName("Y")
    Y,

    @SerialName("Z")
    Z,

    @SerialName("NONE")
    NONE,
}

/** 弹药条随弹药减少而变色的方式 */
@Serializable
enum class AmmoBarColorMode {
    /** 直接跳到剩余弹药所处的档位颜色 */
    @SerialName("Switch")
    SWITCH,

    /** 在相邻档位之间渐变（走 HSV，中间色不会发灰） */
    @SerialName("Blend")
    BLEND,
}

/**
 * [AmmoBarEntry] 与 [AmmoTextEntry] 共用的分级染色表
 *
 * [color] 的键是阈值（0~1），值是弹药降到该阈值后使用的颜色，可选的 `"Default"` 是阈值以上的颜色，
 * 一个颜色都没配就返回白色，等于不染色，颜色一律被 [ModColor] 强制不透明，写不出 alpha
 * 阈值在首次使用时解析并缓存，因为 [colorAt] 跑在渲染路径上
 */
private class AmmoColorTiers(
    private val mode: AmmoBarColorMode,
    private val color: Map<String, ModColor>,
    private val owner: String,
) {
    private val thresholds: List<Pair<Float, Int>> by lazy { parseThresholds() }

    private val fallbackColor: Int by lazy { parseFallbackColor() }

    /** 是否配了颜色，false 表示 [colorAt] 恒返回白色 */
    val isConfigured: Boolean get() = color.isNotEmpty()

    /** 按剩余弹药比例 [progress] 取色，返回不透明 ARGB */
    fun colorAt(progress: Float): Int {
        val tiers = thresholds
        if (tiers.isEmpty()) return fallbackColor

        val value = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 1f

        return when (mode) {
            // 仍不低于当前弹药的最低档，也就是已经跌到的最严重那一档
            AmmoBarColorMode.SWITCH -> tiers.firstOrNull { value <= it.first }?.second ?: fallbackColor

            AmmoBarColorMode.BLEND -> blendAt(tiers, value)
        }
    }

    /** 相邻档位之间渐变，最高档到满弹之间用默认色补齐 */
    private fun blendAt(tiers: List<Pair<Float, Int>>, value: Float): Int {
        val lowest = tiers.first()
        if (value <= lowest.first) return lowest.second

        val highest = tiers.last()
        if (value >= highest.first) {
            // 默认色锚在满弹处，最高阈值已经是 1 时没有可插值的区间，直接保持
            val span = 1f - highest.first
            if (span <= 0f) return highest.second
            return blend(highest.second, fallbackColor, (value - highest.first) / span)
        }

        // 阈值升序且 value 落在最低与最高之间，两个邻居一定都存在
        val upperIndex = tiers.indexOfFirst { it.first >= value }
        val (lowerThreshold, lowerColor) = tiers[upperIndex - 1]
        val (upperThreshold, upperColor) = tiers[upperIndex]

        val span = upperThreshold - lowerThreshold
        if (span <= 0f) return upperColor
        return blend(lowerColor, upperColor, (value - lowerThreshold) / span)
    }

    /** 解析阈值，跳过默认色与非法键 */
    private fun parseThresholds(): List<Pair<Float, Int>> {
        val parsed = ArrayList<Pair<Float, Int>>(color.size)
        for ((key, value) in color) {
            if (key.equals(AMMO_COLOR_DEFAULT_KEY, ignoreCase = true)) continue

            val threshold = key.toFloatOrNull()
            if (threshold == null || !threshold.isFinite() || threshold < 0f || threshold > 1f) {
                Mod.LOGGER.warn(
                    "Ignoring ammo color threshold '{}' on '{}': expected a number between 0 and 1",
                    key,
                    owner
                )
                continue
            }
            parsed += threshold to value.get()
        }
        return parsed.sortedBy { it.first }
    }

    private fun parseFallbackColor(): Int {
        for ((key, value) in color) {
            if (key.equals(AMMO_COLOR_DEFAULT_KEY, ignoreCase = true)) return value.get()
        }
        return AMMO_COLOR_WHITE
    }

    /**
     * 两个不透明 ARGB 颜色之间的 HSV 插值，与载具 HUD 的血量读数同一套做法
     *
     * 直接插 RGB 会在颜色立方体里走直线，中间色调掉饱和度、变成灰色，绕色相环走则保持鲜艳
     */
    private fun blend(start: Int, end: Int, t: Float): Int {
        // RGB → HSV → RGB 往返可能差一级，两端直接返回配置色
        if (t <= 0f) return start
        if (t >= 1f) return end

        // getGradientColor 收 RGB 和整数百分比进度，而 ModColor 的颜色带不透明 alpha，这里先摘掉再补回
        val step = (t.coerceIn(0f, 1f) * 100f).roundToInt()
        val blended = MathTool.getGradientColor(start and 0xFFFFFF, end and 0xFFFFFF, step, GRADIENT_MODE_HSV)
        return OPAQUE_ALPHA or blended
    }
}

/**
 * 一根用来显示剩余弹药的骨骼：按 [axis] 把骨骼压缩到"剩余弹药 / 弹匣容量"
 *
 * 缩放绕骨骼枢轴施加并传递给子骨骼，所以只列出真正要压缩的那根即可，比例从 1（满弹）到 0（空），
 * 超出弹匣容量的弹药也算 1，[color] 可以为骨骼染色（见 [AmmoColorTiers]），不配颜色就只做缩放
 */
@Serializable
data class AmmoBarEntry(
    @SerialName("Bone")
    val bone: String,

    @SerialName("Axis")
    val axis: AmmoBarAxis = AmmoBarAxis.NONE,

    @SerialName("ColorMode")
    val colorMode: AmmoBarColorMode = AmmoBarColorMode.SWITCH,

    @SerialName("Color")
    val color: Map<String, ModColor> = emptyMap(),
) {
    // 派生自上面两个属性，本身不参与序列化，也不参与 data class 的 equals/hashCode/copy
    @Transient
    private val tiers = AmmoColorTiers(colorMode, color, bone)

    /** 该骨骼是否需要从模型渲染里单独拿出来染色 */
    fun isTinted(): Boolean = tiers.isConfigured

    /** 按剩余弹药比例 [progress] 取色，返回不透明 ARGB，没配颜色则返回白色 */
    fun colorAt(progress: Float): Int = tiers.colorAt(progress)
}

/** 文字相对骨骼原点的水平对齐方式 */
@Serializable
enum class TextAlign {
    @SerialName("Left")
    LEFT,

    @SerialName("Center")
    CENTER,

    @SerialName("Right")
    RIGHT,
}

/**
 * 画在骨骼上的一行弹药文字，写法对齐 TACZ 的 `text_show`
 *
 * 骨骼只作为锚点（不需要自带方块），位置与朝向都取自它的全局变换，[scale] 是"每字体像素占多少模型单位"
 * [text] 里的 [AMMO_COUNT_PLACEHOLDER] 会换成当前弹匣数量、`%range%` 换成当前测距读数（单位：格，
 * 无有效读数时是 `---`），不含占位符就原样绘制（例如固定标签 `"AMMO"`）
 * 可见性由骨骼的祖先决定：挂在 `division*` 下时只随分划出现（即开镜时），挂在别处则随镜身常驻
 *
 * [color] 与 [colorMode] 的分级规则同 [AmmoBarEntry]，[align] 或颜色写错会让整份配件数据解析失败
 */
@Serializable
data class AmmoTextEntry(
    @SerialName("Bone")
    val bone: String,

    @SerialName("Scale")
    val scale: Float = DEFAULT_TEXT_SCALE,

    @SerialName("Align")
    val align: TextAlign = TextAlign.CENTER,

    @SerialName("ColorMode")
    val colorMode: AmmoBarColorMode = AmmoBarColorMode.SWITCH,

    @SerialName("Color")
    val color: Map<String, ModColor> = emptyMap(),

    @SerialName("Shadow")
    val shadow: Boolean = false,

    @SerialName("Text")
    val text: String = AMMO_COUNT_PLACEHOLDER,
) {
    @Transient
    private val tiers = AmmoColorTiers(colorMode, color, bone)

    /**
     * 这行文字要用的测距读数。
     *
     * 渲染路径靠它决定**要不要真去测一次** —— 测距是一条 512 格的射线，而绝大多数配件（以及
     * 同一把枪上的绝大多数文字）根本用不上，没有这一位就得每帧白跑一遍
     */
    @Transient
    val usesRange: Boolean = text.contains(RANGE_PLACEHOLDER)

    /** 按剩余弹药比例 [progress] 取色，返回不透明 ARGB，没配颜色则返回白色 */
    fun colorAt(progress: Float): Int = tiers.colorAt(progress)

    /** 把 [AMMO_COUNT_PLACEHOLDER] / `%range%` 换成实际数值 */
    fun resolve(count: Int, range: Int = NO_RANGE): String {
        val resolved = text.replace(AMMO_COUNT_PLACEHOLDER, count.toString())
        if (!usesRange) return resolved
        return resolved.replace(RANGE_PLACEHOLDER, if (range < 0) NO_RANGE_TEXT else range.toString())
    }

    /** 让 [width] 宽的文字按 [align] 对齐、原点仍留在骨骼上的水平偏移 */
    fun offsetX(width: Int): Float {
        return when (align) {
            TextAlign.LEFT -> 0f
            TextAlign.CENTER -> -width / 2f
            TextAlign.RIGHT -> -width.toFloat()
        }
    }

    companion object {
        /**
         * "这一帧没有有效测距读数"，[resolve] 遇到它就把 `%range%` 换成 [NO_RANGE_TEXT]。
         *
         * 用负数而不是 `0`：`0` 是一个合法的读数（贴脸），拿它当哨兵会让"没有读数"和"距离为零"
         * 长得一模一样。距离本身不可能为负，所以这是安全的。
         */
        const val NO_RANGE = -1

        /** 没有读数时画出来的东西，与 `SpyglassRangeOverlay` 超出量程时显示的 `---` 一致 */
        const val NO_RANGE_TEXT = "---"
    }
}

/**
 * 瞄准镜与机瞄的渲染配置
 *
 * 骨骼名沿用 [com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel] 的约定：
 * `scope_body`、`ocular_ring`、`ocular*`、`division*`，[modes] 让一个配件带多档光学，
 * 不做多档时用 [type] / [zoom] 这组单档字段即可
 *
 * 模板渲染会连同整棵子树隐藏 `scope_body*`、`ocular*`、`ocular_ring*` 和 `division*`，
 * 所以 [AttachmentDefinition.ammoBar] / [AttachmentDefinition.textShow] 的骨骼不要挂在这些节点下面
 */
@Serializable
data class ScopeInfo(
    @SerialName("Type")
    val type: ScopeType = ScopeType.SIGHT,

    @SerialName("ViewRadiusModifier")
    val viewRadiusModifier: Float = 1.0f,

    // 移动时的瞄准倍率，用于能够自动调整焦距的瞄准镜，设置成null相当于禁用该功能
    @SerialName("MovingZoom")
    val movingZoom: Double? = null,

    // 完全瞄准后枪械沿 Z 轴（长度方向）压缩到的比例，默认 0.75
    @SerialName("ZoomLengthScale")
    val zoomLengthScale: Float = 0.75f,

    /** 单档配置的倍率，写了 [modes] 时以各档自己的为准 */
    @SerialName("Zoom")
    val zoom: AttachmentZoom? = null,

    @SerialName("Modes")
    val modes: List<ScopeMode> = emptyList(),
) {
    fun mode(index: Int = 0): ScopeMode {
        if (modes.isNotEmpty()) {
            return modes[index.coerceIn(modes.indices)]
        }
        return ScopeMode(
            index = 0,
            type = type,
            viewRadiusModifier = viewRadiusModifier,
            zoomLengthScale = zoomLengthScale,
            zoom = zoom,
        )
    }

    fun modeCount(): Int = if (modes.isEmpty()) 1 else modes.size

    fun supportsModeSwitching(): Boolean = modes.size > 1
}

/** 多档瞄具里的一档，未声明的字段沿用 [ScopeInfo] 上的单档配置 */
@Serializable
data class ScopeMode(
    /** 骨骼组号，决定 `scope_view_<n>` / `division_<n>` 这类带编号的骨骼名 */
    @SerialName("Index")
    val index: Int = 0,

    @SerialName("Type")
    val type: ScopeType = ScopeType.SIGHT,

    @SerialName("ViewRadiusModifier")
    val viewRadiusModifier: Float = 1.0f,

    // 完全瞄准后枪械沿 Z 轴（长度方向）压缩到的比例，默认 0.75
    @SerialName("ZoomLengthScale")
    val zoomLengthScale: Float = 0.75f,

    @SerialName("Zoom")
    val zoom: AttachmentZoom? = null,
) {
    fun viewBone(): String = nameWithIndex(DEFAULT_SCOPE_VIEW_BONE)

    fun divisionBone(): String = nameWithIndex(DEFAULT_DIVISION_BONE)

    fun scopeBodyBone(): String = nameWithIndex(SCOPE_BODY_NODE)

    fun ocularBone(): String = nameWithIndex(OCULAR_NODE)

    fun ocularRingBone(): String = nameWithIndex(OCULAR_RING_NODE)

    private fun nameWithIndex(base: String): String {
        return if (index > 0) "${base}_$index" else base
    }

    fun isSight(): Boolean = type == ScopeType.SIGHT

    fun isScope(): Boolean = type == ScopeType.SCOPE
}
