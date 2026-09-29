package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.StringOrObject
import com.atsuishio.superbwarfare.data.gun.AttachmentOption
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import kotlinx.serialization.json.JsonObject
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraftforge.event.TagsUpdatedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.registries.ForgeRegistries
import java.util.concurrent.ConcurrentHashMap

/** `AvailableAttachments` 里的一条已解析条目；标签展开出来的条目没有 [override]。 */
data class ResolvedAttachmentEntry(
    val id: ResourceLocation,
    val override: JsonObject? = null,
)

/**
 * 把 `AvailableAttachments` 的一条槽位声明解析成具体的配件 id 列表。
 *
 * | 写法 | 含义 |
 * |---|---|
 * | `"superbwarfare:ru_silencer"` | 单个配件 id |
 * | `"#superbwarfare:attachment/muzzle"` | 物品标签，展开成标签里的全部物品 |
 * | `{ "Id": "...", "Override": { ... } }` | 覆写形式，`Id` 同样可以写 `#标签` |
 * | `"!id"` / `"!#tag"` | 排除（`#!tag` 也认） |
 *
 * 按声明顺序处理，`!` 只影响写在它前面的条目；同一条目以先声明者为准。
 * 标签走 `ForgeRegistries.ITEMS.tags()`，客户端也拿得到同步过来的标签。
 */
object AvailableAttachments {

    private val cache = ConcurrentHashMap<String, List<ResolvedAttachmentEntry>>()

    @Volatile
    private var cacheVersion = Int.MIN_VALUE

    /** 丢掉全部解析结果；标签重载后也要走这里，否则 `#标签` 可能被缓存成空表。 */
    @JvmStatic
    fun invalidate() {
        cache.clear()
        cacheVersion = Int.MIN_VALUE
    }

    /** 解析 [gun] 的 [slot] 槽位声明；没写声明时返回空列表。 */
    @JvmStatic
    fun resolve(gun: GunData, slot: AttachmentType): List<ResolvedAttachmentEntry> =
        resolve(gun, slot.attachmentName)

    /** 解析 `AvailableAttachments` 里 [slotKey] 那一条声明；[slotKey] 即 [AttachmentType.attachmentName]。 */
    @JvmStatic
    fun resolve(gun: GunData, slotKey: String): List<ResolvedAttachmentEntry> {
        if (cacheVersion != GunData.DATA_VERSION) {
            cache.clear()
            cacheVersion = GunData.DATA_VERSION
        }

        // 键要带上枪械数据 id：同一个槽位在不同枪上声明的配件不同
        val data = gun.getDefault()
        return cache.getOrPut("${data.itemId}#$slotKey") {
            build(data.availableAttachments[slotKey].orEmpty())
        }
    }

    private fun build(declarations: List<StringOrObject<AttachmentOption>>): List<ResolvedAttachmentEntry> {
        val accepted = LinkedHashMap<ResourceLocation, JsonObject?>()
        val excluded = mutableSetOf<ResourceLocation>()

        for (declaration in declarations) {
            val option = declaration.value
            val raw = option.id.trim()
            if (raw.isEmpty()) continue

            val negated = raw.startsWith("!")
            val body = (if (negated) raw.substring(1) else raw).trim()

            val tagged = body.startsWith("#")
            val target = (if (tagged) body.substring(1) else body).trim()
            if (target.isEmpty()) continue

            val id = ResourceLocation.tryParse(target)
            if (id == null) {
                Mod.LOGGER.warn(
                    "AvailableAttachments: '{}' is not a valid {}",
                    raw, if (tagged) "item tag id" else "item id"
                )
                continue
            }

            val members = if (tagged) {
                val found = tagMembers(id)
                if (found.isEmpty()) {
                    Mod.LOGGER.warn("AvailableAttachments: item tag {} is empty or missing", id)
                    continue
                }
                found
            } else {
                listOf(id)
            }

            for (member in members) {
                if (negated) {
                    accepted.remove(member)
                    excluded += member
                } else if (member !in excluded) {
                    // 先声明者胜；标签条目自带 Override 时才覆盖先前的空覆写
                    if (!accepted.containsKey(member) || option.override != null) {
                        accepted[member] = option.override
                    }
                }
            }
        }

        return accepted.map { (id, override) -> ResolvedAttachmentEntry(id, override) }
    }

    /** 标签里的全部物品 id；注册表还没就绪时返回空集合，不抛异常。 */
    private fun tagMembers(id: ResourceLocation): List<ResourceLocation> {
        val tags = runCatching { ForgeRegistries.ITEMS.tags() }.getOrNull() ?: return emptyList()
        val tag = runCatching { tags.getTag(ItemTags.create(id)) }.getOrNull() ?: return emptyList()

        return tag.mapNotNull { ForgeRegistries.ITEMS.getKey(it) }
    }

    /** 标签重载后丢缓存：客户端刚进世界时标签是随数据包同步才到的。 */
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
    object TagsUpdatedListener {
        @SubscribeEvent
        fun onTagsUpdated(event: TagsUpdatedEvent) {
            invalidate()
        }
    }
}
