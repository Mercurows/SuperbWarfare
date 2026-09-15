package com.atsuishio.superbwarfare.client.tooltip

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.tooltip.component.AttachmentImageComponent
import com.atsuishio.superbwarfare.data.attachment.*
import com.atsuishio.superbwarfare.item.attachment.AttachmentItem
import com.atsuishio.superbwarfare.tools.FormatTool
import com.google.gson.JsonObject
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
        val definition = (stack.item as? AttachmentItem)?.definition() ?: return emptyList()

        return buildList {
            val attachmentId = definition.getId()
            val namespace = attachmentId.substringBefore(':', Mod.MODID)
            val path = attachmentId.substringAfter(':', attachmentId)
            val descriptionKey = "des.$namespace.$path"
            if (I18n.exists(descriptionKey)) {
                add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY))
            }

            add(slotLine(definition))
            addAll(scopeLines(definition))
            definition.modifiers.mapNotNullTo(this) { modifierLine(it) }
            definition.override?.entrySet()?.forEach { entry ->
                add(overrideLine(entry.key, entry.value))
            }

            if (definition.isSilenced) {
                add(propertyComponent("silenced").withStyle(ChatFormatting.GREEN))
            }

            soundRadiusLine(definition.soundRadiusMultiplier)?.let(::add)
            muzzleFlashLine(definition.muzzleFlashScale)?.let(::add)
        }
    }

    open fun slotLine(definition: AttachmentDefinition): MutableComponent {
        val type = definition.slot.attachmentName.lowercase(Locale.ROOT)
        val key = "attachment.superbwarfare.slot.$type"
        val text: MutableComponent = if (I18n.exists(key)) {
            Component.translatable(key)
        } else {
            Component.literal("[${definition.slot.attachmentName}]")
        }
        return text.withStyle(ChatFormatting.GOLD)
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
                    constant.value > 0 -> "+"
                    constant.value < 0 -> "-"
                    else -> null
                }
            }

            AttachmentModifierOp.MUL -> when {
                constant == null -> "×"
                constant.value == 1.0 -> null
                constant.value > 1 -> "+"
                constant.value in 0.0..<1.0 -> "-"
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

    open fun overrideLine(prop: String, value: com.google.gson.JsonElement): Component {
        val line = modifiedPropertyComponent(prop)
        if (prop != "SpreadPattern") return line

        val type = (value as? JsonObject)?.get("Type")?.asString ?: return line
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

        val HIGHER_IS_BETTER = setOf(
            "Damage",
            "Headshot",
            "Magazine",
            "ProjectileAmount",
            "Range",
            "RPM",
            "Velocity",
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
