package com.atsuishio.superbwarfare.command;

import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CommandRegister {
    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        var command = Commands.literal("sbw");
        command.then(AmmoCommand.get());
        command.then(ConfigCommand.get());

        var result = event.getDispatcher().register(command);
        event.getDispatcher().register(Commands.literal("superbwarfare").redirect(result));

        event.getDispatcher().register(TestCommand.get());
    }
}
