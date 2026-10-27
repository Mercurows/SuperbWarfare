package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.ResonancePerkInstallMessage
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import com.sighs.apricityui.event.MouseEvent
import com.sighs.apricityui.init.Document
import com.sighs.apricityui.init.Element
import com.sighs.apricityui.render.BodyRenderNodeProvider
import com.sighs.apricityui.render.Drawer
import com.sighs.apricityui.render.Rect
import com.sighs.apricityui.render.RenderNode
import com.sighs.apricityui.screen.ApricityScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.lwjgl.glfw.GLFW
import java.util.*
import kotlin.math.*

/** Inventory selection and a local Perk draft, committed atomically by the server. */
@OnlyIn(Dist.CLIENT)
class ResonanceReforgingScreen(private val tablePos: BlockPos, private val inventorySlot: Int? = null) : ApricityScreen(
    if (inventorySlot == null) SELECTION_PATH else REFORGING_PATH
) {
    private data class Candidate(val slot: Int, val stack: ItemStack)
    private var documentGeneration = -1L
    private var candidates = emptyList<Candidate>()
    private var displayedWeapon = ItemStack.EMPTY
    private var weaponUuid: String? = null
    private var baselineSignature = ""
    private var draftReady = false
    private val draft = Perk.Type.entries.associateWith { mutableListOf<Perk>() }
    private val pages = Perk.Type.entries.associateWith { 0 }.toMutableMap()
    private var activeType = Perk.Type.AMMO
    private var activeSlot = 0
    private var slotSelected = false
    private var choicePage = 0
    private var availableChoices = emptyList<Perk>()
    private var ownedPerks = emptyMap<Item, Int>()
    private var forceRefresh = true
    private var pending = false
    private var pendingId = 0L
    private var pendingAt = 0L
    private var status = ""
    private var resetHoldStartedAt = 0L
    private var resetProgressStep = -1

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
        if (pending && System.currentTimeMillis() - pendingAt > 10000) {
            pending = false
            status = "提交超时，请检查武器数据后重试"
            forceRefresh = true
        }
        syncPage()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val doc = linkedDocument
        if (doc != null && (!doc.isCurrentGeneration(documentGeneration) || forceRefresh)) syncPage()
        if (doc != null) updateResetHold(doc)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun removed() {
        resetHoldStartedAt = 0L
        super.removed()
    }

    override fun onClose() {
        if (inventorySlot == null) super.onClose() else mc.setScreen(ResonanceReforgingScreen(tablePos))
    }

    fun onInstallResult(requestId: Long, success: Boolean, message: String) {
        if (pendingId != requestId) return
        pending = false
        status = message
        if (success) {
            draftReady = false
            slotSelected = false
        }
        forceRefresh = true
    }

    private fun syncPage() {
        val doc = linkedDocument ?: return
        val player = localPlayer ?: return
        val rebuilt = !doc.isCurrentGeneration(documentGeneration)
        if (rebuilt) {
            resetHoldStartedAt = 0L
            resetProgressStep = -1
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
                    cell.addEventListener("mouseenter") { candidates.getOrNull(index)?.let { showPreview(doc, it.stack) } }
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
            if (rebuilt || forceRefresh || !ItemStack.matches(displayedWeapon, stack) || owned != ownedPerks) {
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
            candidates = owned.map { Candidate(it.index, it.value.copy()) }
            for (index in 0 until GRID_SIZE) {
                val candidate = candidates.getOrNull(index)
                doc.getElementById("weapon-$index")?.apply {
                    setAttribute("class", if (candidate == null) "weapon-cell" else "weapon-cell occupied")
                    setAttribute("aria-disabled", (candidate == null).toString())
                }
                setDisplayItem(doc, "weapon-item-$index", candidate?.stack ?: ItemStack.EMPTY)
                text(doc, "weapon-label-$index", candidate?.stack?.hoverName?.string ?: "")
            }
            text(doc, "weapon-count", candidates.size.toString() + " / $GRID_SIZE")
            doc.getElementById("selection-empty")?.setInlineStyleProperty("display", if (candidates.isEmpty()) "flex" else "none")
            showPreview(doc, candidates.firstOrNull()?.stack ?: ItemStack.EMPTY)
        }
        forceRefresh = false
    }

    private fun bindReforging(doc: Document) {
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
        doc.getElementById("remove-perk")?.addEventListener("click") {
            if (!pending && activeSlot in draft.getValue(activeType).indices) {
                draft.getValue(activeType).removeAt(activeSlot)
                activeSlot = activeSlot.coerceAtMost(draft.getValue(activeType).size)
                forceRefresh = true
                status = "待确认：已卸下 Perk"
            }
        }
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
    }

    private fun syncDetails(doc: Document, stack: ItemStack) {
        // All preview mutations operate on copies, never on the player's live ItemStack.
        val baseline = GunData.from(stack.copy())
        val signature = ResonancePerkInstallMessage.signature(baseline)
        if (!draftReady || signature != baselineSignature) {
            slotSelected = false
            if (draftReady && signature != baselineSignature && !pending) status = "武器 Perk 已更新"
            for (type in Perk.Type.entries) {
                draft.getValue(type).apply { clear(); addAll(baseline.perk.getInstances(type).map { it.perk }) }
            }
            baselineSignature = signature
            draftReady = true
        }
        text(doc, "weapon-name", stack.hoverName.string)
        setDisplayItem(doc, "reforging-weapon-icon", stack)
        val level = baseline.level.get()
        val needed = 20.0 * level * level + 160.0 * level + 20.0
        val exp = baseline.exp.get().takeIf { it.isFinite() } ?: 0.0
        text(doc, "weapon-level", "等级" + level)
        text(doc, "weapon-experience", format(exp, 0) + " / " + format(needed, 0) + " EXP")
        doc.getElementById("weapon-exp-fill")?.setInlineStyleProperty("width", ((exp / needed).coerceIn(0.0, 1.0) * 100).toString() + "%")
        for (type in Perk.Type.entries) renderSlots(doc, baseline, type)
        doc.getElementById("perk-choices-panel")?.setInlineStyleProperty("display", if (slotSelected) "block" else "none")
        if (slotSelected) renderChoices(doc, baseline)
        disable(doc, "reset-perks", pending || draft.values.all { it.isEmpty() })
        val previewStack = stack.copy()
        val preview = GunData.from(previewStack)
        ResonancePerkInstallMessage.applySelection(preview, draft)
        val spentPoints = Perk.Type.entries.sumOf { type -> preview.perk.getInstances(type).sumOf { it.level.toInt() - 1 } }
        text(doc, "weapon-upgrade-points", (preview.level.get() - spentPoints).coerceAtLeast(0).toString())
        setDisplayItem(doc, "reforging-output-icon", previewStack)
        syncStats(doc, baseline, preview)
        val changed = hasChanges(baseline)
        doc.getElementById("complete-reforging")?.setAttribute("class", if (changed && !pending) "output-preview ready" else "output-preview")
        disable(doc, "complete-reforging", !changed || pending || weaponUuid == null)
        text(doc, "complete-label", if (pending) "安装中…" else "完成")
        text(doc, "reforging-status", if (pending) "正在由服务端确认安装…" else status)
    }

    private fun slotPages(type: Perk.Type) = maxOf(1, (draft.getValue(type).size + 3) / 3)

    private fun renderSlots(doc: Document, baseline: GunData, type: Perk.Type) {
        val key = type.typeName
        val entries = draft.getValue(type)
        val total = slotPages(type)
        val page = pages.getValue(type).coerceIn(0, total - 1)
        pages[type] = page
        val levels = baseline.perk.getInstances(type).associate { it.perk to it.level.toInt() }
        doc.getElementById("slots-$key")?.innerHTML = (0..2).joinToString("") { position ->
            val index = page * 3 + position
            val perk = entries.getOrNull(index)
            val state = if (slotSelected && type == activeType && index == activeSlot) " selected" else ""
            val body = if (perk != null) {
                val level = levels[perk] ?: 1
                """<sbw-reforging-weapon id="perk-icon-$key-$position"></sbw-reforging-weapon><span class="perk-level">Lv.$level</span>"""
            } else if (index == entries.size) """<span class="add-perk">+</span>""" else ""
            val disabled = index > entries.size || pending
            """<button type="button" class="perk-card$state" id="perk-slot-$key-$position" aria-disabled="$disabled">$body</button>"""
        }
        for (position in 0..2) {
            val index = page * 3 + position
            entries.getOrNull(index)?.let { setDisplayItem(doc, "perk-icon-$key-$position", ItemStack(it.getItem().get())) }
            doc.getElementById("perk-slot-$key-$position")?.addEventListener("click") {
                if (!pending && index <= draft.getValue(type).size) {
                    activeType = type; activeSlot = index; slotSelected = true; choicePage = 0; forceRefresh = true
                }
            }
        }
        text(doc, "page-label-$key", (page + 1).toString() + " / " + total)
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
        availableChoices = (baseline.availablePerks().filter { it.type == activeType } + baseline.perk.getInstances(activeType).map { it.perk })
            .distinct().filter { it !in entries || it == selected }
        val total = maxOf(1, (availableChoices.size + CHOICES_PER_PAGE - 1) / CHOICES_PER_PAGE)
        choicePage = choicePage.coerceIn(0, total - 1)
        val shown = availableChoices.drop(choicePage * CHOICES_PER_PAGE).take(CHOICES_PER_PAGE)
        val cards = shown.mapIndexed { index, perk ->
            val state = (if (perk == selected) " selected" else "") + (if (!canChoose(perk, baseline)) " unavailable" else "")
            """<button type="button" class="perk-card choice$state" id="perk-choice-$index"><sbw-reforging-weapon id="choice-icon-$index"></sbw-reforging-weapon><span class="choice-caption" id="choice-caption-$index"></span></button>"""
        }
        doc.getElementById("perk-choices")?.innerHTML = (0..1).joinToString("") { row ->
            """<div class="choice-grid-row">""" + cards.drop(row * CHOICES_PER_ROW).take(CHOICES_PER_ROW).joinToString("") + "</div>"
        }
        for ((index, perk) in shown.withIndex()) {
            val item = ItemStack(perk.getItem().get())
            setDisplayItem(doc, "choice-icon-$index", item)
            text(doc, "choice-caption-$index", item.hoverName.string)
            doc.getElementById("perk-choice-$index")?.addEventListener("click") {
                if (pending || perk.type != activeType) return@addEventListener
                if (!canChoose(perk, baseline)) status = "需要背包中对应的 Perk 物品" else {
                    val list = draft.getValue(activeType)
                    if (activeSlot < list.size) list[activeSlot] = perk else list.add(perk)
                    status = "待确认：" + item.hoverName.string
                }
                forceRefresh = true
            }
        }
        text(doc, "selected-perk-name", selected?.let { ItemStack(it.getItem().get()).hoverName.string } ?: "空槽位")
        text(doc, "choice-page-label", if (availableChoices.isEmpty()) "无可用 Perk" else (choicePage + 1).toString() + " / " + total)
        disable(doc, "remove-perk", selected == null || pending)
        disable(doc, "choice-previous", choicePage == 0 || pending)
        disable(doc, "choice-next", choicePage == total - 1 || pending)
    }

    private fun hasChanges(data: GunData) = Perk.Type.entries.any { type ->
        draft.getValue(type) != data.perk.getInstances(type).map { it.perk }
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
        pendingId = System.nanoTime()
        pendingAt = System.currentTimeMillis()
        sendPacketToServer(ResonancePerkInstallMessage(tablePos.asLong(), slot, identity, baselineSignature,
            draft.getValue(Perk.Type.AMMO).map { it.name },
            draft.getValue(Perk.Type.FUNCTIONAL).map { it.name },
            draft.getValue(Perk.Type.DAMAGE).map { it.name }, pendingId))
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
            Perk.Type.entries.forEach { pages[it] = 0 }
            slotSelected = false
            activeSlot = 0
            choicePage = 0
            status = "已清空全部 Perk 方案，点击“完成”提交"
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
        val angle = -PI / 2 + 2 * PI * progress
        val x = 32 + 26 * cos(angle)
        val y = 32 + 26 * sin(angle)
        val path = when {
            progress <= 0 -> ""
            progress >= 1 -> "M32 6 A26 26 0 1 1 32 58 A26 26 0 1 1 32 6"
            else -> "M32 6 A26 26 0 " + (if (progress > 0.5) "1" else "0") + " 1 " + x + " " + y
        }
        doc.getElementById("reset-ring-progress")?.setAttribute("d", path)
    }

    private fun showPreview(doc: Document, stack: ItemStack) {
        setDisplayItem(doc, "selection-preview", stack)
        text(doc, "selection-weapon-name", if (stack.isEmpty) "暂无可选武器" else stack.hoverName.string)
    }
    private fun text(doc: Document, id: String, value: String) {
        doc.getElementById(id)?.let { if (it.textContent != value) it.textContent = value }
    }
    private fun disable(doc: Document, id: String, disabled: Boolean) {
        doc.getElementById(id)?.isDisabled = disabled
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
        private const val CHOICES_PER_PAGE = 12
        private const val RESET_HOLD_NANOS = 1_200_000_000.0
        private const val SELECTION_PATH = "superbwarfare/screens/resonance_weapon_selection.html"
        private const val REFORGING_PATH = "superbwarfare/screens/resonance_reforging.html"
    }
}
