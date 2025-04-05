package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.GunsTool;
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
        if (!stack.is(ModTags.Items.GUN)) return;
        var data = GunData.from(stack);
        var tag = data.tag();

        if (stack.is(ModItems.MINIGUN.get())) {
            double minRpm = 300 - 1200;
            double maxRpm = 2400 - 1200;

            GunsTool.setGunIntTag(tag, "CustomRPM", (int) Mth.clamp(GunsTool.getGunIntTag(tag, "CustomRPM") + 50 * message.scroll, minRpm, maxRpm));
            if (GunsTool.getGunIntTag(tag, "CustomRPM") == 1150 - 1200) {
                GunsTool.setGunIntTag(tag, "CustomRPM", 1145 - 1200);
            }

            if (GunsTool.getGunIntTag(tag, "CustomRPM") == 1195 - 1200) {
                GunsTool.setGunIntTag(tag, "CustomRPM", 0);
            }

            if (GunsTool.getGunIntTag(tag, "CustomRPM") == 1095 - 1200) {
                GunsTool.setGunIntTag(tag, "CustomRPM", 1100 - 1200);
            }
            player.displayClientMessage(Component.literal("RPM: " + FormatTool.format0D(GunsTool.getGunIntTag(tag, "CustomRPM") + 1200)), true);
            int rpm = GunsTool.getGunIntTag(tag, "CustomRPM");
            if (rpm > minRpm && rpm < maxRpm) {
                SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f);
            }
        } else {
            double minZoom = data.minZoom() - 1.25;
            double maxZoom = data.maxZoom() - 1.25;
            double customZoom = GunsTool.getGunDoubleTag(tag, "CustomZoom");
            GunsTool.setGunDoubleTag(tag, "CustomZoom", Mth.clamp(customZoom + 0.5 * message.scroll, minZoom, maxZoom));
            if (GunsTool.getGunDoubleTag(tag, "CustomZoom") > minZoom &&
                    GunsTool.getGunDoubleTag(tag, "CustomZoom") < maxZoom) {
                SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f);
            }
        }
        data.save();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
