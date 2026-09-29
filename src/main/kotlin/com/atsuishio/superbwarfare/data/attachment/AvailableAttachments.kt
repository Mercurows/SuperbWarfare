package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.StringOrObject
import com.atsuishio.superbwarfare.data.attachment.AvailableAttachments.invalidate
import com.atsuishio.superbwarfare.data.attachment.AvailableAttachments.resolve
import com.atsuishio.superbwarfare.data.gun.AttachmentOption
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import kotlinx.serialization.json.JsonObject
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.TagsUpdatedEvent
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

/**
 * `AvailableAttachments` 里的一条**已解析**条目。
 *
 * @param id 配件物品 id。
 * @param override 该条目声明的武器级属性覆写（`AttachmentOption.Override`）；
 *   从标签展开出来的条目没有覆写，为 `null`。
 */
data class ResolvedAttachmentEntry(
    val id: ResourceLocation,
    val override: JsonObject? = null,
)

/**
 * 把枪械数据里的 `AvailableAttachments` 一条槽位声明解析成**具体的配件 id 列表**。
 *
 * 这一层是"数据怎么写"与"代码怎么用"的**唯一**交界：改装界面、`/sbw attachment` 的校验与补全、
 * `Attachment.cycle`（G 键轮换）、`GunItem.hasCustomAttachment` 全都读同一份结果，
 * 所以新增一种写法只需要改这里。
 *
 * ## 支持的写法
 *
 * | 写法 | 含义 |
 * |---|---|
 * | `"superbwarfare:ru_silencer"` | 单个配件 id |
 * | `"#superbwarfare:attachment/muzzle"` | **物品标签**：展开成标签里的全部物品 |
 * | `{ "Id": "...", "Override": { ... } }` | 单条覆写形式，`Id` 同样可以写 `#标签`（覆写应用到标签展开出的每一条） |
 * | `"!superbwarfare:silencer_50_cal"` | **排除**：从已解析结果里去掉这个配件 |
 * | `"!#superbwarfare:attachment/muzzle/virtual"` | **排除标签**：去掉标签里的全部物品（`#!tag` 也认） |
 *
 * ## 顺序与覆盖规则
 *
 * 按声明顺序**依次**处理，所以 `!` 只对写在它**前面**的条目生效（与 `AvailablePerks` 一致）：
 * 「整个槽位都用标签、只挑掉几个」写 `["#tag", "!id"]`，想先排除再加回来就把 `!` 写在前面。
 *
 * 同一条目被声明多次时**以先声明者为准**：先用 [LinkedHashMap.putIfAbsent] 占位，
 * 后面的标签展开不会顶掉它 —— 因此直接写出的条目会赢过标签里的同名条目，
 * 它自己写的 `Override` 也在（唯一例外是标签条目自带 `Override` 时会覆盖掉先前的空覆写）。
 *
 * ## 标签从哪来
 *
 * 走 `ForgeRegistries.ITEMS.tags()`，所以**客户端也能用**：物品标签随
 * `ClientboundUpdateTagsPacket` 同步到客户端，改装界面与渲染路径读到的结果与服务端一致。
 *
 * **不做"只保留该槽位配件"的过滤**：`#superbwarfare:attachment/charm` 里混进一个别的槽位配件时，
 * 它会在 [GunData.canInstall] 那一关被挡掉（那里要求 `AttachmentDefinition.slot == slot`），
 * 这里过滤只会让"标签里到底有什么"变得难以预测。
 *
 * ## 缓存
 *
 * 解析结果按 [GunData.DATA_VERSION] 整体失效（与 `AttachmentSlots.registeredIds` 同一套信号）：
 * 它会经 `GunItem.hasCustomAttachment` 被**渲染路径每帧查询**，而展开标签并不便宜。
 * 数据包重载会递增那个版本号。
 */
object AvailableAttachments {

    /** 解析结果缓存：`枪械数据 id + 槽位键 -> 已解析条目`，按 [GunData.DATA_VERSION] 整体失效。 */
    private val cache = ConcurrentHashMap<String, List<ResolvedAttachmentEntry>>()

    /**
     * 缓存对应的 [GunData.DATA_VERSION]；`Int.MIN_VALUE` = 还没算过。
     *
     * `@Volatile`：写在 [TagsUpdatedListener]（标签重载）里，读在渲染 / 指令线程上。
     */
    @Volatile
    private var cacheVersion = Int.MIN_VALUE

    /**
     * 丢弃全部解析结果，下次 [resolve] 重新展开标签。
     *
     * 除了 [GunData.DATA_VERSION] 变化，**标签自身重载**也要走这里（[TagsUpdatedEvent]）：
     * 客户端刚进世界时物品标签是随 `ClientboundUpdateTagsPacket` 才到的，
     * 而枪械数据可能在那之前就已经被读过一次（改装界面 / 渲染路径），
     * 那时 `#标签` 会展开成空表并被缓存下来。清一次缓存比猜时序便宜。
     */
    @JvmStatic
    fun invalidate() {
        cache.clear()
        cacheVersion = Int.MIN_VALUE
    }

    /**
     * 解析 [gun] 数据里 [slot] 槽位的 `AvailableAttachments` 声明。
     *
     * @return 按声明顺序解析出的配件列表；这把枪没给该槽位写声明时返回空列表。
     */
    @JvmStatic
    fun resolve(gun: GunData, slot: AttachmentType): List<ResolvedAttachmentEntry> =
        resolve(gun, slot.attachmentName)

    /**
     * 解析 `AvailableAttachments` 里 [slotKey] 那一条声明。
     *
     * @param slotKey `AvailableAttachments` 的键，即 [AttachmentType.attachmentName]（例如 `Muzzle`）。
     */
    @JvmStatic
    fun resolve(gun: GunData, slotKey: String): List<ResolvedAttachmentEntry> {
        if (cacheVersion != GunData.DATA_VERSION) {
            cache.clear()
            cacheVersion = GunData.DATA_VERSION
        }

        // 缓存键必须带上"哪一份枪械数据"：同一个槽位在不同枪上声明的配件完全不同，
        // 只按槽位键缓存会把上一把枪的结果发给下一把（而且不会有任何报错）。
        val data = gun.getDefault()
        return cache.getOrPut("${data.itemId}#$slotKey") {
            build(data.availableAttachments[slotKey].orEmpty())
        }
    }

    /** 按声明顺序解析一份条目列表。 */
    private fun build(declarations: List<StringOrObject<AttachmentOption>>): List<ResolvedAttachmentEntry> {
        val accepted = LinkedHashMap<ResourceLocation, JsonObject?>()
        val excluded = mutableSetOf<ResourceLocation>()

        for (declaration in declarations) {
            val option = declaration.value
            val raw = option.id.trim()
            if (raw.isEmpty()) continue

            // `!` = 排除。`!#tag` 与 `#!tag` 两种写法都认，这里只剥前面的 `!`
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

            if (tagged) {
                val members = tagMembers(id)
                if (members.isEmpty()) {
                    Mod.LOGGER.warn("AvailableAttachments: item tag {} is empty or missing", id)
                    continue
                }

                for (member in members) {
                    if (negated) {
                        accepted.remove(member)
                        excluded += member
                    } else if (member !in excluded) {
                        // 先声明者胜；标签条目自带 Override 时才有资格覆盖先前的空覆写
                        if (!accepted.containsKey(member) || option.override != null) {
                            accepted[member] = option.override
                        }
                    }
                }
            } else {
                if (negated) {
                    accepted.remove(id)
                    excluded += id
                } else if (id !in excluded) {
                    if (!accepted.containsKey(id) || option.override != null) {
                        accepted[id] = option.override
                    }
                }
            }
        }

        return accepted.map { (id, override) -> ResolvedAttachmentEntry(id, override) }
    }

    /**
     * 标签里的全部物品 id，按标签自身顺序。
     *
     * `ForgeRegistries.ITEMS.tags()` 在注册表还没就绪时可能是 `null`（启动早期、数据包重载中），
     * 此时返回空集合，让调用方按"这个标签暂时没人"处理而不是抛异常 ——
     * 这个方法会被渲染路径每帧读取，不能被一个还没同步完的标签炸掉。
     */
    private fun tagMembers(id: ResourceLocation): List<ResourceLocation> {
        return BuiltInRegistries.ITEM.getTag(ItemTags.create(id))
            .map { items -> items.map { BuiltInRegistries.ITEM.getKey(it.value()) } }
            .getOrNull() ?: mutableListOf()
    }

    /**
     * 标签重载后丢掉缓存（见 [invalidate]）。
     *
     * 写成嵌套的 `@EventBusSubscriber` 而不是放进 `event/` 包：这条规则只服务于本文件的缓存，
     * 放一起才不会出现"改了缓存策略却忘了改另一个文件里的失效钩子"。
     */
    @EventBusSubscriber(modid = Mod.MODID)
    object TagsUpdatedListener {
        @SubscribeEvent
        fun onTagsUpdated(event: TagsUpdatedEvent) {
            invalidate()
        }
    }
}
