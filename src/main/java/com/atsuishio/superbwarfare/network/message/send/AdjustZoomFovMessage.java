package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record AdjustZoomFovMessage(double scroll) implements CustomPacketPayload {
    public static final Type<AdjustZoomFovMessage> TYPE = new Type<>(Mod.loc("adjust_zoom_fov"));

    public static final StreamCodec<ByteBuf, AdjustZoomFovMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            AdjustZoomFovMessage::scroll,
            AdjustZoomFovMessage::new
    );

    public static void handler(AdjustZoomFovMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var gun = GunData.from(stack);
        var data = gun.data();

        if (stack.is(ModItems.MINIGUN.get())) {
            double minRpm = 300 - 1200;
            double maxRpm = 2400 - 1200;

            var customRPM = data.getInt("CustomRPM");
            data.putInt("CustomRPM", (int) Mth.clamp(customRPM + 50 * message.scroll, minRpm, maxRpm));

            if (customRPM == 1150 - 1200) {
                data.putInt("CustomRPM", 1145 - 1200);
            }

            if (customRPM == 1195 - 1200) {
                data.putInt("CustomRPM", 0);
            }

            if (customRPM == 1095 - 1200) {
                data.putInt("CustomRPM", 1100 - 1200);
            }

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
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
