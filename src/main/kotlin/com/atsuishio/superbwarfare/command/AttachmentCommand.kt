package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.command.builder.*
import com.atsuishio.superbwarfare.config.server.AttachmentConfig
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots
import com.atsuishio.superbwarfare.data.attachment.LaserInfo
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.subdata.Attachment
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.item.attachment.AttachmentProvider
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.util.*

private const val ENTITY_ARG = "entity"
private const val TYPE_ARG = "type"
private const val ATTACHMENT_ARG = "attachment"
private const val COLOR_ARG = "color"
private const val SKIN_ARG = "skinId"

/** `laserColor` 的 `<hex>` 传这个值表示清掉颜色覆盖 */
private const val LASER_COLOR_RESET = "reset"

/** `skin` 的 `<skinId>` 传这个值表示恢复配件本体模型贴图 */
private const val SKIN_RESET = "reset"

/** `RRGGBB` / `#RRGGBB` / `0xRRGGBB` */
private val RGB_PATTERN = Regex("^(?:#|0x)?([A-Fa-f0-9]{6})$", RegexOption.IGNORE_CASE)

val ATTACHMENT_COMMAND = buildCommand("attachment") {
    requirePermission(2)

    entityArg(ENTITY_ARG) {
        "set" {
            enumArg<AttachmentType>(TYPE_ARG) {
                resourceLocationArg(ATTACHMENT_ARG, suggests = attachmentIdSuggestions()) {
                    execute {
                        val type = enumArg
                        val id = resourceLocationArg

                        val data = mainHandGunData(entity) ?: fail { notGunMessage() }

                        val definition = attachmentDefinitionOf(id)
                            ?: fail {
                                Component.translatable(
                                    "commands.superbwarfare.attachment.fail.unknown", id.toString()
                                )
                            }

                        if (type !in definition.acceptedSlots) {
                            fail {
                                Component.translatable(
                                    "commands.superbwarfare.attachment.fail.type",
                                    id.toString(),
                                    acceptedSlotNames(definition),
                                    type.slotName()
                                )
                            }
                        }

                        // 互斥：这把枪自己声明了这对槽位互斥（`AttachmentConflicts`）、
                        // 挂点组被别人占了，或任一方在 `ConflictsWith` 里点了名。
                        // 先于"不可用"报出来，否则只会得到一句含糊的"这把枪不支持该配件"。
                        // 开了自由改装模式时这一步恒不成立（判定收在 `Attachment.conflict` 里）
                        data.attachment.conflict(type, definition)?.let { blocker ->
                            fail {
                                Component.translatable(
                                    "commands.superbwarfare.attachment.fail.conflict",
                                    blocker.slotName()
                                )
                            }
                        }

                        // 配件必须在这把枪的 AvailableAttachments 里声明可用
                        // （开了完全自由改装模式时 `availableAttachments` 会给出该槽位已注册的全部配件）
                        if (!data.canInstall(type, id)) {
                            fail {
                                Component.translatable(
                                    "commands.superbwarfare.attachment.fail.unavailable", id.toString()
                                )
                            }
                        }

                        applyAttachment(data, entity, type, id)

                        success {
                            Component.translatable(
                                "commands.superbwarfare.attachment.success.set",
                                entity.displayName,
                                type.slotName(),
                                id.toString()
                            )
                        }
                    }
                }
            }
        }

        "clear" {
            // 不写 type：清空全部槽位
            execute {
                val data = mainHandGunData(entity) ?: fail { notGunMessage() }

                clearAllAttachments(data, entity)

                success {
                    Component.translatable(
                        "commands.superbwarfare.attachment.success.clear.all", entity.displayName
                    )
                }
            }

            enumArg<AttachmentType>(TYPE_ARG) {
                execute {
                    val type = enumArg
                    val data = mainHandGunData(entity) ?: fail { notGunMessage() }

                    applyAttachment(data, entity, type, null)

                    success {
                        Component.translatable(
                            "commands.superbwarfare.attachment.success.clear",
                            entity.displayName,
                            type.slotName()
                        )
                    }
                }
            }
        }

        "random" {
            // 不写 type：**先清空、再按挂点组各抽一次**，抽出来的就是最终配置
            execute {
                val data = mainHandGunData(entity) ?: fail { notGunMessage() }

                // 先清空：挂点组互斥，留着旧配件会把同组其它槽位的候选**全部过滤掉**
                // （例如已经装着刺刀时，枪口槽位一个候选都抽不到），
                // 那样抽出来的只是"在原配置上小改"，而不是真正的全量随机。
                // 清空之后每个组都能从组内全部槽位里挑，最终结果与旧配置无关。
                clearAllAttachments(data, entity)

                val rolls = rollAllSlots(data)

                if (rolls.isEmpty()) {
                    // 空配件也会保留一条抽签结果；空列表只表示没有任何可用配件。
                    fail { Component.translatable("commands.superbwarfare.attachment.fail.random") }
                }

                for ((type, id) in rolls) {
                    applyAttachment(data, entity, type, id)
                }

                success {
                    Component.translatable(
                        "commands.superbwarfare.attachment.success.random.all",
                        entity.displayName,
                        rolls.map { (type, id) ->
                            Component.translatable(
                                "commands.superbwarfare.attachment.random.entry",
                                type.slotName(),
                                randomAttachmentName(id)
                            )
                        }.reduce { acc, entry -> acc.append(Component.literal(", ")).append(entry) }
                    )
                }
            }

            enumArg<AttachmentType>(TYPE_ARG) {
                execute {
                    val type = enumArg
                    val data = mainHandGunData(entity) ?: fail { notGunMessage() }

                    val options = installableAttachments(data, type)
                    if (options.isEmpty()) {
                        fail {
                            Component.translatable(
                                "commands.superbwarfare.attachment.fail.random.type", type.slotName()
                            )
                        }
                    }
                    val id = randomAttachment(options)

                    applyAttachment(data, entity, type, id)

                    success {
                        Component.translatable(
                            "commands.superbwarfare.attachment.success.random",
                            entity.displayName,
                            type.slotName(),
                            randomAttachmentName(id)
                        )
                    }
                }
            }
        }

        "laserColor" {
            enumArg<AttachmentType>(TYPE_ARG) {
                execute {
                    val type = enumArg
                    val data = mainHandGunData(entity) ?: fail { notGunMessage() }
                    val info = laserInfoAt(data, type) ?: fail { notLaserMessage(type) }

                    val override = data.attachment.getLaserColor(type)
                    val source = if (override >= 0) {
                        "commands.superbwarfare.attachment.laser_color.source.override"
                    } else {
                        "commands.superbwarfare.attachment.laser_color.source.default"
                    }

                    success {
                        Component.translatable(
                            "commands.superbwarfare.attachment.laser_color",
                            entity.displayName,
                            type.slotName(),
                            String.format("%06X", info.resolveColorRgb(override)),
                            Component.translatable(source)
                        )
                    }
                }

                stringWordArg(COLOR_ARG) {
                    execute {
                        val type = enumArg
                        val raw = wordArg
                        val data = mainHandGunData(entity) ?: fail { notGunMessage() }
                        val info = laserInfoAt(data, type) ?: fail { notLaserMessage(type) }

                        val reset = raw.equals(LASER_COLOR_RESET, ignoreCase = true)
                        val rgb = if (reset) {
                            Attachment.NO_LASER_COLOR
                        } else {
                            parseRgb(raw) ?: fail {
                                Component.translatable(
                                    "commands.superbwarfare.attachment.fail.laser_color", raw
                                )
                            }
                        }

                        data.attachment.setLaserColor(type, rgb)
                        data.save()

                        success {
                            if (reset) {
                                Component.translatable(
                                    "commands.superbwarfare.attachment.success.laser_color.reset",
                                    entity.displayName,
                                    type.slotName()
                                )
                            } else {
                                Component.translatable(
                                    "commands.superbwarfare.attachment.success.laser_color",
                                    entity.displayName,
                                    type.slotName(),
                                    String.format("%06X", info.resolveColorRgb(rgb))
                                )
                            }
                        }
                    }
                }
            }
        }

        "skin" {
            enumArg<AttachmentType>(TYPE_ARG) {
                // 不写 skinId：查询当前皮肤
                execute {
                    val type = enumArg
                    val data = mainHandGunData(entity) ?: fail { notGunMessage() }
                    val definition = installedDefinition(data, type) ?: fail { notAttachmentMessage(type) }

                    val skinId = data.attachment.getSkin(type)
                    val skin = definition.skinOrNull(skinId)

                    success {
                        if (skin == null) {
                            Component.translatable(
                                "commands.superbwarfare.attachment.skin.current",
                                entity.displayName,
                                type.slotName(),
                                skinId.ifBlank { AttachmentDefinition.DEFAULT_SKIN_ID },
                                Component.translatable(
                                    if (skinId.isBlank()) {
                                        "commands.superbwarfare.attachment.skin.source.default"
                                    } else {
                                        "commands.superbwarfare.attachment.skin.source.missing"
                                    }
                                )
                            )
                        } else {
                            Component.translatable(
                                "commands.superbwarfare.attachment.skin.current",
                                entity.displayName,
                                type.slotName(),
                                skin.name.ifBlank { skin.id },
                                Component.translatable("commands.superbwarfare.attachment.skin.source.skin")
                            )
                        }
                    }
                }

                stringWordArg(SKIN_ARG, suggests = attachmentSkinSuggestions()) {
                    execute {
                        val type = enumArg
                        val raw = wordArg
                        val data = mainHandGunData(entity) ?: fail { notGunMessage() }
                        val definition = installedDefinition(data, type) ?: fail { notAttachmentMessage(type) }

                        val reset = raw.equals(SKIN_RESET, ignoreCase = true)
                        if (!reset && definition.skinOrNull(raw) == null) {
                            fail {
                                Component.translatable(
                                    "commands.superbwarfare.attachment.fail.skin.unknown",
                                    raw,
                                    definition.skins.joinToString(", ") { it.id },
                                    type.slotName()
                                )
                            }
                        }

                        data.attachment.setSkin(type, if (reset) null else raw)
                        data.save()

                        success {
                            if (reset) {
                                Component.translatable(
                                    "commands.superbwarfare.attachment.success.skin.reset",
                                    entity.displayName,
                                    type.slotName()
                                )
                            } else {
                                Component.translatable(
                                    "commands.superbwarfare.attachment.success.skin",
                                    entity.displayName,
                                    type.slotName(),
                                    raw
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 写入/移除配件，`id` 为 `null` 表示移除该槽位的配件。
 *
 * 弹匣槽位决定弹匣容量，换掉之前先把已装填的弹药退还给 [ammoSupplier]；
 * 槽位内容没有变化时（`set` 同一个配件、`clear` 空槽位）什么都不做。
 */
private fun applyAttachment(data: GunData, ammoSupplier: Entity, type: AttachmentType, id: ResourceLocation?) {
    if (id == null) {
        if (!data.attachment.has(type)) return
    } else if (data.attachment.id(type) == id && !data.attachment.acceptsStacked(type)) {
        return
    }

    if (AttachmentSlots.of(type).withdrawAmmoOnChange) {
        data.withdrawAmmo(ammoSupplier)
    }

    if (id == null) data.attachment.clear(type) else data.attachment.set(type, id)
    data.save()
}

/** 清空该枪的全部槽位；`clear`（不带 type）与 `random` 抽签前都用它 */
private fun clearAllAttachments(data: GunData, ammoSupplier: Entity) {
    for (type in AttachmentType.entries) {
        applyAttachment(data, ammoSupplier, type, null)
    }
}

/** 取该槽位上的激光配置，槽位空或配件没有 `Laser` 段时返回 null */
private fun laserInfoAt(data: GunData, type: AttachmentType): LaserInfo? =
    installedDefinition(data, type)?.laser

/** 该槽位没有激光配件时的报错消息 */
private fun notLaserMessage(type: AttachmentType) = Component.translatable(
    "commands.superbwarfare.attachment.fail.laser", type.slotName()
)

/** 该槽位没装这件配件时的报错消息 */
private fun notAttachmentMessage(type: AttachmentType) = Component.translatable(
    "commands.superbwarfare.attachment.fail.empty", type.slotName()
)

/**
 * [type] 槽位上装着的那件配件的定义；槽位空、或这件配件本来就不认这个槽位时返回 null。
 *
 * 皮肤是**装上去之后**才谈得上的东西，所以 `skin` 子命令的两条路径都先过它。
 */
private fun installedDefinition(data: GunData, type: AttachmentType): AttachmentDefinition? {
    val id = data.attachment.id(type) ?: return null
    val definition = AttachmentDefinition.from(id) ?: return null
    if (type !in definition.acceptedSlots) return null
    return definition
}

/**
 * 补全皮肤 id；目标枪械与槽位都解析得出时，只列**这件配件声明的皮肤**。
 *
 * 与 [attachmentIdSuggestions] 同样必须兜底：补全器抛异常会让整个补全包处理失败，
 * 连 `set` / `clear` 这些字面量补全都收不到。
 */
private fun attachmentSkinSuggestions(): SuggestionProvider<CommandSourceStack> =
    SuggestionProvider { context, builder ->
        val ids = runCatching { suggestedSkinIds(context) }
            .onFailure { Mod.LOGGER.warn("Failed to compute attachment skin suggestions", it) }
            .getOrDefault(emptyList())

        SharedSuggestionProvider.suggest(ids, builder)
    }

private fun suggestedSkinIds(context: CommandContext<CommandSourceStack>): List<String> {
    val type = context.parsedArgument<AttachmentType>(TYPE_ARG) ?: return emptyList()
    // 没匹配到实体（或匹配到多个）时解析会抛异常，此时没有可补全的内容
    val gun = runCatching { EntityArgument.getEntity(context, ENTITY_ARG) }
        .getOrNull()
        ?.let(::mainHandGunData)
        ?: return emptyList()

    return installedDefinition(gun, type)?.skins?.map { it.id }?.filter { it.isNotBlank() }?.sorted()
        ?: emptyList()
}

/** 解析 `RRGGBB` / `#RRGGBB` / `0xRRGGBB`，非法返回 null */
private fun parseRgb(raw: String): Int? {
    val match = RGB_PATTERN.matchEntire(raw.trim()) ?: return null
    return match.groupValues[1].toInt(16)
}

/** 空配件与每个可用配件等概率；`null` 表示抽到了空配件。 */
private fun randomAttachment(options: List<ResourceLocation>): ResourceLocation? =
    options.getOrNull((0..options.size).random() - 1)

private fun randomAttachmentName(id: ResourceLocation?): Component =
    id?.let { Component.literal(it.toString()) }
        ?: Component.translatable("commands.superbwarfare.attachment.random.empty")

/**
 * 全量随机：每个挂点组先选兼容的槽位，再从该槽位的配件与空配件中等概率抽取。
 * 组的处理顺序也要随机，否则跨组互斥时，枚举靠前的握把总会先占位，副武器永远抽不到。
 * 兼容性在选槽位前过滤，避免选中一个候选全被排除的槽位而跳过同组其它可用槽位。
 *
 * 自由改装模式下不限制互斥，每个可用槽位独立抽取。调用方必须先 [clearAllAttachments]，
 * 保证候选不受旧配件影响；空配件保留在结果中，全部抽空也属于成功。
 *
 * @return `槽位 to 配件 id`，`id == null` 表示空配件；没有任何可用配件时为空列表。
 */
private fun rollAllSlots(data: GunData): List<Pair<AttachmentType, ResourceLocation?>> {
    val options = AttachmentType.entries.associateWith { installableAttachments(data, it) }
        .filterValues { it.isNotEmpty() }

    if (AttachmentConfig.freeAttachmentMode) {
        return options.map { (type, ids) ->
            type to randomAttachment(ids)
        }
    }

    val rolls = mutableListOf<Pair<AttachmentType, ResourceLocation?>>()

    for (slots in options.keys.groupBy { AttachmentSlots.mountOf(it) }.values.shuffled()) {
        val candidates = slots.mapNotNull { type ->
            val compatible = options.getValue(type).filter { id ->
                val definition = AttachmentDefinition.from(id)
                rolls.none { (picked, pickedId) ->
                    pickedId != null && AttachmentSlots.conflicts(
                        type, definition, picked, AttachmentDefinition.from(pickedId), data
                    )
                }
            }
            if (compatible.isEmpty()) null else type to compatible
        }
        val (type, ids) = candidates.randomOrNull() ?: continue

        rolls += type to randomAttachment(ids)
    }

    return rolls
}

/**
 * [data] 的 [type] 槽位里真正装得上的配件：在可用列表里，且配件物品与配件数据都齐全。
 *
 * 可用列表本身随自由改装配置变化（见 [GunData.availableAttachments]），所以 `set` 的校验
 * 与 `random` / 补全的候选**永远共用这一份判定**，不会出现"补全里有、指令说装不上"的分叉。
 */
private fun installableAttachments(data: GunData, type: AttachmentType): List<ResourceLocation> =
    data.availableAttachments(type).filter { attachmentDefinitionOf(it) != null && data.canInstall(type, it) }

/**
 * 补全可安装的配件；目标实体与槽位都已解析时，只补全这把枪真正支持的配件。
 *
 * 必须写成函数而不是顶层 `val`：顶层属性按声明顺序初始化，而 [ATTACHMENT_COMMAND] 声明在前面，
 * 写成 `val` 会让这里在赋值前被读到 `null`，补全提供器被静默丢弃。
 */
private fun attachmentIdSuggestions(): SuggestionProvider<CommandSourceStack> =
    SuggestionProvider { context, builder ->
        // 补全提供器一旦抛异常，整个 ServerboundCommandSuggestionPacket 的处理都会失败，
        // 客户端连 `clear` 这种字面量补全都收不到，所以这里必须兜底
        val ids = runCatching { suggestedAttachmentIds(context) }
            .onFailure { Mod.LOGGER.warn("Failed to compute attachment suggestions", it) }
            .getOrDefault(emptyList())

        SharedSuggestionProvider.suggest(ids, builder)
    }

/** 可补全的配件 id；解析不出目标枪械时退回该槽位已注册的全部配件 */
private fun suggestedAttachmentIds(context: CommandContext<CommandSourceStack>): List<String> {
    // 实体参数存的是 EntitySelector（1.20.1 的 EntityArgument 是 ArgumentType<EntitySelector>），
    // 必须按当前命令源解析；没匹配到实体（或匹配到多个）时会抛异常，此时退回不带枪械的全局补全
    val gun = runCatching { EntityArgument.getEntity(context, ENTITY_ARG) }
        .getOrNull()
        ?.let(::mainHandGunData)

    return attachmentIds(gun, context.parsedArgument<AttachmentType>(TYPE_ARG))
}

/**
 * 取出上下文里已解析的参数值。
 *
 * 只适用于实际类型与 [T] 一致的参数（枚举、数字等）。像 [EntityArgument] 那种解析结果是
 * `EntitySelector` 的参数不能走这里，必须用 `EntityArgument.getEntity` 之类的 `getXxx` 解析。
 *
 * 补全请求可能来自更浅的节点，此时参数并不在上下文里，而 [CommandContext.getArgument] 会直接抛异常。
 */
private inline fun <reified T> CommandContext<CommandSourceStack>.parsedArgument(name: String): T? =
    if (nodes.any { it.node.name == name }) getArgument(name, T::class.java) else null

/**
 * 取出 [entity] **主手那把枪**的枪械数据，主手物品不是 [GunItem] 时返回 `null`。
 *
 * ⚠ **这里刻意看"物理上的主手"，不用 `ActiveGun`**（四期）：配件永远只对**主武器**生效，
 * 副武器不具有配件（`SubWeaponItem.canEditAttachments` = false，
 * `GunData.availableAttachments` 对副武器返回空表）。
 * 所以副武器被 G 切出来时，`/sbw attachment` 操作的仍然是手上那把枪 —— 这正是想要的。
 */
private fun mainHandGunData(entity: Entity): GunData? {
    val stack = (entity as? LivingEntity)?.mainHandItem ?: return null
    if (stack.item !is GunItem) return null
    return GunData.from(stack)
}

/** 查找 [id] 对应的配件物品与配件数据，两者缺一不可 */
private fun attachmentDefinitionOf(id: ResourceLocation): AttachmentDefinition? {
    if (BuiltInRegistries.ITEM.get(id) !is AttachmentProvider) return null
    return AttachmentDefinition.from(id)
}

/** 可补全的配件 id：能确定枪械和槽位时只列出该枪可安装的配件，否则退回已注册的配件 */
private fun attachmentIds(gun: GunData?, type: AttachmentType?): List<String> {
    if (gun != null && type != null) {
        // 与安装校验用同一套判定，保证补全出来的配件一定能装上
        return installableAttachments(gun, type).map { it.toString() }.sorted()
    }

    return (type?.let { listOf(it) } ?: AttachmentType.entries)
        .flatMap { AttachmentSlots.registeredIds(it) }
        .distinct()
        .map { it.toString() }
        .sorted()
}

private fun notGunMessage(): Component =
    Component.translatable("commands.superbwarfare.attachment.fail.not_gun")

/** 复用配件 tooltip 中的槽位名称，例如 `[Scope Attachment]` / `[瞄准镜配件]` */
private fun AttachmentType.slotName(): Component =
    Component.translatable("attachment.superbwarfare.slot.${attachmentName.lowercase(Locale.ROOT)}")

/**
 * 一件配件能装的全部槽位名，用 ` / ` 连接，例如 `[上导轨配件] / [下导轨配件] / [左导轨配件]`。
 * 多槽位配件（`ExtraSlots`，如导轨上的激光指示器）不止一个位置，
 * 只报 `definition.slot` 会让玩家以为装错了，这里全部列出来。
 */
private fun acceptedSlotNames(definition: AttachmentDefinition): Component {
    val text = Component.empty()
    definition.acceptedSlots.sortedBy { it.ordinal }.forEachIndexed { index, type ->
        if (index > 0) text.append(Component.literal(" / "))
        text.append(type.slotName())
    }
    return text
}
