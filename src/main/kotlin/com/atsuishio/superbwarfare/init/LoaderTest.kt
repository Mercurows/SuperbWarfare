package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.TestLoader.parseManifest
import com.atsuishio.superbwarfare.item.attachment.BasicAttachmentItem
import com.atsuishio.superbwarfare.item.gun.GeoGunItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.ModList
import net.minecraftforge.forgespi.locating.IModFile
import net.minecraftforge.registries.RegisterEvent
import org.objectweb.asm.Type
import java.lang.annotation.ElementType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.full.createInstance

@Serializable
data class LoaderInfo(
    @SerialName("FormatVersion")
    val formatVersion: Int,
    @SerialName("Items")
    val items: Map<String, ItemRegisterInfo> = emptyMap(),
    @SerialName("Guns")
    val guns: Map<String, ItemRegisterInfo> = emptyMap(),
    @SerialName("Attachments")
    val attachments: Map<String, ItemRegisterInfo> = emptyMap(),
    @SerialName("Vehicles")
    val vehicles: Map<String, VehicleRegisterInfo> = emptyMap(),
)

@Serializable
data class VehicleRegisterInfo(
    @SerialName("Width")
    val width: Float,
    @SerialName("Height")
    val height: Float,
    @SerialName("CreativeContainer")
    val creativeContainer: Boolean = false,
)

@Serializable
data class ItemRegisterInfo(
    @SerialName("Rarity")
    val rarity: String = "common",
    @SerialName("MaxStackSize")
    val maxStackSize: Int = 64,
    @SerialName("FireResistant")
    val fireResistant: Boolean = false,
    @SerialName("Durability")
    val durability: Int? = null,
    @SerialName("ItemTabs")
    val itemTabs: List<String> = emptyList(),
)

/**
 * 「由其他 mod 的 jar 提供注册描述、由 Superb Warfare 代为注册」机制的试验实现。
 *
 * 当前行为：
 *  1. 遍历所有 mod 文件，读取 `META-INF/sbw/registry.json`；
 *  2. 用 `IModFile#getScanResult()` 找出被 [TestLoaderTarget] 标注的类并加载它们
 *     —— 只读 scan data（FML 找 `@Mod` 用的同一套数据），不预先加载整个 jar；
 *  3. 在 `RegisterEvent` 里以「描述文件所属 mod id」为命名空间注册物品和载具。
 *
 * 协议与设计约定见 `localmod/README.md`。跑通后再决定是否迁到独立的 plugin 包。
 *
 * 注意：所有运行期字符串（日志、异常）统一用英文 —— 中文在 Windows 的 GBK 控制台
 * 会被转义成乱码，而日志是给第三方看的。
 */
object TestLoader {

    /** 描述文件在 mod jar 内的位置。放 META-INF 是为了不被资源包/数据包看见或覆盖。 */
    private const val MANIFEST_PATH = "META-INF/sbw/registry.json"

    private const val FORMAT_VERSION = 1

    /**
     * ignoreUnknownKeys：协议要能向前演进，插件写了本版本不认识的字段不应该直接崩。
     * 缺字段 / 类型不对仍然会抛 SerializationException（含 MissingFieldException）。
     */
    private val JSON = Json {
        ignoreUnknownKeys = true
    }

    private val RARITIES by lazy {
        mapOf(
            "common" to Rarity.COMMON,
            "uncommon" to Rarity.UNCOMMON,
            "rare" to Rarity.RARE,
            "epic" to Rarity.EPIC,
            "legendary" to ModRarities.LEGENDARY,
            "superb" to ModRarities.SUPERB,
            "virtual" to ModRarities.VIRTUAL,
            "beast" to ModRarities.BEAST,
        )
    }

    /** 入口类发现用的注解类型。ASM 描述符，和 scan data 里存的形态一致。 */
    private val TARGET_ANNOTATION: Type = Type.getType(TestLoaderTarget::class.java)

    /** 命名空间 -> 该 mod 的注册声明。命名空间由 [IModFile] 推导，不从 JSON 读。 */
    private val manifests = linkedMapOf<String, LoaderInfo>()

    /** Only successfully registered declarations contribute entries to creative tabs. */
    private val tabItems = linkedMapOf<ResourceLocation, MutableList<Item>>()

    private val vehicles = mutableListOf<EntityType<VehicleEntity>>()
    internal val registeredVehicles: List<EntityType<VehicleEntity>>
        get() = vehicles

    private val creativeContainers = mutableListOf<EntityType<VehicleEntity>>()

    /** 被 [TestLoaderTarget] 标注、且成功实例化的入口类，便于后续扩展时取用。 */
    @JvmField
    val loadedEntrypoints: MutableMap<String, MutableList<Any>> = linkedMapOf()

    fun register(bus: IEventBus) {
        discover()
        bus.addListener<RegisterEvent> { onRegister(it) }
        bus.addListener<BuildCreativeModeTabContentsEvent> { event ->
            tabItems[event.tabKey.location()]?.forEach { event.accept(it) }
        }
        bus.addListener<RegisterContainersEvent> { event ->
            creativeContainers.forEach { event.add(it) }
        }
    }

    private fun onRegister(event: RegisterEvent) {
        when (event.registryKey) {
            Registries.ITEM -> for ((namespace, info) in manifests) {
                registerItems(namespace, info.items, event)
                registerItems(namespace, info.guns, event) { _, properties -> GeoGunItem(properties) }
                registerItems(namespace, info.attachments, event) { id, properties ->
                    BasicAttachmentItem(id.toString(), properties)
                }
            }

            Registries.ENTITY_TYPE -> for ((namespace, info) in manifests) {
                registerVehicles(namespace, info.vehicles, event)
            }
        }
    }

    private fun registerVehicles(namespace: String, entries: Map<String, VehicleRegisterInfo>, event: RegisterEvent) {
        for ((id, info) in entries) {
            val location = ResourceLocation(namespace, id)
            if (event.getForgeRegistry<EntityType<*>>()?.containsKey(location) == true) {
                Mod.LOGGER.error("[sbw-loader] entity {} already exists, skipping {}", location, MANIFEST_PATH)
                continue
            }

            val type = ModEntities.vehicle().sized(info.width, info.height).build(location.toString())
            event.register(Registries.ENTITY_TYPE, location) { type }
            vehicles += type
            if (info.creativeContainer) creativeContainers += type
            Mod.LOGGER.info("[sbw-loader] registered vehicle {} (declared by mod {})", location, namespace)
        }
    }

    private fun registerItems(
        namespace: String,
        itemEntry: Map<String, ItemRegisterInfo>,
        event: RegisterEvent,
        itemSupplier: (ResourceLocation, Item.Properties) -> Item = { _, properties -> Item(properties) }
    ) {
        for ((id, item) in itemEntry) {
            val location = ResourceLocation(namespace, id)

            // 所属 mod 自己注册了同名物品时让给它：我们只是"代注册"，不抢所有权。
            if (event.getForgeRegistry<Item>()?.containsKey(location) == true) {
                Mod.LOGGER.error(
                    "[sbw-loader] {} already exists, skipping the declaration from mod {} ({})",
                    location, namespace, MANIFEST_PATH
                )
                continue
            }

            event.register(Registries.ITEM, location) {
                item.create { itemSupplier(location, it) }.also { registered ->
                    item.itemTabs.distinct().forEach { tab ->
                        tabItems.getOrPut(ResourceLocation(tab)) { mutableListOf() }.add(registered)
                    }
                }
            }
            Mod.LOGGER.info("[sbw-loader] registered {} (declared by mod {})", location, namespace)
        }
    }

    private fun discover() {
        for (fileInfo in ModList.get().modFiles) {
            val modIds = fileInfo.mods.map { it.modId }
            // 一个 jar 声明多个 mod 时取第一个；协议要求插件 jar 只声明一个 mod。
            val namespace = modIds.firstOrNull() ?: continue
            // 自己不用这套机制，自己的物品走 ModItems。
            if (modIds.contains(Mod.MODID)) continue

            val file = fileInfo.file
            val entrypoints = scanEntrypoints(file, namespace)

            val manifestPath = runCatching { file.findResource(MANIFEST_PATH) }.getOrNull()
            if (manifestPath == null || !Files.isRegularFile(manifestPath)) {
                if (entrypoints.isNotEmpty()) {
                    Mod.LOGGER.warn(
                        "[sbw-loader] mod {} provides entrypoint classes but no {}",
                        namespace, MANIFEST_PATH
                    )
                }
                continue
            }

            val info = parseManifest(manifestPath, namespace)
            manifests[namespace] = info
            Mod.LOGGER.info(
                "[sbw-loader] discovered plugin {}: {} item(s), {} gun(s), {} attachment(s), {} vehicle(s), {} entrypoint class(es)",
                namespace, info.items.size, info.guns.size, info.attachments.size, info.vehicles.size, entrypoints.size
            )
        }
    }

    private fun scanEntrypoints(file: IModFile, namespace: String): List<String> {
        val names = try {
            // 1.20.1 的 ModFileScanData 没有 getAnnotatedBy，只能自己按
            // annotationType + targetType 过滤；clazz() 是 ASM 的 Type。
            file.scanResult.annotations
                .filter { it.annotationType() == TARGET_ANNOTATION && it.targetType() == ElementType.TYPE }
                .map { it.clazz().className }
                // 排序保证加载顺序稳定：scan data 的顺序不保证跨版本一致。
                .sorted()
        } catch (t: Throwable) {
            Mod.LOGGER.error("[sbw-loader] failed to scan annotations of mod {}", namespace, t)
            return emptyList()
        }

        for (name in names) {
            loadEntrypoint(name, namespace)?.let { instance ->
                loadedEntrypoints.getOrPut(namespace) { mutableListOf() }.add(instance)
            }
        }
        return names
    }

    private fun loadEntrypoint(className: String, namespace: String): Any? {
        return try {
            val clazz = Class.forName(className, true, TestLoader::class.java.classLoader).kotlin
            val instance = clazz.objectInstance ?: clazz.createInstance()
            Mod.LOGGER.info("[sbw-loader] loaded entrypoint {} (mod={})", className, namespace)
            instance
        } catch (t: Throwable) {
            // 一个坏插件不应该炸掉整个启动。
            Mod.LOGGER.error("[sbw-loader] failed to load entrypoint {} (mod={}), skipped", className, namespace, t)
            null
        }
    }

    /**
     * 解析 + 语义校验。校验放在这里是为了「插件写错了就在启动时报错」，
     * 而不是等到注册物品时才发现少了个字段。
     */
    private fun parseManifest(path: Path, namespace: String): LoaderInfo {
        fun fail(message: String): Nothing =
            throw IllegalStateException("[sbw-loader] invalid $MANIFEST_PATH of mod $namespace: $message")

        val text = try {
            Files.readString(path)
        } catch (t: Throwable) {
            fail("cannot be read: $t")
        }

        // 缺字段 / 类型不对 / 语法错误都会以 SerializationException（含 MissingFieldException）抛出。
        val info = try {
            JSON.decodeFromString(LoaderInfo.serializer(), text)
        } catch (t: Exception) {
            fail("cannot be decoded: ${t.message}")
        }

        if (info.formatVersion != FORMAT_VERSION) {
            fail("unsupported FormatVersion=${info.formatVersion} (this build supports $FORMAT_VERSION)")
        }

        val declaredIds = mutableSetOf<String>()
        for ((section, entries) in listOf("Items" to info.items, "Guns" to info.guns, "Attachments" to info.attachments)) {
            for ((id, item) in entries) {
                val entry = "$section['$id']"
                if (!declaredIds.add(id)) {
                    fail("$entry duplicates an item path declared in another section")
                }
                if (':' in id) {
                    fail("$entry must not be namespaced: the namespace is derived from the owning mod")
                }
                if (id.isEmpty() || !ResourceLocation.isValidPath(id)) {
                    fail("$entry is not a valid item path (lowercase a-z, 0-9, '_', '-', '.')")
                }
                if (RARITIES[item.rarity.lowercase()] == null) {
                    fail("$entry.Rarity '${item.rarity}' is unknown (expected one of ${RARITIES.keys.joinToString()})")
                }
                if (item.maxStackSize !in 1..99) {
                    fail("$entry.MaxStackSize=${item.maxStackSize} is out of range 1..99")
                }
                if (item.durability != null && item.durability < 1) {
                    fail("$entry.Durability must be >= 1")
                }
                for (tab in item.itemTabs) {
                    if (':' !in tab || tab.substringBefore(':').isEmpty() || tab.substringAfter(':').isEmpty()
                        || ResourceLocation.tryParse(tab) == null
                    ) {
                        fail("$entry.ItemTabs contains an invalid tab id '$tab' (expected namespace:path)")
                    }
                }
            }
        }

        for ((id, vehicle) in info.vehicles) {
            val entry = "Vehicles['$id']"
            if (id.isEmpty() || !ResourceLocation.isValidPath(id)) {
                fail("$entry must be a valid path without a namespace")
            }
            if (!vehicle.width.isFinite() || vehicle.width <= 0f) {
                fail("$entry.Width must be finite and greater than 0")
            }
            if (!vehicle.height.isFinite() || vehicle.height <= 0f) {
                fail("$entry.Height must be finite and greater than 0")
            }
        }

        return info
    }

    /** Rarity 已在 [parseManifest] 校验过，这里的 getValue 不会抛。 */
    private fun ItemRegisterInfo.create(itemSupplier: (Item.Properties) -> Item = { Item(it) }): Item {
        val properties = Item.Properties()
            .stacksTo(maxStackSize)
            .rarity(RARITIES.getValue(rarity.lowercase()))
        if (fireResistant) properties.fireResistant()
        durability?.let { properties.durability(it) }
        return itemSupplier(properties)
    }
}
