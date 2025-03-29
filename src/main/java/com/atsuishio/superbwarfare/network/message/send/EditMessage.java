package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
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
        if (!stack.is(ModTags.Items.GUN)) return;

        var rootTag = NBTTool.getTag(stack);
        CompoundTag tag = rootTag.getCompound("Attachments");
        switch (type) {
            case 0 -> {
                int att = tag.getInt("Scope");
                att++;
                att %= 4;
                tag.putInt("Scope", att);
            }
            case 1 -> {
                int att = tag.getInt("Barrel");
                att++;
                att %= 3;
                tag.putInt("Barrel", att);
            }
            case 2 -> {
                int att = tag.getInt("Magazine");
                att++;
                att %= 3;
                tag.putInt("Magazine", att);
            }
            case 3 -> {
                int att = tag.getInt("Stock");
                att++;
                att %= 3;
                tag.putInt("Stock", att);
            }
            case 4 -> {
                int att = tag.getInt("Grip");
                att++;
                att %= 4;
                tag.putInt("Grip", att);
            }
        }
        rootTag.put("Attachments", tag);
        NBTTool.saveTag(stack, rootTag);
        SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}


