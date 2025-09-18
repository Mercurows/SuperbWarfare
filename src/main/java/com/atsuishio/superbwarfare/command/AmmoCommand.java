package com.atsuishio.superbwarfare.command;

import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.data.gun.Ammo;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

public class AmmoCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        // mojang你看看你写的是个牛魔Builder😅
        return Commands.literal("ammo").requires(s -> s.hasPermission(0))
                .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("type", LowerCamelCaseEnumArgument.enumArgument(Ammo.class)).executes(context -> {
                    var player = EntityArgument.getPlayer(context, "player");

                    var source = context.getSource();

                    // 权限不足时，只允许玩家查询自己的弹药数量
                    if (source.isPlayer() && !source.hasPermission(2)) {
                        if (source.getPlayer() != null && !source.getPlayer().getUUID().equals(player.getUUID())) {
                            context.getSource().sendFailure(Component.translatable("commands.ammo.no_permission"));
                            return 0;
                        }
                    }

                    var type = context.getArgument("type", Ammo.class);
                    var value = type.get(player);
                    context.getSource().sendSuccess(() -> Component.translatable("commands.ammo.get", Component.translatable(type.translationKey), value), true);
                    return 0;
                }))))
                .then(Commands.literal("set").requires(s -> s.hasPermission(2)).then(Commands.argument("players", EntityArgument.players()).then(Commands.argument("type", LowerCamelCaseEnumArgument.enumArgument(Ammo.class)).then(Commands.argument("value", IntegerArgumentType.integer(0)).executes(context -> {
                    var players = EntityArgument.getPlayers(context, "players");
                    var type = context.getArgument("type", Ammo.class);
                    var value = IntegerArgumentType.getInteger(context, "value");

                    for (var player : players) {
                        PlayerVariable.modify(player, capability -> type.set(capability, value));
                    }

                    context.getSource().sendSuccess(() -> Component.translatable("commands.ammo.set", Component.translatable(type.translationKey), value, players.size()), true);
                    return 0;
                })))))
                .then(Commands.literal("add").requires(s -> s.hasPermission(2)).then(Commands.argument("players", EntityArgument.players()).then(Commands.argument("type", LowerCamelCaseEnumArgument.enumArgument(Ammo.class)).then(Commands.argument("value", IntegerArgumentType.integer(0)).executes(context -> {
                    var players = EntityArgument.getPlayers(context, "players");
                    var type = context.getArgument("type", Ammo.class);
                    var value = IntegerArgumentType.getInteger(context, "value");

                    for (var player : players) {
                        PlayerVariable.modify(player, capability -> type.add(capability, value));
                    }

                    context.getSource().sendSuccess(() -> Component.translatable("commands.ammo.add", Component.translatable(type.translationKey), value, players.size()), true);
                    return 0;
                })))));
    }
}
