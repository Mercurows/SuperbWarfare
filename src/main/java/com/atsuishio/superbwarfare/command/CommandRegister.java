package com.atsuishio.superbwarfare.command;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Mod.MODID)
public class CommandRegister {
    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        var command = Commands.literal("sbw");
        command.then(AmmoCommand.get());
        command.then(ConfigCommand.get());

        event.getDispatcher().register(command);
    }
}
