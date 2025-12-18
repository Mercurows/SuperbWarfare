package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SensitivityMessage {

    private final boolean add;

    public SensitivityMessage(boolean add) {
        this.add = add;
    }

    public static void encode(SensitivityMessage message, FriendlyByteBuf byteBuf) {
        byteBuf.writeBoolean(message.add);
    }

    public static SensitivityMessage decode(FriendlyByteBuf byteBuf) {
        return new SensitivityMessage(byteBuf.readBoolean());
    }

    public static void handler(SensitivityMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem)) return;

            var data = GunData.from(stack);
            if (message.add) {
                data.sensitivity.set(Math.min(10, data.sensitivity.get() + 1));
            } else {
                data.sensitivity.set(Math.max(-10, data.sensitivity.get() - 1));
            }
            data.save();
            player.displayClientMessage(Component.translatable("tips.superbwarfare.sensitivity", data.sensitivity.get()), true);
        });
        context.get().setPacketHandled(true);
    }
}
