package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
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

        if (stack.is(ModItems.MINIGUN.get())) {
            double minRpm = 300;
            double maxRpm = 2400;

            GunsTool.setGunIntTag(stack, "RPM", (int) Mth.clamp(GunsTool.getGunIntTag(stack, "RPM", 0) + 50 * message.scroll, minRpm, maxRpm));
            if (GunsTool.getGunIntTag(stack, "RPM", 0) == 1150) {
                GunsTool.setGunIntTag(stack, "RPM", 1145);
            }

            if (GunsTool.getGunIntTag(stack, "RPM", 0) == 1195) {
                GunsTool.setGunIntTag(stack, "RPM", 1200);
            }

            if (GunsTool.getGunIntTag(stack, "RPM", 0) == 1095) {
                GunsTool.setGunIntTag(stack, "RPM", 1100);
            }
            player.displayClientMessage(Component.literal("RPM: " + FormatTool.format0D(GunsTool.getGunIntTag(stack, "RPM", 0))), true);
            int rpm = GunsTool.getGunIntTag(stack, "RPM", 0);
            if (rpm > minRpm && rpm < maxRpm) {
                SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f);
            }
        } else {
            double minZoom = GunsTool.getGunDoubleTag(stack, "MinZoom", 0) - 1.25;
            double maxZoom = GunsTool.getGunDoubleTag(stack, "MaxZoom", 0) - 1.25;
            double customZoom = GunsTool.getGunDoubleTag(stack, "CustomZoom", 0);
            GunsTool.setGunDoubleTag(stack, "CustomZoom", Mth.clamp(customZoom + 0.5 * message.scroll, minZoom, maxZoom));
            if (GunsTool.getGunDoubleTag(stack, "CustomZoom", 0) > minZoom &&
                    GunsTool.getGunDoubleTag(stack, "CustomZoom", 0) < maxZoom) {
                SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f);
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
