package com.atsuishio.superbwarfare.command;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.config.server.ProjectileConfig;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ClientTacticalSprintSyncMessage;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

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
                            VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.set(false);
                            VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.set(false);
                            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.set(false);
                            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.set(false);

                            saveCollisionConfigs();

                            context.getSource().sendSuccess(() -> Component.translatable("commands.config.collision_destroy.none"), true);
                            return 0;
                        }))
                        .then(Commands.literal("soft").executes(context -> {
                            VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.set(false);
                            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.set(false);
                            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.set(false);

                            saveCollisionConfigs();

                            context.getSource().sendSuccess(() -> Component.translatable("commands.config.collision_destroy.soft"), true);
                            return 0;
                        }))
                        .then(Commands.literal("normal").executes(context -> {
                            VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.set(false);
                            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.set(false);

                            saveCollisionConfigs();

                            context.getSource().sendSuccess(() -> Component.translatable("commands.config.collision_destroy.normal"), true);
                            return 0;
                        }))
                        .then(Commands.literal("hard").executes(context -> {
                            VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.set(false);

                            saveCollisionConfigs();

                            context.getSource().sendSuccess(() -> Component.translatable("commands.config.collision_destroy.hard"), true);
                            return 0;
                        }))
                        .then(Commands.literal("beastly").executes(context -> {
                            VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.set(true);
                            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.set(true);

                            saveCollisionConfigs();

                            context.getSource().sendSuccess(() -> Component.translatable("commands.config.collision_destroy.beastly"), true);
                            return 0;
                        }))
                )
                .then(Commands.literal("tacticalSprint").requires(s -> s.hasPermission(2)).then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
                    var value = BoolArgumentType.getBool(context, "value");
                    MiscConfig.ALLOW_TACTICAL_SPRINT.set(value);
                    MiscConfig.ALLOW_TACTICAL_SPRINT.save();

                    NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new ClientTacticalSprintSyncMessage(value));

                    context.getSource().sendSuccess(() -> Component.translatable(value ? "commands.config.tactical_sprint.enabled" : "commands.config.tactical_sprint.disabled"), true);
                    return 0;
                })))
                .then(Commands.literal("blockDestroy").requires(s -> s.hasPermission(2)).then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
                    var value = BoolArgumentType.getBool(context, "value");
                    ProjectileConfig.ALLOW_PROJECTILE_DESTROY_BLOCKS.set(value);
                    ProjectileConfig.ALLOW_PROJECTILE_DESTROY_BLOCKS.save();

                    context.getSource().sendSuccess(() -> Component.translatable(value ? "commands.config.block_destroy.enabled" : "commands.config.block_destroy.disabled"), true);
                    return 0;
                })))
                .then(Commands.literal("forceDamage").requires(s -> s.hasPermission(2)).then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
                    var value = BoolArgumentType.getBool(context, "value");
                    MiscConfig.ALLOW_FORCE_DAMAGE.set(value);
                    MiscConfig.ALLOW_FORCE_DAMAGE.save();

                    context.getSource().sendSuccess(() -> Component.translatable(value ? "commands.config.force_damage.enabled" : "commands.config.force_damage.disabled"), true);
                    return 0;
                })))
                .then(Commands.literal("dropAmmoBox").requires(s -> s.hasPermission(2)).then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
                    var value = BoolArgumentType.getBool(context, "value");
                    MiscConfig.DROP_AMMO_BOX.set(value);
                    MiscConfig.DROP_AMMO_BOX.save();

                    context.getSource().sendSuccess(() -> Component.translatable(value ? "commands.config.drop_ammo_box.enabled" : "commands.config.drop_ammo_box.disabled"), true);
                    return 0;
                })));
    }

    private static void saveCollisionConfigs() {
        VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.save();
        VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.save();
        VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.save();
        VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.save();
    }
}
