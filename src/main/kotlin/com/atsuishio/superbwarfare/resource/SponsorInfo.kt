package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.data.ModColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

/**
 * 赞助者信息，载具资源（`sbw/vehicles`）与枪械资源（`sbw/guns`）共用。
 *
 * ```jsonc
 * "Sponsor": {
 *   "Name": "RXY02",
 *   "Color": "0x7DEA79"
 * }
 * ```
 *
 * 显示样式统一由 [tooltipLine] 给出（载具集装箱与枪械 tooltip 用的是同一行）。
 */
@Serializable
class SponsorInfo {
    @JvmField
    @SerialName("Name")
    val name: String? = null

    @JvmField
    @SerialName("Color")
    val color: ModColor = ModColor(0x7DEA79)

    fun tooltipLine(): Component = Component.translatable(
        "des.superbwarfare.sponsor",
        Component.literal("[${name}]").withStyle(Style.EMPTY.withColor(color.color))
    ).withStyle(Style.EMPTY.withColor(0x7DEA79))
}
