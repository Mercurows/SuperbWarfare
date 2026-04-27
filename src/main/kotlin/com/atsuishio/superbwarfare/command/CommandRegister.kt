package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.Mod
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.RegisterCommandsEvent

@EventBusSubscriber(modid = Mod.MODID)
object CommandRegister {
    @SubscribeEvent
    fun registerCommand(event: RegisterCommandsEvent) {
        val command = buildCommand("sbw") {
            add(AMMO_COMMAND)
            add(CONFIG_COMMAND)
            add(TDM_COMMAND)
            add(RIDE_COMMAND)
            add(DISMOUNT_COMMAND)
        }

        val result = event.dispatcher.register(command as LiteralArgumentBuilder<CommandSourceStack>)
        event.dispatcher.register(Commands.literal("superbwarfare").redirect(result))
    }
}
