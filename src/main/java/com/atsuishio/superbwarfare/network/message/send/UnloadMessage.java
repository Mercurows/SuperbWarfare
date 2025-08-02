package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public enum UnloadMessage implements CustomPacketPayload {
    INSTANCE;

    public static final Type<UnloadMessage> TYPE = new Type<>(Mod.loc("unload"));

    public static final StreamCodec<ByteBuf, UnloadMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handler(final IPayloadContext context) {
        var player = context.player();

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        GunData data = GunData.from(stack);
        data.withdrawAmmo(player);
        SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
