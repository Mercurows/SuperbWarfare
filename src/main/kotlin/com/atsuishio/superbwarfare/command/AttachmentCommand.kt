package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.command.builder.buildCommand
import com.atsuishio.superbwarfare.command.builder.entityArg
import com.atsuishio.superbwarfare.command.builder.enumArg
import com.atsuishio.superbwarfare.command.builder.resourceLocationArg
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.attachment.AttachmentItem
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.registries.ForgeRegistries
import java.util.*

// 参数名同时被补全逻辑用来从上下文里取出已解析的参数
private const val ENTITY_ARG = "entity"
private const val TYPE_ARG = "type"
private const val ATTACHMENT_ARG = "attachment"

/**
 * `/sbw attachment <entity> <type> <attachment|clear>`
 *
 * 修改实体主手上枪械的配件：
 * - `entity`：只允许单个实体，其主手物品必须是 [GunItem]，否则指令失败
 * - `type`：配件槽位，取自 [AttachmentType] 的五种类型
 * - `attachment`：配件物品的注册名，其配件数据（[AttachmentDefinition]）的槽位必须与 `type` 相同，
 *   且必须在该枪械数据的 `AvailableAttachments` 列表中
 * - `clear`：清空该槽位上的配件
 */
val ATTACHMENT_COMMAND = buildCommand("attachment") {
    requirePermission(2)

    entityArg(ENTITY_ARG) {
        enumArg<AttachmentType>(TYPE_ARG) {
            "clear" {
                execute {
                    val type = enumArg

                    val data = mainHandGunData(entity)
                        ?: fail { Component.translatable("commands.superbwarfare.attachment.fail.not_gun") }

                    // 弹匣配件决定弹匣容量，更换前先把已装填的弹药退还给持有者
                    if (type == AttachmentType.MAGAZINE) {
                        data.withdrawAmmo(entity)
                    }

                    data.attachment.remove(type)
                    data.save()

                    success {
                        Component.translatable(
                            "commands.superbwarfare.attachment.success.clear",
                            entity.displayName,
                            type.slotName()
                        )
                    }
                }
            }

            resourceLocationArg(ATTACHMENT_ARG, suggests = attachmentIdSuggestions()) {
                execute {
                    val type = enumArg
                    val id = resourceLocationArg

                    val data = mainHandGunData(entity)
                        ?: fail { Component.translatable("commands.superbwarfare.attachment.fail.not_gun") }

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

                    // 配件必须在这把枪的 AvailableAttachments 里声明可用
                    if (!data.canInstall(type, id)) {
                        fail {
                            Component.translatable(
                                "commands.superbwarfare.attachment.fail.unavailable", id.toString()
                            )
                        }
                    }

                    // 弹匣配件决定弹匣容量，更换前先把已装填的弹药退还给持有者
                    if (type == AttachmentType.MAGAZINE) {
                        data.withdrawAmmo(entity)
                    }

                    data.attachment.set(type, id)
                    data.save()

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
}

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
    if (ForgeRegistries.ITEMS.getValue(id) !is AttachmentItem) return null
    return AttachmentDefinition.from(id)
}

/** 可补全的配件 id：能确定枪械和槽位时只列出该枪可安装的配件，否则退回已注册的配件 */
private fun attachmentIds(gun: GunData?, type: AttachmentType?): List<String> {
    if (gun != null && type != null) {
        // 与安装校验用同一套判定，保证补全出来的配件一定能装上
        return gun.availableAttachments(type)
            .filter { gun.canInstall(type, it) }
            .map { it.toString() }
            .sorted()
    }

    return ModItems.ATTACHMENTS.entries.mapNotNull { entry ->
        val id = entry.id
        val definition = AttachmentDefinition.from(id) ?: return@mapNotNull null
        if (type != null && definition.slot != type) return@mapNotNull null
        id.toString()
    }.sorted()
}

/** 复用配件 tooltip 中的槽位名称，例如 `[Scope Attachment]` / `[瞄准镜配件]` */
private fun AttachmentType.slotName(): Component =
    Component.translatable("attachment.superbwarfare.slot.${attachmentName.lowercase(Locale.ROOT)}")
