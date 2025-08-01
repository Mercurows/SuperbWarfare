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

public record EditMessage(int msgType,boolean add) implements CustomPacketPayload {
    public static final Type<EditMessage> TYPE = new Type<>(Mod.loc("edit"));

    public static final StreamCodec<ByteBuf, EditMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            EditMessage::msgType,
            ByteBufCodecs.BOOL,
            EditMessage::add,
            EditMessage::new
    );

    public static void handler(EditMessage message, final IPayloadContext context) {
        pressAction(context.player(), message);
    }

    public static void pressAction(Player player, EditMessage message) {
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        switch (message.msgType) {
            case 0 -> {
                int att = data.attachment.get(AttachmentType.BARREL);
                if (message.add) {
                    att = (att + 1) % 3;
                } else {
                    att = (att + 3 - 1) % 3;
                }
                data.attachment.set(AttachmentType.BARREL, att);
            }
            case 1 -> {
                int att = data.attachment.get(AttachmentType.SCOPE);
                if (message.add) {
                    att = (att + 1) % 4;
                } else {
                    att = (att + 4 - 1) % 4;
                }
                data.attachment.set(AttachmentType.SCOPE, att);
            }
            case 3 -> {
                int att = data.attachment.get(AttachmentType.STOCK);
                if (message.add) {
                    att = (att + 1) % 3;
                } else {
                    att = (att + 3 - 1) % 3;
                }
                data.attachment.set(AttachmentType.STOCK, att);
            }
            case 2 -> {
                int att = data.attachment.get(AttachmentType.GRIP);
                if (message.add) {
                    att = (att + 1) % 4;
                } else {
                    att = (att + 4 - 1) % 4;
                }
                data.attachment.set(AttachmentType.GRIP, att);
            }
            case 4 -> {
                int att = data.attachment.get(AttachmentType.MAGAZINE);
                if (message.add) {
                    att = (att + 1) % 3;
                } else {
                    att = (att + 3 - 1) % 3;
                }
                data.withdrawAmmo(player);
                data.attachment.set(AttachmentType.MAGAZINE, att);
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


