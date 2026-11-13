package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.overlay.ammo.AmmoHudText
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.perk.Perk
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.sighs.apricityui.ApricityUI
import com.sighs.apricityui.init.Document
import com.sighs.apricityui.layout.Position
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.RegistryManager
import java.util.*
import kotlin.math.sin

@Mod.EventBusSubscriber(Dist.CLIENT)
object PerkOverlay : CommonOverlay("perks") {
    private const val LIMIT = 5
    private const val ROW_HEIGHT = 12
    private var document: Document? = null
    private var generation = -1L
    private var gunIdentity: Any? = null
    private val values = mutableMapOf<String, String>()
    private val rows = linkedMapOf<String, Row>()

    private enum class Effect { BUFF, DEBUFF }

    private data class Entry(
        val id: String,
        val type: Perk.Type,
        val detail: String = "",
        val passive: Boolean = false,
        val countdown: Boolean = false,
        val effect: Effect = Effect.BUFF
    ) {
        val icon: ResourceLocation
            get() = ResourceLocation(
                "superbwarfare",
                "textures/overlay/perk/${type.typeName.lowercase(Locale.ROOT)}_perk_${effect.name.lowercase(Locale.ROOT)}.png"
            )
    }

    private class Row(var entry: Entry, val appeared: Long, var removed: Long = 0)

    override fun shouldRender(): Boolean {
        val visible = super.shouldRender() && DisplayConfig.PERK_HUD.get() &&
                !MiscConfig.HIDE_COMBAT_HUD.get() && !ClientEventHandler.isEditing &&
                Minecraft.getInstance().screen == null
        if (!visible) close()
        return visible
    }

    private fun currentData(): GunData? {
        val player = Minecraft.getInstance().player ?: return null
        val stack = ActiveGun.stackOf(player)
        val vehicle = player.vehicle
        if (stack.item !is GunItem || !GunItem.isOperable(stack) ||
            vehicle is VehicleEntity && vehicle.banHand(player)
        ) return null
        return GunData.from(stack)
    }

    override fun RenderContext.render() {
        val data = currentData()
        if (data == null) {
            close()
            return
        }
        val identity = data.uuid ?: data.stack
        if (gunIdentity != identity) {
            close()
            gunIdentity = identity
        }
        val now = Util.getMillis()
        val entries = collect(data)
        if (entries.isEmpty() && rows.isEmpty()) return
        var doc = document
        if (doc == null || !doc.isActive) {
            doc?.remove()
            AmmoHudText.register()
            doc = ApricityUI.createDocument("superbwarfare/perks.html") ?: return
            document = doc
            generation = -1L
        }
        if (generation != doc.refreshGeneration) {
            rows.clear()
            values.clear()
            generation = doc.refreshGeneration
        }
        entries.forEach { entry ->
            val row = rows[entry.id]
            if (row == null || row.removed != 0L || row.entry.effect != entry.effect) {
                rows.remove(entry.id)
                rows[entry.id] = Row(entry, now)
            }
            else row.entry = entry
        }
        val activeIds = entries.map { it.id }.toSet()
        rows.values.forEach { if (it.entry.id !in activeIds && it.removed == 0L) it.removed = now }
        rows.entries.removeIf { it.value.removed != 0L && now - it.value.removed >= 200 }
        if (rows.isEmpty()) {
            close()
            return
        }
        val active = doc
        Document.runWithContext(active) { update(active, now) }
    }

    private data class Context(val data: GunData, val tag: CompoundTag, val level: Int)
    private data class Rule(
        val matches: (Context) -> Boolean,
        val detail: (Context) -> String,
        val effect: Effect,
        val breathe: Boolean,
        val blink: Boolean
    )

    fun registerReloadListener(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(Rules)
    }

    private fun collect(data: GunData): List<Entry> = buildList {
        val registry = RegistryManager.ACTIVE.getRegistry(ModPerks.PERK_KEY)
        for (type in Perk.Type.entries) {
            for (instance in data.perk.getInstances(type)) {
                val perk = instance.perk
                val id = registry.getKey(perk) ?: continue
                val rules = Rules.definitions[id] ?: continue
                val context = Context(data, data.perk.getTag(perk) ?: continue, instance.level.toInt())
                val rule = rules.firstOrNull { it.matches(context) } ?: continue
                add(Entry(id.toString(), type, rule.detail(context), rule.breathe, rule.blink, rule.effect))
            }
        }
    }

    private object Rules : SimpleJsonResourceReloadListener(Gson(), "perk_hud") {
        var definitions: Map<ResourceLocation, List<Rule>> = emptyMap()
            private set

        override fun apply(json: MutableMap<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
            definitions = buildMap {
                for ((id, element) in json) {
                    try {
                        val root = element.asJsonObject
                        fields(root, "States")
                        put(id, root.getAsJsonArray("States").map { parseRule(it.asJsonObject) })
                    } catch (e: Exception) {
                        com.atsuishio.superbwarfare.Mod.LOGGER.error("Invalid perk HUD definition {}: {}", id, e.message)
                    }
                }
            }
            close()
        }

        private fun fields(obj: JsonObject, vararg allowed: String) {
            require(obj.keySet().all { it in allowed }) { "Unknown fields: ${obj.keySet() - allowed.toSet()}" }
        }

        private fun parseRule(obj: JsonObject): Rule {
            fields(obj, "When", "Effect", "Display", "Value", "Maximum", "Text", "Parts", "Separator", "Breathe", "Blink")
            val effect = Effect.valueOf((obj.get("Effect")?.asString ?: "buff").uppercase(Locale.ROOT))
            val display = obj.get("Display")?.asString ?: "name"
            return Rule(condition(obj.get("When")), detail(obj), effect,
                obj.get("Breathe")?.asBoolean ?: false, obj.get("Blink")?.asBoolean ?: (display == "timer"))
        }

        private fun condition(element: JsonElement?): (Context) -> Boolean {
            if (element == null) return { true }
            if (element.isJsonPrimitive && element.asJsonPrimitive.isBoolean) return { element.asBoolean }
            val obj = element.asJsonObject
            fields(obj, "All", "Any", "Not", "Tag", "Source", "Value", "Gt", "Gte", "Lt", "Lte", "Equals")
            require(listOf("Tag", "Source", "Value").count { obj.has(it) } <= 1) { "Use one of Tag, Source or Value" }
            val all = obj.getAsJsonArray("All")?.map { condition(it) } ?: emptyList()
            val any = obj.getAsJsonArray("Any")?.map { condition(it) }
            val not = obj.get("Not")?.let { condition(it) }
            val input = when {
                obj.has("Tag") -> number(JsonObject().apply { add("Tag", obj.get("Tag")) })
                obj.has("Source") -> number(JsonObject().apply { add("Source", obj.get("Source")) })
                else -> number(obj.get("Value"))
            }
            val comparisons = listOf("Gt", "Gte", "Lt", "Lte", "Equals").mapNotNull { op ->
                obj.get(op)?.let { op to number(it) }
            }
            return { context ->
                all.all { it(context) } && (any == null || any.any { it(context) }) &&
                    (not == null || !not(context)) && comparisons.all { (op, target) ->
                        val a = input(context)
                        val b = target(context)
                        when (op) {
                            "Gt" -> a > b
                            "Gte" -> a >= b
                            "Lt" -> a < b
                            "Lte" -> a <= b
                            else -> a == b
                        }
                    }
            }
        }

        private fun number(element: JsonElement?): (Context) -> Double {
            if (element == null) return { 0.0 }
            if (element.isJsonPrimitive) {
                val primitive = element.asJsonPrimitive
                if (primitive.isString) return number(JsonObject().apply { add("Tag", primitive) })
                val value = if (primitive.isBoolean) { if (primitive.asBoolean) 1.0 else 0.0 } else primitive.asDouble
                require(value.isFinite()) { "Non-finite number" }
                return { value }
            }
            val obj = element.asJsonObject
            fields(obj, "Tag", "Source", "Constant", "PerLevel", "Scale", "Offset", "Min", "Max")
            require(listOf("Tag", "Source", "Constant").count { obj.has(it) } <= 1) { "Use one of Tag, Source or Constant" }
            val base: (Context) -> Double = when {
                obj.has("Tag") -> {
                    val path = obj.get("Tag").asString.split('.')
                    require(path.all { it.isNotEmpty() }) { "Empty tag path" }
                    val read: (Context) -> Double = { context ->
                        var tag = context.tag
                        for (part in path.dropLast(1)) tag = tag.getCompound(part)
                        tag.getDouble(path.last())
                    }
                    read
                }
                obj.has("Source") -> source(obj.get("Source").asString)
                else -> {
                    val constant = obj.get("Constant")?.asDouble ?: 0.0
                    require(constant.isFinite()) { "Non-finite constant" }
                    val read: (Context) -> Double = { constant }
                    read
                }
            }
            val perLevel = obj.get("PerLevel")?.asDouble ?: 0.0
            val scale = obj.get("Scale")?.asDouble ?: 1.0
            val offset = obj.get("Offset")?.asDouble ?: 0.0
            val min = obj.get("Min")?.asDouble ?: -Double.MAX_VALUE
            val max = obj.get("Max")?.asDouble ?: Double.MAX_VALUE
            require(listOf(perLevel, scale, offset, min, max).all { it.isFinite() } && min <= max) { "Invalid numeric range" }
            return { context ->
                val value = (base(context) + perLevel * (context.level - 1)) * scale + offset
                if (value.isFinite()) value.coerceIn(min, max) else 0.0
            }
        }

        private fun source(name: String): (Context) -> Double = when (name) {
            "Level" -> { context -> context.level.toDouble() }
            "Ammo" -> { context -> context.data.ammo.get().toDouble() }
            "Magazine" -> { context -> context.data.get(GunProp.MAGAZINE).toDouble() }
            "AmmoRatio" -> { context -> context.data.ammo.get().toDouble() / context.data.get(GunProp.MAGAZINE).coerceAtLeast(1) }
            "Zooming" -> { context -> if (context.data.zooming.get()) 1.0 else 0.0 }
            "Reloading" -> { context -> if (context.data.reloading()) 1.0 else 0.0 }
            "HealthRatio" -> { _ -> Minecraft.getInstance().player?.let { it.health.toDouble() / it.maxHealth.coerceAtLeast(1f) } ?: 1.0 }
            else -> throw IllegalArgumentException("Unknown source: $name")
        }

        private fun detail(obj: JsonObject): (Context) -> String {
            val display = obj.get("Display")?.asString ?: "name"
            require(display in setOf("name", "text", "timer", "stacks", "percent", "number", "parts")) { "Unknown display: $display" }
            val value = number(obj.get("Value"))
            val maximum = obj.get("Maximum")?.let { number(it) } ?: { _: Context -> 100.0 }
            val text = obj.get("Text")?.asString
            val separator = obj.get("Separator")?.asString ?: " / "
            val parts = obj.getAsJsonArray("Parts")?.map { element ->
                val part = element.asJsonObject
                fields(part, "When", "Display", "Value", "Maximum", "Text", "Parts", "Separator")
                condition(part.get("When")) to detail(part)
            } ?: emptyList()
            return { context ->
                val amount = value(context)
                val formatted = when (display) {
                    "timer" -> {
                        val seconds = kotlin.math.ceil(amount.coerceAtLeast(0.0) / 20.0).toLong()
                        "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
                    }
                    "stacks" -> "x${amount.toLong()}"
                    "percent" -> {
                        val max = maximum(context)
                        "${(if (max > 0) amount / max * 100 else 0.0).coerceIn(0.0, 100.0).toInt()}%"
                    }
                    "number" -> amount.toLong().toString()
                    "parts" -> parts.filter { it.first(context) }.map { it.second(context) }.filter { it.isNotEmpty() }.joinToString(separator)
                    else -> ""
                }
                if (text == null) formatted
                else {
                    val translated = net.minecraft.locale.Language.getInstance().getOrDefault(text)
                    when {
                        translated.contains("%s") -> I18n.get(text, formatted)
                        formatted.isEmpty() -> translated
                        else -> "$translated $formatted"
                    }
                }
            }
        }
    }

    private fun update(doc: Document, now: Long) {
        val mc = Minecraft.getInstance()
        val origin = doc.guiToDocumentPosition(Position(0.0, 0.0))
        val unit = doc.guiToDocumentPosition(Position(1.0, 1.0))
        val top =
            (mc.window.guiScaledHeight * 0.65).coerceAtMost((mc.window.guiScaledHeight - LIMIT * ROW_HEIGHT.toDouble()).coerceAtLeast(0.0))
        val anchor = doc.guiToDocumentPosition(Position(mc.window.guiScaledWidth * 0.025, top))
        attribute(
            "perks",
            "style",
            "left:${anchor.x}px;top:${anchor.y}px;transform:scale(${unit.x - origin.x},${unit.y - origin.y});"
        )
        val active = rows.values.filter { it.removed == 0L }.takeLast(LIMIT)
        val visible = active + rows.values.filter { it.removed != 0L }.take(LIMIT - active.size)
        for (index in 0 until LIMIT) {
            val row = visible.getOrNull(index)
            if (row == null) {
                attribute("row-$index", "style", "display:none;")
                continue
            }
            val entry = row.entry
            val fade =
                if (row.removed != 0L) 1 - (now - row.removed) / 200.0 else ((now - row.appeared) / 150.0).coerceAtMost(
                    1.0
                )
            val breath = if (entry.passive) 0.775 + 0.125 * sin(now % 2500 / 2500.0 * Math.PI * 2) else 1.0
            val blink = if (entry.countdown) 0.775 + 0.225 * sin(now % 1200 / 1200.0 * Math.PI * 2) else 1.0
            attribute(
                "row-$index",
                "style",
                "top:${index * ROW_HEIGHT}px;display:flex;opacity:${(fade * breath * blink).coerceIn(0.0, 1.0)};"
            )
            changed("icon:$index", "${entry.id}:${entry.type}:${entry.effect}") {
                val icon = entry.icon
                val available = mc.resourceManager.getResource(icon).isPresent
                doc.getElementById("icon-$index")?.let {
                    if (available) it.setAttribute("src", icon.toString())
                    it.setInlineStyleProperty("opacity", if (available) "1" else "0")
                }
            }
            val perkId = ResourceLocation(entry.id)
            val name = I18n.get("item.${perkId.namespace}.${perkId.path}")
            label("name-$index", if (entry.detail.isEmpty()) name else "$name  ${entry.detail}")
        }
    }

    private fun label(id: String, value: String) = changed("text:$id", value) {
        (document?.getElementById(id) as? AmmoHudText)?.setLabel(value)
    }

    private fun attribute(id: String, name: String, value: String) = changed("$name:$id", value) {
        document?.getElementById(id)?.setAttribute(name, value)
    }

    private inline fun changed(key: String, value: String, update: () -> Unit) {
        if (values[key] == value) return
        update()
        values[key] = value
    }

    private fun close() {
        document?.remove()
        document = null
        generation = -1L
        gunIdentity = null
        rows.clear()
        values.clear()
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || document == null) return
        if (!shouldRender() || Minecraft.getInstance().level == null || currentData() == null) close()
    }
}
