package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RadarSetParametersMessage {

    private final byte mode;

    public RadarSetParametersMessage(byte mode) {
        this.mode = mode;
    }

    public static void encode(RadarSetParametersMessage message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.mode);
    }

    public static RadarSetParametersMessage decode(FriendlyByteBuf buffer) {
        return new RadarSetParametersMessage(buffer.readByte());
    }

    public static void handler(RadarSetParametersMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            AbstractContainerMenu menu = player.containerMenu;
            if (menu instanceof FuMO25Menu fuMO25Menu) {
                if (!player.containerMenu.stillValid(player)) {
                    return;
                }
                fuMO25Menu.setPosToParameters();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
