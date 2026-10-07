package com.atsuishio.superbwarfare.client.tooltip

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.tooltip.component.AttachmentImageComponent
import com.atsuishio.superbwarfare.data.attachment.*
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.item.attachment.AttachmentProvider
import com.atsuishio.superbwarfare.item.attachment.definition
import com.atsuishio.superbwarfare.tools.FormatTool
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.math.abs

open class ClientAttachmentImageTooltip(tooltip: AttachmentImageComponent) : ClientTooltipComponent {
    private val lines = buildLines(tooltip.stack)

    override fun renderImage(font: Font, x: Int, y: Int, guiGraphics: GuiGraphics) {
        guiGraphics.pose().pushPose()
        lines.forEachIndexed { index, line ->
            if (line.string.isNotEmpty()) {
                guiGraphics.drawString(font, line, x, y + index * LINE_HEIGHT, 0xFFFFFF)
            }
        }
        guiGraphics.pose().popPose()
    }

    override fun getHeight(): Int = lines.size * LINE_HEIGHT

    override fun getWidth(font: Font): Int {
        return lines.maxOfOrNull { font.width(it.visualOrderText) } ?: 0
    }

    open fun buildLines(stack: ItemStack): List<Component> {
        val definition = (stack.item as? AttachmentProvider)?.definition() ?: return emptyList()

        return buildList {
            val attachmentId = definition.getId()
            val namespace = attachmentId.substringBefore(':', Mod.MODID)
            val path = attachmentId.substringAfter(':', attachmentId)
            val descriptionKey = "des.$namespace.$path"
            if (I18n.exists(descriptionKey)) {
                add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY))
            }

            add(slotLine(definition))
            additionalSlotsLine(definition)?.let(::add)
            addAll(scopeLines(definition))
            definition.modifiers.mapNotNullTo(this) { modifierLine(it) }
            definition.override?.forEach { (key, value) ->
                add(overrideLine(key, value))
            }

            if (definition.isSilenced) {
                add(propertyComponent("silenced").withStyle(ChatFormatting.GREEN))
            }

            if (definition.hasBipod) {
                add(propertyComponent("bipod").withStyle(ChatFormatting.GREEN))
            }

            // 刺刀并入枪口槽后，槽位名与普通枪口配件相同（`[枪口配件]`），
            // 靠这一行把"枪刺功能"标出来
            if (definition.isBayonet) {
                add(propertyComponent("bayonet").withStyle(ChatFormatting.GREEN))
            }

            soundRadiusLine(definition.soundRadiusMultiplier)?.let(::add)
            muzzleFlashLine(definition.muzzleFlashScale)?.let(::add)
        }
    }

    /**
     * 槽位名那一行：**只报主槽位**（[AttachmentDefinition.slot]），也就是这件配件"定义在哪个槽位上"。
     *
     * 这是玩家判断"这东西是干什么用的"的依据（`Bone` 没写时的挂载骨骼、`ScopeInfo` 的取值下标都取自它），
     * 所以它单独占一行、用亮金色标出来，额外能装的槽位另起一行见 [additionalSlotsLine]。
     */
    open fun slotLine(definition: AttachmentDefinition): MutableComponent =
        slotName(definition.slot).withStyle(ChatFormatting.GOLD)

    /**
     * "也可以作为 [XX配件] 使用"那一行，只在配件声明了 `ExtraSlots` 时出现。
     *
     * 槽位名走 **`%1$s` 占位符**传进去，不拼在标签后面：不同语言里这一句的语序不一样
     * （中文"也可以作为 X 使用"、英文"Can also be used as X"），把列表交给翻译自己摆，
     * 翻译才写得对；拼在后面就等于锁死了标签必须在前的语序。
     *
     * `ExtraSlots` 是"顺便也能装"的槽位，不是平级关系，所以这一行比主槽位那行弱：标签用灰色、
     * 槽位名用暗一档的金色。不列出来的话，玩家看到 `[上导轨配件]` 会以为它装不上别的导轨。
     */
    open fun additionalSlotsLine(definition: AttachmentDefinition): Component? {
        val extra = definition.extraSlots
        if (extra.isEmpty()) return null

        // 先拼成一整个 Component，再作为 `%1$s` 传进译文，这样槽位名的颜色能跟着参数一起走
        val names = Component.empty()
        extra.sortedBy { it.ordinal }.forEachIndexed { index, type ->
            if (index > 0) names.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
            names.append(slotName(type).withStyle { it.withColor(ADDITIONAL_SLOT_COLOR) })
        }

        return Component.translatable("attachment.superbwarfare.additional_slots", names)
            .withStyle(ChatFormatting.GRAY)
    }

    /** 单个槽位的显示名，例如 `[Scope Attachment]` / `[瞄准镜配件]`；没有翻译时退回枚举名 */
    open fun slotName(type: AttachmentType): MutableComponent {
        val key = "attachment.superbwarfare.slot.${type.attachmentName.lowercase(Locale.ROOT)}"
        return if (I18n.exists(key)) {
            Component.translatable(key)
        } else {
            Component.literal("[${type.attachmentName}]")
        }
    }

    open fun modifierLine(modifier: AttachmentModifier): Component? {
        if (modifier.op == AttachmentModifierOp.SET || modifier.value is AttachmentModifierValue.Reference) {
            return modifiedPropertyComponent(modifier.prop)
        }

        val suffix = modifierSuffix(modifier) ?: return null
        val style = effectStyle(modifier)
        return propertyComponent(modifier.prop)
            .append(Component.literal(" $suffix"))
            .withStyle(style)
    }

    open fun modifierSuffix(modifier: AttachmentModifier): String? {
        val constant = modifier.value as? AttachmentModifierValue.Constant

        return when (modifier.op) {
            AttachmentModifierOp.ADD -> {
                if (constant != null && constant.value == 0.0) return null
                when {
                    constant == null -> "+"
                    constant.value > 0 -> "+" + FormatTool.format1D(abs(constant.value))
                    constant.value < 0 -> "-" + FormatTool.format1D(abs(constant.value))
                    else -> null
                }
            }

            AttachmentModifierOp.MUL -> when {
                constant == null -> "×"
                constant.value == 1.0 -> null
                constant.value > 1 -> "+" + FormatTool.format1D(abs(1 - constant.value) * 100) + "%"
                constant.value in 0.0..<1.0 -> "-" + FormatTool.format1D(abs(1 - constant.value) * 100) + "%"
                else -> "×"
            }

            AttachmentModifierOp.SET -> null

            AttachmentModifierOp.CLAMP_MIN -> {
                if (constant == null) "≥" else "≥ ${FormatTool.format2D(constant.value)}"
            }

            AttachmentModifierOp.CLAMP_MAX -> {
                if (constant == null) "≤" else "≤ ${FormatTool.format2D(constant.value)}"
            }
        }
    }

    open fun overrideLine(prop: String, value: JsonElement): Component {
        val line = modifiedPropertyComponent(prop)
        if (prop != "SpreadPattern") return line

        val type = (value as? JsonObject)?.get("Type")?.jsonPrimitive?.contentOrNull ?: return line
        val key = "prop.superbwarfare.spread_pattern.${type.lowercase(Locale.ROOT)}"
        val typeName = if (I18n.exists(key)) {
            Component.translatable(key)
        } else {
            Component.literal(type)
        }

        return line
            .append(Component.literal(": "))
            .append(typeName)
    }

    open fun scopeLines(definition: AttachmentDefinition): List<Component> {
        val info = definition.scopeInfo
        if (info == null || info.modes.size <= 1) {
            return listOfNotNull(
                definition.scopeZoom(0)?.let(::zoomComponent),
                info?.movingZoom?.let(::movingZoomComponent),
            )
        }

        return buildList {
            info.modes.forEachIndexed { index, _ ->
                add(scopeModeHeader(index + 1))
                definition.scopeZoom(index)?.let { add(zoomComponent(it)) }
                info.movingZoom?.let { add(movingZoomComponent(it)) }
            }
        }
    }

    open fun zoomComponent(zoom: AttachmentZoom): Component {
        return if (zoom.min == zoom.max) {
            propertyComponent("zoom")
                .append(Component.literal(" ${FormatTool.format2D(zoom.min)}x"))
                .withStyle(SCOPE_COLOR)
        } else {
            propertyComponent("zoom_range")
                .append(
                    Component.literal(
                        " ${FormatTool.format2D(zoom.min)}~${FormatTool.format2D(zoom.max)}x"
                    )
                )
                .withStyle(SCOPE_COLOR)
        }
    }

    open fun movingZoomComponent(movingZoom: Double): Component {
        return propertyComponent("moving_zoom")
            .append(Component.literal(" ${FormatTool.format2D(movingZoom)}x"))
            .withStyle(SCOPE_COLOR)
    }

    open fun scopeModeHeader(index: Int): MutableComponent {
        return Component.literal("<")
            .append(Component.literal(index.toString()))
            .append(Component.literal("#"))
            .append(Component.translatable("attachment.superbwarfare.scope_mode"))
            .append(Component.literal(">"))
            .withStyle { style -> style.withColor(SCOPE_MODE_COLOR) }
    }

    open fun modifiedPropertyComponent(prop: String): MutableComponent {
        return Component.translatable("attachment.superbwarfare.modify")
            .append(propertyComponent(prop))
            .withStyle(MODIFICATION_COLOR)
    }

    open fun effectStyle(modifier: AttachmentModifier): ChatFormatting {
        val constant = modifier.value as? AttachmentModifierValue.Constant ?: return ChatFormatting.GRAY
        val direction = when (modifier.op) {
            AttachmentModifierOp.ADD -> when {
                constant.value > 0 -> Direction.INCREASE
                constant.value < 0 -> Direction.DECREASE
                else -> Direction.NONE
            }

            AttachmentModifierOp.MUL -> when {
                constant.value > 1 -> Direction.INCREASE
                constant.value in 0.0..<1.0 -> Direction.DECREASE
                else -> Direction.NONE
            }

            AttachmentModifierOp.SET,
            AttachmentModifierOp.CLAMP_MIN,
            AttachmentModifierOp.CLAMP_MAX -> Direction.NONE
        }

        return when {
            direction == Direction.INCREASE && modifier.prop in HIGHER_IS_BETTER -> ChatFormatting.GREEN
            direction == Direction.INCREASE && modifier.prop in LOWER_IS_BETTER -> ChatFormatting.RED
            direction == Direction.DECREASE && modifier.prop in LOWER_IS_BETTER -> ChatFormatting.GREEN
            direction == Direction.DECREASE && modifier.prop in HIGHER_IS_BETTER -> ChatFormatting.RED
            else -> ChatFormatting.GRAY
        }
    }

    open fun soundRadiusLine(multiplier: Double): Component? {
        if (abs(multiplier - 1.0) < EPSILON) return null
        val suffix = if (multiplier < 1.0) "-" else "+"
        val style = if (multiplier < 1.0) ChatFormatting.GREEN else ChatFormatting.RED
        return propertyComponent("sound_radius_multiplier")
            .append(Component.literal(" $suffix"))
            .withStyle(style)
    }

    open fun muzzleFlashLine(scale: Float): Component? {
        if (abs(scale - 1.0f) < EPSILON) return null
        val suffix = if (scale < 1.0f) "-" else "+"
        val style = if (scale < 1.0f) ChatFormatting.GREEN else ChatFormatting.RED
        return propertyComponent("muzzle_flash_scale")
            .append(Component.literal(" $suffix"))
            .withStyle(style)
    }

    open fun propertyComponent(prop: String): MutableComponent {
        val key = "prop.superbwarfare.${FormatTool.camelToSnake(prop)}"
        return if (I18n.exists(key)) {
            Component.translatable(key)
        } else {
            Component.literal(prop)
        }
    }

    enum class Direction {
        INCREASE,
        DECREASE,
        NONE,
    }

    companion object {
        private const val LINE_HEIGHT = 10
        private const val EPSILON = 1.0e-6

        const val SCOPE_MODE_COLOR = 0xB99CFF
        val MODIFICATION_COLOR = ChatFormatting.AQUA
        val SCOPE_COLOR = ChatFormatting.YELLOW

        /** 额外可装槽位的槽位名：比主槽位那行的 `GOLD`（`0xFFAA00`）暗一档，主次分明 */
        const val ADDITIONAL_SLOT_COLOR = 0xE0A84B

        val HIGHER_IS_BETTER = setOf(
            "Damage",
            "Headshot",
            "Magazine",
            "ProjectileAmount",
            "Range",
            "RPM",
            "Velocity",
            "MeleeDamage",
            "MeleeRange",
            "MeleeHeadshot",
            "MeleeLegshot",
        )

        val LOWER_IS_BETTER = setOf(
            "BoltActionTime",
            "DrawTime",
            "HeatPerShoot",
            "Recoil",
            "RecoilForce",
            "RecoilTime",
            "RecoilX",
            "RecoilY",
            "ReloadTime",
            "SoundRadius",
            "Spread",
            "Weight",
            "ZoomTime",
        )
    }
}
