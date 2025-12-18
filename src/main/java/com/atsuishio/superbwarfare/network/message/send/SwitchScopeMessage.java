package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SwitchScopeMessage {

    private final double scroll;

    public SwitchScopeMessage(double scroll) {
        this.scroll = scroll;
    }

    public static void encode(SwitchScopeMessage message, FriendlyByteBuf byteBuf) {
        byteBuf.writeDouble(message.scroll);
    }

    public static SwitchScopeMessage decode(FriendlyByteBuf byteBuf) {
        return new SwitchScopeMessage(byteBuf.readDouble());
    }

    public static void handler(SwitchScopeMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem)) return;

            var data = GunData.from(stack);
            var tag = data.tag();
            tag.putBoolean("ScopeAlt", !tag.getBoolean("ScopeAlt"));
            data.save();
        });
        context.get().setPacketHandled(true);
    }

}
