package com.atsuishio.superbwarfare.command;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
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
                })))
                .then(Commands.literal("collisionDestroy").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("none").executes(context -> {
                            VehicleConfig.COLLISION_DESTROY_BLOCKS.set(false);
                            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.set(false);
                            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.set(false);

                            VehicleConfig.COLLISION_DESTROY_BLOCKS.save();

                            context.getSource().sendSuccess(() -> Component.translatable("commands.config.collision_destroy.none"), true);
                            return 0;
                        }))
                        .then(Commands.literal("soft").executes(context -> {
                            VehicleConfig.COLLISION_DESTROY_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.set(false);
                            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.set(false);

                            VehicleConfig.COLLISION_DESTROY_BLOCKS.save();
                            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.save();
                            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.save();

                            context.getSource().sendSuccess(() -> Component.translatable("commands.config.collision_destroy.soft"), true);
                            return 0;
                        }))
                        .then(Commands.literal("hard").executes(context -> {
                            VehicleConfig.COLLISION_DESTROY_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.set(false);

                            VehicleConfig.COLLISION_DESTROY_BLOCKS.save();
                            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.save();
                            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.save();

                            context.getSource().sendSuccess(() -> Component.translatable("commands.config.collision_destroy.hard"), true);
                            return 0;
                        }))
                        .then(Commands.literal("beastly").executes(context -> {
                            VehicleConfig.COLLISION_DESTROY_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.set(true);

                            VehicleConfig.COLLISION_DESTROY_BLOCKS.save();
                            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.save();
                            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.save();

                            context.getSource().sendSuccess(() -> Component.translatable("commands.config.collision_destroy.beastly"), true);
                            return 0;
                        }))
                )
                .then(Commands.literal("tacticalSprint").requires(s -> s.hasPermission(2)).then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
                    var value = BoolArgumentType.getBool(context, "value");
                    MiscConfig.ALLOW_TACTICAL_SPRINT.set(value);
                    MiscConfig.ALLOW_TACTICAL_SPRINT.save();

                    context.getSource().sendSuccess(() -> Component.translatable(value ? "commands.config.tactical_sprint.enabled" : "commands.config.tactical_sprint.disabled"), true);
                    return 0;
                })));
    }
}
