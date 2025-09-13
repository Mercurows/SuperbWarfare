package com.atsuishio.superbwarfare.command;

import com.atsuishio.superbwarfare.entity.mixin.ModTeam;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.Component;

public class TestCommand {

    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_TDM_ENABLED =
            new SimpleCommandExceptionType(Component.translatable("commands.team.option.sbw_enable_death_match.already_enabled"));
    private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_TDM_DISABLED =
            new SimpleCommandExceptionType(Component.translatable("commands.team.option.sbw_enable_death_match.already_disabled"));

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("team").requires(s -> s.hasPermission(2))
                .then(Commands.literal("modify").then(Commands.argument("team", TeamArgument.team())
                                .then(Commands.literal("sbwEnableDeathMatch").then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> {
                                            var originalTeam = TeamArgument.getTeam(context, "team");
                                            var team = ModTeam.of(originalTeam);
                                            var flag = BoolArgumentType.getBool(context, "enabled");

                                            if (team.superbWarfare$isDeathMatchEnabled() == flag) {
                                                if (flag) {
                                                    throw ERROR_TEAM_ALREADY_TDM_ENABLED.create();
                                                } else {
                                                    throw ERROR_TEAM_ALREADY_TDM_DISABLED.create();
                                                }
                                            } else {
                                                team.superbWarfare$setDeathMatch(flag);
                                                context.getSource().sendSuccess(
                                                        () -> Component.translatable("commands.team.option.sbw_enable_death_match." + (flag ? "enabled" : "disabled"), originalTeam.getFormattedDisplayName()),
                                                        true
                                                );
                                                return 0;
                                            }
                                        }))
                                )
                        )
                );
    }
}
