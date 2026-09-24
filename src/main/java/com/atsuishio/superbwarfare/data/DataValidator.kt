package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.melee.MeleeHitboxType
import com.atsuishio.superbwarfare.data.gun.melee.isMeleeProjectileMarker
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

    /** 弹种 / 开火模式的 `Override` 里用来改弹丸数量的键名 */
    private const val PROJECTILE_AMOUNT_KEY = "ProjectileAmount"

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
        val warnings = mutableListOf<Issue>()

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
                    validateDerivedState(roundTripped) { warning ->
                        warnings += Issue(directory, id, warning)
                    }
                } catch (exception: Exception) {
                    issues += Issue(directory, id, firstLine(exception))
                }
            }
        }

        if (warnings.isNotEmpty()) {
            Mod.LOGGER.warn("[DataValidator] {} non-fatal data warning(s):", warnings.size)
            for (warning in warnings) {
                Mod.LOGGER.warn("[DataValidator]   {}/{} -> {}", warning.directory, warning.file, warning.message)
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
     *
     * @param warn 非致命问题的回调（例如"能跑但建议迁移"的写法）。不传就当没有。
     */
    @JvmOverloads
    private fun validateDerivedState(decoded: Any, warn: (String) -> Unit = {}) {
        if (decoded !is DefaultGunData) return

        for (entry in decoded.ammoConsumers) {
            // 强制求值 lazy 派生出来的 sources
            entry.value.primary.type
        }

        decoded.fireModes.size
        decoded.availablePerks()

        validateMeleeData(decoded, warn)
    }

    /**
     * 近战相关数据的校验规则（§11.1-14）。
     *
     * 分两类：
     * - **致命**（抛异常，由调用方记成 `Issue`）：参数自相矛盾到无法判定，比如
     *   `Cone` 的总张角不是正数、`Box` 三轴有非正数、`ZFrom + Length <= 0`（判定体整个在身后）、
     *   负的 `HitTime`、`HitTime` 超过本段时长（永远不会结算）。
     * - **警告**（走 `warn` 回调）：能跑但建议改，比如靠 `ProjectileAmount <= 0` 隐式表达近战
     *   （应迁移到 `"Projectile": "@melee"`）、旧的无 `@` 前缀标记写法。
     *
     * 注意：动作表里的字段是**可选**的（不写就走全局继承），所以这里只校验"写了的那部分"。
     */
    private fun validateMeleeData(data: DefaultGunData, warn: (String) -> Unit) {
        val hitbox = data.meleeHitbox
        if (hitbox != null) {
            require(hitbox.range >= 0) { "MeleeHitbox.Range must be >= 0, got ${hitbox.range}" }
            require(hitbox.angle >= 0) { "MeleeHitbox.Angle must be >= 0, got ${hitbox.angle}" }
            require(hitbox.pitch >= 0) { "MeleeHitbox.Pitch must be >= 0, got ${hitbox.pitch}" }

            when (hitbox.type) {
                MeleeHitboxType.CONE -> {
                    require(hitbox.angle > 0) { "MeleeHitbox.Angle must be > 0 for a Cone, got ${hitbox.angle}" }
                    require(hitbox.pitch > 0) { "MeleeHitbox.Pitch must be > 0 for a Cone, got ${hitbox.pitch}" }
                }

                MeleeHitboxType.BOX -> {
                    require(hitbox.width > 0) { "MeleeHitbox.Width must be > 0 for a Box, got ${hitbox.width}" }
                    require(hitbox.height > 0) { "MeleeHitbox.Height must be > 0 for a Box, got ${hitbox.height}" }
                    require(hitbox.length > 0) { "MeleeHitbox.Length must be > 0 for a Box, got ${hitbox.length}" }
                    require(hitbox.zFrom + hitbox.length > 0) {
                        "MeleeHitbox Box is entirely behind the player: ZFrom=${hitbox.zFrom} + Length=${hitbox.length} <= 0"
                    }
                }

                MeleeHitboxType.CAPSULE -> {
                    require(hitbox.radius > 0) { "MeleeHitbox.Radius must be > 0 for a Capsule, got ${hitbox.radius}" }
                    require(hitbox.zFrom + hitbox.range > 0) {
                        "MeleeHitbox Capsule is entirely behind the player: ZFrom=${hitbox.zFrom} + Range=${hitbox.range} <= 0"
                    }
                }
            }
        }

        val sweep = data.meleeSweep
        if (sweep != null) {
            require(sweep.steps >= 0) { "MeleeSweep.Steps must be >= 0 (0 = auto), got ${sweep.steps}" }
            require(sweep.span <= 360.0) { "MeleeSweep spans ${sweep.span} degrees, which exceeds a full turn" }
        }

        require(data.meleeComboReset >= 0) { "MeleeComboReset must be >= 0, got ${data.meleeComboReset}" }

        for ((index, action) in data.meleeActions.list.withIndex()) {
            require(action.duration == null || action.duration > 0) {
                "MeleeActions[$index].Duration must be > 0 when written, got ${action.duration}"
            }
            require(action.hitTime == null || action.hitTime >= 0) {
                "MeleeActions[$index].HitTime must be >= 0, got ${action.hitTime}"
            }
            require(action.maxTargets == null || action.maxTargets >= 0) {
                "MeleeActions[$index].MaxTargets must be >= 0 (0 = unlimited), got ${action.maxTargets}"
            }
            require(action.falloff == null || action.falloff in 0.0..1.0) {
                "MeleeActions[$index].Falloff must be within [0, 1], got ${action.falloff}"
            }
            require(action.cooldown == null || action.cooldown >= 0) {
                "MeleeActions[$index].Cooldown must be >= 0, got ${action.cooldown}"
            }
            require(action.durability == null || action.durability >= 0) {
                "MeleeActions[$index].Durability must be >= 0, got ${action.durability}"
            }
            require(action.damage == null || action.damage >= 0) {
                "MeleeActions[$index].Damage must be >= 0, got ${action.damage}"
            }
            require(action.damageMultiplier == null || action.damageMultiplier >= 0) {
                "MeleeActions[$index].DamageMultiplier must be >= 0, got ${action.damageMultiplier}"
            }
            require(action.bypassesArmor == null || action.bypassesArmor in 0.0..1.0) {
                "MeleeActions[$index].BypassesArmor must be within [0, 1], got ${action.bypassesArmor}"
            }
            require(action.headshot == null || action.headshot >= 0) {
                "MeleeActions[$index].Headshot must be >= 0, got ${action.headshot}"
            }
            require(action.legshot == null || action.legshot >= 0) {
                "MeleeActions[$index].Legshot must be >= 0, got ${action.legshot}"
            }

            // HitTime 超过本段时长时结算永远不会发生，这是最容易写错的一处
            val effectiveDuration = action.duration ?: data.meleeDuration
            val effectiveHitTime = action.hitTime ?: data.meleeDamageTime
            require(effectiveHitTime <= effectiveDuration) {
                "MeleeActions[$index] resolves at HitTime=$effectiveHitTime but the swing only lasts " +
                        "Duration=$effectiveDuration ticks; it would never hit"
            }

            for (effect in action.effects.orEmpty()) {
                require(effect.effect != null || effect.type != null) {
                    "MeleeActions[$index] has an Effects entry with neither 'Effect' nor 'Type'"
                }
                require(effect.chance in 0.0..1.0) {
                    "MeleeActions[$index] has an Effects entry with Chance=${effect.chance}, must be within [0, 1]"
                }
                require(effect.cooldown >= 0) {
                    "MeleeActions[$index] has an Effects entry with Cooldown=${effect.cooldown}, must be >= 0"
                }
            }
        }

        val projectileMarker = data.projectile.value.itemId

        // 迁移提示：靠 `ProjectileAmount <= 0` 隐式表达"近战专属"，建议改成显式的 `@melee`。
        //
        // 只对「**本身就是近战专属**的枪」提示：`ProjectileAmount <= 0` 在别处是合法写法
        // （例如霰弹枪的一个弹种把弹丸数覆盖成 0，那把枪平时照样开枪），
        // 所以弹种/开火模式的 `Override` 里一旦能改回 `ProjectileAmount`，就不该催迁移。
        if (!projectileMarker.isMeleeProjectileMarker()
            && data.projectileAmount <= 0
            && data.meleeDamage > 0
            && !anyModifierTouchesProjectileAmount(data)
        ) {
            warn(
                "melee is expressed implicitly (ProjectileAmount=${data.projectileAmount} && " +
                        "MeleeDamage=${data.meleeDamage}); migrate to \"Projectile\": \"@melee\""
            )
        }

        // 迁移提示：旧的裸标记写法
        val normalizedMarker = projectileMarker.trim().lowercase()
        if (projectileMarker != projectileMarker.trim() || normalizedMarker == "empty" || normalizedMarker == "ray") {
            warn(
                "Projectile marker '$projectileMarker' has no '@' prefix; " +
                    "engine markers should be written as \"@empty\" / \"@ray\" / \"@melee\""
            )
        }
    }

    /**
     * 是否有任何弹种把 `ProjectileAmount` 覆盖成别的值。
     *
     * 有的话说明"不发射"只是某个弹种的局部行为，不是这把枪的定位——
     * 此时不该提示迁移到 `@melee`（那把枪平时还是要开枪的）。
     */
    private fun anyModifierTouchesProjectileAmount(data: DefaultGunData): Boolean {
        for (entry in data.ammoConsumers) {
            val override = entry.value.override ?: continue
            if (override.containsKey(PROJECTILE_AMOUNT_KEY)) return true
        }
        return false
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
