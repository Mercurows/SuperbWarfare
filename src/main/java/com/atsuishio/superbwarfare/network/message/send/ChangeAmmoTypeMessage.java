package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ChangeAmmoTypeMessage(int index) implements CustomPacketPayload {
    public static final Type<ChangeAmmoTypeMessage> TYPE = new Type<>(Mod.loc("change_ammo_type"));

    public static final StreamCodec<ByteBuf, ChangeAmmoTypeMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ChangeAmmoTypeMessage::index,
            ChangeAmmoTypeMessage::new
    );

    public static void handler(ChangeAmmoTypeMessage message, final IPayloadContext context) {
        Player player = context.player();

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        var data = GunData.from(stack);
        data.withdrawAmmo(player);
        data.changeAmmoConsumer(message.index);
        data.save();

        player.displayClientMessage(Component.literal("selected index: " + message.index), true);
        SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
