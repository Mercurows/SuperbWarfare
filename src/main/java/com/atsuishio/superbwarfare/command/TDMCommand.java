package com.atsuishio.superbwarfare.command;

import com.atsuishio.superbwarfare.world.TDMSavedData;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.SavedData;

public class TDMCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("tdm").requires(s -> s.hasPermission(2))
                .then(Commands.literal("add").then(Commands.argument("entity", EntityArgument.entities())
                        .executes(context -> {
                            var level = context.getSource().getLevel();
                            var entities = EntityArgument.getEntities(context, "entity");

                            var tdm = level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(TDMSavedData::new, TDMSavedData::load, null), TDMSavedData.FILE_ID);

                            entities.forEach(entity -> tdm.addEntity(entity.getStringUUID()));
                            tdm.sync();

                            if (entities.size() == 1) {
                                context.getSource().sendSuccess(() -> Component.translatable("commands.tdm.add.single", entities.iterator().next()), true);
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable("commands.tdm.add.multiple", entities.size()), true);
                            }

                            return 0;
                        })))
                .then(Commands.literal("remove").then(Commands.argument("entity", EntityArgument.entities())
                        .executes(context -> {
                            var level = context.getSource().getLevel();
                            var entities = EntityArgument.getEntities(context, "entity");

                            var tdm = level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(TDMSavedData::new, TDMSavedData::load, null), TDMSavedData.FILE_ID);

                            entities.forEach(entity -> tdm.removeEntity(entity.getStringUUID()));
                            tdm.sync();

                            if (entities.size() == 1) {
                                context.getSource().sendSuccess(() -> Component.translatable("commands.tdm.remove.single", entities.iterator().next()), true);
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable("commands.tdm.remove.multiple", entities.size()), true);
                            }

                            return 0;
                        })));
    }
}
