package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FiringParametersEditMessage {

    private final int posX;
    private final int posY;
    private final int posZ;
    private final int radius;
    private final boolean isDepressed;
    private final boolean mainHand;

    public FiringParametersEditMessage(int posX, int posY, int posZ, int radius, boolean isDepressed, boolean mainHand) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.radius = radius;
        this.isDepressed = isDepressed;
        this.mainHand = mainHand;
    }

    public static void encode(FiringParametersEditMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.posX);
        buffer.writeInt(message.posY);
        buffer.writeInt(message.posZ);
        buffer.writeInt(message.radius);
        buffer.writeBoolean(message.isDepressed);
        buffer.writeBoolean(message.mainHand);
    }

    public static FiringParametersEditMessage decode(FriendlyByteBuf buffer) {
        return new FiringParametersEditMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handler(FiringParametersEditMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = ctx.get().getSender();
            if (serverPlayer == null) return;

            ItemStack stack = message.mainHand ? serverPlayer.getMainHandItem() : serverPlayer.getOffhandItem();
            if (!stack.is(ModItems.FIRING_PARAMETERS.get()) || !stack.is(ModItems.ARTILLERY_INDICATOR.get())) return;

            stack.getOrCreateTag().putInt("TargetX", message.posX);
            stack.getOrCreateTag().putInt("TargetY", message.posY);
            stack.getOrCreateTag().putInt("TargetZ", message.posZ);
            stack.getOrCreateTag().putInt("Radius", message.radius);
            stack.getOrCreateTag().putBoolean("IsDepressed", message.isDepressed);
        });
        ctx.get().setPacketHandled(true);
    }
}
