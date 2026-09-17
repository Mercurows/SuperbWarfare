package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import net.minecraft.resources.FileToIdConverter
import net.minecraft.server.packs.resources.ResourceManager
import net.neoforged.fml.loading.FMLEnvironment

/**
 * 数据包严格校验器。
 *
 * 背景：`DataLoader.JSON` 开着 `ignoreUnknownKeys = true`，所以数据文件里键名写错（或者迁移
 * `@SerializedName` → `@SerialName` 时漏改）**不会报错**，只会让该字段静默地留在默认值上。
 * 本校验器用一份除 `ignoreUnknownKeys` 外与 [DataLoader.JSON] 完全一致的 Json 配置重新解析
 * 每个 kotlinx 数据文件，把这类问题变成显式的错误日志。
 *
 * 覆盖范围：
 * - 覆盖 `LOADED_DATA` / `LOADED_RESOURCE` 里的全部数据集（所有数据集都已迁到 kotlinx）。
 * - 因为所有属性都有默认值，**缺键不是错误**（这是数据包部分覆写的前提）；
 *   真正会被抓到的是「JSON 里有、数据类里没有」的键和类型不匹配。
 *
 * 触发方式：开发环境（`FMLEnvironment.production == false`）在每次数据包 reload 后自动运行；
 * 生产环境默认关闭，可用 `-Dsuperbwarfare.validateData=true` 或环境变量 `SBW_VALIDATE_DATA=true` 打开。
 */
object DataValidator {

    @JvmField
    val ENABLED: Boolean = !FMLEnvironment.production
            || System.getProperty("superbwarfare.validateData") == "true"
            || System.getenv("SBW_VALIDATE_DATA")?.lowercase() == "true"

    /** 严格模式：除未知键外，其余设置与 [DataLoader.JSON] 保持一致，避免两边漂移 */
    private val STRICT: Json = Json(DataLoader.JSON) {
        ignoreUnknownKeys = false
    }

    /**
     * 允许出现在数据文件里、但数据类不声明的顶层键。
     *
     * `ID` 是历史遗留：真实 id 由文件路径决定（见 [ComplexJsonResourceReloadListener]），
     * 写进 JSON 里没有任何作用，这里放行只是为了不打扰已有数据包。
     */
    private val TOLERATED_TOP_LEVEL_KEYS = setOf("ID")

    data class Issue(val directory: String, val file: String, val message: String)

    data class Report(val checked: Int, val issues: List<Issue>) {
        val ok: Boolean get() = issues.isEmpty()
    }

    /**
     * 校验 [data] 中所有数据集（现在全部是 kotlinx）的全部 JSON 文件。
     *
     * @param resourceManager 当前数据包资源管理器
     * @param data 要校验的数据集，默认 [DataLoader.LOADED_DATA]
     */
    @JvmOverloads
    fun validate(
        resourceManager: ResourceManager,
        data: Map<String, DataLoader.GeneralData<*>> = DataLoader.LOADED_DATA
    ): Report {
        var checked = 0
        val issues = mutableListOf<Issue>()

        for ((directory, general) in data) {
            val serializer = try {
                STRICT.serializersModule.serializer(general.type)
            } catch (exception: Exception) {
                issues += Issue(directory, "<serializer>", firstLine(exception))
                continue
            }

            val converter = FileToIdConverter.json(directory)
            for (entry in converter.listMatchingResources(resourceManager).entries) {
                val id = converter.fileToId(entry.key).toString()
                checked++
                try {
                    val text = entry.value.openAsReader().use { it.readText() }
                    val decoded = STRICT.decodeFromString(serializer, text.withoutToleratedKeys())
                    // 再往返一次：IDBasedData.copy() 与 JsonOverrideApplier.computeProperties 都依赖
                    // "编码回 JsonElement 再解析" 这条路，这里顺带保证所有数据类都能往返
                    val roundTripped =
                        STRICT.decodeFromJsonElement(serializer, STRICT.encodeToJsonElement(serializer, decoded))
                    validateDerivedState(roundTripped)
                } catch (exception: Exception) {
                    issues += Issue(directory, id, firstLine(exception))
                }
            }
        }

        return Report(checked, issues)
    }

    /**
     * 触发那些"解码时不会走到"的派生状态。
     *
     * 枪械数据里的弹药来源（[com.atsuishio.superbwarfare.data.gun.AmmoConsumer.sources]）和开火模式
     * 都是 lazy / 缓存派生的，只解码不访问就验证不到；这里主动读一遍，把解析异常暴露出来。
     *
     * 这里刻意**不**判断 [com.atsuishio.superbwarfare.data.gun.AmmoConsumer.isValid]：
     * 声明了 `AmmoSlot`/`Override` 但没写 `Ammo` 的条目是合法的（例如 Secondary Cataclysm 的
     * 近战模式，`ProjectileAmount: 0` 且 `AmmoSlot: "Melee"`，本来就不需要弹药来源）。
     */
    private fun validateDerivedState(decoded: Any) {
        if (decoded !is DefaultGunData) return

        for (entry in decoded.ammoConsumers) {
            // 强制求值 lazy 派生出来的 sources
            entry.value.primary.type
        }

        decoded.fireModes.size
        decoded.availablePerks()
    }

    /** 校验并输出报告；未启用时直接返回 */
    fun validateAndLog(resourceManager: ResourceManager, data: Map<String, DataLoader.GeneralData<*>>) {
        if (!ENABLED) return

        val report = validate(resourceManager, data)
        if (report.ok) {
            Mod.LOGGER.info("[DataValidator] all {} data files are valid", report.checked)
            return
        }

        // 故意用英文输出：日志文件/控制台经常是 GBK，中文会变乱码
        Mod.LOGGER.error(
            "[DataValidator] {} of {} files failed validation:",
            report.issues.size,
            report.checked
        )
        for (issue in report.issues) {
            Mod.LOGGER.error("[DataValidator]   {}/{} -> {}", issue.directory, issue.file, issue.message)
        }
    }

    /**
     * 去掉数据类不声明的顶层遗留键，让严格解析不因它们误报。
     * 不是合法 JSON 对象时原样返回，交给严格解析去报错。
     */
    private fun String.withoutToleratedKeys(): String {
        val element = try {
            STRICT.parseToJsonElement(this)
        } catch (_: Exception) {
            return this
        }
        val obj = element as? JsonObject ?: return this
        if (obj.keys.none { it in TOLERATED_TOP_LEVEL_KEYS }) return this
        return JsonObject(obj.filterKeys { it !in TOLERATED_TOP_LEVEL_KEYS }).toString()
    }

    private fun firstLine(exception: Exception): String =
        (exception.message ?: exception.toString()).lineSequence().first()
}
