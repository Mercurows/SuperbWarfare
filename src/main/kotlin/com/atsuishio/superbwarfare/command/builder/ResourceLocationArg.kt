package com.atsuishio.superbwarfare.command.builder

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.ResourceLocationArgument
import net.minecraft.resources.ResourceLocation

class CommandNodeWithResourceLocationArg(builder: ArgumentBuilder<CommandSourceStack, *>, argName: String) :
    CommandNodeWithArg<ResourceLocation>(builder, argName) {

    val CommandContext<CommandSourceStack>.resourceLocationArg
        get() = getArg(this@CommandNodeWithResourceLocationArg)

    override fun CommandContext<CommandSourceStack>.getArg(
        ctx: CommandNodeWithArg<ResourceLocation>
    ): ResourceLocation = ResourceLocationArgument.getId(this, ctx.name)
}

inline fun CommandNode.resourceLocationArg(
    argName: String = "$name.resourceLocation",
    suggests: SuggestionProvider<CommandSourceStack>? = null,
    builder: CommandNodeWithResourceLocationArg.() -> Unit
) {
    val argument = Commands.argument(argName, ResourceLocationArgument.id())
    suggests?.let { argument.suggests(it) }
    cmd += CommandNodeWithResourceLocationArg(argument, argName).apply(builder)
}
