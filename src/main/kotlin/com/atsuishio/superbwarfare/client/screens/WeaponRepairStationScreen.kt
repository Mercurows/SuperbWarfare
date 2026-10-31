package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.inventory.menu.WeaponRepairStationMenu
import com.sighs.apricityui.init.Document
import com.sighs.apricityui.layout.Position
import com.sighs.apricityui.render.Base
import com.sighs.apricityui.screen.ApricityContainerMenu
import com.sighs.apricityui.screen.ApricityContainerScreen
import com.sighs.apricityui.spi.AuiItemRenderRequest
import com.sighs.apricityui.spi.AuiServices
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil

/** AUI owns real container slots and their input; only synchronized server data drives the dashboard. */
open class WeaponRepairStationScreen(menu: ApricityContainerMenu, inventory: Inventory, title: Component) :
    ApricityContainerScreen(menu, inventory, title) {
    private val station get() = menu as WeaponRepairStationMenu
    private var generation = -1L
    private var boundDocument: Document? = null
    private var layoutWidth = -1.0
    private var layoutHeight = -1.0
    private var layoutScale = 1.0
    private data class FloatingItem(val stack: ItemStack, val centerX: Int, val centerY: Int, val overlay: String?)
    private val floatingItems = mutableListOf<FloatingItem>()
    private val texts = mutableMapOf<String, String>()
    private val styles = mutableMapOf<String, String>()

    override fun init() {
        boundDocument = null
        generation = -1L
        texts.clear()
        styles.clear()
        layoutWidth = -1.0
        layoutHeight = -1.0
        floatingItems.clear()
        super.init()
        syncDashboard()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        syncDashboard()
        floatingItems.clear()
        super.render(graphics, mouseX, mouseY, partialTick)
        renderFloatingItems(graphics)
        renderStationTooltip(graphics, mouseX, mouseY)
    }

    // AUI's default floating ItemNode is fixed at scale=1. Capture the native position,
    // including returning-stack animations, then paint with the inventory icon's actual GUI size.
    override fun captureFloatingItem(stack: ItemStack, relativeX: Int, relativeY: Int, overlayText: String?) {
        if (!stack.isEmpty) floatingItems += FloatingItem(stack.copy(), relativeX + guiLeft + 8, relativeY + guiTop + 8, overlayText)
    }

    private fun renderFloatingItems(graphics: GuiGraphics) {
        val doc = linkedDocument ?: return
        if (floatingItems.isEmpty()) return
        val origin = doc.documentToGuiPosition(Position(0.0, 0.0))
        val extent = doc.documentToGuiPosition(Position(32.0 * layoutScale, 0.0))
        val iconScale = ((extent.x - origin.x) / 16.0).toFloat()
        graphics.flush()
        Base.pushGuiItemZ(150f, 200f)
        try {
            for (item in floatingItems) {
                val pose = graphics.pose()
                pose.pushPose()
                try {
                    pose.translate(item.centerX.toDouble(), item.centerY.toDouble(), Base.getFlatOverlayZ().toDouble())
                    pose.scale(iconScale, iconScale, 1f)
                    pose.translate(-8.0, -8.0, 0.0)
                    AuiServices.items().render(AuiItemRenderRequest(pose, item.stack, 0, true, item.overlay, 0f, false))
                    Base.commitLocalDraws()
                    graphics.flush()
                } finally {
                    pose.popPose()
                }
            }
        } finally {
            Base.popGuiItemZ()
        }
    }

    /** AUI's item decorators can write above vanilla's tooltip Z. Paint one tooltip after all items. */
    private fun renderStationTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (!station.carried.isEmpty || floatingItems.isNotEmpty()) return
        val index = findSlotIndexAt(mouseX.toDouble(), mouseY.toDouble())
        val stack = station.slots.getOrNull(index)?.item ?: return
        if (stack.isEmpty) return
        graphics.flush()
        val pose = graphics.pose()
        pose.pushPose()
        try {
            pose.translate(0.0, 0.0, Base.getFlatOverlayZ().toDouble())
            graphics.renderTooltip(font, stack, mouseX, mouseY)
            graphics.flush()
        } finally {
            pose.popPose()
        }
    }

    private fun syncDashboard() {
        val doc = linkedDocument ?: return
        if (boundDocument !== doc || !doc.isCurrentGeneration(generation)) {
            boundDocument = doc
            generation = doc.refreshGeneration
            texts.clear(); styles.clear()
            layoutWidth = -1.0
            layoutHeight = -1.0
            doc.getElementById("close")?.addEventListener("click") { onClose() }
            doc.getElementById("toggle")?.addEventListener("click") {
                minecraft?.gameMode?.handleInventoryButtonClick(station.containerId, 0)
            }
        }
        syncLayout(doc)
        val item = station.getSlot(1).item
        val damage = if (item.isDamageableItem) item.damageValue.coerceAtLeast(0) else 0
        val max = if (item.isDamageableItem) item.maxDamage else 0
        val ratio = if (max > 0) ((max - damage).toDouble() / max).coerceIn(0.0, 1.0) else 0.0
        val status = when {
            item.isEmpty -> "idle"
            damage == 0 -> "complete"
            !station.enabled -> "paused"
            station.energy < station.energyCost -> "no_energy"
            else -> "repairing"
        }
        text(doc, "status", tr(status))
        text(doc, "item-name", if (item.isEmpty) "" else item.hoverName.string)
        text(doc, "durability", if (max > 0) "${max - damage} / $max" else "— / —")
        text(doc, "health-percent", if (max > 0) "${(ratio * 100).toInt()}%" else "—")
        text(doc, "energy-value", "${station.energy} / ${station.capacity}")
        text(doc, "kit-value", tr("kit_value", station.materialEnergy))
        text(doc, "required", (damage.toLong() * station.energyCost).toString())
        val seconds = ceil((damage.toLong() * station.interval - station.progress).coerceAtLeast(0) / 20.0).toLong()
        text(doc, "eta", if (damage == 0) "—" else tr("seconds", seconds))
        text(doc, "toggle", tr(if (station.enabled) "pause" else "resume"))
        style(doc, "health-fill", "width: ${ratio * 100}%;")
        val energyRatio = station.energy.toDouble() / station.capacity.coerceAtLeast(1)
        style(doc, "energy-fill", "width: ${energyRatio.coerceIn(0.0, 1.0) * 100}%;")
        val color = when (status) {
            "no_energy" -> "#efb765"; "repairing", "complete" -> "#73eed0"; else -> "#8faaa9"
        }
        style(doc, "status", "color: $color;")
        style(doc, "scan-line", "display: ${if (status == "repairing") "block" else "none"};")
        style(doc, "pulse-dot", "background: $color;")
    }

    private fun syncLayout(doc: Document) {
        val viewport = doc.viewportSize
        val width = viewport.width()
        val height = viewport.height()
        if (width == layoutWidth && height == layoutHeight) return
        layoutWidth = width
        layoutHeight = height
        layoutScale = minOf(width / 1280.0, height / 720.0).coerceAtLeast(0.001)
        val left = (width - 1280.0 * layoutScale) / 2.0
        val top = (height - 720.0 * layoutScale) / 2.0
        style(doc, "repair-layout", "left: ${left}px; top: ${top}px; transform: scale($layoutScale);")
        // Commit transformed bounds before AUI synchronizes native slot positions and hover tests.
        doc.commitRenderState()
    }

    private fun tr(key: String, vararg args: Any) =
        Component.translatable("container.superbwarfare.weapon_repair_station.$key", *args).string

    private fun text(doc: Document, id: String, value: String) {
        if (texts[id] == value) return
        doc.getElementById(id)?.let { it.textContent = value; texts[id] = value }
    }

    private fun style(doc: Document, id: String, value: String) {
        if (styles[id] == value) return
        doc.getElementById(id)?.let { it.setAttribute("style", value); styles[id] = value }
    }
}
