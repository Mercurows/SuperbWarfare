package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.ArtilleryIndicator;
import com.atsuishio.superbwarfare.item.FiringParameters;
import com.atsuishio.superbwarfare.item.FiringParametersKt;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FiringParametersEditMessage {

    private final int x;
    private final int y;
    private final int z;
    private final int radius;
    private final boolean isDepressed;
    private final boolean mainHand;

    public FiringParametersEditMessage(int x, int y, int z, int radius, boolean isDepressed, boolean mainHand) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.isDepressed = isDepressed;
        this.mainHand = mainHand;
    }

    public static void encode(FiringParametersEditMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.x);
        buffer.writeInt(message.y);
        buffer.writeInt(message.z);
        buffer.writeInt(message.radius);
        buffer.writeBoolean(message.isDepressed);
        buffer.writeBoolean(message.mainHand);
    }

    public static FiringParametersEditMessage decode(FriendlyByteBuf buffer) {
        return new FiringParametersEditMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handler(FiringParametersEditMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = message.mainHand ? player.getMainHandItem() : player.getOffhandItem();
            if (!stack.is(ModItems.FIRING_PARAMETERS.get()) && !stack.is(ModItems.ARTILLERY_INDICATOR.get())) return;

            FiringParametersKt.setFiringParameters(stack, new FiringParameters.Parameters(new BlockPos(message.x, message.y, message.z), message.radius, message.isDepressed));

            if (stack.getItem() instanceof ArtilleryIndicator indicator) {
                indicator.setTarget(stack, player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
