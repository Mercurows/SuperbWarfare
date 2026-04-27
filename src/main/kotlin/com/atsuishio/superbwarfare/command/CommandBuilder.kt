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
import kotlin.reflect.KClass

// 这才是真正的Builder！
open class SingleCommand(val argumentBuilder: ArgumentBuilder<CommandSourceStack, *>, val name: String = "default") {
    val cmd: MutableList<SingleCommand> = mutableListOf()

    fun execute(executor: CommandContext<CommandSourceStack>.() -> Int) {
        this.argumentBuilder.executes(executor)
    }

    fun requires(executor: CommandSourceStack.() -> Boolean) {
        this.argumentBuilder.requires(executor)
    }

    fun requirePermission(level: Int) = requires { hasPermission(level) }

    // literal命令
    inline operator fun String.invoke(builder: SingleCommand.() -> Unit) {
        cmd += SingleCommand(Commands.literal(this), "$name.$this").apply(builder)
    }

    // 直接添加已有的ArgumentBuilder
    fun add(argument: ArgumentBuilder<CommandSourceStack, *>) {
        cmd += SingleCommand(argument, "$name.${cmd.size}")
    }

    // args
    inline fun <A> arg(argName: String = "$name.arg", type: ArgumentType<A>, builder: SingleCommand.() -> Unit) {
        cmd += SingleCommand(Commands.argument(argName, type), argName).apply(builder)
    }

    inline fun playerArg(argName: String = "$name.player", builder: CommandWithPlayerArg.() -> Unit) {
        cmd += CommandWithPlayerArg(Commands.argument(argName, EntityArgument.player()), argName).apply(builder)
    }

    inline fun playersArg(argName: String = "$name.players", builder: CommandWithPlayersArg.() -> Unit) {
        cmd += CommandWithPlayersArg(Commands.argument(argName, EntityArgument.players()), argName).apply(builder)
    }

    inline fun entityArg(argName: String = "$name.entity", builder: CommandWithEntityArg.() -> Unit) {
        cmd += CommandWithEntityArg(Commands.argument(argName, EntityArgument.entity()), argName).apply(builder)
    }

    inline fun entitiesArg(argName: String = "$name.entities", builder: CommandWithEntitiesArg.() -> Unit) {
        cmd += CommandWithEntitiesArg(Commands.argument(argName, EntityArgument.entities()), argName).apply(builder)
    }

    inline fun <reified T : Enum<T>> enumArg(
        argName: String = "$name.${T::class.simpleName}",
        noinline builder: CommandWithEnumArg<T>.() -> Unit
    ) {
        cmd += CommandWithEnumArg(
            Commands.argument(argName, LowerCamelCaseEnumArgument.enumArgument(T::class.java)),
            argName,
            T::class
        ).apply(builder)
    }

    inline fun intArg(
        argName: String = "$name.int",
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        builder: CommandWithIntArg.() -> Unit
    ) {
        cmd += CommandWithIntArg(Commands.argument(argName, IntegerArgumentType.integer(min, max)), argName).apply(
            builder
        )
    }

    inline fun boolArg(argName: String = "$name.bool", builder: CommandWithBoolArg.() -> Unit) {
        cmd += CommandWithBoolArg(Commands.argument(argName, BoolArgumentType.bool()), argName).apply(builder)
    }

    fun build(): ArgumentBuilder<CommandSourceStack, *> = run {
        cmd.map { it.build() }.forEach { this.argumentBuilder.then(it) }
        this.argumentBuilder
    }

    // execute部分
    // 有逆天property和getter后似乎不需要用这些了（
//    inline fun <reified T> CommandContext<CommandSourceStack>.getArgument(name: String): T =
//        this.getArgument(name, T::class.java)
//
//    fun CommandContext<CommandSourceStack>.getPlayer(name: String): ServerPlayer =
//        EntityArgument.getPlayer(this, name)
//
//    fun CommandContext<CommandSourceStack>.getPlayers(name: String): Collection<ServerPlayer> =
//        EntityArgument.getPlayers(this, name)
//
//    fun CommandContext<CommandSourceStack>.getEntity(name: String): Entity =
//        EntityArgument.getEntity(this, name)
//
//    fun CommandContext<CommandSourceStack>.getEntities(name: String): Collection<Entity> =
//        EntityArgument.getEntities(this, name)
//
//    fun CommandContext<CommandSourceStack>.getInt(name: String) =
//        IntegerArgumentType.getInteger(this, name)
//
//    fun CommandContext<CommandSourceStack>.getBool(name: String) =
//        BoolArgumentType.getBool(this, name)

    fun CommandContext<CommandSourceStack>.success(allowLogging: Boolean = true, msg: () -> Component) {
        source.success(allowLogging, msg)
    }

    fun CommandContext<CommandSourceStack>.fail(msg: () -> Component) {
        fail(msg())
    }

    fun CommandContext<CommandSourceStack>.fail(msg: Component) {
        source.sendFailure(msg)
    }

    // 通知
    fun CommandSourceStack.success(allowLogging: Boolean = true, msg: () -> Component) =
        this.sendSuccess(msg, allowLogging)
}

class CommandWithPlayerArg(val builder: ArgumentBuilder<CommandSourceStack, *>, val argName: String) :
    SingleCommand(builder, argName) {

    val CommandContext<CommandSourceStack>.player get() = getArg(this@CommandWithPlayerArg)
    fun CommandContext<CommandSourceStack>.getArg(ctx: CommandWithPlayerArg): ServerPlayer =
        EntityArgument.getPlayer(this, ctx.argName)
}

class CommandWithPlayersArg(val builder: ArgumentBuilder<CommandSourceStack, *>, val argName: String) :
    SingleCommand(builder, argName) {

    val CommandContext<CommandSourceStack>.players get() = getArg(this@CommandWithPlayersArg)
    fun CommandContext<CommandSourceStack>.getArg(ctx: CommandWithPlayersArg): Collection<ServerPlayer> =
        EntityArgument.getPlayers(this, ctx.argName)
}

class CommandWithEntityArg(val builder: ArgumentBuilder<CommandSourceStack, *>, val argName: String) :
    SingleCommand(builder, argName) {

    val CommandContext<CommandSourceStack>.entity get() = getArg(this@CommandWithEntityArg)
    fun CommandContext<CommandSourceStack>.getArg(ctx: CommandWithEntityArg): Entity =
        EntityArgument.getEntity(this, ctx.argName)
}

class CommandWithEntitiesArg(val builder: ArgumentBuilder<CommandSourceStack, *>, val argName: String) :
    SingleCommand(builder, argName) {

    val CommandContext<CommandSourceStack>.entities get() = getArg(this@CommandWithEntitiesArg)
    fun CommandContext<CommandSourceStack>.getArg(ctx: CommandWithEntitiesArg): Collection<Entity> =
        EntityArgument.getEntities(this, ctx.argName)
}

class CommandWithEnumArg<T : Enum<T>>(
    val builder: ArgumentBuilder<CommandSourceStack, *>,
    val argName: String,
    val type: KClass<T>
) : SingleCommand(builder, argName) {

    val CommandContext<CommandSourceStack>.enumArg: T get() = getArgument(argName, type.java)
    inline fun <reified E : Enum<E>> CommandContext<CommandSourceStack>.getArg(ctx: CommandWithEnumArg<E>): E =
        getArgument(ctx.argName, E::class.java)
}

class CommandWithIntArg(val builder: ArgumentBuilder<CommandSourceStack, *>, val argName: String) :
    SingleCommand(builder, argName) {

    val CommandContext<CommandSourceStack>.intArg get() = getArg(this@CommandWithIntArg)
    fun CommandContext<CommandSourceStack>.getArg(ctx: CommandWithIntArg) =
        IntegerArgumentType.getInteger(this, ctx.argName)
}

class CommandWithBoolArg(val builder: ArgumentBuilder<CommandSourceStack, *>, val argName: String) :
    SingleCommand(builder, argName) {

    val CommandContext<CommandSourceStack>.boolArg get() = getArg(this@CommandWithBoolArg)
    fun CommandContext<CommandSourceStack>.getArg(ctx: CommandWithBoolArg) =
        BoolArgumentType.getBool(this, ctx.argName)
}

fun buildCommand(name: String, builder: SingleCommand.() -> Unit) = run {
    SingleCommand(Commands.literal(name), name).apply(builder).build()
}