package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record WeaponZoomingMessage(boolean zooming) implements CustomPacketPayload {
    public static final Type<WeaponZoomingMessage> TYPE = new Type<>(Mod.loc("seeking_weapon_warning"));

    public static final StreamCodec<ByteBuf, WeaponZoomingMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            WeaponZoomingMessage::zooming,
            WeaponZoomingMessage::new
    );

    public static void handler(WeaponZoomingMessage message, final IPayloadContext context) {
        ItemStack stack = context.player().getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);
        data.zooming.set(message.zooming);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
