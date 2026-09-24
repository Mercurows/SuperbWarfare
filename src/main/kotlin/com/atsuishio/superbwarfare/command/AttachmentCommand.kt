package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.command.builder.buildCommand
import com.atsuishio.superbwarfare.command.builder.entityArg
import com.atsuishio.superbwarfare.command.builder.enumArg
import com.atsuishio.superbwarfare.command.builder.resourceLocationArg
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.AttachmentSlots
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.init.ModItems
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

// 参数名同时被补全逻辑用来从上下文里取出已解析的参数
private const val ENTITY_ARG = "entity"
private const val TYPE_ARG = "type"
private const val ATTACHMENT_ARG = "attachment"

/**
 * ```
 * /sbw attachment <entity> set <type> <attachment>
 * /sbw attachment <entity> clear [<type>]
 * /sbw attachment <entity> random [<type>]
 * ```
 *
 * 三条指令都作用于实体主手的枪械，主手物品不是 [GunItem] 时指令失败：
 * - `set`：把槽位换成指定配件。物品与配件数据必须存在、配件数据的槽位必须与 `type` 一致、
 *   不能与该枪上已安装的配件抢同一个挂点组，
 *   并且必须出现在该枪械数据的 `AvailableAttachments` 列表里
 * - `clear`：清空指定槽位，不写 `type` 时清空全部槽位（槽位清单来自 `AttachmentSlots.ALL`）
 * - `random`：在可用列表里随机抽一个配件装上，不写 `type` 时每个槽位各抽一次
 *   （该槽位没有可用配件时跳过，因此有可用配件的槽位都会被随机填上）
 */
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

                        if (definition.slot != type) {
                            fail {
                                Component.translatable(
                                    "commands.superbwarfare.attachment.fail.type",
                                    id.toString(),
                                    definition.slot.slotName(),
                                    type.slotName()
                                )
                            }
                        }

                        // 挂点组冲突：同一个挂点上已经装了别的配件，先于"不可用"报出来，
                        // 否则只会得到一句含糊的"这把枪不支持该配件"
                        data.attachment.mountConflict(type, definition)?.let { blocker ->
                            fail {
                                Component.translatable(
                                    "commands.superbwarfare.attachment.fail.mount",
                                    blocker.slotName(),
                                    AttachmentSlots.mountOf(type, definition)
                                )
                            }
                        }

                        // 配件必须在这把枪的 AvailableAttachments 里声明可用
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

                for (type in AttachmentType.entries) {
                    applyAttachment(data, entity, type, null)
                }

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
            // 不写 type：每个槽位各抽一次，保证有可用配件的槽位都被随机填上
            execute {
                val data = mainHandGunData(entity) ?: fail { notGunMessage() }

                val rolls = AttachmentType.entries.mapNotNull { type ->
                    randomAttachment(data, type)?.let { type to it }
                }

                if (rolls.isEmpty()) {
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
                                id.toString()
                            )
                        }.reduce { acc, entry -> acc.append(Component.literal(", ")).append(entry) }
                    )
                }
            }

            enumArg<AttachmentType>(TYPE_ARG) {
                execute {
                    val type = enumArg
                    val data = mainHandGunData(entity) ?: fail { notGunMessage() }

                    val id = randomAttachment(data, type)
                        ?: fail {
                            Component.translatable(
                                "commands.superbwarfare.attachment.fail.random.type", type.slotName()
                            )
                        }

                    applyAttachment(data, entity, type, id)

                    success {
                        Component.translatable(
                            "commands.superbwarfare.attachment.success.random",
                            entity.displayName,
                            type.slotName(),
                            id.toString()
                        )
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
    if (data.attachment.id(type) == id) return

    if (AttachmentSlots.of(type).withdrawAmmoOnChange) {
        data.withdrawAmmo(ammoSupplier)
    }

    data.attachment.set(type, id)
    data.save()
}

/** 在 [type] 槽位的可用配件里随机抽一个，该槽位没有可用配件时返回 `null` */
private fun randomAttachment(data: GunData, type: AttachmentType): ResourceLocation? =
    installableAttachments(data, type).randomOrNull()

/** [data] 的 [type] 槽位里真正装得上的配件：在可用列表里，且配件物品与配件数据都齐全 */
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

/** 取出 [entity] 主手的枪械数据，主手物品不是 [GunItem] 时返回 `null` */
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

    return ModItems.ATTACHMENTS.entries.mapNotNull { entry ->
        val id = entry.id
        val definition = AttachmentDefinition.from(id) ?: return@mapNotNull null
        if (type != null && definition.slot != type) return@mapNotNull null
        id.toString()
    }.sorted()
}

private fun notGunMessage(): Component =
    Component.translatable("commands.superbwarfare.attachment.fail.not_gun")

/** 复用配件 tooltip 中的槽位名称，例如 `[Scope Attachment]` / `[瞄准镜配件]` */
private fun AttachmentType.slotName(): Component =
    Component.translatable("attachment.superbwarfare.slot.${attachmentName.lowercase(Locale.ROOT)}")
