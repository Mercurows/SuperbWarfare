package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.inventory.menu.WeaponRepairStationMenu
import com.sighs.apricityui.init.Document
import com.sighs.apricityui.init.Element
import com.sighs.apricityui.layout.Position
import com.sighs.apricityui.render.Base
import com.sighs.apricityui.render.Drawer
import com.sighs.apricityui.resource.async.style.StyleAsyncHandler
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
    ApricityContainerScreen<ApricityContainerMenu>(menu, inventory, title) {
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
    private val elements = mutableMapOf<String, Element>()
    private var firstFrameReady = false

    override fun init() {
        floatingItems.clear()
        super.init()
        syncDashboard()
    }

    override fun containerTick() {
        super.containerTick()
        syncDashboard()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val doc = linkedDocument ?: return
        Document.withContext(doc).use {
            renderDocument(graphics, mouseX, mouseY, partialTick, doc)
        }
    }

    private fun renderDocument(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float, doc: Document) {
        if (boundDocument !== doc || !doc.isCurrentGeneration(generation)) syncDashboard()
        syncLayout(doc)
        if (!firstFrameReady) {
            // Async CSS/font tasks run on the client resource queue, independently of screen rendering.
            // Do not paint or expose native slots with fallback metrics or the unscaled initial layout.
            if ((StyleAsyncHandler.peekHandle(doc)?.pendingCount() ?: 0) > 0) {
                graphics.fill(0, 0, width, height, 0xF2040B10.toInt())
                return
            }
            commitScreenLayout(doc)
            firstFrameReady = true
        }
        floatingItems.clear()
        super.render(graphics, mouseX, mouseY, partialTick)
        renderFloatingItems(graphics)
        renderStationTooltip(graphics, mouseX, mouseY)
    }

    private fun commitScreenLayout(doc: Document) {
        Document.runWithContext(doc) {
            // The base container's initial slot binding may have measured the root in MC GUI units.
            // Rebuild it in the document's CSS viewport before exposing any geometry or painting.
            doc.markDirty(Drawer.RELAYOUT or Drawer.REPAINT or Drawer.REORDER)
            doc.commitRenderState()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int) =
        !firstFrameReady || inputInDocument { super.mouseClicked(mouseX, mouseY, button) }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int) =
        !firstFrameReady || inputInDocument { super.mouseReleased(mouseX, mouseY, button) }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double) =
        !firstFrameReady || inputInDocument { super.mouseDragged(mouseX, mouseY, button, dragX, dragY) }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double) =
        !firstFrameReady || inputInDocument { super.mouseScrolled(mouseX, mouseY, delta) }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) =
        inputInDocument { super.keyPressed(keyCode, scanCode, modifiers) }

    private fun inputInDocument(action: () -> Boolean): Boolean {
        val doc = linkedDocument ?: return false
        return Document.withContext(doc).use { action() }
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
        Document.runWithContext(doc) { syncDashboard(doc) }
    }

    private fun syncDashboard(doc: Document) {
        if (boundDocument !== doc || !doc.isCurrentGeneration(generation)) {
            boundDocument = doc
            generation = doc.refreshGeneration
            texts.clear(); styles.clear(); elements.clear()
            firstFrameReady = false
            layoutWidth = -1.0
            layoutHeight = -1.0
            doc.getElementById("close")?.addEventListener("click") { onClose() }
            doc.getElementById("toggle")?.addEventListener("click") {
                minecraft?.gameMode?.handleInventoryButtonClick(station.containerId, 0)
            }
        }
        val item = station.getSlot(0).item
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
        text(doc, "fe-value", "${station.fe} / ${station.feCapacity} FE")
        text(doc, "conversion-rate", tr("conversion_rate", station.fePerRepairEnergy))
        text(doc, "required", (damage.toLong() * station.energyCost).toString())
        val seconds = ceil((damage.toLong() * station.interval - station.progress).coerceAtLeast(0) / 20.0).toLong()
        text(doc, "eta", if (damage == 0) "—" else tr("seconds", seconds))
        text(doc, "toggle", tr(if (station.enabled) "pause" else "resume"))
        style(doc, "health-fill", "width: ${ratio * 100}%;")
        val energyRatio = station.energy.toDouble() / station.capacity.coerceAtLeast(1)
        style(doc, "energy-fill", "width: ${energyRatio.coerceIn(0.0, 1.0) * 100}%;")
        val feRatio = station.fe.toDouble() / station.feCapacity.coerceAtLeast(1)
        style(doc, "fe-fill", "width: ${feRatio.coerceIn(0.0, 1.0) * 100}%;")
        val color = when (status) {
            "no_energy" -> "#efb765"; "repairing", "complete" -> "#73eed0"; else -> "#8faaa9"
        }
        style(doc, "status", "color: $color;")
        style(doc, "scan-line", "display: ${if (status == "repairing") "block" else "none"};")
        style(doc, "pulse-dot", "background: $color;")
        // Batch the initial dashboard mutations with the viewport transform before committing layout.
        syncLayout(doc)
    }

    private fun syncLayout(doc: Document) {
        val viewport = doc.viewportSize
        val width = viewport.width()
        val height = viewport.height()
        if (width == layoutWidth && height == layoutHeight) return
        layoutWidth = width
        layoutHeight = height
        firstFrameReady = false
        layoutScale = minOf(width / 1280.0, height / 720.0).coerceAtLeast(0.001)
        val left = (width - 1280.0 * layoutScale) / 2.0
        val top = (height - 720.0 * layoutScale) / 2.0
        style(doc, "repair-layout", "left: ${left}px; top: ${top}px; transform: scale($layoutScale);")
        // The first render commits all dashboard/viewport changes together before native slot sync.
    }

    private fun tr(key: String, vararg args: Any) =
        Component.translatable("container.superbwarfare.weapon_repair_station.$key", *args).string

    private fun text(doc: Document, id: String, value: String) {
        if (texts[id] == value) return
        element(doc, id)?.let { it.textContent = value; texts[id] = value }
    }

    private fun style(doc: Document, id: String, value: String) {
        if (styles[id] == value) return
        element(doc, id)?.let { it.setAttribute("style", value); styles[id] = value }
    }

    private fun element(doc: Document, id: String): Element? =
        elements[id] ?: doc.getElementById(id)?.also { elements[id] = it }
}
