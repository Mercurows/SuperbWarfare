package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.tools.invoke
import com.google.gson.JsonObject
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import java.util.concurrent.CompletableFuture
import java.util.function.Function

/**
 * 用于指令的枚举类型参数，会把枚举常量的名称转换为小驼峰形式
 *
 *
 * Code based on [CaerulaArbor](https://github.com/Apocalypse114/CaerulaArbor)
 *
 * @author Mercurows
 */
class LowerCamelCaseEnumArgument<T : Enum<T>> private constructor(private val enumClass: Class<T>) : ArgumentType<T> {

    private val names by lazy {
        enumClass.enumConstants.map { e -> valueMapper.apply(e) }.toList()
    }

    val valueMapper = Function { e: T ->
        val input = e.name.trim()
        val trimmed = input.replace("^_+|_+$".toRegex(), "").ifEmpty { return@Function input }

        val parts = trimmed.lowercase().split("_+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        return@Function buildString {
            append(parts[0])

            for (i in 1..<parts.size) {
                if (!parts[i].isEmpty()) {
                    append(parts[i][0].uppercaseChar())
                    append(parts[i].substring(1))
                }
            }
        }
    }

    @Throws(CommandSyntaxException::class)
    override fun parse(reader: StringReader): T {
        val input = reader.readUnquotedString()

        return enumClass.enumConstants.find { valueMapper(it) == input }
            ?: throw INVALID_ENUM.createWithContext(reader, input, names.toString())
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> = SharedSuggestionProvider.suggest(names, builder)

    override fun getExamples() = names

    class Info<T : Enum<T>> : ArgumentTypeInfo<LowerCamelCaseEnumArgument<T>, Info<T>.Template> {

        override fun serializeToNetwork(template: Template, buffer: FriendlyByteBuf) {
            buffer.writeUtf(template.enumClass.getName())
        }

        @Suppress("unchecked_cast")
        override fun deserializeFromNetwork(buffer: FriendlyByteBuf) =
            Template(Class.forName(buffer.readUtf()) as Class<T>)

        override fun serializeToJson(template: Template, json: JsonObject) {
            json.addProperty("enum", template.enumClass.getName())
        }

        override fun unpack(argument: LowerCamelCaseEnumArgument<T>) = Template(argument.enumClass)

        inner class Template(val enumClass: Class<T>) : ArgumentTypeInfo.Template<LowerCamelCaseEnumArgument<T>> {
            override fun instantiate(pStructure: CommandBuildContext) = LowerCamelCaseEnumArgument(this.enumClass)
            override fun type() = this@Info
        }
    }

    companion object {
        private val INVALID_ENUM = Dynamic2CommandExceptionType { found, constants ->
            Component.translatable("commands.neoforge.arguments.enum.invalid", constants, found)
        }

        fun <T : Enum<T>> enumArgument(enumClass: Class<T>) = LowerCamelCaseEnumArgument(enumClass)
    }
}
