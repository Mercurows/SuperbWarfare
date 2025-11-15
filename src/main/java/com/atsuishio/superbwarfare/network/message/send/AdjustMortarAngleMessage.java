package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.SoundTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.entity.vehicle.MortarEntity.TARGET_PITCH;

public class AdjustMortarAngleMessage {

    private final double scroll;

    public AdjustMortarAngleMessage(double scroll) {
        this.scroll = scroll;
    }

    public static void encode(AdjustMortarAngleMessage message, FriendlyByteBuf byteBuf) {
        byteBuf.writeDouble(message.scroll);
    }

    public static AdjustMortarAngleMessage decode(FriendlyByteBuf byteBuf) {
        return new AdjustMortarAngleMessage(byteBuf.readDouble());
    }

    public static void handler(AdjustMortarAngleMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }

            Entity looking = TraceTool.findLookingEntity(player, 6);
            if (looking == null) return;

            if (looking instanceof MortarEntity mortar) {
                mortar.getEntityData().set(TARGET_PITCH, (float) Mth.clamp(mortar.getEntityData().get(TARGET_PITCH) + 0.5 * message.scroll, -89, -20));
            }

            SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f);
        });
        context.get().setPacketHandled(true);
    }
}
