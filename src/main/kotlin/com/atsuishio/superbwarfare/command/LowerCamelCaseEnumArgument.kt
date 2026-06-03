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

    class Info : ArgumentTypeInfo<LowerCamelCaseEnumArgument<*>, Info.Template> {
        override fun serializeToNetwork(template: Template, buffer: FriendlyByteBuf) {
            buffer.writeUtf(template.enumClass.name)
        }

        @Suppress("unchecked_cast")
        override fun deserializeFromNetwork(buffer: FriendlyByteBuf): Template {
            return Template(Class.forName(buffer.readUtf()) as Class<out Enum<*>>)
        }

        override fun serializeToJson(template: Template, json: JsonObject) {
            json.addProperty("enum", template.enumClass.name)
        }

        @Suppress("unchecked_cast")
        override fun unpack(argument: LowerCamelCaseEnumArgument<*>): Template {
            return Template(argument.enumClass)
        }

        inner class Template(val enumClass: Class<out Enum<*>>) :
            ArgumentTypeInfo.Template<LowerCamelCaseEnumArgument<*>> {
            @Suppress("unchecked_cast")
            override fun instantiate(pStructure: CommandBuildContext): LowerCamelCaseEnumArgument<*> {
                return LowerCamelCaseEnumArgument(this.enumClass as Class<Nothing>)
            }

            override fun type() = this@Info
        }
    }

    companion object {
        private val INVALID_ENUM = Dynamic2CommandExceptionType { found, constants ->
            Component.translatable("commands.forge.arguments.enum.invalid", constants, found)
        }

        fun <T : Enum<T>> enumArgument(enumClass: Class<T>) = LowerCamelCaseEnumArgument(enumClass)
    }
}
