package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.config.server.GunConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.inventory.menu.ReforgingTableMenu
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.ResonancePerkInstallMessage
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import com.mojang.blaze3d.systems.RenderSystem
import com.sighs.apricityui.event.MouseEvent
import com.sighs.apricityui.init.Document
import com.sighs.apricityui.init.Element
import com.sighs.apricityui.layout.Position
import com.sighs.apricityui.parser.Color
import com.sighs.apricityui.render.*
import com.sighs.apricityui.screen.ApricityScreen
import com.sighs.apricityui.style.Text
import net.minecraft.client.StringSplitter
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.glfw.GLFW
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.*

/** Inventory selection and a local Perk draft, committed atomically by the server. */
@OnlyIn(Dist.CLIENT)
class ResonanceReforgingScreen(private val tablePos: BlockPos, private val inventorySlot: Int? = null) : ApricityScreen(
    if (inventorySlot == null) SELECTION_PATH else REFORGING_PATH
) {
    private data class Candidate(val slot: Int, val stack: ItemStack, val uuid: UUID?)
    private var documentGeneration = -1L
    private var candidates = emptyList<Candidate>()
    private var previewedCandidate: Candidate? = null
    private var displayedWeapon = ItemStack.EMPTY
    private var weaponUuid: String? = null
    private var baselineSignature = ""
    private var draftReady = false
    private val draft = Perk.Type.entries.associateWith { mutableListOf<Perk>() }
    private val draftLevels = mutableMapOf<Perk, Int>()
    private val pages = Perk.Type.entries.associateWith { 0 }.toMutableMap()
    private var activeType = Perk.Type.AMMO
    private var activeSlot = 0
    private var slotSelected = false
    private var choicePage = 0
    private var availableChoices = emptyList<Perk>()
    private var shownChoices = emptyList<Perk>()
    private val indicatorCounts = mutableMapOf<String, Int>()
    private var ownedPerks = emptyMap<Item, Int>()
    private var forceRefresh = true
    private var unrestrictedPerks = false
    private var pending = false
    private var pendingAt = 0L
    private var resetHoldStartedAt = 0L
    private var resetProgressStep = -1
    private val perkHoverTargets = linkedMapOf<String, Perk>()
    private var tooltipPerk: Perk? = null
    private var tooltipTitle = Text()
    private var tooltipType = Text()
    private var tooltipDescription = emptyList<Text>()

    override fun isPauseScreen() = false

    override fun init() {
        resetHoldStartedAt = 0L
        resetProgressStep = -1
        super.init()
        documentGeneration = -1L
        syncPage()
    }

    override fun tick() {
        super.tick()
        // Inventory synchronization clears pending on success; silently unlock retries after a rejected/lost request.
        if (pending && System.currentTimeMillis() - pendingAt > 10000) {
            pending = false
            forceRefresh = true
        }
        syncPage()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val doc = linkedDocument
        if (doc != null && (!doc.isCurrentGeneration(documentGeneration) || forceRefresh)) syncPage()
        if (doc != null) updateResetHold(doc)
        super.render(graphics, mouseX, mouseY, partialTick)
        if (doc != null && inventorySlot != null) renderPerkTooltip(graphics, doc)
    }

    override fun removed() {
        resetHoldStartedAt = 0L
        super.removed()
    }

    override fun onClose() {
        if (inventorySlot == null) super.onClose() else mc.setScreen(ResonanceReforgingScreen(tablePos))
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (mc.options.keyInventory.matches(keyCode, scanCode)) {
            onClose()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    private fun syncPage() {
        val doc = linkedDocument ?: return
        val player = localPlayer ?: return
        val rebuilt = !doc.isCurrentGeneration(documentGeneration)
        if (rebuilt) {
            resetHoldStartedAt = 0L
            resetProgressStep = -1
            perkHoverTargets.clear()
            indicatorCounts.clear()
            shownChoices = emptyList()
            tooltipPerk = null
            documentGeneration = doc.refreshGeneration
            displayedWeapon = ItemStack.EMPTY
            forceRefresh = true
            doc.getElementById("back-to-weapons")?.addEventListener("click") { onClose() }
            doc.getElementById("close-selection")?.addEventListener("click") { onClose() }
            if (inventorySlot == null) {
                for (index in 0 until GRID_SIZE) {
                    val cell = doc.getElementById("weapon-$index") ?: continue
                    cell.addEventListener("click") {
                        val slot = candidates.getOrNull(index)?.slot ?: return@addEventListener
                        if (player.inventory.getItem(slot).item is GunItem) mc.setScreen(ResonanceReforgingScreen(tablePos, slot))
                    }
                    cell.addEventListener("mouseenter") { candidates.getOrNull(index)?.let { showPreview(doc, it) } }
                }
            } else bindReforging(doc)
        }
        if (inventorySlot != null) {
            val stack = player.inventory.getItem(inventorySlot)
            if (stack.isEmpty || stack.item !is GunItem) { onClose(); return }
            val uuid = GunData.from(stack).uuid?.toString()
            if (weaponUuid != null && uuid != weaponUuid) { onClose(); return }
            weaponUuid = uuid
            val owned = player.inventory.items.filter { !it.isEmpty }.groupingBy { it.item }.fold(0) { n, item -> n + item.count }
            val unrestricted = GunConfig.allowsAnyPerk(player.isCreative)
            if (rebuilt || forceRefresh || unrestricted != unrestrictedPerks || !ItemStack.matches(displayedWeapon, stack) || owned != ownedPerks) {
                unrestrictedPerks = unrestricted
                ownedPerks = owned
                displayedWeapon = stack.copy()
                syncDetails(doc, stack)
            }
            forceRefresh = false
            return
        }
        val owned = player.inventory.items.withIndex().filter { !it.value.isEmpty && it.value.item is GunItem }
        val changed = rebuilt || owned.size != candidates.size || owned.indices.any {
            owned[it].index != candidates[it].slot || !ItemStack.matches(owned[it].value, candidates[it].stack)
        }
        if (changed) {
            val previousCandidates = candidates
            candidates = owned.mapIndexed { index, entry ->
                val previous = previousCandidates.getOrNull(index)
                if (previous?.slot == entry.index && ItemStack.matches(previous.stack, entry.value)) previous
                else Candidate(entry.index, entry.value.copy(), GunData.from(entry.value).uuid)
            }
            for (index in 0 until GRID_SIZE) {
                val candidate = candidates.getOrNull(index)
                val previous = previousCandidates.getOrNull(index)
                if (!rebuilt && candidate?.slot == previous?.slot && ItemStack.matches(
                        candidate?.stack ?: ItemStack.EMPTY, previous?.stack ?: ItemStack.EMPTY)) continue
                attribute(doc, "weapon-$index", "class", if (candidate == null) "weapon-cell" else "weapon-cell occupied")
                attribute(doc, "weapon-$index", "aria-disabled", (candidate == null).toString())
                setDisplayItem(doc, "weapon-item-$index", candidate?.stack ?: ItemStack.EMPTY)
                text(doc, "weapon-label-$index", candidate?.stack?.hoverName?.string ?: "")
            }
            text(doc, "weapon-count", candidates.size.toString() + " / $GRID_SIZE")
            doc.getElementById("selection-empty")?.setInlineStyleProperty("display", if (candidates.isEmpty()) "flex" else "none")
            showPreview(doc, choosePreviewCandidate())
        }
        forceRefresh = false
    }

    private fun bindReforging(doc: Document) {
        createPerkControls(doc)
        doc.addEventListener("click", { event ->
            val inside = event.composedPath().filterIsInstance<Element>().any {
                it.id == "perk-choices-panel" || (it.id?.startsWith("perk-slot-") == true && it.getAttribute("aria-disabled") != "true")
            }
            if (!inside && slotSelected) {
                slotSelected = false
                forceRefresh = true
            }
        }, true)
        for (type in Perk.Type.entries) for (step in listOf(-1, 1)) {
            val suffix = if (step < 0) "previous" else "next"
            doc.getElementById("page-" + type.typeName + "-" + suffix)?.addEventListener("click") {
                if (!pending) {
                    pages[type] = (pages.getValue(type) + step).coerceIn(0, slotPages(type) - 1)
                    slotSelected = false
                    forceRefresh = true
                }
            }
        }
        doc.getElementById("reset-perks")?.apply {
            addEventListener("mousedown") { event ->
                if ((event as? MouseEvent)?.button == 0 && !pending && draft.values.any { it.isNotEmpty() }) {
                    event.preventDefault()
                    resetHoldStartedAt = System.nanoTime()
                    paintResetProgress(doc, 0.0)
                }
            }
            addEventListener("mouseleave") { cancelResetHold(doc) }
        }
        doc.addEventListener("mouseup", { cancelResetHold(doc) }, true)
        for (step in listOf(-1, 1)) {
            val suffix = if (step < 0) "previous" else "next"
            doc.getElementById("choice-$suffix")?.addEventListener("click") {
                if (!pending) {
                    choicePage = (choicePage + step).coerceIn(0, maxOf(1, (availableChoices.size + CHOICES_PER_PAGE - 1) / CHOICES_PER_PAGE) - 1)
                    forceRefresh = true
                }
            }
        }
        doc.getElementById("complete-reforging")?.addEventListener("click") { submit() }
        for (action in listOf("min", "decrease", "increase", "max")) {
            doc.getElementById("perk-level-$action")?.addEventListener("click") {
                val perk = activePerk() ?: return@addEventListener
                val current = draftLevels[perk] ?: 1
                val target = levelTarget(action, current)
                if (canSetLevel(current, target)) {
                    draftLevels[perk] = target
                    forceRefresh = true
                }
            }
        }
    }

    /** Build interactive targets once per document generation; ticking ItemStacks must not replace pressed nodes. */
    private fun createPerkControls(doc: Document) {
        for (type in Perk.Type.entries) {
            val key = type.typeName
            doc.getElementById("slots-$key")?.innerHTML = (0..2).joinToString("") { position ->
                """<button type="button" class="perk-card" id="perk-slot-$key-$position"><sbw-reforging-weapon id="perk-icon-$key-$position"></sbw-reforging-weapon><span class="perk-level" id="perk-level-$key-$position"></span><span class="add-perk" id="perk-add-$key-$position">+</span></button>"""
            }
            for (position in 0..2) {
                doc.getElementById("perk-slot-$key-$position")?.addEventListener("click") {
                    val index = pages.getValue(type) * 3 + position
                    if (!pending && index <= draft.getValue(type).size) {
                        activeType = type; activeSlot = index; slotSelected = true; choicePage = 0; forceRefresh = true
                    }
                }
            }
        }
        val corners = """<span class="choice-frame" aria-hidden="true"><span class="choice-corner corner-nw"></span><span class="choice-corner corner-ne"></span><span class="choice-corner corner-sw"></span><span class="choice-corner corner-se"></span></span>"""
        val cards = listOf("""<button type="button" class="perk-card choice" id="perk-choice-empty">$corners<span class="empty-perk"><translation>tips.superbwarfare.barrel_empty</translation></span></button>""") +
            (0 until CHOICES_PER_PAGE).map { index ->
                """<button type="button" class="perk-card choice" id="perk-choice-$index">$corners<sbw-reforging-weapon id="choice-icon-$index"></sbw-reforging-weapon></button>"""
            }
        doc.getElementById("perk-choices")?.innerHTML = (0..1).joinToString("") { row ->
            """<div class="choice-grid-row">""" + cards.drop(row * CHOICES_PER_ROW).take(CHOICES_PER_ROW).joinToString("") + "</div>"
        }
        doc.getElementById("perk-choice-empty")?.addEventListener("click") {
            if (!pending && slotSelected && activeSlot in draft.getValue(activeType).indices) {
                val removed = draft.getValue(activeType).removeAt(activeSlot)
                draftLevels.remove(removed)
                activeSlot = activeSlot.coerceAtMost(draft.getValue(activeType).size)
                forceRefresh = true
            }
        }
        for (index in 0 until CHOICES_PER_PAGE) {
            doc.getElementById("perk-choice-$index")?.addEventListener("click") {
                val perk = shownChoices.getOrNull(index) ?: return@addEventListener
                if (pending || !slotSelected || perk.type != activeType) return@addEventListener
                val slot = inventorySlot ?: return@addEventListener
                val stack = localPlayer?.inventory?.getItem(slot) ?: return@addEventListener
                if (stack.item !is GunItem || !canChoose(perk, GunData.from(stack))) return@addEventListener
                val list = draft.getValue(activeType)
                if (activeSlot < list.size) {
                    val previous = list[activeSlot]
                    if (previous != perk) draftLevels.remove(previous)
                    list[activeSlot] = perk
                } else list.add(perk)
                draftLevels.putIfAbsent(perk, 1)
                forceRefresh = true
            }
        }
    }

    private fun activePerk(): Perk? = if (slotSelected) draft.getValue(activeType).getOrNull(activeSlot) else null

    private fun levelTarget(action: String, current: Int): Int = when (action) {
        "min" -> 1
        "decrease" -> (current - 1).coerceAtLeast(1)
        "increase" -> if (current < ReforgingTableMenu.MAX_PERK_LEVEL) current + 1 else current
        else -> ResonancePerkInstallMessage.maximumPerkLevel(current,
            inventorySlot?.let { localPlayer?.inventory?.getItem(it) }?.let { draftPoints(GunData.from(it).level.get()) } ?: 0,
            localPlayer?.isCreative == true)
    }

    private fun draftPoints(weaponLevel: Int): Int = ResonancePerkInstallMessage.availableUpgradePoints(
        weaponLevel, draft.values.flatten().map { draftLevels[it] ?: 1 })

    private fun canSetLevel(current: Int, target: Int): Boolean {
        if (pending || target == current || target !in 1..ReforgingTableMenu.MAX_PERK_LEVEL) return false
        val slot = inventorySlot ?: return false
        val stack = localPlayer?.inventory?.getItem(slot) ?: return false
        return localPlayer?.isCreative == true || target < current || target - current <= draftPoints(GunData.from(stack).level.get())
    }

    private fun syncDetails(doc: Document, stack: ItemStack) {
        // All preview mutations operate on copies, never on the player's live ItemStack.
        val baseline = GunData.from(stack.copy())
        val signature = ResonancePerkInstallMessage.signature(baseline)
        if (!draftReady || signature != baselineSignature) {
            slotSelected = false
            pending = false
            draftLevels.clear()
            for (type in Perk.Type.entries) {
                draft.getValue(type).apply { clear(); addAll(baseline.perk.getInstances(type).map { it.perk }) }
                baseline.perk.getInstances(type).forEach { draftLevels[it.perk] = it.level.toInt() }
            }
            baselineSignature = signature
            draftReady = true
        }
        text(doc, "weapon-name", stack.hoverName.string)
        text(doc, "weapon-description", GunResource.compute(stack).description.filterNot { it.hidden }
            .joinToString(" ") { it.asComponent().string.replace('\n', ' ') })
        text(doc, "weapon-type", Component.translatable("gun_type.superbwarfare." + baseline.get(GunProp.GUN_TYPE).name.lowercase(Locale.ROOT)).string)
        setDisplayItem(doc, "reforging-weapon-icon", stack)
        val level = baseline.level.get()
        val needed = 20.0 * level * level + 160.0 * level + 20.0
        val exp = baseline.exp.get().takeIf { it.isFinite() } ?: 0.0
        text(doc, "weapon-level", Component.translatable("des.superbwarfare.guns.level").append(level.toString()).string)
        text(doc, "weapon-manufacture-date", manufactureDate(baseline.createdTime))
        text(doc, "weapon-experience", translated("experience", format(exp, 0), format(needed, 0)))
        doc.getElementById("weapon-exp-fill")?.setInlineStyleProperty("width", ((exp / needed).coerceIn(0.0, 1.0) * 100).toString() + "%")
        perkHoverTargets.clear()
        for (type in Perk.Type.entries) renderSlots(doc, type)
        show(doc, "perk-choices-panel", slotSelected)
        if (slotSelected) renderChoices(doc, baseline) else shownChoices = emptyList()
        disable(doc, "reset-perks", pending || draft.values.all { it.isEmpty() })
        val previewStack = stack.copy()
        val preview = GunData.from(previewStack)
        ResonancePerkInstallMessage.applySelection(preview, draft, draft.values.flatten().associateWith { draftLevels[it] ?: 1 })
        text(doc, "weapon-upgrade-points", draftPoints(preview.level.get()).toString())
        val selectedPerk = activePerk()
        val selectedLevel = selectedPerk?.let { draftLevels[it] ?: 1 } ?: 1
        for (action in listOf("min", "decrease", "increase", "max")) {
            disable(doc, "perk-level-$action", selectedPerk == null || !canSetLevel(selectedLevel, levelTarget(action, selectedLevel)))
        }
        setDisplayItem(doc, "reforging-output-icon", previewStack)
        syncStats(doc, baseline, preview)
        val changed = hasChanges(baseline)
        doc.getElementById("complete-reforging")?.setAttribute("class", if (changed && !pending) "output-preview ready" else "output-preview")
        disable(doc, "complete-reforging", !changed || pending || weaponUuid == null)
    }

    private fun slotPages(type: Perk.Type) = maxOf(1, (draft.getValue(type).size + 3) / 3)

    private fun renderSlots(doc: Document, type: Perk.Type) {
        val key = type.typeName
        val entries = draft.getValue(type)
        val total = slotPages(type)
        val page = pages.getValue(type).coerceIn(0, total - 1)
        pages[type] = page
        for (position in 0..2) {
            val index = page * 3 + position
            val perk = entries.getOrNull(index)
            val selected = slotSelected && type == activeType && index == activeSlot
            attribute(doc, "perk-slot-$key-$position", "class", if (selected) "perk-card selected" else "perk-card")
            attribute(doc, "perk-slot-$key-$position", "aria-disabled", (index > entries.size || pending).toString())
            setDisplayItem(doc, "perk-icon-$key-$position", perk?.let { ItemStack(it.getItem().get()) } ?: ItemStack.EMPTY)
            text(doc, "perk-level-$key-$position", perk?.let { "Lv." + (draftLevels[it] ?: 1) } ?: "")
            show(doc, "perk-level-$key-$position", perk != null)
            show(doc, "perk-add-$key-$position", perk == null && index == entries.size)
            if (perk != null) perkHoverTargets["perk-slot-$key-$position"] = perk
        }
        renderPageIndicators(doc, "page-indicators-$key", page, total)
        disable(doc, "page-$key-previous", pending || page == 0)
        disable(doc, "page-$key-next", pending || page == total - 1)
    }

    private fun canChoose(perk: Perk, baseline: GunData): Boolean =
        localPlayer?.isCreative == true || baseline.perk.getInstances(perk.type).any { it.perk == perk }
            || (ownedPerks[perk.getItem().get()] ?: 0) > 0

    private fun renderChoices(doc: Document, baseline: GunData) {
        val entries = draft.getValue(activeType)
        activeSlot = activeSlot.coerceAtMost(entries.size)
        val selected = entries.getOrNull(activeSlot)
        availableChoices = (baseline.availablePerks(localPlayer?.isCreative == true).filter { it.type == activeType } + baseline.perk.getInstances(activeType).map { it.perk })
            .distinct().filter { it !in entries || it == selected }
        val total = maxOf(1, (availableChoices.size + CHOICES_PER_PAGE - 1) / CHOICES_PER_PAGE)
        choicePage = choicePage.coerceIn(0, total - 1)
        shownChoices = availableChoices.drop(choicePage * CHOICES_PER_PAGE).take(CHOICES_PER_PAGE)
        attribute(doc, "perk-choice-empty", "class", if (selected == null) "perk-card choice selected" else "perk-card choice")
        disable(doc, "perk-choice-empty", pending)
        for (index in 0 until CHOICES_PER_PAGE) {
            val perk = shownChoices.getOrNull(index)
            show(doc, "perk-choice-$index", perk != null)
            setDisplayItem(doc, "choice-icon-$index", perk?.let { ItemStack(it.getItem().get()) } ?: ItemStack.EMPTY)
            if (perk == null) continue
            val state = (if (perk == selected) " selected" else "") + (if (!canChoose(perk, baseline)) " unavailable" else "")
            attribute(doc, "perk-choice-$index", "class", "perk-card choice$state")
            perkHoverTargets["perk-choice-$index"] = perk
        }
        renderPageIndicators(doc, "choice-page-indicators", choicePage, total)
        disable(doc, "choice-previous", choicePage == 0 || pending)
        disable(doc, "choice-next", choicePage == total - 1 || pending)
    }

    private fun hasChanges(data: GunData) = Perk.Type.entries.any { type ->
        draft.getValue(type) != data.perk.getInstances(type).map { it.perk }
            || data.perk.getInstances(type).any { (draftLevels[it.perk] ?: 1) != it.level.toInt() }
    }

    private fun renderPageIndicators(doc: Document, id: String, current: Int, total: Int) {
        val container = doc.getElementById(id) ?: return
        if (indicatorCounts[id] != total) {
            container.innerHTML = (0 until total).joinToString("") { page ->
                """<span class="page-mark" id="$id-$page"></span>"""
            }
            indicatorCounts[id] = total
        }
        for (page in 0 until total) attribute(doc, "$id-$page", "class", if (page == current) "page-mark active" else "page-mark")
    }

    /** Painted after AUI's deferred item layer so models cannot cover the tooltip. No item icon/name list. */
    private fun renderPerkTooltip(graphics: GuiGraphics, doc: Document) {
        val mouse = doc.mouseDocumentPosition
        val perk = if (mc.isWindowActive) perkHoverTargets.entries.firstOrNull { (id, _) ->
            val element = doc.getElementById(id) ?: return@firstOrNull false
            val bounds = element.boundingClientRect
            mouse.x >= bounds.x && mouse.x < bounds.x + bounds.width && mouse.y >= bounds.y && mouse.y < bounds.y + bounds.height
        }?.value else null
        if (perk == null) {
            tooltipPerk = null
            return
        }
        if (tooltipPerk != perk) {
            tooltipPerk = perk
            val stack = ItemStack(perk.getItem().get())
            val type = Component.translatable("perk.superbwarfare.slot_" + perk.type.typeName)
            tooltipTitle = tooltipText(stack.hoverName.string, 22.0, "#f3f3f5", 28.0)
            tooltipType = tooltipText(type.string, 14.0, "#b9bbc1", 20.0)
            val description = mutableListOf<Component>()
            stack.item.appendHoverText(stack, localPlayer?.level(), description, TooltipFlag.NORMAL)
            val slotLine = Component.translatable("perk.superbwarfare.slot").append(type).string
            val measure = tooltipText("", 14.0, "#c7c9cf", 20.0)
            val splitter = StringSplitter { codePoint, _ ->
                Text.measureLine(measure, String(Character.toChars(codePoint))).toFloat()
            }
            tooltipDescription = description.filter { it.string.isNotBlank() && it.string != slotLine }
                .flatMap { splitter.splitLines(it, 312, Style.EMPTY) }
                .map { tooltipText(it.string, 14.0, "#c7c9cf", 20.0) }
        }
        val viewport = doc.viewport
        val width = 340f
        val height = 66f + 24f + tooltipDescription.size * 20f
        val scale = minOf(1f, (viewport.layoutWidth() - 16f) / width, (viewport.layoutHeight() - 16f) / height).coerceAtLeast(0.1f)
        val visibleWidth = width * scale
        val visibleHeight = height * scale
        val desiredX = if (mouse.x + 16 + visibleWidth <= viewport.layoutWidth() - 8) mouse.x.toFloat() + 16f
            else mouse.x.toFloat() - visibleWidth - 16f
        val left = desiredX.coerceIn(8f, (viewport.layoutWidth() - visibleWidth - 8f).coerceAtLeast(8f))
        val top = (mouse.y.toFloat() + 16f).coerceIn(8f, (viewport.layoutHeight() - visibleHeight - 8f).coerceAtLeast(8f))
        graphics.flush()
        val pose = graphics.pose()
        val color = RenderSystem.getShaderColor().copyOf()
        pose.pushPose()
        FontDrawer.pushDocumentPixelScale(viewport.scissorScale() * scale)
        try {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            pose.scale(viewport.renderScale(), viewport.renderScale(), 1f)
            pose.translate(left.toDouble(), top.toDouble(), Base.getFlatOverlayZ().toDouble())
            pose.scale(scale, scale, 1f)
            graphics.fill(0, 0, width.toInt(), height.toInt(), 0xf016171a.toInt())
            graphics.fill(0, 0, width.toInt(), 66, 0xf03a3b3e.toInt())
            graphics.flush()
            pose.translate(0.0, 0.0, 1.0)
            FontDrawer.drawFont(pose, tooltipTitle, Position(14.0, 8.0))
            FontDrawer.drawFont(pose, tooltipType, Position(14.0, 38.0))
            tooltipDescription.forEachIndexed { index, line ->
                FontDrawer.drawFont(pose, line, Position(14.0, 78.0 + index * 20.0))
            }
            graphics.flush()
        } finally {
            Base.commitLocalDraws()
            FontDrawer.popDocumentPixelScale()
            pose.popPose()
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3])
        }
    }

    private fun tooltipText(content: String, size: Double, foreground: String, leading: Double) = Text().apply {
        fontFamily = "ResonanceUI, sans-serif"
        fontSize = size
        fontWeight = 400
        lineHeight = leading
        color = Color(foreground)
        this.content = content
    }

    private fun submit() {
        if (pending) return
        val slot = inventorySlot ?: return
        val stack = localPlayer?.inventory?.getItem(slot) ?: return
        if (stack.item !is GunItem || !draftReady) return
        val data = GunData.from(stack)
        val identity = weaponUuid ?: return
        if (data.uuid?.toString() != identity || !hasChanges(data)) return
        pending = true
        pendingAt = System.currentTimeMillis()
        slotSelected = false
        sendPacketToServer(ResonancePerkInstallMessage(tablePos.asLong(), slot, identity, baselineSignature,
            draft.getValue(Perk.Type.AMMO).map { it.name },
            draft.getValue(Perk.Type.FUNCTIONAL).map { it.name },
            draft.getValue(Perk.Type.DAMAGE).map { it.name },
            draft.values.flatten().associate { it.name to (draftLevels[it] ?: 1) }))
        forceRefresh = true
    }

    private fun syncStats(doc: Document, baseline: GunData, preview: GunData) {
        val stats = listOf(Triple("rpm", 1200.0, 0), Triple("damage", 100.0, 2), Triple("headshot", 4.0, 2),
            Triple("recoil_x", 0.005, 5), Triple("recoil_y", 0.04, 5), Triple("spread", 10.0, 2),
            Triple("normal_reload_time", 6.0, 2), Triple("magazine", 100.0, 0))
        fun value(data: GunData, key: String) = when (key) {
            "rpm" -> (data.get(GunProp.RPM) * data.get(GunProp.RPM_MULTIPLIER)).let { if (it.isFinite()) it.roundToInt().toDouble() else Double.NaN }
            "damage" -> data.get(GunProp.DAMAGE)
            "headshot" -> data.get(GunProp.HEADSHOT)
            "recoil_x" -> data.get(GunProp.RECOIL_X)
            "recoil_y" -> data.get(GunProp.RECOIL_Y)
            "spread" -> data.get(GunProp.SPREAD)
            "normal_reload_time" -> data.get(GunProp.NORMAL_RELOAD_TIME) / 20.0
            else -> data.get(GunProp.MAGAZINE).toDouble()
        }
        for ((key, reference, decimals) in stats) {
            val before = value(baseline, key)
            val after = value(preview, key)
            val valid = before.isFinite() && after.isFinite()
            val delta = after - before
            val unit = when (key) { "rpm" -> " rpm"; "headshot" -> "×"; "normal_reload_time" -> " s"; else -> "" }
            text(doc, "stat-value-$key", if (valid) format(after, decimals) + unit else "—")
            text(doc, "stat-delta-$key", if (!valid || abs(delta) < 0.000001) "" else "(" + (if (delta > 0) "+" else "") + format(delta, decimals) + ")")
            val good = if (key.startsWith("recoil") || key == "spread" || key == "normal_reload_time") delta < 0 else delta > 0
            val color = if (good) "#52fcda" else "#ed777d"
            doc.getElementById("stat-delta-$key")?.setInlineStyleProperty("color", color)
            val from = if (valid) (before / reference).coerceIn(0.0, 1.0) else 0.0
            val to = if (valid) (after / reference).coerceIn(0.0, 1.0) else 0.0
            doc.getElementById("stat-bar-$key")?.setInlineStyleProperty("width", (min(from, to) * 100).toString() + "%")
            doc.getElementById("stat-change-$key")?.apply {
                setInlineStyleProperty("left", (min(from, to) * 100).toString() + "%")
                setInlineStyleProperty("width", (abs(to - from) * 100).toString() + "%")
                setInlineStyleProperty("background-color", color)
            }
        }
    }

    private fun updateResetHold(doc: Document) {
        if (resetHoldStartedAt == 0L) return
        val button = doc.getElementById("reset-perks") ?: return cancelResetHold(doc)
        val bounds = button.boundingClientRect
        val mouse = doc.mouseDocumentPosition
        if (pending || button.isDisabled || !mc.isWindowActive
            || GLFW.glfwGetMouseButton(mc.window.window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS
            || mouse.x < bounds.x || mouse.x > bounds.x + bounds.width || mouse.y < bounds.y || mouse.y > bounds.y + bounds.height) {
            cancelResetHold(doc)
            return
        }
        val progress = ((System.nanoTime() - resetHoldStartedAt).toDouble() / RESET_HOLD_NANOS).coerceIn(0.0, 1.0)
        paintResetProgress(doc, progress)
        if (progress >= 1.0) {
            cancelResetHold(doc)
            draft.values.forEach { it.clear() }
            draftLevels.clear()
            Perk.Type.entries.forEach { pages[it] = 0 }
            slotSelected = false
            activeSlot = 0
            choicePage = 0
            forceRefresh = true
        }
    }

    private fun cancelResetHold(doc: Document) {
        resetHoldStartedAt = 0L
        paintResetProgress(doc, 0.0)
    }

    private fun paintResetProgress(doc: Document, progress: Double) {
        val step = (progress * 100).toInt()
        if (step == resetProgressStep) return
        resetProgressStep = step
        doc.getElementById("reset-ring-progress")?.setAttribute("d", resetRingPath(progress))
    }

    /** Equal-radius circular segments, at most a quarter turn each, keep the hold indicator smooth and concentric. */
    private fun resetRingPath(progress: Double): String {
        val fraction = progress.coerceIn(0.0, 1.0)
        if (fraction <= 0) return ""
        val segments = ceil(fraction * 4).toInt()
        val sweep = fraction * 2 * PI / segments
        val handle = 4.0 / 3.0 * tan(sweep / 4)
        val radius = 27.0
        return buildString {
            append("M32 5")
            for (segment in 0 until segments) {
                val start = -PI / 2 + segment * sweep
                val end = start + sweep
                append(" C").append(32 + radius * (cos(start) - handle * sin(start)))
                append(' ').append(32 + radius * (sin(start) + handle * cos(start)))
                append(' ').append(32 + radius * (cos(end) + handle * sin(end)))
                append(' ').append(32 + radius * (sin(end) - handle * cos(end)))
                append(' ').append(32 + radius * cos(end)).append(' ').append(32 + radius * sin(end))
            }
            if (fraction >= 1) append(" Z")
        }
    }

    private fun choosePreviewCandidate(): Candidate? {
        val previous = previewedCandidate ?: return candidates.firstOrNull()
        // Runtime NBT changes do not change identity. Prefer the same slot, then follow a moved UUID.
        return candidates.firstOrNull {
            it.slot == previous.slot && if (previous.uuid != null) it.uuid == previous.uuid else it.stack.item === previous.stack.item
        } ?: previous.uuid?.let { uuid -> candidates.firstOrNull { it.uuid == uuid } } ?: candidates.firstOrNull()
    }

    private fun showPreview(doc: Document, candidate: Candidate?) {
        previewedCandidate = candidate
        val stack = candidate?.stack ?: ItemStack.EMPTY
        setDisplayItem(doc, "selection-preview", stack)
        text(doc, "selection-weapon-name", if (stack.isEmpty) translated("no_weapons") else stack.hoverName.string)
    }
    private fun translated(suffix: String, vararg args: Any): String =
        Component.translatable("container.superbwarfare.reforging_table.$suffix", *args).string

    private fun manufactureDate(createdTime: Long): String {
        val date = try {
            if (createdTime > 0) Instant.ofEpochMilli(createdTime).atZone(ZoneId.systemDefault()).toLocalDate() else null
        } catch (_: DateTimeException) {
            null
        } ?: return translated("manufacture_date", "--", "--", "--")
        return translated("manufacture_date", date.year.toString().padStart(4, '0'),
            date.monthValue.toString().padStart(2, '0'), date.dayOfMonth.toString().padStart(2, '0'))
    }

    private fun text(doc: Document, id: String, value: String) {
        doc.getElementById(id)?.let { if (it.textContent != value) it.textContent = value }
    }
    private fun disable(doc: Document, id: String, disabled: Boolean) {
        doc.getElementById(id)?.let { if (it.isDisabled != disabled) it.isDisabled = disabled }
    }
    private fun attribute(doc: Document, id: String, name: String, value: String) {
        doc.getElementById(id)?.let { if (it.getAttribute(name) != value) it.setAttribute(name, value) }
    }
    private fun show(doc: Document, id: String, visible: Boolean) {
        doc.getElementById(id)?.let {
            val display = if (visible) "block" else "none"
            if (it.getInlineStylePropertyValue("display") != display) it.setInlineStyleProperty("display", display)
        }
    }
    private fun setDisplayItem(doc: Document, id: String, stack: ItemStack) {
        (doc.getElementById(id) as? ResonanceWeaponIcon)?.setStack(stack)
    }
    private fun format(value: Double, decimals: Int): String {
        val number = String.format(Locale.ROOT, "%." + decimals + "f", value)
        return if (decimals == 0) number else number.trimEnd('0').trimEnd('.')
    }

    /** Screen-local native model painter with no inventory decorations. */
    private class ResonanceWeaponIcon(document: Document) : Element(document, TAG), BodyRenderNodeProvider {
        private var stack = ItemStack.EMPTY
        fun setStack(value: ItemStack) {
            if (ItemStack.matches(stack, value)) return
            stack = value.copy()
            document.markDirty(this, Drawer.REPAINT)
        }
        override fun createBodyRenderNodes(): List<RenderNode> = listOf(
            RenderNode.ItemNode(this, { stack }, { !stack.isEmpty }, {
                val size = Rect.of(this).bodyRectSize
                min(size.width(), size.height()).coerceAtLeast(0.0) / 16.0
            }, { 0 }, false)
        )
        companion object {
            private const val TAG = "SBW-REFORGING-WEAPON"
            private var registered = false
            fun register() {
                if (registered) return
                Element.register(TAG) { document, _ -> ResonanceWeaponIcon(document) }
                registered = true
            }
        }
    }

    companion object {
        init { ResonanceWeaponIcon.register() }
        private const val GRID_SIZE = 36
        private const val CHOICES_PER_ROW = 6
        private const val CHOICES_PER_PAGE = CHOICES_PER_ROW * 2 - 1 // Reserve the first cell for removing the selected Perk.
        private const val RESET_HOLD_NANOS = 1_200_000_000.0
        private const val SELECTION_PATH = "superbwarfare/screens/resonance_weapon_selection.html"
        private const val REFORGING_PATH = "superbwarfare/screens/resonance_reforging.html"
    }
}
