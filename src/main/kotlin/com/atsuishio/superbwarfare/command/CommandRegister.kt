package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.Mod
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
object CommandRegister {
    @SubscribeEvent
    fun registerCommand(event: RegisterCommandsEvent) {
        val command = buildCommand("sbw") {
            add(AMMO_COMMAND)
            add(CONFIG_COMMAND)
            add(TDM_COMMAND)
            add(RIDE_COMMAND)
            add(DISMOUNT_COMMAND)
            add(SKIN_COMMAND)
            add(LOITER_COMMAND)
        }

        val result = event.dispatcher.register(command as LiteralArgumentBuilder<CommandSourceStack>)
        event.dispatcher.register(Commands.literal("superbwarfare").redirect(result))
    }
}
