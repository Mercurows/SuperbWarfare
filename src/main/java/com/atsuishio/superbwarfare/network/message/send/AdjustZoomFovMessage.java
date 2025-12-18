package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AdjustZoomFovMessage {

    private final double scroll;

    public AdjustZoomFovMessage(double scroll) {
        this.scroll = scroll;
    }

    public static void encode(AdjustZoomFovMessage message, FriendlyByteBuf byteBuf) {
        byteBuf.writeDouble(message.scroll);
    }

    public static AdjustZoomFovMessage decode(FriendlyByteBuf byteBuf) {
        return new AdjustZoomFovMessage(byteBuf.readDouble());
    }

    public static void handler(AdjustZoomFovMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem)) return;
            var gun = GunData.from(stack);
            var data = gun.data();

            if (stack.is(ModItems.MINIGUN.get())) {
                double minRpm = 300 - 1200;
                double maxRpm = 2400 - 1200;

                var customRPM = data.getInt("CustomRPM");
                var targetCustomRPM = (int) Mth.clamp(customRPM + 50 * message.scroll, minRpm, maxRpm);

                if (targetCustomRPM == 1150 - 1200) {
                    targetCustomRPM = 1145 - 1200;
                } else {
                    targetCustomRPM = Math.toIntExact(Math.round(targetCustomRPM / 50.0) * 50);
                }

                data.putInt("CustomRPM", targetCustomRPM);

                player.displayClientMessage(Component.literal("RPM: " + FormatTool.format0D(customRPM + 1200)), true);
                if (customRPM > minRpm && customRPM < maxRpm) {
                    SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f);
                }
            } else {
                double minZoom = gun.minZoom() - 1.25;
                double maxZoom = gun.maxZoom() - 1.25;
                double customZoom = data.getDouble("CustomZoom");
                data.putDouble("CustomZoom", Mth.clamp(customZoom + 0.5 * message.scroll, minZoom, maxZoom));

                if (customZoom > minZoom && customZoom < maxZoom) {
                    SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f);
                }
            }
            gun.save();
        });
        context.get().setPacketHandled(true);
    }
}
