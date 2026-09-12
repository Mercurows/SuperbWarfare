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

/** Color map key holding the tint used above every threshold. */
private const val AMMO_COLOR_DEFAULT_KEY = "Default"

/** Opaque white, which leaves a bone looking exactly as it does untinted. */
private const val AMMO_COLOR_WHITE = -0x1

/** Alpha bits that [ModColor] forces onto every color it parses. */
private const val OPAQUE_ALPHA = -0x1000000

/** `MathTool.getGradientColor` mode selecting the HSV interpolation over the HSL one. */
private const val GRADIENT_MODE_HSV = 2

/** Placeholder in [AmmoTextEntry.text] replaced with the current magazine count. */
private const val AMMO_COUNT_PLACEHOLDER = "%ammo_count%"

/**
 * Model units per font pixel used when [AmmoTextEntry.scale] is not written.
 * One model unit is one BlockBench unit, i.e. 1/16 of a block, so an 8px tall digit comes out at
 * half a unit — the same ratio TACZ ships for its `ammo_count_text` bones.
 */
private const val DEFAULT_TEXT_SCALE = 0.0625f

@Serializable
enum class ScopeType {
    @SerialName("Sight")
    SIGHT,

    @SerialName("Scope")
    SCOPE,
}

/**
 * Axis an ammo bar bone is squashed along, written as `"X"`, `"Y"`, `"Z"` or `"NONE"` in JSON.
 *
 * `NONE` squashes nothing: the bone keeps the scale the model gives it, which is what an entry that
 * only wants a tint — or that has its own squash baked into the animation — is written as.
 */
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

/**
 * How an ammo bar picks a color as the magazine drains.
 */
@Serializable
enum class AmmoBarColorMode {
    /** Snaps to the color of the lowest threshold the remaining ammo has dropped to. */
    @SerialName("Switch")
    SWITCH,

    /**
     * Interpolates between adjacent thresholds instead of snapping, travelling through HSV so the
     * colors in between stay vivid rather than washing out to grey.
     */
    @SerialName("Blend")
    BLEND,
}

/**
 * The tiered color mechanism shared by [AmmoBarEntry] and [AmmoTextEntry].
 *
 * Each key of [color] is a threshold (`0`-`1`, e.g. `"0.25"`) and each value the color used once the
 * remaining ammo reaches it, while the optional `"Default"` key is the color above every threshold.
 * A configuration with no colors at all resolves to white, which leaves the target looking exactly as
 * it does untinted — so omitting both `Color` and `ColorMode` is what gives a white readout.
 *
 * Two pitfalls inherited from [ModColor], which is what parses the values: a color is always forced
 * opaque, so alpha cannot be expressed here; and because
 * [com.atsuishio.superbwarfare.data.ModColorSerializer] throws on a value it cannot parse, a single
 * misspelled color discards the whole attachment definition rather than just the tint.
 * `COLOR_PATTERN` only reads the last six hex digits, so an eight digit `"80FF0000"` silently
 * becomes `FF0000`.
 *
 * [mode] is just as unforgiving: [com.atsuishio.superbwarfare.data.DataLoader] does not set
 * `coerceInputValues`, so a value that is not exactly `"Switch"` or `"Blend"` fails the same way.
 *
 * The thresholds are parsed once, on first use, rather than inside [colorAt], because [colorAt] runs
 * on the render path. [owner] only names the offender in a warning.
 */
private class AmmoColorTiers(
    private val mode: AmmoBarColorMode,
    private val color: Map<String, ModColor>,
    private val owner: String,
) {
    private val thresholds: List<Pair<Float, Int>> by lazy { parseThresholds() }

    private val fallbackColor: Int by lazy { parseFallbackColor() }

    /** Whether anything was configured at all. False means [colorAt] always returns white. */
    val isConfigured: Boolean get() = color.isNotEmpty()

    /**
     * Resolves the tint for a remaining-ammo ratio of [progress] as an opaque ARGB value.
     * Configurations without a usable color resolve to white, which leaves the target as-is.
     */
    fun colorAt(progress: Float): Int {
        val tiers = thresholds
        if (tiers.isEmpty()) return fallbackColor

        val value = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 1f

        return when (mode) {
            // The lowest threshold still at or above the progress, i.e. the most severe tier reached
            AmmoBarColorMode.SWITCH -> tiers.firstOrNull { value <= it.first }?.second ?: fallbackColor

            AmmoBarColorMode.BLEND -> blendAt(tiers, value)
        }
    }

    private fun blendAt(tiers: List<Pair<Float, Int>>, value: Float): Int {
        val lowest = tiers.first()
        if (value <= lowest.first) return lowest.second

        val highest = tiers.last()
        if (value >= highest.first) {
            // The fallback color is anchored at a full magazine; a threshold already at 1 leaves no
            // room to interpolate into, so it just holds.
            val span = 1f - highest.first
            if (span <= 0f) return highest.second
            return blend(highest.second, fallbackColor, (value - highest.first) / span)
        }

        // Ascending order and a value strictly between the lowest and highest threshold guarantee
        // that both neighbours exist.
        val upperIndex = tiers.indexOfFirst { it.first >= value }
        val (lowerThreshold, lowerColor) = tiers[upperIndex - 1]
        val (upperThreshold, upperColor) = tiers[upperIndex]

        val span = upperThreshold - lowerThreshold
        if (span <= 0f) return upperColor
        return blend(lowerColor, upperColor, (value - lowerThreshold) / span)
    }

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
     * Blends two opaque ARGB colors through HSV, the same interpolation the vehicle HUDs use for
     * their health readout.
     *
     * Interpolating the RGB channels directly would run a straight line through the inside of the
     * color cube, which drains the saturation out of every mid tone and leaves it grey. Travelling
     * around the hue wheel instead keeps the intermediate colors vivid.
     */
    private fun blend(start: Int, end: Int, t: Float): Int {
        // The RGB -> HSV -> RGB round trip can land a level off, so the two ends are pinned to the
        // configured colors instead of being approached through the interpolation.
        if (t <= 0f) return start
        if (t >= 1f) return end

        // getGradientColor takes RGB and a whole-percent progress, while ModColor always forces the
        // alpha opaque, so the alpha bits are masked off here and put back afterwards. A percent is
        // 2.55 levels per channel, well below what the texture shading can show.
        val step = (t.coerceIn(0f, 1f) * 100f).roundToInt()
        val blended = MathTool.getGradientColor(start and 0xFFFFFF, end and 0xFFFFFF, step, GRADIENT_MODE_HSV)
        return OPAQUE_ALPHA or blended
    }
}

/**
 * One bone whose declared axis is squashed to display the remaining magazine ammo.
 *
 * The scale is applied around the bone's pivot and propagates to its children, so only the
 * bone that should actually be compressed needs to be listed. The value always runs from
 * `1` (full magazine) down to `0` (empty); ammo beyond the magazine capacity also counts as `1`.
 *
 * [color] optionally tints the bone and [colorMode] chooses between snapping to a threshold and
 * interpolating between them; see [AmmoColorTiers] for the map format and its pitfalls. Leaving
 * [color] empty keeps the bone in the normal model pass, drawn untinted.
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
    // Transient because kotlinx.serialization would otherwise try to find a serializer for it; this
    // class is derived from the two properties above, which are already serialized. A body property
    // never takes part in a data class's generated equals/hashCode/copy either way.
    @Transient
    private val tiers = AmmoColorTiers(colorMode, color, bone)

    /** Whether this bone should be pulled out of the model pass so it can be drawn with a tint. */
    fun isTinted(): Boolean = tiers.isConfigured

    /**
     * Resolves the tint for a remaining-ammo ratio of [progress] as an opaque ARGB value.
     * Entries without a usable color configuration resolve to white, which leaves the bone as-is.
     */
    fun colorAt(progress: Float): Int = tiers.colorAt(progress)
}

/**
 * Horizontal alignment of an [AmmoTextEntry] relative to its bone's origin.
 */
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
 * One line of text drawn at a bone, mirroring TACZ's `text_show` display block.
 *
 * The bone is used purely as an anchor: it needs no cubes of its own, and its global transform
 * supplies both the position and the orientation of the text. [scale] is expressed in model units
 * per font pixel (see [DEFAULT_TEXT_SCALE]), so lowering it shrinks the glyphs.
 *
 * [text] is a template where [AMMO_COUNT_PLACEHOLDER] expands to the current magazine count;
 * a string without the placeholder is drawn literally, which is how a static label like `"AMMO"`
 * is written.
 *
 * Where the text ends up being visible is decided entirely by the bone's ancestry, because a hidden
 * bone drops its whole subtree:
 * - Under a `division*` root, the bone is hidden along with the reticle and only comes back where
 *   the reticle is drawn separately, i.e. while aiming down the sights.
 * - Anywhere else it renders with the scope body in every context.
 *
 * [color] and [colorMode] use the same tiered scheme as [AmmoBarEntry], so the count can change color
 * as the magazine drains; see [AmmoColorTiers] for the map format and its pitfalls. Both default to
 * white, and leaving both out is what a plain white readout is written as.
 *
 * [align] is unforgiving in the same way the color map is, since
 * [com.atsuishio.superbwarfare.data.DataLoader] does not set `coerceInputValues` and an unknown enum
 * value fails the same way: the whole definition is discarded, not just the text.
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
     * Resolves the text color for a remaining-ammo ratio of [progress] as an opaque ARGB value.
     * Entries without a usable color configuration resolve to white.
     */
    fun colorAt(progress: Float): Int = tiers.colorAt(progress)

    /** Expands [AMMO_COUNT_PLACEHOLDER] in [text]; a template without it is returned unchanged. */
    fun resolve(count: Int): String = text.replace(AMMO_COUNT_PLACEHOLDER, count.toString())

    /**
     * Horizontal offset in font pixels that puts [width] worth of glyphs in the configured
     * alignment, with the origin staying at the bone.
     */
    fun offsetX(width: Int): Float {
        return when (align) {
            TextAlign.LEFT -> 0f
            TextAlign.CENTER -> -width / 2f
            TextAlign.RIGHT -> -width.toFloat()
        }
    }
}

/**
 * Scope and sight rendering configuration.
 *
 * Bone names follow a fixed convention in [com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel]:
 * `scope_body`, `ocular_ring`, `ocular*`, and `division*`.
 * `modes` lets a single attachment expose multiple selectable optics without changing existing single-mode JSON.
 * Modes may point to different `scope_view`/`division` groups while sharing the ocular geometry.
 *
 * Ammo bar bones are configured freely through [ammoBar], but they must stay visible whenever the
 * scope renders: the stencil pass hides `scope_body*`, `ocular*`, `ocular_ring*` and `division*`,
 * and a hidden bone skips its entire subtree. Bones parented under those nodes therefore disappear
 * while aiming down the sights even though they render correctly in every other context.
 *
 * Text anchors in [textShow] follow the same rule, with one nuance: a `division*` anchor is only
 * meaningful while aiming, because that is the only time the reticle subtree is drawn at all.
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

    @SerialName("Modes")
    val modes: List<ScopeMode> = emptyList(),

    // 参与弹药显示的骨骼，空列表表示不启用；骨骼名可自由书写，多模式镜可用 ammo_bar_1 等显式区分
    @SerialName("AmmoBar")
    val ammoBar: List<AmmoBarEntry> = emptyList(),

    // 显示当前弹药数的文字锚点，空列表表示不启用；写法对齐 TACZ 的 text_show
    @SerialName("TextShow")
    val textShow: List<AmmoTextEntry> = emptyList(),
) {
    fun mode(index: Int = 0): ScopeMode {
        if (modes.isNotEmpty()) {
            return modes[index.coerceIn(modes.indices)]
        }
        return ScopeMode(index = 0, type = type, viewRadiusModifier = viewRadiusModifier, zoomLengthScale = zoomLengthScale)
    }

    fun modeCount(): Int = if (modes.isEmpty()) 1 else modes.size

    fun supportsModeSwitching(): Boolean = modes.size > 1

    fun isSight(): Boolean = mode().type == ScopeType.SIGHT

    fun isScope(): Boolean = mode().type == ScopeType.SCOPE
}

/**
 * One selectable optic inside a multi-mode scope attachment.
 */
@Serializable
data class ScopeMode(
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
