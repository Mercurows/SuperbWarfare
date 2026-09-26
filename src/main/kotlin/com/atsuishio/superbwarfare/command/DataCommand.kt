package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.command.builder.CommandNode
import com.atsuishio.superbwarfare.command.builder.buildCommand
import com.atsuishio.superbwarfare.command.builder.stringArg
import com.atsuishio.superbwarfare.command.builder.stringWordArg
import com.atsuishio.superbwarfare.debug.DataDump
import com.atsuishio.superbwarfare.debug.DataDump.Kind
import com.atsuishio.superbwarfare.network.message.receive.OpenFolderMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

// 参数名同时被补全逻辑用来从上下文里取出已解析的参数
private const val NAME_ARG = "name"
private const val ID_ARG = "id"

/** `<kind>` 字面量：覆盖两个来源的数据集，以及只导 `data` / 只导 `resource` */
private const val ALL = "all"
private const val DATA = "data"
private const val RESOURCE = "resource"

private val ALL_KINDS = setOf(Kind.DATA, Kind.RESOURCE)

/**
 * ```
 * /sbw data list [<kind>]
 * /sbw data dump all [clean]
 * /sbw data dump data <name> [<id>] [clean]
 * /sbw data dump resource <name> [<id>] [clean]
 * ```
 *
 * 把 [DataDump] 收集的**已加载数据**写到磁盘而不是打在聊天栏：
 * 每个数据集一个目录、每个条目一个格式化 JSON 文件，外加一份 `_index.json`。
 * 导出后可以直接和 `src/main/resources` 下的源数据 diff —— 缺键会以数据类默认值补齐，
 * 能看到「游戏里实际生效的值」。目录结构见 [DataDump] 的文档。
 *
 * **为什么 `data` / `resource` 是字面量而不是 `data/sbw/guns` 这样的单个参数**：
 * Brigadier 只在**带引号**时接受 `/`、`:` 这类字符（`StringReader.isAllowedInUnquotedString`
 * 只放行 `[A-Za-z0-9_.+-]`），所以 `dump data/sbw/guns` 会在斜杠处直接报
 * 「参数后应有空格分隔」——补全列表里列得出来，却一个都执行不了。
 * 拆成「来源字面量 + 目录名」之后所有参数都是纯字母数字下划线，怎么敲都能解析。
 *
 * `<name>` 取目录名（`guns` / `vehicles` / `melee_effects`…，见 `/sbw data list`），
 * `<id>` 推荐裸写条目名（`ak_47`），带命名空间的完整 id 需要加引号（`"superbwarfare:ak_47"`）。
 */
val DATA_COMMAND = buildCommand("data") {
    requirePermission(2)

    "list" {
        // 不写 kind：两个来源都列
        execute { listDatasets(this, ALL_KINDS) }

        kindLiterals { kind: Set<Kind> ->
            execute { listDatasets(this, kind) }
        }
    }

    "dump" {
        // all：两个来源一起导（先清目录用 /sbw data dump all clean）
        ALL {
            execute { exportData(this, ALL_KINDS, name = null, id = null, clean = false) }

            "clean" {
                execute { exportData(this, ALL_KINDS, name = null, id = null, clean = true) }
            }
        }

        kindLiterals { kind: Set<Kind> ->
            stringWordArg(NAME_ARG, suggests = nameSuggestions(kind)) {
                execute { exportData(this, kind, name = wordArg, id = null, clean = false) }

                stringArg(ID_ARG) {
                    execute { exportData(this, kind, name = wordArg, id = stringArg, clean = false) }

                    "clean" {
                        execute { exportData(this, kind, name = wordArg, id = stringArg, clean = true) }
                    }
                }

                "clean" {
                    execute { exportData(this, kind, name = wordArg, id = null, clean = true) }
                }
            }
        }
    }
}

/**
 * 同时挂上 `data` / `resource` / `all` 三个字面量，把对应的 [Kind] 交给 [builder]。
 *
 * 写成一个函数而不是三段复制，是为了让 `list` 与 `dump` 的子命令结构永远一致。
 * 字面量与 [builder] 里的参数名互不冲突（参数在下一层），所以三份 `execute` 不会互相遮蔽。
 */
private fun CommandNode.kindLiterals(builder: CommandNode.(Set<Kind>) -> Unit) {
    DATA { builder(setOf(Kind.DATA)) }
    RESOURCE { builder(setOf(Kind.RESOURCE)) }
    ALL { builder(ALL_KINDS) }
}

/** `list`：数据集清单，一行一个，点击可填入对应的 dump 命令 */
private fun CommandNode.listDatasets(context: CommandContext<CommandSourceStack>, kind: Set<Kind>): Int {
    with(context) {
        val datasets = DataDump.datasets().filter { it.kind in kind }
        if (datasets.isEmpty()) {
            fail { Component.translatable("commands.superbwarfare.data.fail.empty") }
        }

        // 清单可能很长（几十条），只回给执行者，不刷别人的屏
        source.sendSuccess({ header(datasets.size) }, false)
        for (dataset in datasets) {
            source.sendSuccess({ describe(dataset) }, false)
        }
    }

    return 1
}

/**
 * 导出并回报结果。写成 [CommandNode] 的扩展才能用 [CommandNode.fail] / [CommandNode.success] 早退。
 *
 * @param kind 只在这些来源里找数据集
 * @param name 目录名（`guns`），`null` 表示该来源下的全部数据集
 * @param id 单个条目 id（裸写 `ak_47`，带命名空间要加引号），`null` 表示导出整个数据集
 * @param clean 覆盖前清空 `debug_data/`，让目录里只剩本次导出的内容
 */
private fun CommandNode.exportData(
    context: CommandContext<CommandSourceStack>,
    kind: Set<Kind>,
    name: String?,
    id: String?,
    clean: Boolean
): Int {
    with(context) {
        val available = DataDump.datasets().filter { it.kind in kind }

        val datasets = if (name == null) {
            available
        } else {
            available.filter { it.matches(name) }
        }

        if (datasets.isEmpty()) {
            fail {
                if (name == null) {
                    Component.translatable("commands.superbwarfare.data.fail.empty")
                } else {
                    Component.translatable("commands.superbwarfare.data.fail.unknown_dataset", name)
                }
            }
        }

        val entryIds = id?.let { requested ->
            val resolved = resolveEntryIds(datasets, requested)

            if (resolved.isEmpty()) {
                // 名字没写错、只是解析不出条目时，把提示做得具体一点
                val known = datasets.flatMap { it.entries }.map { it.id }
                fail {
                    if (known.isEmpty()) {
                        Component.translatable("commands.superbwarfare.data.fail.empty_dataset", datasets[0].key)
                    } else {
                        Component.translatable("commands.superbwarfare.data.fail.unknown_id", requested)
                    }
                }
            }

            resolved
        }.orEmpty()

        val result = try {
            DataDump.dump(datasets, entryIds, clean)
        } catch (exception: Exception) {
            fail { Component.translatable("commands.superbwarfare.data.fail.export", exception.message ?: "?") }
        }

        // 详细情况留在日志里，聊天栏只给一行结论 + 可点击的路径
        for (failure in result.failures) {
            Mod.LOGGER.warn("[DataDump] {}/{} -> {}", failure.dataset, failure.id, failure.reason)
        }
        Mod.LOGGER.info(
            "[DataDump] {} data set(s), {} file(s) written under {}",
            result.datasetCount,
            result.entryCount,
            result.directory
        )

        val target = if (datasets.size == 1 && id != null) {
            Component.translatable(
                "commands.superbwarfare.data.success.single",
                datasets[0].displayName,
                entryIds.first(),
                result.directory.name
            )
        } else {
            Component.translatable(
                "commands.superbwarfare.data.success.dump",
                datasets.size,
                result.entryCount,
                result.elapsedMillis,
                result.directory.name
            )
        }

        val message = Component.empty()
            .append(target.withStyle(ChatFormatting.GREEN))
            .append(" ")
            .append(Component.translatable("commands.superbwarfare.data.path", result.directory.absolutePath))

        source.sendSuccess({ message }, false)

        // 「点击打开目录」只能由客户端完成：OPEN_FILE 不允许从服务端发（见 OpenFolderMessage 的说明），
        // 所以这里只把导出的绝对路径告诉执行者，由客户端自己确认目录存在、挂上点击事件并打开。
        source.player?.sendPacket(OpenFolderMessage(result.directory.absolutePath))
    }

    return 1
}

/** `list` 的表头 */
private fun header(count: Int): Component =
    Component.translatable("commands.superbwarfare.data.header", count).withStyle(ChatFormatting.AQUA)

/** `list` 里的一行：`data guns`（DefaultGunData，48 条），点击填入对应的 dump 命令 */
private fun describe(dataset: DataDump.Dataset): Component {
    val command = "/sbw data dump ${dataset.kind.name.lowercase()} ${dataset.shortName} "

    return Component.literal("  ")
        .append(
            Component.literal("${dataset.kind.name.lowercase()} ${dataset.shortName}")
                .withStyle(ChatFormatting.WHITE)
                .withStyle {
                    it.withHoverEvent(
                        HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.translatable("commands.superbwarfare.data.list.hover", command.trim())
                        )
                    ).withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                }
        )
        .append(
            Component.translatable(
                "commands.superbwarfare.data.list.entry", dataset.typeName, dataset.entries.size
            ).withStyle(ChatFormatting.GRAY)
        )
}

/** 目录名补全：该来源下所有已加载数据集的目录名 */
private fun nameSuggestions(kind: Set<Kind>): SuggestionProvider<CommandSourceStack> =
    SuggestionProvider { _, builder ->
        SharedSuggestionProvider.suggest(DataDump.datasets().filter { it.kind in kind }.map { it.shortName }, builder)
    }

/**
 * 把用户写的条目名解析成数据集里真实的条目 id。
 *
 * 真实 id 带命名空间（`superbwarfare:ak_47`），裸写 `ak_47` 时只按 path 匹配；
 * 同一个数据集里 path 撞名（不同命名空间）就都返回，由调用方一起导出。
 */
private fun resolveEntryIds(datasets: List<DataDump.Dataset>, requested: String): Set<String> {
    val wanted = requested.trim()
    if (wanted.isEmpty()) return emptySet()

    return datasets
        .flatMap { it.entries }
        .map { it.id }
        .filter { it.equals(wanted, ignoreCase = true) || it.substringAfter(':').equals(wanted, ignoreCase = true) }
        .toSet()
}

/** 给 [DataDump.Dataset] 挂上补全用的判定与显示名 */
private fun DataDump.Dataset.matches(name: String): Boolean =
    shortName.equals(name.trim(), ignoreCase = true) || path.equals(name.trim(), ignoreCase = true)
