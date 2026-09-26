package com.atsuishio.superbwarfare.command.builder

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

class CommandNodeWithStringArg(argBuilder: ArgumentBuilder<CommandSourceStack, *>, argName: String) :
    CommandNodeWithArg<String>(argBuilder, argName) {

    val CommandContext<CommandSourceStack>.stringArg get() = getArg(this@CommandNodeWithStringArg)

    override fun CommandContext<CommandSourceStack>.getArg(ctx: CommandNodeWithArg<String>): String =
        StringArgumentType.getString(this, ctx.name)
}

class CommandNodeWithWordArg(argBuilder: ArgumentBuilder<CommandSourceStack, *>, argName: String) :
    CommandNodeWithArg<String>(argBuilder, argName) {

    val CommandContext<CommandSourceStack>.wordArg get() = getArg(this@CommandNodeWithWordArg)

    override fun CommandContext<CommandSourceStack>.getArg(ctx: CommandNodeWithArg<String>): String =
        StringArgumentType.getString(this, ctx.name)
}

inline fun CommandNode.stringArg(argName: String = "$name.string", builder: CommandNodeWithStringArg.() -> Unit) {
    cmd += CommandNodeWithStringArg(Commands.argument(argName, StringArgumentType.string()), argName).apply(builder)
}

/**
 * 与 [stringArg] 相同的读取方式，但参数按 **单个词**（`StringArgumentType.word()`）解析：
 * 只接受 `[A-Za-z0-9_.+-]`，**斜杠和冒号会被拒绝**（`StringReader.isAllowedInUnquotedString`）。
 *
 * 需要确保用户"怎么敲都能解析"时用它 —— 例如目录名、枚举名这类不含特殊字符的输入。
 */
inline fun CommandNode.stringWordArg(
    argName: String = "$name.word",
    suggests: SuggestionProvider<CommandSourceStack>? = null,
    builder: CommandNodeWithWordArg.() -> Unit
) {
    val argument = Commands.argument(argName, StringArgumentType.word())
    suggests?.let { argument.suggests(it) }
    cmd += CommandNodeWithWordArg(argument, argName).apply(builder)
}
