package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.inventory.menu.ReforgingTableMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public enum GunReforgeMessage {
    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            AbstractContainerMenu abstractcontainermenu = player.containerMenu;
            if (abstractcontainermenu instanceof ReforgingTableMenu menu) {
                if (!menu.stillValid(player)) {
                    return;
                }
                menu.generateResult();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
