package com.atsuishio.superbwarfare.command

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity

// 这才是真正的Builder！
class SingleCommand(val argumentBuilder: ArgumentBuilder<CommandSourceStack, *>) {
    val cmd: MutableList<SingleCommand> = mutableListOf()

    fun execute(executor: CommandContext<CommandSourceStack>.() -> Int) {
        this.argumentBuilder.executes(executor)
    }

    fun requires(executor: CommandSourceStack.() -> Boolean) {
        this.argumentBuilder.requires(executor)
    }

    fun requirePermission(level: Int) = requires { hasPermission(level) }

    operator fun String.invoke(builder: SingleCommand.() -> Unit) {
        cmd += SingleCommand(Commands.literal(this)).apply(builder)
    }

    fun add(argument: ArgumentBuilder<CommandSourceStack, *>) {
        cmd += SingleCommand(argument)
    }

    // args
    fun <A> arg(name: String, type: ArgumentType<A>, builder: SingleCommand.() -> Unit) {
        cmd += SingleCommand(Commands.argument(name, type)).apply(builder)
    }

    fun playerArg(name: String, builder: SingleCommand.() -> Unit) {
        arg(name, EntityArgument.player(), builder)
    }

    fun playersArg(name: String, builder: SingleCommand.() -> Unit) {
        arg(name, EntityArgument.players(), builder)
    }

    fun entityArg(name: String, builder: SingleCommand.() -> Unit) {
        arg(name, EntityArgument.entity(), builder)
    }

    fun entitiesArg(name: String, builder: SingleCommand.() -> Unit) {
        arg(name, EntityArgument.entities(), builder)
    }

    inline fun <reified T : Enum<T>> enumArg(name: String, noinline builder: SingleCommand.() -> Unit) {
        arg(name, LowerCamelCaseEnumArgument.enumArgument(T::class.java), builder)
    }

    fun intArg(name: String, builder: SingleCommand.() -> Unit) {
        arg(name, IntegerArgumentType.integer(), builder)
    }

    fun boolArg(name: String, builder: SingleCommand.() -> Unit) {
        arg(name, BoolArgumentType.bool(), builder)
    }

    fun build(): ArgumentBuilder<CommandSourceStack, *> = run {
        cmd.map { it.build() }.forEach { this.argumentBuilder.then(it) }
        this.argumentBuilder
    }

    // execute部分
    inline fun <reified T> CommandContext<CommandSourceStack>.getArgument(name: String): T =
        this.getArgument(name, T::class.java)

    fun CommandContext<CommandSourceStack>.getPlayer(name: String): ServerPlayer =
        EntityArgument.getPlayer(this, name)

    fun CommandContext<CommandSourceStack>.getPlayers(name: String): Collection<ServerPlayer> =
        EntityArgument.getPlayers(this, name)

    fun CommandContext<CommandSourceStack>.getEntity(name: String): Entity =
        EntityArgument.getEntity(this, name)

    fun CommandContext<CommandSourceStack>.getEntities(name: String): Collection<Entity> =
        EntityArgument.getEntities(this, name)

    fun CommandContext<CommandSourceStack>.getInt(name: String) =
        IntegerArgumentType.getInteger(this, name)

    fun CommandContext<CommandSourceStack>.getBool(name: String) =
        BoolArgumentType.getBool(this, name)

    fun CommandSourceStack.success(allowLogging: Boolean = true, msg: () -> Component) =
        this.sendSuccess(msg, allowLogging)
}

fun buildCommand(name: String, builder: SingleCommand.() -> Unit) = run {
    SingleCommand(Commands.literal(name)).apply(builder).build()
}