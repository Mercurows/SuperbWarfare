package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.item.Monitor;
import com.atsuishio.superbwarfare.tools.DronesTool;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

/**
 * Code based on @Mafuyu404's <a href="https://github.com/Mafuyu404/DiligentStalker">DiligentStalker</a>
 */
//@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID, value = Dist.CLIENT)
public class DroneEventHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().screen != null) return;

        var player = Minecraft.getInstance().player;
        UUID entityUUID = Monitor.getDroneUUID(player);
        if (entityUUID != null) {
            ClientLevel level = player.clientLevel;
            level.entitiesForRendering().forEach(entity -> {
                if (entity.getUUID().equals(entityUUID)) {
                    DronesTool.connect(player, entity);
                }
            });
        }
        if (!DronesTool.hasInstanceOf(player)) return;
        DronesTool instance = DronesTool.getInstanceOf(player);
        Entity stalker = instance.getDrone();
        // TODO 调整角度？
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getSide().isClient()) {
            var player = event.getEntity();
            if (player.isShiftKeyDown()) return;
            if (event.getItemStack().is(ModItems.MONITOR.get())) {
                DronesTool.connect(player, event.getTarget());
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onInputKey(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) return;
        Player player = Minecraft.getInstance().player;
        if (!DronesTool.hasInstanceOf(player)) return;
        if (event.getAction() == InputConstants.PRESS) {
            if (event.getKey() == ModKeyMappings.DISMOUNT.getKey().getValue()) {
                if (DronesTool.hasInstanceOf(player)) {
                    DronesTool.getInstanceOf(player).disconnect();
                }
            }
        }
    }
}
