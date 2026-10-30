package com.atsuishio.superbwarfare.client.overlay.ammo

import com.atsuishio.superbwarfare.client.language.ClientLanguageGetter
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer.AmmoConsumeType
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.FormatTool.format1DZZ
import com.sighs.apricityui.ApricityUI
import com.sighs.apricityui.init.Document
import com.sighs.apricityui.layout.Position
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.regex.Pattern
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
object AmmoBarHud {
    private const val PATH = "superbwarfare/ammo_bar.html"
    private val FORMAT_CODE = Pattern.compile("§.")
    private val LARGE_FIRE_MODE_ICONS = setOf("rpm600", "rpm1200", "rpm1800")
    // 与 ammo_bar.html 中 .sbw-muted 一致：备用弹药与不可切换的开火模式键共用此灰色。
    private const val MUTED_CLASS = "sbw-muted"
    private var document: Document? = null
    private var generation = -1L
    private val values = mutableMapOf<String, String>()
    private var gunIdentity: Any? = null
    private var lastIcon = ""
    private var wasReloading = false
    private var gunAnimationAt = 0L
    private var reloadAt = 0L
    private var mode = ""
    private var modeAt = 0L
    private var selectedAmmo = -1
    private var sourceSignature = ""
    private val carousel = AmmoHudCarousel()
    private val resourceFrames = AmmoHudFrames()
    private val sourceSlots = arrayOf<List<AmmoHudResources.Resource>>(emptyList(), emptyList())
    private data class DirectionHint(val gun: Any, val target: Int, val direction: Int, val expires: Long)
    private var directionHint: DirectionHint? = null

    // 两种弹种循环时，仅凭新旧序号无法判断方向；动画仍等待服务器确认切换。
    fun noteAmmoSwitch(data: GunData, add: Boolean) {
        val count = data.get(GunProp.AMMO_CONSUMER).size
        if (count < 2) return
        val direction = if (add) 1 else -1
        directionHint = DirectionHint(
            data.uuid ?: data.stack.item, (data.selectedAmmoType.get() + direction + count) % count,
            direction, Util.getMillis() + 2000
        )
    }

    fun render(data: GunData, player: Player) {
        if (player.isSpectator || Minecraft.getInstance().screen != null) {
            close()
            return
        }
        var doc = document
        if (doc == null || !doc.isActive) {
            close()
            AmmoHudText.register()
            AmmoHudResourceGroup.register()
            doc = ApricityUI.createDocument(PATH) ?: return
            document = doc
        }
        val active = doc
        if (generation != active.refreshGeneration) {
            reset()
            generation = active.refreshGeneration
        }
        Document.runWithContext(active) {
            update(active, data, player)
        }
    }

    private fun update(doc: Document, data: GunData, player: Player) {
        val now = Util.getMillis()
        val reduced = DisplayConfig.REDUCED_AMMO_HUD_MOTION.get()
        val mc = Minecraft.getInstance()
        // AUI caps gui mode at scale 5. Convert the origin and unit instead of assuming 1:1.
        val origin = doc.guiToDocumentPosition(Position(0.0, 0.0))
        val unit = doc.guiToDocumentPosition(Position(1.0, 1.0))
        val x = mc.window.guiScaledWidth + DisplayConfig.WEAPON_HUD_X_OFFSET.get() - 160.0
        val y = mc.window.guiScaledHeight + DisplayConfig.WEAPON_HUD_Y_OFFSET.get() - 61.0
        val anchor = doc.guiToDocumentPosition(Position(x, y))
        val scaleX = unit.x - origin.x
        val scaleY = unit.y - origin.y
        // 序号及阴影占到 66px；使用 AUI 的视口尺寸预留底边，避免 GUI 尺寸取整差异导致裁剪。
        val top = anchor.y.coerceIn(0.0, (doc.viewportSize.height() - 68.0 * scaleY).coerceAtLeast(0.0))
        attr("ammo-bar", "style", "left:${anchor.x}px;top:${top}px;transform:scale($scaleX,$scaleY);")
        text("gun-name", ClientLanguageGetter.EN_US.getOrDefault(data.stack.descriptionId))
        val ammoName = if (data.meleeOnly()) "Melee" else data.selectedAmmoConsumer().getDisplayName()
        text("ammo-name", FORMAT_CODE.matcher(ammoName).replaceAll(""))
        text("ammo-count", getGunAmmoString(data, player))
        text("backup-ammo", getBackupAmmoString(data, player))
        attr("ammo-row", "style", "top:${if (data.useBackpackAmmo()) 26 else 21}px;")
        // Flex uses actual rendered text width; no fixed digit count or guessed font metrics.
        text("virtual-ammo", if (data.virtualAmmo.get() > 0 && !data.meleeOnly()) "+${data.virtualAmmo.get()}" else "")
        text("fire-mode-key", "[${ModKeyMappings.FIRE_MODE.key.displayName.string}]")
        // 只有一种开火模式时这个键切不了模式，按备用弹药的灰色弱化。
        val modes = data.get(GunProp.AVAILABLE_FIRE_MODES)
        toggleClass("fire-mode-key", MUTED_CLASS, modes.size <= 1)

        val icon = (data.stack.item as GunItem).getGunIcon(data).toString()
        val identity = data.uuid ?: data.stack.item
        val switched = gunIdentity != identity || lastIcon != icon
        val reloading = data.reloading()
        if (switched) {
            gunIdentity = identity
            lastIcon = icon
            gunAnimationAt = now
            reloadAt = now
            wasReloading = false
            selectedAmmo = -1
            sourceSignature = ""
            carousel.clear()
            resourceFrames.clear()
            sourceSlots.fill(emptyList())
            mode = ""
        }
        if (reloading && !wasReloading) reloadAt = now
        if (!reloading && wasReloading) gunAnimationAt = now
        attr("gun-icon", "src", icon)
        animateGun(now, reloading, reduced)
        wasReloading = reloading

        val fireMode = fireModeTextureName(data.selectedFireModeInfo().name)
        val fireModeSize = if (fireMode in LARGE_FIRE_MODE_ICONS) 16 else 8
        if (fireMode != mode) {
            mode = fireMode
            modeAt = now
        }
        attr("fire-mode-icon", "src", "superbwarfare:textures/overlay/ammo_bar/fire_mode/$fireMode.png")
        val fade = if (reduced) 1.0 else progress(now, modeAt, 120)
        attr("fire-mode-icon", "style", "width:${fireModeSize}px;height:${fireModeSize}px;opacity:$fade;")
        val showModeIndex = DisplayConfig.ADVANCED_AMMO_HUD.get() && modes.size > 3
        ordinal("fire-mode-index", data.selectedFireMode.get(), if (showModeIndex) modes.size else 0)
        updateSources(data, player, now, reduced)
    }

    private fun animateGun(now: Long, reloading: Boolean, reduced: Boolean) {
        var scale = 1.0
        var opacity = 1.0
        if (reloading) {
            if (reduced) opacity = 0.45
            else {
                val elapsed = (now - reloadAt).coerceAtLeast(0)
                val phase = (elapsed % 300) / 300.0
                opacity = if ((elapsed / 300) % 2 == 0L) 1 - ease(phase) else ease(phase)
            }
        } else if (!reduced) {
            val t = progress(now, gunAnimationAt, 500)
            if (t < 0.3) {
                val segment = ease(t / 0.3)
                scale = 1.08 + 0.07 * segment
                opacity = segment
            } else scale = 1.15 - 0.15 * ease((t - 0.3) / 0.7)
        }
        attr("gun-icon", "style", "transform:scale($scale);opacity:$opacity;")
    }

    private fun updateSources(data: GunData, player: Player, now: Long, reduced: Boolean) {
        val consumers = data.get(GunProp.AMMO_CONSUMER)
        val consumer = data.selectedAmmoConsumer()
        val shown = DisplayConfig.ADVANCED_AMMO_HUD.get() && !data.meleeOnly() && consumer.sources.isNotEmpty() &&
            (consumers.size > 1 || consumer.sources.size > 1 || consumer.type != AmmoConsumeType.PLAYER_AMMO)
        attr("resource-seat", "style", if (shown) "display:block;" else "display:none;")
        if (!shown) {
            selectedAmmo = -1
            sourceSignature = ""
            carousel.clear()
            resourceFrames.clear()
            sourceSlots.fill(emptyList())
            (document?.getElementById("resource-content") as? AmmoHudResourceGroup)?.clear()
            ordinal("ammo-type-index", 0, 0)
            return
        }
        val selected = data.selectedAmmoType.get().coerceIn(consumers.indices)
        val signature = consumer.sources.joinToString("|") { it.ammo }
        val group = AmmoHudResources.group(data, player)
        val target = AmmoHudFrames.width(group.size)
        var direction = 1
        if (selected != selectedAmmo || signature != sourceSignature) {
            val next = if (selectedAmmo < 0) 0 else (selected - selectedAmmo + consumers.size) % consumers.size
            val previous = if (selectedAmmo < 0) 0 else (selectedAmmo - selected + consumers.size) % consumers.size
            val hint = directionHint?.takeIf {
                it.gun == gunIdentity && it.target == selected && now <= it.expires
            }
            direction = if (previous == next && hint != null) hint.direction else if (previous < next) -1 else 1
            directionHint = null
            selectedAmmo = selected
            sourceSignature = signature
        }
        val frame = carousel.select("$selected:$signature", target, direction, now, reduced)
        val corners = resourceFrames.select(group.size, direction, now, reduced)
        style("ammo-type-index", "width", "${frame.width}px")
        ordinal("ammo-type-index", selected, consumers.size)
        for ((index, pose) in frame.slots.withIndex()) {
            if (pose.key == null) sourceSlots[index] = emptyList()
            else if (index == frame.current) sourceSlots[index] = group
        }
        // One immutable paint snapshot keeps item position, alpha and stack identity in sync.
        // The current group always paints last; no per-frame CSS transform/display/z-index commits.
        val groups = frame.slots.indices.sortedBy { if (it == frame.current) 1 else 0 }.mapNotNull { index ->
            val pose = frame.slots[index]
            if (!pose.visible) null else AmmoHudResourceGroup.Group(sourceSlots[index], pose.x, pose.opacity)
        }
        (document?.getElementById("resource-content") as? AmmoHudResourceGroup)?.show(groups, corners)
        val paintWidth = maxOf(target, AmmoHudFrames.width(corners.size),
            frame.slots.filter { it.visible }.maxOfOrNull { it.width } ?: target)
        // Keep the clip stable for the entire transition, including the outgoing wider group.
        style("resource-content", "width", "${paintWidth + 10}px")
        style("resource-viewport", "width", "${paintWidth + 20}px")
    }

    private fun ordinal(id: String, selected: Int, total: Int) {
        style(id, "display", if (total > 1) "flex" else "none")
        text("$id-current", if (total > 1) (selected.coerceIn(0, total - 1) + 1).toString() else "")
        text("$id-total", if (total > 1) "/$total" else "")
    }

    private fun text(id: String, value: String) = changed("text:$id", value) {
        (document?.getElementById(id) as? AmmoHudText)?.setLabel(value)
    }
    private fun attr(id: String, name: String, value: String) = changed("$name:$id", value) { document?.getElementById(id)?.setAttribute(name, value) }
    private fun style(id: String, property: String, value: String) = changed("css:$property:$id", value) {
        document?.getElementById(id)?.setInlineStyleProperty(property, value)
    }
    private fun toggleClass(id: String, name: String, enabled: Boolean) = changed("class:$id:$name", enabled.toString()) {
        document?.getElementById(id)?.classList?.toggle(name, enabled)
    }

    private inline fun changed(key: String, value: String, write: () -> Unit) {
        if (values[key] == value) return
        write()
        values[key] = value
    }

    private fun progress(now: Long, start: Long, duration: Int) = ((now - start).toDouble() / duration).coerceIn(0.0, 1.0)

    // cubic-bezier(.25,.1,.25,1)
    private fun ease(t: Double): Double {
        if (t <= 0 || t >= 1) return t
        fun bezier(u: Double, a: Double, b: Double) = 3 * (1 - u) * (1 - u) * u * a + 3 * (1 - u) * u * u * b + u * u * u
        var low = 0.0
        var high = 1.0
        repeat(16) {
            val mid = (low + high) / 2
            if (bezier(mid, 0.25, 0.25) < t) low = mid else high = mid
        }
        return bezier((low + high) / 2, 0.1, 1.0)
    }

    fun close() {
        document?.remove()
        document = null
        generation = -1
        reset()
    }

    // 保留 RPM600 等连续缩写，SemiAuto 则转换为 semi_auto。
    private fun fireModeTextureName(name: String): String = buildString(name.length + 4) {
        for ((index, character) in name.withIndex()) {
            if (character.isUpperCase()) {
                val previous = name.getOrNull(index - 1)
                if (previous != null && (previous.isLowerCase() || previous.isDigit())) append('_')
                append(character.lowercaseChar())
            } else append(character)
        }
    }

    private fun getEnergyPercentString(data: GunData): String {
        val rate = data.stack.getCapability(ForgeCapabilities.ENERGY)
            .map { storage ->
                (storage.energyStored.toDouble() / max(1, storage.maxEnergyStored)).coerceIn(0.0, 1.0)
            }
            .orElseGet { 0.0 }
        return format1DZZ(rate * 100) + "%"
    }

    private fun getGunAmmoString(data: GunData, player: Player): String {
        if (data.isEnergyMagazine()) return data.ammo.get().toString()
        val consumer = data.selectedAmmoConsumer()
        if (consumer.type == AmmoConsumeType.ENERGY) return getEnergyPercentString(data)
        if (data.meleeOnly() || data.useBackpackAmmo() && data.hasInfiniteBackupAmmo(player)) return "∞"
        if (data.useBackpackAmmo()) {
            return displayAmmoCount(data, data.countBackupAmmo(player) - data.virtualAmmo.get()).toString()
        }
        return data.ammo.get().toString()
    }

    private fun getBackupAmmoString(data: GunData, player: Player): String {
        if (data.isEnergyMagazine()) return getEnergyPercentString(data)
        val consumer = data.selectedAmmoConsumer()
        if (data.meleeOnly() || data.useBackpackAmmo() || consumer.type == AmmoConsumeType.ENERGY) return ""
        if (data.hasInfiniteBackupAmmo(player)) return "∞"
        return displayAmmoCount(data, data.countBackupAmmo(player) - data.virtualAmmo.get()).toString()
    }

    private fun displayAmmoCount(data: GunData, count: Int): Int {
        val source = data.selectedAmmoConsumer().primary
        return source.strategy.getDisplayCount(data, source, count)
    }

    private fun reset() {
        values.clear()
        gunIdentity = null
        lastIcon = ""
        wasReloading = false
        gunAnimationAt = 0
        reloadAt = 0
        mode = ""
        modeAt = 0
        selectedAmmo = -1
        sourceSignature = ""
        carousel.clear()
        resourceFrames.clear()
        sourceSlots.fill(emptyList())
        directionHint = null
    }

    // 菜单、F1 和退出世界可能跳过渲染事件，仍需在 tick 中清理文档。
    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || document == null) return
        val mc = Minecraft.getInstance()
        val player = mc.player
        if (player == null || player.isSpectator || mc.level == null || mc.screen != null ||
            mc.options.hideGui || !DisplayConfig.AMMO_HUD.get()
        ) {
            close()
            return
        }
        val stack = ActiveGun.stackOf(player)
        val vehicle = player.vehicle
        if (stack.item !is GunItem || !GunItem.isOperable(stack) || vehicle is VehicleEntity && vehicle.banHand(player)) close()
    }
}
