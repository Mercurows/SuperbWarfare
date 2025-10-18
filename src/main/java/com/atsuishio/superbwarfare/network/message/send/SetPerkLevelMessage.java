package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.inventory.menu.ReforgingTableMenu;
import com.atsuishio.superbwarfare.perk.Perk;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetPerkLevelMessage {

    int type;
    boolean add;

    public SetPerkLevelMessage(int type, boolean add) {
        this.type = type;
        this.add = add;
    }

    public static void encode(SetPerkLevelMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
        buffer.writeBoolean(message.add);
    }

    public static SetPerkLevelMessage decode(FriendlyByteBuf buffer) {
        return new SetPerkLevelMessage(buffer.readInt(), buffer.readBoolean());
    }

    public static void handler(SetPerkLevelMessage message, Supplier<NetworkEvent.Context> ctx) {
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

                menu.setPerkLevel(Perk.Type.values()[message.type], message.add, player.getAbilities().instabuild);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
