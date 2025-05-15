package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record EditMessage(int msgType) implements CustomPacketPayload {
    public static final Type<EditMessage> TYPE = new Type<>(Mod.loc("edit"));

    public static final StreamCodec<ByteBuf, EditMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            EditMessage::msgType,
            EditMessage::new
    );

    public static void handler(EditMessage message, final IPayloadContext context) {
        pressAction(context.player(), message.msgType);
    }

    public static void pressAction(Player player, int type) {
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        var data = GunData.from(stack);
        var attachment = data.attachment;

        switch (type) {
            case 0 -> {
                int att = attachment.get(AttachmentType.SCOPE);
                att++;
                att %= 4;
                attachment.set(AttachmentType.SCOPE, att);
            }
            case 1 -> {
                int att = attachment.get(AttachmentType.BARREL);
                att++;
                att %= 3;
                attachment.set(AttachmentType.BARREL, att);
            }
            case 2 -> {
                int att = attachment.get(AttachmentType.MAGAZINE);
                att++;
                att %= 3;
                attachment.set(AttachmentType.MAGAZINE, att);
            }
            case 3 -> {
                int att = attachment.get(AttachmentType.STOCK);
                att++;
                att %= 3;
                attachment.set(AttachmentType.STOCK, att);
            }
            case 4 -> {
                int att = attachment.get(AttachmentType.GRIP);
                att++;
                att %= 4;
                attachment.set(AttachmentType.GRIP, att);
            }
        }
        data.save();
        SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}


