package com.atsuishio.superbwarfare.client.screens
import com.atsuishio.superbwarfare.client.tooltip.ClientAttachmentImageTooltip
import com.atsuishio.superbwarfare.client.tooltip.component.AttachmentImageComponent
import com.atsuishio.superbwarfare.config.server.AttachmentConfig
import com.atsuishio.superbwarfare.data.attachment.*
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.GunState
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.event.ClientEventHandler.editModelShake
import com.atsuishio.superbwarfare.event.ClientEventHandler.editingAttachmentType
import com.atsuishio.superbwarfare.event.ClientEventHandler.onCloseEditScreen
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.attachment.AttachmentProvider
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.AttachmentAdjustMessage
import com.atsuishio.superbwarfare.network.message.send.AttachmentInstallMessage
import com.atsuishio.superbwarfare.network.message.send.AttachmentSkinMessage
import com.atsuishio.superbwarfare.network.message.send.EditMessage
import com.atsuishio.superbwarfare.tools.BoneScreenPoint
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexConsumer
import com.sighs.apricityui.element.Input
import com.sighs.apricityui.element.Select
import com.sighs.apricityui.event.Event
import com.sighs.apricityui.event.KeyEvent
import com.sighs.apricityui.event.MouseEvent
import com.sighs.apricityui.init.Document
import com.sighs.apricityui.init.Element
import com.sighs.apricityui.layout.Position
import com.sighs.apricityui.parser.Color
import com.sighs.apricityui.render.Base
import com.sighs.apricityui.render.FontDrawer
import com.sighs.apricityui.screen.ApricityScreen
import com.sighs.apricityui.style.Text
import net.minecraft.client.StringSplitter
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.locale.Language
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.lwjgl.glfw.GLFW
import java.util.*
import kotlin.math.*
import com.sighs.apricityui.element.Item as AuiItem

/**
 * 改装界面：骨骼挂点附近显示圆点，悬停时显示槽位名，已安装的挂点标为青色。
 * 已安装配件另放图标并用青色细线相连。
 * 点位与图标均可打开二级菜单。可装列表由枪械数据与背包共同决定，安装仍由服务端校验。
 * 候选列表按兼容状态与背包缓存；固定数量的 DOM 格子随滚动复用，投影位置、显隐与样式增量同步。
 */
@OnlyIn(Dist.CLIENT)
class WeaponEditScreen(private val stack: ItemStack) : ApricityScreen(DOCUMENT_PATH) {
    private var openingTicks = 0
    private var documentGeneration = -1L
    private val shownItems = mutableMapOf<String, String>()
    private val inlineStyles = IdentityHashMap<Element, MutableMap<String, String>>()
    private val pendingStyles = Collections.newSetFromMap(IdentityHashMap<Element, Boolean>())
    private val connectors = mutableMapOf<String, Pair<Box, Box>>()
    private val iconBounds = mutableMapOf<String, Box>()
    private var panelIds: List<ResourceLocation> = emptyList()
    private var panelType: AttachmentType? = null
    private var panelInstalled: ResourceLocation? = null
    private var panelSkinTarget: AttachmentSkinMessage? = null
    private var panelSkins: List<AttachmentSkinInfo> = emptyList()
    private var panelSkin: String? = null
    private val gridRows = mutableListOf<GridRow>()
    private val gridWindow = RecyclingRowWindow(GRID_WINDOW_ROWS)
    private var gridSelected: ResourceLocation? = null
    private var candidateKey: CandidateKey? = null
    private val compatibleBySlot = mutableMapOf<AttachmentType, List<ResourceLocation>>()
    private val installableBySlot = mutableMapOf<AttachmentType, List<ResourceLocation>>()
    private var ownedAttachments: Set<ResourceLocation> = emptySet()
    private var inventoryCheckTick = Long.MIN_VALUE
    private var inventoryCreative = false
    private var hoveredItem: String? = null
    private var tooltipKey: String? = null
    private var tooltipLines: List<List<TooltipRun>> = emptyList()
    private var tooltipWrapWidth = 0
    private var viewportWidth = 0f
    private var viewportHeight = 0f
    private val adjustmentDraft = AttachmentEditDraft()
    private var statsOpen = false
    private val statRows = mutableListOf<StatRow>()
    private var statsRefreshTick = Long.MIN_VALUE
    private var statsBaseline: GunData? = null
    private var statsState: GunState? = null
    private var statsPerks: CompoundTag? = null
    private var statsAttachments: CompoundTag? = null
    private var statsDataVersion = -1

    override fun isPauseScreen(): Boolean = false

    override fun tick() {
        super.tick()
        adjustmentDraft.tick()
        if (openingTicks < MOUNT_REVEAL_DELAY_TICKS) openingTicks++
    }

    override fun init() {
        super.init()
        clearUiState()
        WeaponEditMountTracker.clear()
        linkedDocument?.let(::prepareDocument)
    }

    override fun removed() {
        discardAdjustments()
        openingTicks = 0
        clearUiState()
        WeaponEditMountTracker.clear()
        onCloseEditScreen()
        super.removed()
    }

    private fun clearUiState() {
        shownItems.clear()
        inlineStyles.clear()
        pendingStyles.clear()
        connectors.clear()
        iconBounds.clear()
        panelIds = emptyList()
        panelType = null
        panelInstalled = null
        panelSkinTarget = null
        panelSkins = emptyList()
        panelSkin = null
        gridRows.clear()
        gridWindow.reset()
        gridSelected = null
        candidateKey = null
        compatibleBySlot.clear()
        installableBySlot.clear()
        ownedAttachments = emptySet()
        inventoryCheckTick = Long.MIN_VALUE
        hoveredItem = null
        tooltipKey = null
        tooltipLines = emptyList()
        tooltipWrapWidth = 0
        documentGeneration = -1L
        adjustmentDraft.discard()
        statRows.clear()
        statsRefreshTick = Long.MIN_VALUE
        statsBaseline = null
        statsState = null
        statsPerks = null
        statsAttachments = null
        statsDataVersion = -1
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val doc = linkedDocument ?: return
        val player = localPlayer ?: return
        val held = player.mainHandItem
        if (held.item !is GunItem || held.item !== stack.item) {
            onClose()
            return
        }
        if (!doc.isCurrentGeneration(documentGeneration)) prepareDocument(doc)
        val viewport = doc.viewportSize
        viewportWidth = viewport.width().toFloat()
        viewportHeight = viewport.height().toFloat()
        doc.getElementById("title-weapon")?.let {
            val name = held.hoverName.string
            if (it.textContent != name) it.textContent = name
        }
        connectors.clear()
        iconBounds.clear()
        val data = GunData.from(held)
        refreshAttachmentLists(player, data)
        val settings = syncAdjustments(doc, data)
        val stats = syncStats(doc, data, player.level().gameTime)
        val reserved = listOfNotNull(settings, stats) + Box(0f, 0f, viewportWidth, TITLE_BAR_HEIGHT)
        var mounts = syncMounts(doc, data, reserved)
        val panel = syncPanel(doc, player, data, mounts, reserved)
        // 紧凑窗口的点位可能占满菜单四周；先确定菜单，再把点位移到剩余空间。
        if (panel != null && mounts.any { it.bounds.overlaps(panel, 6f) }) {
            mounts = syncMounts(doc, data, reserved + panel)
            mounts.firstOrNull { it.type == panelType }?.let {
                connect("panel", it.bounds, panel)
            }
        }
        val obstacles = syncIcons(doc, mounts, reserved + listOfNotNull(panel))
        syncMountLabels(doc, mounts, obstacles)
        flushStyles()
        // 连线先提交，保持在菜单和物品下方；圆圈和外框则在 AUI 绘制完成后单独提交。
        drawDecorationPass(graphics, doc, false) { vertices, matrix ->
            for ((mount, target) in connectors.values) drawConnector(vertices, matrix, mount, target)
            val scale = editorPanelScale()
            if (statsOpen && stats != null) {
                val chartTop = STATS_CHART_TOP * scale
                drawPanelBackground(vertices, matrix,
                    Box(stats.left, stats.top + chartTop, stats.width, stats.height - chartTop), scale, PANEL_BACKGROUND)
            }
            panel?.let {
                drawPanelBackground(vertices, matrix, it, scale, PANEL_BACKGROUND)
                drawQuad(vertices, matrix, it.left + 6f * scale, it.top + 23f * scale,
                    it.left + 174f * scale, it.top + 23.5f * scale, PANEL_BORDER)
                // 与设置分隔线一样独立绘制，不依赖滚动容器或按钮悬停触发 AUI 边框批次。
                drawQuad(vertices, matrix, it.left + 6f * scale, it.top + 67f * scale,
                    it.left + 174f * scale, it.top + 67.5f * scale, PANEL_BORDER)
            }
            settings?.let {
                drawPanelBackground(vertices, matrix, it, scale, PANEL_BACKGROUND)
                drawQuad(vertices, matrix, it.left + 5f * scale, it.top + 23f * scale,
                    it.left + 173f * scale, it.top + 23.5f * scale, PANEL_BORDER)
            }
        }
        // 平面 UI 不应与物品模型残留的深度互相遮挡；由 AUI Screen 统一绘制，避免全局 overlay 再画一次。
        graphics.flush()
        Base.pushDepthTest(false)
        try {
            super.render(graphics, mouseX, mouseY, partialTick)
        } finally {
            Base.popDepthTest()
        }
        renderMountDecorations(graphics, doc, mounts)
        renderAttachmentTooltip(graphics, doc)
    }

    // AUI 在 Screen 的 Pre 事件中派发 DOM 滚轮；不把未处理的滚轮传给游戏。
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalScroll: Double, delta: Double): Boolean = true

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val skinSelect = linkedDocument?.getElementById("panel-skin") as? Select
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && skinSelect?.isPopupOpen == true) {
            skinSelect.closePopup()
            return true
        }
        if (keyCode == ModKeyMappings.EDIT_MODE.key.value && !settingsTextInputs().any { it.isFocus }) {
            onClose()
            return true
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && editingAttachmentType != -1) {
            leaveSlot()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    private fun leaveSlot() {
        (linkedDocument?.getElementById("panel-skin") as? Select)?.closePopup()
        panelSkinTarget = null
        discardAdjustments()
        editingAttachmentType = -1
        hoveredItem = null
    }

    private fun prepareDocument(doc: Document) {
        clearUiState()
        documentGeneration = doc.refreshGeneration
        doc.getElementById("title-mode")?.textContent = label("title")
        doc.addEventListener("wheel", { event ->
            // 原生 select 的顶层弹出列表自行处理滚动，不交给底下的配件列表。
            if (event.composedPath().any { (it as? Element)?.classList?.contains("aui-select-popup") == true }) return@addEventListener
            val scroll = doc.getElementById("panel-scroll")
            val list = doc.getElementById("panel-list")
            val statsScroll = doc.getElementById("stats-scroll")
            val settingsScroll = doc.getElementById("settings-controls")
            val inStats = statsOpen && statsScroll != null && event.composedPath().any { it === statsScroll }
            val inSettings = settingsScroll != null && event.composedPath().any { it === settingsScroll }
            val inList = panelType != null && AttachmentSlots.slotAt(editingAttachmentType) == panelType &&
                scroll != null && list != null && event.composedPath().any { it === list }
            // 始终取消默认滚动，列表到边界时也不能把滚动传给 body 或其他祖先。
            event.preventDefault()
            event.stopPropagation()
            if (event is MouseEvent) {
                if (inStats) scrollStatsBy(statsScroll, event.deltaY)
                else if (inList) scroll.scrollBy(0.0, event.deltaY)
                else if (inSettings) settingsScroll.scrollBy(0.0, event.deltaY)
            }
        }, true)
        for (type in AttachmentType.entries) {
            val id = mountId(type)
            addMount(doc, id, slotName(type)) { onMountClicked(type) }
            doc.getElementById("icon-layer")?.appendChild(
                doc.createHTML(
                    """<div id="icon-$id" class="attachment-icon">${itemSlot()}</div>"""
                )
            )
            doc.getElementById("icon-$id")?.let { icon ->
                icon.addEventListener("click") { onMountClicked(type) }
                wireItemHover(icon)
            }
        }
        addMount(doc, AMMO_MOUNT_ID, Component.translatable("attachment.superbwarfare.slot.ammo_type").string) {
            onAmmoClicked()
        }
        addMount(doc, SCOPE_STACK_MOUNT_ID, Component.translatable("attachment.superbwarfare.slot.scope_stack").string) {
            onMountClicked(AttachmentType.SCOPE)
        }
        doc.getElementById("backdrop")?.addEventListener("click") {
            if (editingAttachmentType != -1) leaveSlot() else onClose()
        }
        doc.getElementById("panel-uninstall")?.addEventListener("click") {
            val type = AttachmentSlots.slotAt(editingAttachmentType) ?: return@addEventListener
            val data = localPlayer?.mainHandItem?.let(GunData::from) ?: return@addEventListener
            if (data.attachment.id(type) != null) {
                discardAdjustments()
                hoveredItem = null
                sendPacketToServer(AttachmentInstallMessage(type.attachmentName, ""))
            }
        }
        doc.getElementById("panel-current")?.let(::wireItemHover)
        for (id in listOf("panel-name", "panel-registry-name")) doc.getElementById(id)?.let { name ->
            name.addEventListener("mouseenter") { hoveredItem = "panel-current" }
            name.addEventListener("mouseleave") { hoveredItem = null }
        }
        doc.getElementById("panel-empty")?.textContent = label("no_available")
        doc.getElementById("panel-uninstall-label")?.textContent = label("uninstall")
        doc.getElementById("panel-uninstall")?.let {
            it.setAttribute("title", label("uninstall"))
            it.setAttribute("aria-label", label("uninstall"))
        }
        doc.getElementById("panel-skin-label")?.textContent = label("skin")
        doc.getElementById("panel-skin")?.let { select ->
            select.setAttribute("title", label("switch_skin"))
            select.setAttribute("aria-label", label("switch_skin"))
            select.addEventListener("change") {
                val target = panelSkinTarget ?: return@addEventListener
                if (AttachmentSlots.slotAt(editingAttachmentType)?.attachmentName != target.slot) return@addEventListener
                val skinId = select.getValue()
                if (skinId != AttachmentDefinition.DEFAULT_SKIN_ID && panelSkins.none { it.id == skinId }) return@addEventListener
                hoveredItem = null
                sendPacketToServer(target.copy(skinId = skinId))
            }
        }
        doc.getElementById("settings-title")?.textContent = label("settings")
        doc.getElementById("settings-reset")?.let { reset ->
            doc.getElementById("settings-reset-label")?.textContent = label("reset")
            reset.setAttribute("title", label("reset"))
            reset.setAttribute("aria-label", label("reset"))
            reset.addEventListener("click") { adjustAttachment(reset = true) }
        }
        doc.getElementById("settings-confirm")?.let { confirm ->
            doc.getElementById("settings-confirm-label")?.textContent = label("confirm")
            confirm.setAttribute("title", label("confirm"))
            confirm.setAttribute("aria-label", label("confirm"))
            confirm.addEventListener("click") { confirmAdjustments() }
        }
        for (property in ADJUSTMENT_PROPERTIES) {
            doc.getElementById("settings-$property-minus")?.addEventListener("click") { stepAdjustment(property, -1) }
            doc.getElementById("settings-$property-plus")?.addEventListener("click") { stepAdjustment(property, 1) }
            doc.getElementById("settings-$property-label")?.textContent = label(property)
            doc.getElementById("settings-$property")?.let { input ->
                input.addEventListener("input") {
                    val value = input.getValue().toDoubleOrNull() ?: return@addEventListener
                    adjustAttachment(property, value)
                }
            }
            doc.getElementById("settings-$property-value")?.let { input ->
                wireSettingsInput(input,
                    { if (AttachmentColorInput.shift(property) != null) RGB_NUMBER.matches(it) else ADJUSTMENT_NUMBER.matches(it) },
                    { finish -> commitAdjustmentInput(input, property, finish) },
                    { direction -> stepAdjustment(property, direction) })
            }
        }
        doc.getElementById("settings-color-hex")?.let { input ->
            wireSettingsInput(input, AttachmentColorInput::isDraft, { finish -> commitColorInput(input, finish) })
        }
        prepareGrid(doc)
        prepareStats(doc)
    }

    private fun wireSettingsInput(
        input: Element, accepts: (String) -> Boolean, commit: (Boolean) -> Unit, step: ((Int) -> Unit)? = null,
    ) {
        input.addEventListener("focus") {
            val (type, data) = selectedAdjustmentData() ?: return@addEventListener
            input.setAttribute("data-adjustment-slot", type.attachmentName)
            input.setAttribute("data-adjustment-gun", data.uuid?.toString().orEmpty())
            input.setAttribute("data-adjustment-id", data.attachment.id(type)?.toString().orEmpty())
            input.setAttribute("data-adjustment-draft", input.getValue())
        }
        input.addEventListener("beforeinput") { event ->
            if (event is Event.InputEvent && event.inputType.startsWith("insert")) {
                val text = input as? Input ?: return@addEventListener
                val inserted = event.data ?: return@addEventListener
                val current = input.getValue()
                val start = text.getSelectionStart().coerceIn(0, current.length)
                val end = text.getSelectionEnd().coerceIn(start, current.length)
                if (!accepts(current.replaceRange(start, end, inserted))) event.preventDefault()
            }
        }
        input.addEventListener("input") {
            if (!accepts(input.getValue())) input.setValue(input.getAttribute("data-adjustment-draft") ?: "")
            else {
                input.setAttribute("data-adjustment-draft", input.getValue())
                commit(false)
            }
        }
        input.addEventListener("change") { commit(true) }
        input.addEventListener("blur") { commit(true) }
        input.addEventListener("keydown") { event ->
            if (event !is KeyEvent) return@addEventListener
            when (event.keyCode) {
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    event.preventDefault()
                    commit(true)
                    input.blur()
                }
                GLFW.GLFW_KEY_ESCAPE -> {
                    event.preventDefault()
                    leaveSlot()
                }
                GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> if (step != null) {
                    event.preventDefault()
                    step(if (event.keyCode == GLFW.GLFW_KEY_UP) 1 else -1)
                }
            }
        }
    }

    private fun settingsTextInputs(): List<Element> = linkedDocument?.let { doc ->
        (ADJUSTMENT_PROPERTIES.map { "settings-$it-value" } + "settings-color-hex").mapNotNull(doc::getElementById)
    } ?: emptyList()

    private fun finishAdjustmentInputs() {
        settingsTextInputs().filter { it.isFocus }.forEach { it.blur() }
    }

    private fun discardAdjustments() {
        finishAdjustmentInputs()
        adjustmentDraft.discard()
    }

    /** 整个设置面板唯一的发包入口，回车/失焦/退出只处理草稿。 */
    private fun confirmAdjustments() {
        finishAdjustmentInputs()
        val (type, data) = selectedAdjustmentData() ?: return
        if (previewFor(data, type) == null) return
        adjustmentDraft.submit()?.let(::sendPacketToServer)
    }

    private fun selectedAdjustmentData(): Pair<AttachmentType, GunData>? {
        val type = AttachmentSlots.slotAt(editingAttachmentType) ?: return null
        val held = localPlayer?.mainHandItem ?: return null
        if (held.item !is GunItem || held.item !== stack.item) return null
        val data = GunData.from(held)
        if (!data.attachment.has(type)) return null
        return type to data
    }

    private fun laserInfo(data: GunData, type: AttachmentType): LaserInfo? =
        data.attachment.id(type)?.let(AttachmentDefinition::from)?.laser

    private fun currentAdjustment(data: GunData, type: AttachmentType): AttachmentAdjustMessage =
        previewFor(data, type) ?: AttachmentAdjustMessage(data.getDefault().itemId, data.uuid?.toString(),
            localPlayer?.inventory?.selected ?: -1, type.attachmentName, data.attachment.id(type).toString(),
            data.attachment.getRotation(type), data.attachment.getOffset(type),
            laserColor = if (laserInfo(data, type) != null) data.attachment.getLaserColor(type) else null)

    private fun currentColor(data: GunData, type: AttachmentType, preview: AttachmentAdjustMessage? = previewFor(data, type)): Int =
        laserInfo(data, type)?.resolveColorRgb(preview?.laserColor ?: data.attachment.getLaserColor(type)) ?: 0

    private fun adjustmentRange(data: GunData, type: AttachmentType, property: String): ClosedFloatingPointRange<Double>? = when {
        property == "rotation" && AttachmentAdjustments.canRotate(data, type) -> AttachmentAdjustments.MIN_ROTATION..AttachmentAdjustments.MAX_ROTATION
        property == "offset" && AttachmentAdjustments.canOffset(type) -> AttachmentAdjustments.offsetRange(data, type)
        AttachmentColorInput.shift(property) != null && laserInfo(data, type) != null -> 0.0..255.0
        else -> null
    }

    private fun stepAdjustment(property: String, direction: Int) {
        finishAdjustmentInputs()
        val (type, data) = selectedAdjustmentData() ?: return
        val current = adjustmentValue(data, type, property)
        val step = if (property == "offset") AttachmentAdjustments.OFFSET_STEP else 1.0
        adjustAttachment(property, current + step * direction)
    }

    /** 输入中允许空串/负号等过渡状态；结束编辑时归一化、钳制，防止每帧同步打断输入。 */
    private fun commitAdjustmentInput(input: Element, property: String, finish: Boolean) {
        val (type, data) = selectedAdjustmentData() ?: return
        val range = adjustmentRange(data, type, property) ?: return
        // 失焦可能发生在点击另一个槽位之后；旧输入框不能把旧值提交到新槽位或新配件。
        if (input.getAttribute("data-adjustment-slot") != type.attachmentName ||
            input.getAttribute("data-adjustment-gun") != data.uuid?.toString().orEmpty() ||
            input.getAttribute("data-adjustment-id") != data.attachment.id(type)?.toString()) return
        val value = input.getValue().toDoubleOrNull()?.takeIf { it.isFinite() }
        if (!finish && (value == null || value !in range)) return
        val fallback = adjustmentValue(data, type, property)
        val clamped = formatAdjustment(property, (value ?: fallback).coerceIn(range.start, range.endInclusive))
            .toDouble().coerceIn(range.start, range.endInclusive)
        adjustAttachment(property, clamped)
        if (finish) input.setValue(formatAdjustment(property, clamped))
    }

    private fun formatAdjustment(property: String, value: Double) =
        String.format(Locale.ROOT, if (property == "offset") "%.1f" else "%.0f", value)

    private fun adjustmentValue(data: GunData, type: AttachmentType, property: String): Double {
        AttachmentColorInput.shift(property)?.let { return AttachmentColorInput.channel(currentColor(data, type), it).toDouble() }
        val current = currentAdjustment(data, type)
        return if (property == "rotation") current.rotation else current.offset
    }

    private fun commitColorInput(input: Element, finish: Boolean) {
        val (type, data) = selectedAdjustmentData() ?: return
        if (laserInfo(data, type) == null || input.getAttribute("data-adjustment-slot") != type.attachmentName ||
            input.getAttribute("data-adjustment-gun") != data.uuid?.toString().orEmpty() ||
            input.getAttribute("data-adjustment-id") != data.attachment.id(type)?.toString()) return
        AttachmentColorInput.parse(input.getValue())?.let { color ->
            if (color != currentColor(data, type)) adjustmentDraft.update(currentAdjustment(data, type).copy(laserColor = color))
        }
        if (finish) input.setValue(AttachmentColorInput.format(currentColor(data, type)))
    }

    private fun adjustAttachment(property: String = "", value: Double = 0.0, reset: Boolean = false) {
        if (!value.isFinite()) return
        if (reset) finishAdjustmentInputs()
        val (type, data) = selectedAdjustmentData() ?: return
        val previous = currentAdjustment(data, type)
        val next: AttachmentAdjustMessage
        if (reset) {
            next = previous.copy(rotation = 0.0, offset = 0.0, reset = true,
                laserColor = if (laserInfo(data, type) != null) -1 else null)
        } else {
            val range = adjustmentRange(data, type, property) ?: return
            val normalized = formatAdjustment(property, value.coerceIn(range.start, range.endInclusive))
                .toDouble().coerceIn(range.start, range.endInclusive)
            if (normalized == adjustmentValue(data, type, property)) return
            val shift = AttachmentColorInput.shift(property)
            next = when {
                shift != null -> previous.copy(laserColor = AttachmentColorInput.withChannel(currentColor(data, type), shift, normalized.toInt()))
                property == "rotation" -> previous.copy(rotation = normalized)
                else -> previous.copy(offset = normalized)
            }
        }
        // 预览留在界面内存，不能在客户端 save()：这会递增 GunData revision，阻碍服务端快照接管。
        adjustmentDraft.update(next)
    }

    internal fun previewFor(data: GunData, type: AttachmentType): AttachmentAdjustMessage? =
        adjustmentDraft.preview?.takeIf { AttachmentSlots.slotAt(editingAttachmentType) == type &&
            it.gunId == data.getDefault().itemId && it.gunUuid == data.uuid?.toString() &&
            it.hotbarSlot == localPlayer?.inventory?.selected && it.slot == type.attachmentName &&
            it.attachmentId == data.attachment.id(type)?.toString() }

    /**
     * 第一人称渲染可能持有主手物品的旧副本，不能用 ItemStack 引用相等判断是否正在改装。
     * 草稿优先；提交完成后也从主手最新快照读取，避免渲染副本尚未刷新时跳回旧位置。
     * 仅供本地主手渲染调用，仍校验枪械身份与配件，不能把草稿套到其他枪或旧配件上。
     */
    internal fun renderAdjustmentFor(renderedData: GunData, type: AttachmentType): AttachmentAdjustMessage? {
        val held = localPlayer?.mainHandItem ?: return null
        if (held.item !== stack.item || renderedData.item !== held.item) return null
        val data = GunData.from(held)
        if (data.getDefault().itemId != renderedData.getDefault().itemId ||
            (renderedData.stack !== held && (data.uuid == null || data.uuid != renderedData.uuid)) ||
            data.attachment.id(type) != renderedData.attachment.id(type)) return null
        return currentAdjustment(data, type)
    }

    private fun syncAdjustments(doc: Document, data: GunData): Box? {
        val settings = doc.getElementById("settings") ?: return null
        val type = AttachmentSlots.slotAt(editingAttachmentType)
        val rotation = type != null && AttachmentAdjustments.canRotate(data, type)
        val offset = type != null && AttachmentAdjustments.canOffset(type)
        val laser = type?.let { laserInfo(data, it) }
        if (type == null || !data.attachment.has(type) || (!rotation && !offset && laser == null)) {
            setStyle(settings, "display", "none")
            adjustmentDraft.discard()
            return null
        }
        if (adjustmentDraft.preview != null && previewFor(data, type) == null) discardAdjustments()
        fun sync(property: String, visible: Boolean, value: Double, min: Double, max: Double, step: Double) {
            setStyle(doc.getElementById("settings-$property-row"), "display", if (visible) "flex" else "none")
            if (!visible) return
            val live = doc.getElementById("settings-$property")?.getValue()?.toDoubleOrNull()
            for (id in listOf("settings-$property", "settings-$property-value")) {
                val input = doc.getElementById(id) ?: continue
                for ((key, number) in listOf("min" to min, "max" to max, "step" to step)) {
                    val text = number.toString()
                    if (input.getAttribute(key) != text) input.setAttribute(key, text)
                }
                val clamped = value.coerceIn(min, max)
                if (id == "settings-$property" || !input.isFocus) {
                    val text = formatAdjustment(property, clamped)
                    if (input.getValue() != text) input.setValue(text)
                }
            }
            val current = live ?: value
            val fraction = if (max > min) ((current - min) / (max - min)).coerceIn(0.0, 1.0) else 0.0
            setStyle(doc.getElementById("settings-$property-fill"), "width",
                px((SLIDER_TRACK_WIDTH * fraction).toFloat()))
            setStyle(doc.getElementById("settings-$property-thumb"), "transform",
                "translateX(${px((SLIDER_TRACK_LEFT + SLIDER_TRACK_WIDTH * fraction - SLIDER_THUMB_RADIUS).toFloat())})")
        }
        val submitted = previewFor(data, type)
        adjustmentDraft.acknowledge(submitted != null && submitted.rotation == data.attachment.getRotation(type) &&
            submitted.offset == data.attachment.getOffset(type) &&
            (submitted.laserColor == null || submitted.laserColor == data.attachment.getLaserColor(type)))
        val preview = previewFor(data, type)
        sync("rotation", rotation, preview?.rotation ?: data.attachment.getRotation(type), AttachmentAdjustments.MIN_ROTATION,
            AttachmentAdjustments.MAX_ROTATION, AttachmentAdjustments.ROTATION_STEP)
        val range = AttachmentAdjustments.offsetRange(data, type)
        sync("offset", offset, preview?.offset ?: data.attachment.getOffset(type), range.start, range.endInclusive,
            AttachmentAdjustments.OFFSET_STEP)
        val color = currentColor(data, type, preview)
        for (property in listOf("red", "green", "blue")) {
            val channel = AttachmentColorInput.channel(color, AttachmentColorInput.shift(property)!!)
            sync(property, laser != null, channel.toDouble(), 0.0, 255.0, 1.0)
        }
        setStyle(doc.getElementById("settings-hex-row"), "display", if (laser != null) "flex" else "none")
        if (laser != null) {
            val hex = AttachmentColorInput.format(color)
            doc.getElementById("settings-color-hex")?.let { if (!it.isFocus && it.getValue() != hex) it.setValue(hex) }
            setStyle(doc.getElementById("settings-color-swatch"), "background-color", hex)
        }
        doc.getElementById("settings-confirm")?.let {
            val disabled = adjustmentDraft.preview == null || adjustmentDraft.awaitingServer
            setClass(it, "disabled", disabled)
            it.toggleAttribute("disabled", disabled)
        }
        val rows = (if (rotation) 1 else 0) + (if (offset) 1 else 0) + (if (laser != null) 4 else 0)
        val scale = editorPanelScale()
        // 空间不足时只滚动属性卡片，标题、底部操作与全局缩放保持稳定。
        val heightBudget = (viewportHeight - SETTINGS_BOTTOM_INSET - TITLE_BAR_HEIGHT - 14f) / scale - STATS_HEIGHT
        val visibleRows = minOf(rows, floor((heightBudget - SETTINGS_BASE_HEIGHT) / SETTINGS_ROW_HEIGHT + 0.001f)
            .toInt().coerceAtLeast(1))
        val height = SETTINGS_BASE_HEIGHT + visibleRows * SETTINGS_ROW_HEIGHT
        setStyle(settings, "height", px(height))
        doc.getElementById("settings-controls")?.let { controls ->
            setStyle(controls, "height", px(visibleRows * SETTINGS_ROW_HEIGHT - SETTINGS_ROW_GAP))
            val scrollKey = "${type.attachmentName}:${data.attachment.id(type)}"
            if (controls.getAttribute("data-scroll-key") != scrollKey) {
                controls.setAttribute("data-scroll-key", scrollKey)
                controls.scrollTo(0.0, 0.0)
            }
        }
        // 比原来的底部锚点下移 10 GUI px，拉开属性面板与设置面板，同时避开快捷栏。
        val bounds = Box(6f, (viewportHeight - SETTINGS_BOTTOM_INSET - height * scale).coerceAtLeast(TITLE_BAR_HEIGHT + 4f),
            SETTINGS_WIDTH * scale, height * scale)
        place(settings, bounds, scale)
        return bounds
    }

    private fun addMount(doc: Document, id: String, name: String, action: () -> Unit) {
        doc.getElementById("mount-layer")?.appendChild(
            doc.createHTML(
                """<div id="$id" class="mount"></div>"""
            )
        )
        doc.getElementById(id)?.addEventListener("click") { action() }
        doc.getElementById("mount-layer")?.appendChild(
            doc.createHTML(
                """<div id="label-$id" class="mount-label"></div>"""
            )
        )
        doc.getElementById("label-$id")?.textContent = name
    }

    private fun wireItemHover(element: Element) {
        val id = element.getAttribute("id") ?: return
        element.addEventListener("mouseenter") { hoveredItem = id }
        element.addEventListener("mouseleave") { if (hoveredItem == id) hoveredItem = null }
    }

    private fun onMountClicked(type: AttachmentType) {
        val index = AttachmentSlots.editIndex(type)
        if (index < 0 || editingAttachmentType == index) return
        discardAdjustments()
        hoveredItem = null
        editingAttachmentType = index
        editModelShake()
        localPlayer?.playSound(ModSounds.EDIT.get(), 1f, 1f)
    }

    private fun onAmmoClicked() {
        discardAdjustments()
        hoveredItem = null
        editingAttachmentType = AttachmentSlots.AMMO_TYPE_EDIT_INDEX
        editModelShake()
        sendPacketToServer(EditMessage(AttachmentSlots.AMMO_TYPE_EDIT_INDEX, add = true, isVehicle = false))
    }

    private data class Mount(
        val id: String,
        val type: AttachmentType?,
        val bounds: Box,
        val installed: ResourceLocation?
    )

    private fun syncMounts(doc: Document, data: GunData, reservedPanels: List<Box> = emptyList()): List<Mount> {
        val mounts = mutableListOf<Mount>()
        val selectedType = AttachmentSlots.slotAt(editingAttachmentType)
        fun sync(id: String, type: AttachmentType?, point: BoneScreenPoint?, available: Boolean, stacked: Boolean = false) {
            val element = doc.getElementById(id) ?: return
            val installed = if (stacked) null else type?.let { data.attachment.id(it) }
            // 入场动画的前 n tick 隐藏挂点；二级菜单打开后只保留当前挂点。
            // 图标、连线和标签也只按返回的点位列表绘制。
            if (openingTicks < MOUNT_REVEAL_DELAY_TICKS || (selectedType != null && type != selectedType) ||
                point == null || !point.visible || (!available && installed == null)
            ) {
                setStyle(element, "display", "none")
                return
            }
            // gui 模式在 GUI scale >= 6 时不再是 1:1，必须使用 AUI 的坐标转换。
            val projected = doc.guiToDocumentPosition(Position(point.x.toDouble(), point.y.toDouble()))
            val bounds = findSpace(projected.x.toFloat(), projected.y.toFloat(), MOUNT_SIZE, MOUNT_SIZE,
                mounts.map { it.bounds } + reservedPanels, 3f, step = 4f)
            place(element, bounds)
            val index = type?.let(AttachmentSlots::editIndex) ?: AttachmentSlots.AMMO_TYPE_EDIT_INDEX
            setClass(element, "active", editingAttachmentType == index)
            setClass(element, "filled", installed != null)
            mounts += Mount(id, type, bounds, installed)
        }
        for (type in AttachmentType.entries) {
            sync(mountId(type), type, WeaponEditMountTracker.positionOf(type), compatibleAttachments(data, type).isNotEmpty())
        }
        sync(AMMO_MOUNT_ID, null, WeaponEditMountTracker.ammoPosition, data.get(GunProp.AMMO_CONSUMER).size > 1)
        sync(
            SCOPE_STACK_MOUNT_ID, AttachmentType.SCOPE, WeaponEditMountTracker.scopeStackPosition,
            compatibleAttachments(data, AttachmentType.SCOPE).isNotEmpty(), stacked = true
        )
        return mounts
    }

    private fun syncPanel(doc: Document, player: Player, data: GunData, mounts: List<Mount>, reserved: List<Box>): Box? {
        val panel = doc.getElementById("panel") ?: return null
        val type = AttachmentSlots.slotAt(editingAttachmentType)
        val mount = mounts.firstOrNull { it.type == type && type != null }
        if (type == null || mount == null) {
            (doc.getElementById("panel-skin") as? Select)?.closePopup()
            panelSkinTarget = null
            setStyle(panel, "display", "none")
            if (panelType != null) hoveredItem = null
            panelType = null
            return null
        }
        val installed = data.attachment.id(type)
        val changedSlot = panelType != type
        if (changedSlot || panelInstalled != installed) {
            doc.getElementById("panel-current")?.let {
                setElementItem(it, installed?.toString() ?: AIR)
                setClass(it, "filled", installed != null)
            }
            doc.getElementById("panel-name")?.let {
                val current = itemStack(installed)
                it.textContent = if (current.isEmpty) label("empty") else current.hoverName.string
                // Forge 的自定义稀有度也可能指定 RGB，沿用完整 Style modifier。
                setStyle(it, "color", if (current.isEmpty) "#bfc5c9" else String.format(Locale.ROOT, "#%06x",
                    current.rarity.styleModifier.apply(Style.EMPTY).color?.value ?: 0xffffff))
            }
            doc.getElementById("panel-registry-name")?.textContent = installed?.toString().orEmpty()
            doc.getElementById("panel-title")?.textContent = slotName(type)
            doc.getElementById("panel-uninstall")?.let { setClass(it, "disabled", installed == null) }
            panelInstalled = installed
        }
        syncAttachmentSkins(doc, player, data, type, installed)
        val ids = installableAttachments(player, data, type)
        if (changedSlot || panelIds !== ids) {
            panelIds = ids
            panelType = type
            gridWindow.reset()
            val rowCount = (ids.size + GRID_COLUMNS - 1) / GRID_COLUMNS
            val contentHeight = px((rowCount * GRID_ROW_PITCH - GRID_ROW_GAP).coerceAtLeast(0f))
            setStyle(doc.getElementById("panel-space"), "height", contentHeight)
            // AUI 在 overflow 裁剪区按父容器矩形剔除整棵子树；绝对定位行不会撑开父容器。
            // 格子容器必须覆盖整份列表，滚动到任意位置时都与固定视口相交。
            setStyle(doc.getElementById("panel-grid"), "height", contentHeight)
            setStyle(doc.getElementById("panel-empty"), "display", if (ids.isEmpty()) "block" else "none")
            if (changedSlot) doc.getElementById("panel-scroll")?.scrollTo(0.0, 0.0)
        }
        syncGrid(doc, installed)
        // 小窗口为点位和图标保留半屏空间；所有避让使用缩放后的外框。
        val scale = editorPanelScale()
        val bounds = findSpace(mount.bounds.right + 24f + PANEL_WIDTH * scale / 2f, mount.bounds.centerY,
            PANEL_WIDTH * scale, PANEL_HEIGHT * scale, mounts.map { it.bounds } + reserved, 6f)
        place(panel, bounds, scale)
        connect("panel", mount.bounds, bounds)
        return bounds
    }

    private fun syncAttachmentSkins(doc: Document, player: Player, data: GunData, type: AttachmentType, installed: ResourceLocation?) {
        val select = doc.getElementById("panel-skin") as? Select ?: return
        val definition = installed?.let(AttachmentDefinition::from)
        val skins = definition?.skins.orEmpty()
        val available = skins.any { it.id.isNotBlank() && it.id != AttachmentDefinition.DEFAULT_SKIN_ID }
        setStyle(doc.getElementById("panel-skin-control"), "display", if (available) "flex" else "none")
        setStyle(doc.getElementById("panel-uninstall-label"), "width", if (available) "64px" else "150px")
        if (!available || installed == null) {
            select.closePopup()
            panelSkinTarget = null
            panelSkin = null
            return
        }
        val target = AttachmentSkinMessage(data.getDefault().itemId, data.uuid?.toString(),
            player.inventory.selected, type.attachmentName, installed.toString(), "")
        val changed = panelSkinTarget != target || panelSkins !== skins
        if (changed) {
            select.closePopup()
            panelSkinTarget = target
            panelSkins = skins
            panelSkin = null
            select.innerHTML = ""
            fun option(id: String, name: String) {
                // 用 DOM API 写入数据包中的 id/名称，避免把名称解析为 HTML。
                val element = doc.createElement("option")
                element.setAttribute("value", id)
                element.textContent = name
                select.appendChild(element)
            }
            option(AttachmentDefinition.DEFAULT_SKIN_ID, AttachmentDefinition.DEFAULT_SKIN_ID)
            skins.distinctBy { it.id }.filter { it.id.isNotBlank() && it.id != AttachmentDefinition.DEFAULT_SKIN_ID }.forEach {
                option(it.id, Component.translatable(it.name.ifBlank { it.id }).string)
            }
        }
        val current = definition?.skinOrNull(data, type)?.id ?: AttachmentDefinition.DEFAULT_SKIN_ID
        if (panelSkin != current) {
            select.setValue(current)
            panelSkin = current
        }
    }

    private data class GridCell(val key: String, val element: Element, val item: AuiItem)
    private data class GridRow(val element: Element, val cells: List<GridCell>)

    /** 窗口移动时保留重叠行，只把离开窗口的池条目绑定到新行。 */
    private class RecyclingRowWindow(private val size: Int) {
        private val boundRows = IntArray(size) { -1 }
        private var firstRow = -1

        fun reset() {
            boundRows.fill(-1)
            firstRow = -1
        }

        fun rowAt(poolIndex: Int): Int = boundRows[poolIndex]

        fun update(firstRow: Int, bind: (poolIndex: Int, listRow: Int) -> Unit) {
            if (this.firstRow == firstRow) return
            for (listRow in firstRow until firstRow + size) {
                val poolIndex = listRow % size
                if (boundRows[poolIndex] == listRow) continue
                bind(poolIndex, listRow)
                boundRows[poolIndex] = listRow
            }
            this.firstRow = firstRow
        }
    }

    /** 格子只创建一次；长列表只保留可见三排及滚动前后的缓冲行。 */
    private fun prepareGrid(doc: Document) {
        val grid = doc.getElementById("panel-grid") ?: return
        grid.innerHTML = (0 until GRID_WINDOW_ROWS).joinToString("") { row ->
            (0 until GRID_COLUMNS).joinToString("", "<div id=\"grid-row-$row\" class=\"row\">", "</div>") { column ->
                """<div id="cell-${row * GRID_COLUMNS + column}" class="cell">${itemSlot(size = 15)}</div>"""
            }
        }
        for (row in 0 until GRID_WINDOW_ROWS) {
            val rowElement = doc.getElementById("grid-row-$row") ?: continue
            val cells = (0 until GRID_COLUMNS).mapNotNull { column ->
                val key = "cell-${row * GRID_COLUMNS + column}"
                val cell = doc.getElementById(key) ?: return@mapNotNull null
                val item = cell.querySelector("item") as? AuiItem ?: return@mapNotNull null
                item.setIngredientStack(ItemStack.EMPTY)
                cell.addEventListener("click") {
                    val type = panelType
                    if (type != null && AttachmentSlots.slotAt(editingAttachmentType) == type) {
                        val id = shownItems[key] ?: return@addEventListener
                        discardAdjustments()
                        hoveredItem = null
                        sendPacketToServer(AttachmentInstallMessage(type.attachmentName, id))
                    }
                }
                wireItemHover(cell)
                GridCell(key, cell, item)
            }
            gridRows += GridRow(rowElement, cells)
        }
    }

    private fun syncGrid(doc: Document, installed: ResourceLocation?) {
        val scroll = doc.getElementById("panel-scroll") ?: return
        val rowCount = (panelIds.size + GRID_COLUMNS - 1) / GRID_COLUMNS
        // 列表缩短时 AUI 的滚动范围要到下一次布局才更新；当前窗口先夹到新范围。
        val maxScroll = (rowCount * GRID_ROW_PITCH - GRID_ROW_GAP - GRID_VIEWPORT_HEIGHT).coerceAtLeast(0f)
        val scrollTop = scroll.getScrollTop().coerceIn(0.0, maxScroll.toDouble())
        // 格子仍在滚动容器之外，沿用本帧准备窗口的时序；只提交平移，不改布局坐标。
        setStyle(doc.getElementById("panel-grid"), "transform", "translateY(${px(-scrollTop.toFloat())})")
        val firstRow = (floor(scrollTop / GRID_ROW_PITCH).toInt() - 1)
            .coerceIn(0, (rowCount - GRID_WINDOW_ROWS).coerceAtLeast(0))
        val selected = installed?.toString()
        gridWindow.update(firstRow) { poolIndex, index ->
            val row = gridRows[poolIndex]
            setStyle(row.element, "transform", "translateY(${px(index * GRID_ROW_PITCH)})")
            row.cells.forEachIndexed { column, cell ->
                val id = panelIds.getOrNull(index * GRID_COLUMNS + column)?.toString()
                setStyle(cell.element, "display", if (id != null) "flex" else "none")
                setItem(cell.key, cell.item, id)
                setClass(cell.element, "selected", id != null && id == selected)
            }
        }
        gridRows.forEachIndexed { poolIndex, row ->
            val top = gridWindow.rowAt(poolIndex) * GRID_ROW_PITCH
            // AUI 对变换后的裁剪采取保守剔除，显式隐藏缓冲行，避免把屏外物品也送入渲染后端。
            // visibility 不影响布局；部分露出的行也必须保留，交给 panel-items 精确裁剪。
            val visible = top + GRID_ROW_PITCH - GRID_ROW_GAP > scrollTop && top < scrollTop + GRID_VIEWPORT_HEIGHT
            setStyle(row.element, "visibility", if (visible) "visible" else "hidden")
            if (!visible && row.cells.any { it.key == hoveredItem }) hoveredItem = null
            if (gridSelected != installed) {
                for (cell in row.cells) {
                    setClass(cell.element, "selected", selected != null && shownItems[cell.key] == selected)
                }
            }
        }
        gridSelected = installed
    }

    private fun syncIcons(doc: Document, mounts: List<Mount>, reserved: List<Box>): MutableList<Box> {
        val obstacles = mounts.mapTo(mutableListOf()) { it.bounds }
        obstacles += reserved
        for (type in AttachmentType.entries) {
            val id = mountId(type)
            val icon = doc.getElementById("icon-$id") ?: continue
            val mount = mounts.firstOrNull { it.type == type }
            val installed = mount?.installed
            if (mount == null || installed == null) {
                setStyle(icon, "display", "none")
                if (hoveredItem == "icon-$id") hoveredItem = null
                continue
            }
            val bounds = findSpace(
                mount.bounds.centerX, mount.bounds.centerY - 24f,
                ICON_SIZE, ICON_SIZE, obstacles, 5f
            )
            obstacles += bounds
            iconBounds[id] = bounds
            place(icon, bounds)
            setElementItem(icon, installed.toString())
            setClass(icon, "filled", true)
            setClass(icon, "active", editingAttachmentType == AttachmentSlots.editIndex(type))
            connect(id, mount.bounds, bounds)
        }
        return obstacles
    }

    /** 只显示鼠标所在圆圈的名称，并避让物品图标和菜单。 */
    private fun syncMountLabels(doc: Document, mounts: List<Mount>, obstacles: MutableList<Box>) {
        val ids = AttachmentType.entries.map(::mountId) + AMMO_MOUNT_ID + SCOPE_STACK_MOUNT_ID
        val mouse = doc.mouseDocumentPosition
        for (id in ids) {
            val element = doc.getElementById("label-$id") ?: continue
            val mount = mounts.firstOrNull { it.id == id }
            if (mount == null || !mount.bounds.contains(mouse.x.toFloat(), mouse.y.toFloat())) {
                setStyle(element, "display", "none")
                continue
            }
            val labelWidth = (Text.measureLine(Text.of(element), element.textContent).toFloat() + 4f)
                .coerceIn(1f, (viewportWidth - 8f).coerceAtLeast(1f))
            val bounds = findSpace(mount.bounds.centerX, mount.bounds.bottom + 8f,
                labelWidth, 11f, obstacles, 2f)
            setStyle(element, "width", px(labelWidth))
            place(element, bounds)
            setClass(element, "filled", mount.installed != null)
            obstacles += bounds
        }
    }

    /** 复用配件 tooltip 中的槽位、描述与属性。属性图表不弹出悬浮提示。 */
    private data class TooltipRun(val text: Text, val left: Double, val width: Double)

    private fun tooltipText(style: Style, content: String = "") = Text().apply {
        fontFamily = UI_FONT_FAMILY
        fontSize = TOOLTIP_FONT_SIZE
        fontWeight = if (style.isBold) 700 else 400
        lineHeight = TOOLTIP_LINE_HEIGHT.toDouble()
        color = Color(String.format(Locale.ROOT, "#%06x", style.color?.value ?: 0xffffff))
        oblique = style.isItalic
        textDecoration = if (style.isUnderlined) "underline" else if (style.isStrikethrough) "line-through" else "none"
        this.content = content
    }

    /** 用与绘制相同的 AUI 字体度量换行，保留原有颜色、强调和双向文字顺序。 */
    private fun wrapTooltip(lines: List<Component>, wrapWidth: Int): List<List<TooltipRun>> {
        val styles = mutableMapOf<Style, Text>()
        val splitter = StringSplitter { codePoint, style ->
            val text = styles.getOrPut(style) { tooltipText(style) }
            Text.measureLine(text, String(Character.toChars(codePoint))).toFloat()
        }
        return lines.flatMap { splitter.splitLines(it, wrapWidth, Style.EMPTY) }.map { line ->
            val runs = mutableListOf<TooltipRun>()
            val content = StringBuilder()
            var currentStyle: Style? = null
            var left = 0.0
            fun flushRun() {
                if (content.isEmpty()) return
                val text = tooltipText(currentStyle ?: Style.EMPTY, content.toString())
                val width = Text.measureLine(text, text.content)
                runs += TooltipRun(text, left, width)
                left += width
                content.setLength(0)
            }
            Language.getInstance().getVisualOrder(line).accept { _, style, codePoint ->
                if (currentStyle != style) {
                    flushRun()
                    currentStyle = style
                }
                content.appendCodePoint(codePoint)
                true
            }
            flushRun()
            runs
        }
    }

    private fun renderAttachmentTooltip(graphics: GuiGraphics, doc: Document) {
        val id = hoveredItem?.let { shownItems[it] }?.takeUnless { it == AIR }
        if (id == null) {
            tooltipKey = null
            tooltipLines = emptyList()
            return
        }
        val maxWidth = minOf(TOOLTIP_MAX_WIDTH, viewportWidth - 8f).coerceAtLeast(TOOLTIP_PADDING * 2f + 1f)
        val wrapWidth = (maxWidth - TOOLTIP_PADDING * 2f).toInt().coerceAtLeast(1)
        if (tooltipKey != id || tooltipWrapWidth != wrapWidth) {
            val item = itemStack(ResourceLocation.tryParse(id))
            val lines = listOf(item.hoverName.copy().withStyle(item.rarity.styleModifier).withStyle { it.withBold(false) }) +
                ClientAttachmentImageTooltip(AttachmentImageComponent(item)).buildLines(item)
            tooltipLines = wrapTooltip(lines, wrapWidth)
            tooltipKey = id
            tooltipWrapWidth = wrapWidth
        }
        if (tooltipLines.isEmpty()) return
        val contentWidth = tooltipLines.maxOf { line -> line.lastOrNull()?.let { it.left + it.width } ?: 0.0 }.toFloat()
        val tooltipWidth = (contentWidth + TOOLTIP_PADDING * 2f).coerceIn(minOf(TOOLTIP_MIN_WIDTH, maxWidth), maxWidth)
        val height = tooltipLines.size * TOOLTIP_LINE_HEIGHT + TOOLTIP_PADDING * 2f
        val scale = minOf(1f, (viewportHeight - 8f) / height, (viewportWidth - 8f) / tooltipWidth)
            .coerceAtLeast(0.1f)
        val visibleWidth = tooltipWidth * scale
        val visibleHeight = height * scale
        val mouse = doc.mouseDocumentPosition
        val x = if (mouse.x + 12 + visibleWidth <= viewportWidth - 4) mouse.x.toFloat() + 12f
        else mouse.x.toFloat() - visibleWidth - 12f
        val left = x.coerceIn(4f, (viewportWidth - visibleWidth - 4f).coerceAtLeast(4f))
        val top = (mouse.y.toFloat() + 12f).coerceIn(4f, (viewportHeight - visibleHeight - 4f).coerceAtLeast(4f))

        // 所有 AUI 物品及装饰已提交。背景先提交，文字再提交，整个描述框位于全部文档的物品层上方。
        graphics.flush()
        val pose = graphics.pose()
        val previousColor = RenderSystem.getShaderColor().copyOf()
        pose.pushPose()
        FontDrawer.pushDocumentPixelScale(doc.viewport.scissorScale() * scale)
        try {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            val documentScale = doc.viewport.renderScale()
            pose.scale(documentScale, documentScale, 1f)
            pose.translate(left.toDouble(), top.toDouble(), Base.getFlatOverlayZ().toDouble())
            pose.scale(scale, scale, 1f)
            drawPanelBackground(graphics.bufferSource().getBuffer(RenderType.gui()), pose.last().pose(),
                Box(0f, 0f, tooltipWidth, height), 1f, PANEL_BACKGROUND)
            graphics.flush()
            pose.translate(0.0, 0.0, 1.0)
            tooltipLines.forEachIndexed { index, runs ->
                for (run in runs) {
                    FontDrawer.drawFont(pose, run.text, Position(TOOLTIP_PADDING + run.left,
                        (TOOLTIP_PADDING + index * TOOLTIP_LINE_HEIGHT).toDouble()))
                }
            }
            graphics.flush()
        } finally {
            Base.commitLocalDraws()
            FontDrawer.popDocumentPixelScale()
            pose.popPose()
            RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3])
        }
    }

    private data class CandidateKey(
        val dataId: String, val version: Int, val tags: Long, val free: Boolean, val fullyFree: Boolean,
        val installed: List<ResourceLocation?>,
    )

    private fun refreshAttachmentLists(player: Player, data: GunData) {
        val key = CandidateKey(data.getDefault().itemId, GunData.DATA_VERSION, AvailableAttachments.reloadGeneration,
            AttachmentConfig.freeAttachmentMode, AttachmentConfig.fullyFreeAttachmentMode,
            AttachmentType.entries.map { data.attachment.id(it) })
        if (candidateKey != key) {
            candidateKey = key
            compatibleBySlot.clear()
            installableBySlot.clear()
            tooltipKey = null
        }
        // 每 tick 扫一次背包，避免依赖客户端同步不一定递增的 inventory.timesChanged。
        val tick = player.level().gameTime
        if (inventoryCheckTick != tick || inventoryCreative != player.isCreative) {
            val owned = if (player.isCreative) emptySet() else
                (player.inventory.items.asSequence() + player.inventory.offhand.asSequence())
                    .filterNot { it.isEmpty }
                    .mapNotNull { (it.item as? AttachmentProvider)?.attachmentId?.let(ResourceLocation::tryParse) }
                    .toSet()
            if (ownedAttachments != owned || inventoryCreative != player.isCreative) installableBySlot.clear()
            ownedAttachments = owned
            inventoryCreative = player.isCreative
            inventoryCheckTick = tick
        }
    }

    private fun compatibleAttachments(data: GunData, type: AttachmentType): List<ResourceLocation> =
        compatibleBySlot.getOrPut(type) {
            // availableAttachments 已检查枪械声明和互斥；只补数据存在/槽位检查。
            // canInstall 会为每个候选再次生成并搜索整份列表，导致二次方开销。
            data.availableAttachments(type)
                .filter { AttachmentDefinition.from(it)?.acceptedSlots?.contains(type) == true }.distinct()
        }

    private fun installableAttachments(player: Player, data: GunData, type: AttachmentType): List<ResourceLocation> =
        installableBySlot.getOrPut(type) {
            val available = compatibleAttachments(data, type)
            if (player.isCreative) available else available.filter { it in ownedAttachments }
        }

    private data class Box(val left: Float, val top: Float, val width: Float, val height: Float) {
        val right get() = left + width
        val bottom get() = top + height
        val centerX get() = left + width / 2f
        val centerY get() = top + height / 2f
        fun contains(x: Float, y: Float) = x in left..<right && y >= top && y < bottom
        fun overlaps(other: Box, gap: Float) = left < other.right + gap && right + gap > other.left &&
                top < other.bottom + gap && bottom + gap > other.top

        fun overlapArea(other: Box, gap: Float): Float =
            (min(right + gap, other.right) - max(left - gap, other.left)).coerceAtLeast(0f) *
                    (min(bottom + gap, other.bottom) - max(top - gap, other.top)).coerceAtLeast(0f)
    }

    private fun usableHeight() = (viewportHeight - 28f).coerceAtLeast(1f)

    // 缩放只由视口与固定布局预算决定，任何菜单显隐、配件类型、属性数量都不会改变它。
    private fun editorPanelScale() =
        minOf(1f, (viewportWidth - 8f) / (PANEL_WIDTH * 2f),
            (usableHeight() - TITLE_BAR_HEIGHT - 8f).coerceAtLeast(1f) / PANEL_HEIGHT,
            statsHeightBudget() / STATS_HEIGHT,
            // 固定预留展开的 01、至少一张设置卡片及 8px 间隔；更多卡片在 02 内滚动。
            (viewportHeight - SETTINGS_BOTTOM_INSET - TITLE_BAR_HEIGHT - 14f).coerceAtLeast(1f) /
                (STATS_HEIGHT + SETTINGS_MIN_HEIGHT)).coerceAtLeast(0.05f)

    private fun statsHeightBudget() = minOf(viewportHeight / 2f - 8f,
        usableHeight() - TITLE_BAR_HEIGHT - 12f).coerceAtLeast(1f)

    /** 从期望位置向外搜索，矩形间保留 gap；极小窗口放不下时选择遮挡最少的位置。 */
    private fun findSpace(
        x: Float, y: Float, width: Float, height: Float, obstacles: List<Box>, gap: Float, step: Float = 12f
    ): Box {
        fun candidate(cx: Float, cy: Float) = Box(
            (cx - width / 2f).coerceIn(4f, (viewportWidth - width - 4f).coerceAtLeast(4f)),
            (cy - height / 2f).coerceIn(TITLE_BAR_HEIGHT + 4f,
                (usableHeight() - height - 4f).coerceAtLeast(TITLE_BAR_HEIGHT + 4f)), width, height
        )

        var best = candidate(x, y)
        var bestOverlap = Float.POSITIVE_INFINITY
        val rings = ceil(max(viewportWidth, viewportHeight) / step).toInt()
        for (ring in 0..rings) {
            for (direction in 0 until if (ring == 0) 1 else 24) {
                val box = candidate(x + (SEARCH_COS[direction] * ring * step).toFloat(),
                    y + (SEARCH_SIN[direction] * ring * step).toFloat())
                if (obstacles.none { box.overlaps(it, gap) }) return box
                val overlap = obstacles.sumOf { box.overlapArea(it, gap).toDouble() }.toFloat()
                if (overlap < bestOverlap) {
                    best = box
                    bestOverlap = overlap
                }
            }
        }
        return best
    }

    private fun place(element: Element, bounds: Box, scale: Float = 1f) {
        setStyle(element, "display", "flex")
        // 原点固定为 0 0，先在文档坐标平移再缩放，位置变化不再使整个菜单子树重排。
        setStyle(element, "transform", "translate(${px(bounds.left)}, ${px(bounds.top)}) scale($scale)")
    }

    /** 本界面的动态内联样式统一由这里维护；每个元素每帧最多提交一次，不暴露 AUI 的可变 Style。 */
    private fun setStyle(element: Element?, property: String, value: String) {
        if (element == null) return
        val properties = inlineStyles.getOrPut(element) { linkedMapOf() }
        if (properties.put(property, value) != value) pendingStyles.add(element)
    }

    private fun flushStyles() {
        for (element in pendingStyles) {
            val properties = inlineStyles[element] ?: continue
            val style = buildString {
                for ((property, value) in properties) append(property).append(':').append(value).append(';')
            }
            // 一帧内可能先摆放挂点再避让菜单；最终值没变时也不重新提交样式。
            if (element.getAttribute("style") != style) element.setAttribute("style", style)
        }
        pendingStyles.clear()
    }

    private fun connect(id: String, mount: Box, target: Box) {
        connectors[id] = mount to target
    }

    /** 装饰几何独立提交，不与 AUI 的悬停文字、描边或背景图形共用批次。 */
    private fun drawDecorationPass(
        graphics: GuiGraphics, doc: Document, foreground: Boolean, draw: (VertexConsumer, Matrix4f) -> Unit
    ) {
        graphics.flush()
        val pose = graphics.pose()
        val previousColor = RenderSystem.getShaderColor().copyOf()
        pose.pushPose()
        try {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            val scale = doc.viewport.renderScale()
            pose.scale(scale, scale, 1f)
            if (foreground) pose.translate(0.0, 0.0, Base.getFlatOverlayZ().toDouble() - 2.0)
            draw(graphics.bufferSource().getBuffer(RenderType.gui()), pose.last().pose())
            graphics.flush()
        } finally {
            pose.popPose()
            RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3])
        }
    }

    private fun renderMountDecorations(graphics: GuiGraphics, doc: Document, mounts: List<Mount>) {
        val mouse = doc.mouseDocumentPosition
        drawDecorationPass(graphics, doc, true) { vertices, matrix ->
            for (mount in mounts) {
                val bounds = mount.bounds
                val selected = editingAttachmentType ==
                        (mount.type?.let(AttachmentSlots::editIndex) ?: AttachmentSlots.AMMO_TYPE_EDIT_INDEX)
                val highlighted = selected || bounds.contains(mouse.x.toFloat(), mouse.y.toFloat())
                val color = if (mount.installed == null) MOUNT_EMPTY_COLOR else MOUNT_FILLED_COLOR
                val background = if (highlighted) (color and 0xffffff) or 0x59000000 else 0x1f000000
                val radius = MOUNT_SIZE / 2f
                drawCircle(vertices, matrix, bounds.centerX, bounds.centerY, radius, 0f, background)
                drawCircle(
                    vertices, matrix, bounds.centerX, bounds.centerY, radius,
                    radius - if (highlighted) 1f else 0.5f, color
                )
                drawCircle(vertices, matrix, bounds.centerX, bounds.centerY, 2.5f, 0f, color)
                iconBounds[mount.id]?.let { icon ->
                    val border = if (selected) 1f else 0.5f
                    drawQuad(vertices, matrix, icon.left, icon.top, icon.right, icon.top + border, color)
                    drawQuad(vertices, matrix, icon.left, icon.bottom - border, icon.right, icon.bottom, color)
                    drawQuad(
                        vertices,
                        matrix,
                        icon.left,
                        icon.top + border,
                        icon.left + border,
                        icon.bottom - border,
                        color
                    )
                    drawQuad(
                        vertices,
                        matrix,
                        icon.right - border,
                        icon.top + border,
                        icon.right,
                        icon.bottom - border,
                        color
                    )
                }
            }
        }
    }

    /** 与挂点外框一样独立提交背景和边框，避免 AUI 的物品、输入控件批次影响面板底板。 */
    private fun drawPanelBackground(vertices: VertexConsumer, matrix: Matrix4f, bounds: Box, scale: Float, color: Int) {
        drawQuad(vertices, matrix, bounds.left, bounds.top, bounds.right, bounds.bottom, color)
        val border = 0.5f * scale
        drawQuad(vertices, matrix, bounds.left, bounds.top, bounds.right, bounds.top + border, PANEL_BORDER)
        drawQuad(vertices, matrix, bounds.left, bounds.bottom - border, bounds.right, bounds.bottom, PANEL_BORDER)
        drawQuad(vertices, matrix, bounds.left, bounds.top + border, bounds.left + border, bounds.bottom - border, PANEL_BORDER)
        drawQuad(vertices, matrix, bounds.right - border, bounds.top + border, bounds.right, bounds.bottom - border, PANEL_BORDER)
        // 短角标保留工业面板的层次，不增加面板尺寸或改变命中范围。
        val corner = 6f * scale
        drawQuad(vertices, matrix, bounds.left, bounds.top, bounds.left + corner, bounds.top + scale, UI_ACCENT)
        drawQuad(vertices, matrix, bounds.left, bounds.top, bounds.left + scale, bounds.top + corner, UI_ACCENT)
        drawQuad(vertices, matrix, bounds.right - corner, bounds.top, bounds.right, bounds.top + scale, UI_ACCENT)
        drawQuad(vertices, matrix, bounds.right - scale, bounds.top, bounds.right, bounds.top + corner, UI_ACCENT)
        drawQuad(vertices, matrix, bounds.left, bounds.bottom - scale, bounds.left + corner, bounds.bottom, UI_ACCENT)
        drawQuad(vertices, matrix, bounds.left, bounds.bottom - corner, bounds.left + scale, bounds.bottom, UI_ACCENT)
        drawQuad(vertices, matrix, bounds.right - corner, bounds.bottom - scale, bounds.right, bounds.bottom, UI_ACCENT)
        drawQuad(vertices, matrix, bounds.right - scale, bounds.bottom - corner, bounds.right, bounds.bottom, UI_ACCENT)
    }

    private fun vertex(vertices: VertexConsumer, matrix: Matrix4f, x: Float, y: Float, color: Int) {
        vertices.addVertex(matrix, x, y, 0f)
            .setColor((color ushr 16) and 255, (color ushr 8) and 255, color and 255, (color ushr 24) and 255)
    }

    private fun drawQuad(
        vertices: VertexConsumer, matrix: Matrix4f, left: Float, top: Float, right: Float, bottom: Float, color: Int
    ) {
        // 与 GuiGraphics.fill 的顶点顺序一致；GUI RenderType 默认剔除背面。
        vertex(vertices, matrix, right, bottom, color)
        vertex(vertices, matrix, right, top, color)
        vertex(vertices, matrix, left, top, color)
        vertex(vertices, matrix, left, bottom, color)
    }

    private fun drawCircle(
        vertices: VertexConsumer, matrix: Matrix4f, x: Float, y: Float, outer: Float, inner: Float, color: Int
    ) {
        for (segment in 0 until CIRCLE_SEGMENTS) {
            val ax = CIRCLE_COS[segment]
            val ay = CIRCLE_SIN[segment]
            val bx = CIRCLE_COS[segment + 1]
            val by = CIRCLE_SIN[segment + 1]
            vertex(vertices, matrix, x + ax * outer, y + ay * outer, color)
            vertex(vertices, matrix, x + ax * inner, y + ay * inner, color)
            vertex(vertices, matrix, x + bx * inner, y + by * inner, color)
            vertex(vertices, matrix, x + bx * outer, y + by * outer, color)
        }
    }

    /** 从圆环外缘连到目标矩形最近的边缘，装饰线不参加命中测试。 */
    private fun drawConnector(vertices: VertexConsumer, matrix: Matrix4f, mount: Box, target: Box) {
        val endX = mount.centerX.coerceIn(target.left, target.right)
        val endY = mount.centerY.coerceIn(target.top, target.bottom)
        val dx = endX - mount.centerX
        val dy = endY - mount.centerY
        val distance = hypot(dx, dy)
        val radius = MOUNT_SIZE / 2f
        if (distance <= radius) return
        val startX = mount.centerX + dx / distance * radius
        val startY = mount.centerY + dy / distance * radius
        val normalX = -dy / distance * 0.5f
        val normalY = dx / distance * 0.5f
        vertex(vertices, matrix, startX - normalX, startY - normalY, CONNECTOR_COLOR)
        vertex(vertices, matrix, startX + normalX, startY + normalY, CONNECTOR_COLOR)
        vertex(vertices, matrix, endX + normalX, endY + normalY, CONNECTOR_COLOR)
        vertex(vertices, matrix, endX - normalX, endY - normalY, CONNECTOR_COLOR)
    }

    private fun setElementItem(container: Element, itemId: String) {
        val key = container.getAttribute("id") ?: return
        if (shownItems[key] == itemId) return
        val item = container.querySelector("item") as? AuiItem ?: return
        setItem(key, item, itemId)
    }

    private fun setItem(key: String, item: AuiItem, itemId: String?) {
        if (shownItems[key] == itemId) return
        if (hoveredItem == key) hoveredItem = null
        // AUI 的展示栈接口只标记重绘；textContent 会额外失效布局、绘制列表和命中缓存。
        item.setIngredientStack(itemStack(itemId?.let(ResourceLocation::tryParse)))
        if (itemId == null) shownItems.remove(key) else shownItems[key] = itemId
    }

    /** 只列出有明确标量含义的属性；数值与单位转换在这里集中定义。 */
    private enum class WeaponStat(
        val key: String, val reference: Double, val decimals: Int = 3,
        val unit: String = "", val higherIsBetter: Boolean = true,
        val optional: Boolean = false, val firearmOnly: Boolean = false,
        val read: (GunData) -> Double,
    ) {
        RPM("rpm", 1200.0, 0, " rpm", firearmOnly = true,
            read = {
                val rpm = it.get(GunProp.RPM) * it.get(GunProp.RPM_MULTIPLIER)
                if (rpm.isFinite()) rpm.roundToInt().coerceIn(1, 114514).toDouble() else Double.NaN
            }),
        DAMAGE("damage", 25.0, firearmOnly = true, read = { it.get(GunProp.DAMAGE) }),
        HEADSHOT("headshot", 4.0, unit = "×", firearmOnly = true, read = { it.get(GunProp.HEADSHOT) }),
        RECOIL_X("recoil_x", 0.005, higherIsBetter = false, firearmOnly = true, read = { it.get(GunProp.RECOIL_X) }),
        RECOIL_Y("recoil_y", 0.04, higherIsBetter = false, firearmOnly = true, read = { it.get(GunProp.RECOIL_Y) }),
        MAGAZINE("magazine", 60.0, 0, optional = true, firearmOnly = true, read = { it.get(GunProp.MAGAZINE).toDouble() }),
        ARMOR_BYPASS("bypasses_armor", 100.0, unit = "%", optional = true, firearmOnly = true, read = { it.get(GunProp.BYPASSES_ARMOR) * 100.0 }),
        VELOCITY("velocity", 600.0, unit = " m/s", optional = true, firearmOnly = true, read = { it.get(GunProp.VELOCITY) * 20.0 }),
        SPREAD("spread", 10.0, higherIsBetter = false, firearmOnly = true, read = { it.get(GunProp.SPREAD) }),
        AIMED_SPREAD("zoom_spread_rate", 1.0, unit = "×", higherIsBetter = false, firearmOnly = true,
            read = { it.get(GunProp.ZOOM_SPREAD_RATE) }),
        ZOOM_TIME("zoom_time", 1.0, unit = " s", higherIsBetter = false, firearmOnly = true,
            read = { (it.get(GunProp.ZOOM_TIME).coerceAtLeast(1) + 0.4 * it.item.getCustomWeight(it)).coerceAtLeast(0.0) / 20.0 }),
        NORMAL_RELOAD("normal_reload_time", 6.0, unit = " s", higherIsBetter = false, optional = true, firearmOnly = true,
            read = { it.get(GunProp.NORMAL_RELOAD_TIME) / 20.0 }),
        EMPTY_RELOAD("empty_reload_time", 8.0, unit = " s", higherIsBetter = false, optional = true, firearmOnly = true,
            read = { it.get(GunProp.EMPTY_RELOAD_TIME) / 20.0 }),
        WEIGHT("weight", 15.0, higherIsBetter = false, read = { it.get(GunProp.WEIGHT) }),
        SWAY("sway", 2.0, higherIsBetter = false, firearmOnly = true, read = { it.get(GunProp.SWAY) }),
        // 原始单位为区块；数值和横条量程一起换算成米。
        SOUND_RADIUS("sound_radius", 24.0 * 16.0, unit = " m", higherIsBetter = false, optional = true, firearmOnly = true,
            read = { it.get(GunProp.SOUND_RADIUS) * 16.0 }),
        PROJECTILES("projectile_amount", 16.0, 0, optional = true, firearmOnly = true, read = { it.get(GunProp.PROJECTILE_AMOUNT).toDouble() }),
        EXPLOSION_DAMAGE("explosion_damage", 200.0, optional = true, read = { it.get(GunProp.EXPLOSION_DAMAGE) }),
        EXPLOSION_RADIUS("explosion_radius", 12.0, unit = " m", optional = true, read = { it.get(GunProp.EXPLOSION_RADIUS) }),
        MAX_ENERGY("max_energy", 100000.0, 0, " FE", optional = true, read = { it.get(GunProp.MAX_ENERGY).toDouble() }),
        ENERGY_REGEN("energy_regeneration", 100.0, unit = " FE/s", optional = true, read = { it.get(GunProp.ENERGY_REGENERATION) * 20.0 }),
        MELEE_DAMAGE("melee_damage", 20.0, optional = true, read = { it.get(GunProp.MELEE_DAMAGE) });

        fun format(value: Double): String {
            val number = String.format(Locale.ROOT, "%.${decimals}f", value)
            return (if (decimals > 0) number.trimEnd('0').trimEnd('.') else number) + unit
        }
    }

    /** 固定量程；超出部分从左侧用金条覆盖，配件差值仍以实际数值计算。 */
    private data class StatComparison(val value: Double, val baseline: Double, val reference: Double, val higherIsBetter: Boolean) {
        val delta = value - baseline
        private val currentFraction get() = (value / reference).coerceIn(0.0, 1.0)
        private val baselineFraction get() = (baseline / reference).coerceIn(0.0, 1.0)
        val commonFraction get() = min(currentFraction, baselineFraction)
        val deltaFraction get() = abs(currentFraction - baselineFraction)
        val overflowFraction get() = (value / reference - 1.0).coerceIn(0.0, 1.0)
        val beneficial get() = if (higherIsBetter) delta > 0 else delta < 0
    }

    private data class StatRow(
        val stat: WeaponStat, val element: Element, val value: Element, val delta: Element,
        val commonBar: Element, val deltaBar: Element, val overflowBar: Element,
        val texts: List<StatText>,
        var lastValue: Double = Double.NaN, var lastBaseline: Double = Double.NaN,
        var visible: Boolean = true,
    )

    private class StatText(val viewport: Element, val element: Element, val rightAligned: Boolean) {
        var shown = false
        var content = ""
        var width = Double.NaN
        var overflow = Double.NaN
        var startedAt = 0L

        /** 两端停留后匀速往返，不改写文字、不让父列表跟着水平滚动。 */
        fun offset(now: Long): Double {
            if (overflow <= 0.0) return 0.0
            val travel = overflow / STATS_TEXT_SPEED
            val phase = ((now - startedAt).coerceAtLeast(0L) / 1e9) % (2.0 * (STATS_TEXT_PAUSE + travel))
            return when {
                phase < STATS_TEXT_PAUSE -> 0.0
                phase < STATS_TEXT_PAUSE + travel -> (phase - STATS_TEXT_PAUSE) * STATS_TEXT_SPEED
                phase < 2.0 * STATS_TEXT_PAUSE + travel -> overflow
                else -> overflow - (phase - 2.0 * STATS_TEXT_PAUSE - travel) * STATS_TEXT_SPEED
            }
        }
    }

    private fun scrollStatsBy(scroll: Element, delta: Double) {
        if (!delta.isFinite() || delta == 0.0) return
        val maxRow = (statRows.count { it.visible } - STATS_VISIBLE_ROWS).coerceAtLeast(0).toDouble()
        val steps = max(1.0, round(abs(delta) / STATS_ROW_HEIGHT)) * sign(delta)
        val row = (round(scroll.getTargetScrollTop() / STATS_ROW_HEIGHT) + steps).coerceIn(0.0, maxRow)
        scroll.setScrollTop(row * STATS_ROW_HEIGHT)
    }

    private fun syncStatsPresentation(doc: Document, now: Long = System.nanoTime()) {
        val scroll = doc.getElementById("stats-scroll") ?: return
        val count = statRows.count { it.visible }
        setStyle(doc.getElementById("stats-content"), "height", px(count * STATS_ROW_HEIGHT))
        // 滚轮按整行移动；拖动原生滚动条松手后也吸附到整行，避免长期停在半行。
        if (!scroll.isScrollbarInteractionActive) {
            val maxRow = (count - STATS_VISIBLE_ROWS).coerceAtLeast(0).toDouble()
            val top = round(scroll.getTargetScrollTop() / STATS_ROW_HEIGHT).coerceIn(0.0, maxRow) * STATS_ROW_HEIGHT
            if (abs(scroll.getTargetScrollTop() - top) > 0.01) scroll.setScrollTop(top)
        }
        val top = scroll.getScrollTop()
        val bottom = top + STATS_VISIBLE_ROWS * STATS_ROW_HEIGHT
        var index = 0
        for (row in statRows) {
            val rowTop = index * STATS_ROW_HEIGHT
            val shown = row.visible && rowTop + STATS_ROW_HEIGHT > top && rowTop < bottom
            if (row.visible) index++
            for (text in row.texts) {
                if (!shown) {
                    text.shown = false
                    continue
                }
                val content = text.element.textContent
                val width = text.viewport.clientWidth
                val measured = Text.measureLine(Text.of(text.element), content)
                val overflow = (measured - width).coerceAtLeast(0.0)
                if (!text.shown || text.content != content || text.width != width || text.overflow != overflow) {
                    text.startedAt = now
                    text.content = content
                    text.width = width
                    text.overflow = overflow
                }
                text.shown = true
                val offset = if (overflow > 0.0) -text.offset(now) else if (text.rightAligned) width - measured else 0.0
                setStyle(text.element, "transform", "translateX(${px(offset.toFloat())})")
            }
        }
    }

    private fun statLabel(key: String) = label("stats.$key")

    private fun prepareStats(doc: Document) {
        doc.getElementById("stats-toggle")?.let { button ->
            button.addEventListener("click") {
                statsOpen = !statsOpen
                statsRefreshTick = Long.MIN_VALUE
            }
        }
        doc.getElementById("stats-title")?.textContent = statLabel("title")
        val content = doc.getElementById("stats-content") ?: return
        content.innerHTML = WeaponStat.entries.joinToString("") { stat ->
            val id = stat.key
            val texts = listOf("label", "value", "delta").joinToString("") { part ->
                """<div id="stat-$id-$part" class="stat-$part"><div id="stat-$id-$part-text" class="stat-text"></div></div>"""
            }
            """<div id="stat-$id" class="stat-row"><div id="stat-$id-icon" class="stat-icon"></div>$texts<div class="stat-track"><div id="stat-$id-common" class="stat-common"></div><div id="stat-$id-change" class="stat-change"></div><div id="stat-$id-overflow" class="stat-overflow"></div></div></div>"""
        }
        for (stat in WeaponStat.entries) {
            val id = "stat-${stat.key}"
            val texts = listOf("label", "value", "delta").mapNotNull { part ->
                val viewport = doc.getElementById("$id-$part") ?: return@mapNotNull null
                val element = doc.getElementById("$id-$part-text") ?: return@mapNotNull null
                StatText(viewport, element, part == "value")
            }
            if (texts.size != 3) continue
            texts[0].element.textContent = Component.translatable("prop.superbwarfare.${stat.key}").string
            statRows += StatRow(stat, doc.getElementById(id) ?: continue,
                texts[1].element, texts[2].element,
                doc.getElementById("$id-common") ?: continue, doc.getElementById("$id-change") ?: continue,
                doc.getElementById("$id-overflow") ?: continue, texts)
        }
    }

    private fun syncStats(doc: Document, data: GunData, tick: Long): Box? {
        val root = doc.getElementById("weapon-stats") ?: return null
        doc.getElementById("stats-toggle")?.let {
            val text = statLabel(if (statsOpen) "hide" else "show")
            doc.getElementById("stats-toggle-label")?.let { label ->
                if (label.textContent != text) label.textContent = text
            }
            val expanded = statsOpen.toString()
            if (it.getAttribute("aria-expanded") != expanded) it.setAttribute("aria-expanded", expanded)
        }
        setStyle(doc.getElementById("stats-chart"), "display", if (statsOpen) "block" else "none")
        // 字号跟随其他面板；通过收窄属性面板保持四分之一屏幕的面积限制。
        val scale = editorPanelScale()
        val height = if (statsOpen) STATS_HEIGHT else STATS_BUTTON_HEIGHT
        val width = if (statsOpen) minOf(STATS_WIDTH, (viewportWidth / 2f - 12f).coerceAtLeast(1f) / scale) else STATS_BUTTON_WIDTH
        setStyle(root, "width", px(width))
        setStyle(root, "height", px(height))
        val bounds = Box(6f, TITLE_BAR_HEIGHT + 6f, width * scale, height * scale)
        place(root, bounds, scale)
        if (!statsOpen) {
            statRows.forEach { row -> row.texts.forEach { it.shown = false } }
            return bounds
        }
        val chartHeight = height - STATS_CHART_TOP
        setStyle(doc.getElementById("stats-chart"), "height", px(chartHeight))
        setStyle(doc.getElementById("stats-scroll"), "height", px(chartHeight - STATS_CHART_CHROME))
        setStyle(doc.getElementById("stats-scroll"), "width", px(width - 14f))
        setStyle(doc.getElementById("stats-content"), "width", px(width - 22f))
        if (statsRefreshTick != Long.MIN_VALUE && tick >= statsRefreshTick && tick - statsRefreshTick < 2) {
            syncStatsPresentation(doc)
            return bounds
        }
        statsRefreshTick = tick

        val state = data.state
        val oldState = statsState
        if (statsBaseline?.item !== data.item || statsDataVersion != GunData.DATA_VERSION || oldState == null ||
            oldState.uuid != state.uuid || state.structurallyDiffersFrom(oldState) ||
            statsPerks != data.perkTag || statsAttachments != data.attachmentTag
        ) {
            // 只操作副本。保留相同等级、Perk、弹种、覆写及运行状态，卸下全部配件后重新跑 PMC。
            // 不调用原 GunData 的 save/copy/flush，避免推进客户端 revision。
            statsBaseline = GunData.from(data.stack.copy()).also { baseline ->
                baseline.updateLocal { state }
                baseline.attachmentTag.allKeys.toList().forEach(baseline.attachmentTag::remove)
                baseline.invalidateProperties()
            }
            statsState = state
            statsPerks = data.perkTag.copy()
            statsAttachments = data.attachmentTag.copy()
            statsDataVersion = GunData.DATA_VERSION
        }
        val baseline = statsBaseline ?: return bounds
        val hasFirearm = !data.meleeOnly() || !baseline.meleeOnly()
        for (row in statRows) {
            val stat = row.stat
            val current = stat.read(data)
            val original = stat.read(baseline)
            val visible = current.isFinite() && original.isFinite() && current >= 0.0 && original >= 0.0 &&
                (!stat.firearmOnly || hasFirearm) && (!stat.optional || max(current, original) > 0.0) &&
                (stat != WeaponStat.PROJECTILES || max(current, original) > 1.0)
            row.visible = visible
            setStyle(row.element, "display", if (visible) "block" else "none")
            if (!visible || (row.lastValue == current && row.lastBaseline == original)) continue
            row.lastValue = current
            row.lastBaseline = original
            val comparison = StatComparison(current, original, stat.reference, stat.higherIsBetter)
            row.value.textContent = stat.format(current)
            // 三位小数以下的真实变化仍保留方向，不能把微小的后坐力增减显示成零。
            val precision = 10.0.pow(stat.decimals)
            val roundedDelta = round(comparison.delta * precision) / precision
            val changed = abs(comparison.delta) > max(1e-9, max(Math.ulp(current), Math.ulp(original)) * 4.0)
            val sign = if (comparison.delta > 0) "+" else "-"
            row.delta.textContent = when {
                !changed -> ""
                roundedDelta == 0.0 -> "($sign<${stat.format(1.0 / precision)})"
                else -> "($sign${stat.format(abs(roundedDelta))})"
            }
            val color = if (comparison.beneficial) STATS_GAIN_COLOR else STATS_LOSS_COLOR
            setStyle(row.delta, "color", color)
            setStyle(row.commonBar, "width", "${comparison.commonFraction * 100.0}%")
            setStyle(row.deltaBar, "left", "${comparison.commonFraction * 100.0}%")
            setStyle(row.deltaBar, "width", "${if (changed) comparison.deltaFraction * 100.0 else 0.0}%")
            setStyle(row.deltaBar, "background-color", color)
            setStyle(row.overflowBar, "width", "${comparison.overflowFraction * 100.0}%")
        }
        syncStatsPresentation(doc)
        return bounds
    }

    private fun setClass(element: Element, name: String, enabled: Boolean) {
        val classes = element.classList ?: return
        if (classes.contains(name) == enabled) return
        if (enabled) classes.add(name) else classes.remove(name)
    }

    private fun itemStack(id: ResourceLocation?): ItemStack =
        id?.let { BuiltInRegistries.ITEM.get(it) }?.let(::ItemStack) ?: ItemStack.EMPTY

    private fun px(value: Float) = "${round(value.toDouble() * 100.0) / 100.0}px"
    private fun slotName(type: AttachmentType) =
        Component.translatable("attachment.superbwarfare.slot.${type.attachmentName.lowercase(Locale.ROOT)}").string

    private fun label(key: String) = Component.translatable("screen.superbwarfare.weapon_edit.$key").string

    companion object {
        private const val DOCUMENT_PATH = "superbwarfare/weapon_edit.html"
        private const val UI_FONT_FAMILY = "WeaponEditHeading, sans-serif"
        private const val TOOLTIP_FONT_SIZE = 7.0
        private const val TOOLTIP_LINE_HEIGHT = 10f
        private const val TOOLTIP_PADDING = 6f
        private const val TOOLTIP_MIN_WIDTH = 96f
        private const val TOOLTIP_MAX_WIDTH = 160f
        // 与 weapon_edit.html 的主题色及 title-bar 高度保持一致。
        private const val UI_ACCENT = 0xff52fcda.toInt()
        private const val PANEL_BORDER = 0xff237b87.toInt()
        private const val PANEL_BACKGROUND = 0xe6001219.toInt()
        private const val CONNECTOR_COLOR = 0xcc52fcda.toInt()
        private const val MOUNT_EMPTY_COLOR = 0xffffffff.toInt()
        private const val MOUNT_FILLED_COLOR = UI_ACCENT
        private const val TITLE_BAR_HEIGHT = 18f
        private const val MOUNT_REVEAL_DELAY_TICKS = 10
        private const val AIR = "minecraft:air"
        private const val AMMO_MOUNT_ID = "mount-ammo_type"
        private const val SCOPE_STACK_MOUNT_ID = "mount-scope_stack"
        private const val MOUNT_SIZE = 9f
        private const val ICON_SIZE = 17f
        private const val PANEL_WIDTH = 180f
        private const val PANEL_HEIGHT = 134f
        private const val SETTINGS_WIDTH = PANEL_WIDTH
        private val ADJUSTMENT_NUMBER = Regex("-?[0-9]*(\\.[0-9]*)?")
        private val RGB_NUMBER = Regex("[0-9]*")
        private val ADJUSTMENT_PROPERTIES = listOf("rotation", "offset", "red", "green", "blue")
        private const val SETTINGS_BASE_HEIGHT = 56f
        private const val SETTINGS_BOTTOM_INSET = 24f
        private const val SETTINGS_ROW_HEIGHT = 28f // 24px 卡片 + 4px 间隔
        private const val SETTINGS_ROW_GAP = 4f
        private const val SETTINGS_MIN_HEIGHT = SETTINGS_BASE_HEIGHT + SETTINGS_ROW_HEIGHT
        private const val SLIDER_TRACK_LEFT = 3.5f
        private const val SLIDER_TRACK_WIDTH = 58f
        private const val SLIDER_THUMB_RADIUS = 3.5f
        private const val GRID_COLUMNS = 8
        private const val GRID_VISIBLE_ROWS = 3
        private const val GRID_WINDOW_ROWS = GRID_VISIBLE_ROWS + 3
        private const val GRID_ROW_GAP = 4f
        private const val GRID_ROW_PITCH = 20f
        private const val GRID_VIEWPORT_HEIGHT = GRID_VISIBLE_ROWS * GRID_ROW_PITCH - GRID_ROW_GAP
        private const val STATS_WIDTH = 210f
        private const val STATS_BUTTON_WIDTH = 112f
        private const val STATS_BUTTON_HEIGHT = 17f
        private const val STATS_CHART_TOP = 21f
        private const val STATS_CHART_CHROME = 28f
        private const val STATS_VISIBLE_ROWS = 5
        private const val STATS_ROW_HEIGHT = 14f
        private const val STATS_HEIGHT = STATS_CHART_TOP + STATS_CHART_CHROME + STATS_VISIBLE_ROWS * STATS_ROW_HEIGHT
        private const val STATS_TEXT_SPEED = 14.0
        private const val STATS_TEXT_PAUSE = 1.0
        private const val STATS_GAIN_COLOR = "#52fcda"
        private const val STATS_LOSS_COLOR = "#ff7070"
        private val SEARCH_COS = DoubleArray(24) { cos(it * PI * 2 / 24) }
        private val SEARCH_SIN = DoubleArray(24) { sin(it * PI * 2 / 24) }
        private const val CIRCLE_SEGMENTS = 48
        private val CIRCLE_COS = FloatArray(CIRCLE_SEGMENTS + 1) { cos(it * PI * 2 / CIRCLE_SEGMENTS).toFloat() }
        private val CIRCLE_SIN = FloatArray(CIRCLE_SEGMENTS + 1) { sin(it * PI * 2 / CIRCLE_SEGMENTS).toFloat() }
        private fun mountId(type: AttachmentType) = "mount-" + type.attachmentName.lowercase(Locale.ROOT)
        private fun itemSlot(id: String = AIR, size: Int = 16) =
            """<slot render="item" interactive="0" pointer="0" size="$size" iconScale="${size / 16f}"><item>$id</item></slot>"""
    }
}
