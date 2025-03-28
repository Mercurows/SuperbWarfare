
package com.atsuishio.superbwarfare.command;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ConfigCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> get() {

        return Commands.literal("config").requires(s -> s.hasPermission(0))
                .then(Commands.literal("explosionDestroy").requires(s -> s.hasPermission(2)).then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
                    var value = BoolArgumentType.getBool(context, "value");
                    ExplosionConfig.EXPLOSION_DESTROY.set(value);
                    ExplosionConfig.EXPLOSION_DESTROY.save();

                    context.getSource().sendSuccess(() -> Component.translatable(value ? "commands.config.explosion_destroy.enabled" : "commands.config.explosion_destroy.disabled"), true);
                    return 0;
                })));
    }
}
