package com.atsuishio.superbwarfare.client.tooltip

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent
import com.atsuishio.superbwarfare.data.gun.FireMode
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.FormatTool
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.FormatTool.format2D
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.capabilities.ForgeCapabilities
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

open class ClientGunImageTooltip(tooltip: GunImageComponent) : ClientTooltipComponent {
    protected val tipWidth: Int = tooltip.width
    protected val tipHeight: Int = tooltip.height
    protected val stack: ItemStack = tooltip.stack
    protected val data: GunData = from(stack)

    override fun renderImage(font: Font, x: Int, y: Int, guiGraphics: GuiGraphics) {
        guiGraphics.pose().pushPose()

        renderDamageAndRpmTooltip(font, guiGraphics, x, y)
        renderLevelAndUpgradePointTooltip(font, guiGraphics, x, y + 10)

        var yo = 20
        if (shouldRenderBypassAndHeadshotTooltip()) {
            renderBypassAndHeadshotTooltip(font, guiGraphics, x, y + yo)
            yo += 10
        }

        if (shouldRenderEnergyTooltip()) {
            yo += 10
            renderEnergyTooltip(font, guiGraphics, x, y + yo)
            yo += 10
        }

        if (shouldRenderEditTooltip()) {
            renderWeaponEditTooltip(font, guiGraphics, x, y + yo)
            yo += 20
        }

        if (shouldRenderPerks()) {
            if (!Screen.hasShiftDown()) {
                renderPerksShortcut(font, guiGraphics, x, y + yo)
            } else {
                renderPerks(font, guiGraphics, x, y + yo)
            }
        }

        guiGraphics.pose().popPose()
    }

    protected fun shouldRenderBypassAndHeadshotTooltip(): Boolean {
//        return data.get(GunProp.BYPASSES_ARMOR) > 0 || data.get(GunProp.HEADSHOT) > 0
        return true
    }

    protected fun shouldRenderPerks(): Boolean {
        return data.perk.get(Perk.Type.AMMO) != null || data.perk.get(Perk.Type.DAMAGE) != null || data.perk.get(Perk.Type.FUNCTIONAL) != null
    }

    protected fun shouldRenderEnergyTooltip(): Boolean {
        return stack.getCapability(ForgeCapabilities.ENERGY)
            .map { it.maxEnergyStored > 0 }
            .orElseGet { false }
    }

    protected fun shouldRenderEditTooltip(): Boolean {
        val item = this.stack.item
        if (item is GunItem) {
            return item.canEditAttachments(from(stack))
        }
        return false
    }

    /**
     * 渲染武器伤害和射速
     */
    protected fun renderDamageAndRpmTooltip(font: Font, guiGraphics: GuiGraphics, x: Int, y: Int) {
        guiGraphics.drawString(font, this.damageComponent, x, y, 0xFFFFFF)
        val xo = font.width(this.damageComponent.visualOrderText)
        guiGraphics.drawString(font, this.rpmComponent, x + xo + 16, y, 0xFFFFFF)
    }

    protected open val damageComponent: Component
        /**
         * 获取武器伤害的文本组件
         */
        get() {
            val damage = data.get(GunProp.DAMAGE)
            val explosionDamage = data.get(GunProp.EXPLOSION_DAMAGE)

            var dmgStr = format1D(damage)
            if (data.get(GunProp.PROJECTILE_AMOUNT) > 1) {
                dmgStr = dmgStr + " * " + data.get(GunProp.PROJECTILE_AMOUNT)
            }

            var component =
                Component.translatable("des.superbwarfare.guns.damage")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(Component.literal(dmgStr).withStyle(ChatFormatting.GREEN))

            if (explosionDamage > 0) {
                var expDmgStr = format1D(explosionDamage)
                if (data.get(GunProp.PROJECTILE_AMOUNT) > 1) {
                    expDmgStr = expDmgStr + " * " + data.get(GunProp.PROJECTILE_AMOUNT)
                }
                component = component
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(
                        Component.literal(" + $expDmgStr").withStyle(ChatFormatting.GOLD)
                    )
            }

            return component
        }

    protected val rpmComponent: Component
        /**
         * 获取武器射速的文本组件
         */
        get() {
            if (this.stack.item !is GunItem) return Component.empty()
            val data =
                from(this.stack)
            val info = data.selectedFireModeInfo()

            if (info.mode == FireMode.AUTO || info.mode == FireMode.BURST) {
                return Component.translatable("des.superbwarfare.guns.rpm")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.empty().withStyle(ChatFormatting.RESET))
                    .append(
                        Component.literal(format0D(data.get(GunProp.RPM).toDouble())).withStyle(ChatFormatting.GREEN)
                    )
            }
            return Component.empty()
        }

    /**
     * 渲染武器等级和强化点数
     */
    protected fun renderLevelAndUpgradePointTooltip(font: Font, guiGraphics: GuiGraphics, x: Int, y: Int) {
        guiGraphics.drawString(font, this.levelComponent, x, y, 0xFFFFFF)
        val xo = font.width(this.levelComponent.visualOrderText)
        guiGraphics.drawString(font, this.upgradePointComponent, x + xo + 16, y, 0xFFFFFF)
    }

    protected val levelComponent: Component
        /**
         * 获取武器等级文本组件
         */
        get() {
            val level = data.level.get()
            val rate = data.exp.get() / (20 * level.toDouble().pow(2.0) + 160 * level + 20)
            val formatting = if (level < 10) {
                ChatFormatting.WHITE
            } else if (level < 20) {
                ChatFormatting.AQUA
            } else if (level < 30) {
                ChatFormatting.LIGHT_PURPLE
            } else if (level < 40) {
                ChatFormatting.GOLD
            } else {
                ChatFormatting.RED
            }

            return Component.translatable("des.superbwarfare.guns.level")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(
                    Component.literal(level.toString() + "").withStyle(formatting)
                        .withStyle(ChatFormatting.BOLD)
                )
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(
                    Component.literal(" (" + FormatTool.DECIMAL_FORMAT_2ZZZ.format(rate * 100) + "%)")
                        .withStyle(ChatFormatting.GRAY)
                )
        }

    protected val upgradePointComponent: Component
        /**
         * 获取武器强化点数文本组件
         */
        get() {
            var upgradePoint = data.level.get()
            for (type in Perk.Type.entries) {
                val list = data.perk.getInstances(type)
                if (list.isEmpty()) continue

                for (perkInstance in list) {
                    upgradePoint -= perkInstance.level - 1
                }
            }
            upgradePoint = max(upgradePoint, 0)

            return Component.translatable("des.superbwarfare.guns.upgrade_point")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(
                    Component.literal(upgradePoint.toString())
                        .withStyle(ChatFormatting.WHITE).withStyle(ChatFormatting.BOLD)
                )
        }

    /**
     * 渲染武器穿甲比例和爆头倍率
     */
    protected fun renderBypassAndHeadshotTooltip(font: Font, guiGraphics: GuiGraphics, x: Int, y: Int) {
        guiGraphics.drawString(font, this.bypassComponent, x, y, 0xFFFFFF)
        val xo = font.width(this.bypassComponent.visualOrderText)
        guiGraphics.drawString(font, this.headshotComponent, x + xo + 16, y, 0xFFFFFF)
    }

    protected val bypassComponent: Component
        /**
         * 获取武器穿甲比例文本组件
         */
        get() {
            val bypassRate = max(data.get(GunProp.BYPASSES_ARMOR), 0.0)
            return Component.translatable("des.superbwarfare.guns.bypass")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(
                    Component.literal(format2D(bypassRate * 100, "%"))
                        .withStyle(ChatFormatting.GOLD)
                )
        }

    protected val headshotComponent: Component
        /**
         * 获取武器爆头倍率文本组件
         */
        get() {
            val headshot = data.get(GunProp.HEADSHOT)
            return Component.translatable("des.superbwarfare.guns.headshot")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(
                    Component.literal(format1D(headshot, "x"))
                        .withStyle(ChatFormatting.AQUA)
                )
        }

    /**
     * 渲染武器能量信息
     */
    protected fun renderEnergyTooltip(font: Font, guiGraphics: GuiGraphics, x: Int, y: Int) {
        guiGraphics.drawString(font, this.energyComponent, x, y, 0xFFFFFF)
    }

    protected val energyComponent: Component
        /**
         * 获取武器能量文本组件
         */
        get() {
            assert(stack.getCapability(ForgeCapabilities.ENERGY).resolve().isPresent)
            val storage = stack.getCapability(ForgeCapabilities.ENERGY).resolve().get()
            val energy = storage.energyStored
            val maxEnergy = storage.maxEnergyStored
            val percentage = (energy.toFloat() / maxEnergy).coerceIn(0f, 1f)
            val component = Component.empty()
            val format = if (percentage <= .2f) {
                ChatFormatting.RED
            } else if (percentage <= .6f) {
                ChatFormatting.YELLOW
            } else {
                ChatFormatting.GREEN
            }

            val count = (percentage * 50).toInt()
            repeat(count) {
                component.append(Component.literal("|").withStyle(format))
            }
            component.append(Component.empty().withStyle(ChatFormatting.RESET))
            repeat(50 - count) {
                component.append(Component.literal("|").withStyle(ChatFormatting.GRAY))
            }

            component.append(
                Component.literal(" $energy/$maxEnergy FE")
                    .withStyle(ChatFormatting.GRAY)
            )

            return component
        }

    /**
     * 渲染武器改装信息
     */
    protected fun renderWeaponEditTooltip(font: Font, guiGraphics: GuiGraphics, x: Int, y: Int) {
        guiGraphics.drawString(font, this.editComponent, x, y + 10, 0xFFFFFF)
    }

    protected val editComponent: Component
        /**
         * 获取武器改装信息文本组件
         */
        get() = Component.translatable(
            "des.superbwarfare.guns.edit",
            "[" + ModKeyMappings.EDIT_MODE.key.displayName.string + "]"
        ).withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(ChatFormatting.ITALIC)

    /**
     * 渲染武器模组缩略图
     */
    protected fun renderPerksShortcut(font: Font, guiGraphics: GuiGraphics, x: Int, y: Int) {
        guiGraphics.pose().pushPose()

        var xOffset = -20

        var count = 0
        for (type in Perk.Type.entries) {
            val list = data.perk.getInstances(type)
            if (list.isEmpty()) continue

            for (perkInstance in list) {
                count++
                if (count > 5) continue

                xOffset += 20
                val ammoItem = perkInstance.perk.getItem().get()
                val perkStack = ammoItem.defaultInstance

                val level = perkInstance.level.toInt()
                perkStack.count = level
                guiGraphics.renderItem(perkStack, x + xOffset, y + 2)
                guiGraphics.renderItemDecorations(font, perkStack, x + xOffset, y + 2)
            }
        }

        if (count > 5) {
            guiGraphics.drawString(font, "(+" + (count - 5) + ")", x + xOffset + 20, y + 8, 11184810)
        }

        guiGraphics.pose().popPose()
    }

    /**
     * 渲染武器模组详细信息
     */
    protected fun renderPerks(font: Font, guiGraphics: GuiGraphics, x: Int, y: Int) {
        guiGraphics.pose().pushPose()

        var tip = Component.empty().append(
            Component.translatable("perk.superbwarfare.tips").withStyle(ChatFormatting.GOLD)
                .withStyle(ChatFormatting.UNDERLINE)
        )
        var yOffset = -5

        val list: MutableList<PerkInstance> = mutableListOf()
        for (type in Perk.Type.entries) {
            list.addAll(data.perk.getInstances(type))
        }

        val page: Int = (getCurrentPage(list.size) - 1)
        val count: Int = page * PAGE_SIZE
        val maxPage = ceil(list.size / (PAGE_SIZE * 1.0)).toInt()

        if (maxPage > 1) {
            tip = tip.append(
                Component.empty().withStyle(ChatFormatting.RESET)
                    .append(Component.literal(" (" + (page + 1) + " / " + maxPage + ")").withStyle(ChatFormatting.GRAY))
            )
        }

        guiGraphics.drawString(font, tip, x, y + 10, 0xFFFFFF)

        for (i in count..<min(list.size, count + PAGE_SIZE)) {
            yOffset += 25

            val perkInstance = list[i]
            val ammoItem = perkInstance.perk.getItem().get()
            guiGraphics.renderItem(ammoItem.defaultInstance, x, y + 4 + yOffset)

            val id = perkInstance.perk.descriptionId

            val component = Component.translatable("item.superbwarfare.$id").withStyle(perkInstance.perk.type.color)
                .append(Component.literal(" ").withStyle(ChatFormatting.RESET))
                .append(Component.literal(" Lvl. ${perkInstance.level}").withStyle(ChatFormatting.WHITE))
            val descComponent = Component.translatable("des.superbwarfare.$id").withStyle(ChatFormatting.GRAY)

            guiGraphics.drawString(font, component, x + 20, y + yOffset + 2, 0xFFFFFF)
            guiGraphics.drawString(font, descComponent, x + 20, y + yOffset + 12, 0xFFFFFF)
        }

        guiGraphics.pose().popPose()
    }

    protected fun getDefaultMaxWidth(font: Font): Int {
        var width =
            font.width(this.damageComponent.visualOrderText) + font.width(this.rpmComponent.visualOrderText) + 16
        width = max(
            width,
            font.width(this.levelComponent.visualOrderText) + font.width(this.upgradePointComponent.visualOrderText) + 16
        )
        if (shouldRenderBypassAndHeadshotTooltip()) {
            width = max(
                width,
                font.width(this.bypassComponent.visualOrderText) + font.width(this.headshotComponent.visualOrderText) + 16
            )
        }
        if (shouldRenderEditTooltip()) {
            width = max(width, font.width(this.editComponent.visualOrderText) + 16)
        }

        return width + 4
    }

    protected fun getMaxPerkDesWidth(font: Font): Int {
        if (!shouldRenderPerks()) return 0

        var width = 0

        for (type in Perk.Type.entries) {
            val list = data.perk.getInstances(type)
            if (list.isEmpty()) continue

            for (perkInstance in list) {
                val id = perkInstance.perk.descriptionId

                val ammoDesComponent = Component.translatable("des.superbwarfare.$id").withStyle(ChatFormatting.GRAY)
                width = max(width, font.width(ammoDesComponent))
            }
        }

        return width + 25
    }

    override fun getHeight(): Int {
        var height = max(20, this.tipHeight)

        if (shouldRenderBypassAndHeadshotTooltip()) height += 10
        if (shouldRenderEnergyTooltip()) height += 20
        if (shouldRenderEditTooltip()) height += 20
        if (shouldRenderPerks()) {
            height += 16

            if (Screen.hasShiftDown()) {
                var count = 0
                for (type in Perk.Type.entries) {
                    count += data.perk.getInstances(type).size
                    if (count >= PAGE_SIZE) break
                }
                height += 25 * min(count, PAGE_SIZE)
            }
        }

        return height
    }

    override fun getWidth(font: Font): Int {
        var width = getMaxPerkDesWidth(font)

        width = if (Screen.hasShiftDown()) {
            if (width == 0) {
                max(this.tipWidth, getDefaultMaxWidth(font))
            } else {
                max(width, getDefaultMaxWidth(font))
            }
        } else {
            getDefaultMaxWidth(font)
        }

        if (shouldRenderEnergyTooltip()) {
            width = max(width, font.width(this.energyComponent.visualOrderText) + 10)
        }

        return width
    }

    companion object {
        const val PAGE_SIZE: Int = 4

        fun getCurrentPage(size: Int): Int {
            if (size <= PAGE_SIZE) return 1
            val totalPages = ceil(size / (PAGE_SIZE * 1.0)).toInt()
            if (totalPages <= 1) return 1
            return (System.currentTimeMillis() / 5000 % totalPages).toInt() + 1
        }
    }
}
