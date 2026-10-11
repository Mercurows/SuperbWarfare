package com.atsuishio.superbwarfare.resource.gun.pojo

import com.atsuishio.superbwarfare.data.ModColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

@Serializable
class ComponentInfo {
    @JvmField
    @SerialName("Text")
    var text: String = ""

    @JvmField
    @SerialName("Color")
    var color: ModColor = ModColor(0xFFFFFF)

    @JvmField
    @SerialName("Italic")
    var italic: Boolean = false

    @JvmField
    @SerialName("Bold")
    var bold: Boolean = false

    @JvmField
    @SerialName("Hidden")
    var hidden: Boolean = false

    fun asComponent(): Component {
        val component = if (text.startsWith(TRANSLATION_PREFIX)) {
            Component.translatable(text.substringAfter(TRANSLATION_PREFIX))
        } else {
            Component.literal(text)
        }

        var style = Style.EMPTY.withColor(color.get())
        if (italic) style = style.withItalic(true)
        if (bold) style = style.withBold(true)
        return component.withStyle(style)
    }

    companion object {
        const val TRANSLATION_PREFIX: String = "Component#"
    }
}
