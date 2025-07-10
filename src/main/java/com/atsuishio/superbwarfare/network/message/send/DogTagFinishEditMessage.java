package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DogTagFinishEditMessage {

    private final short[][] colors;
    private final String name;

    public DogTagFinishEditMessage(short[][] colors, String name) {
        this.colors = colors;
        this.name = name;
    }

    public static void encode(DogTagFinishEditMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.colors.length);
        for (short[] color : message.colors) {
            buffer.writeVarInt(color.length);
            for (short c : color) {
                buffer.writeShort(c);
            }
        }
        buffer.writeUtf(message.name);
    }

    public static DogTagFinishEditMessage decode(FriendlyByteBuf buffer) {
        short[][] colors = new short[buffer.readVarInt()][];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = new short[buffer.readVarInt()];
            for (int j = 0; j < colors[i].length; j++) {
                colors[i][j] = buffer.readShort();
            }
        }
        String name = buffer.readUtf();
        return new DogTagFinishEditMessage(colors, name);
    }

    public static void handler(DogTagFinishEditMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = ctx.get().getSender();
            if (serverPlayer == null) return;

            ItemStack stack = serverPlayer.getMainHandItem();
            if (!stack.is(ModItems.DOG_TAG.get())) return;

            CompoundTag colorsTag = new CompoundTag();
            for (int i = 0; i < message.colors.length; i++) {
                int[] color = new int[message.colors[i].length];
                for (int j = 0; j < message.colors[i].length; j++) {
                    color[j] = message.colors[i][j];
                }
                colorsTag.putIntArray("Color" + i, color);
            }
            stack.getOrCreateTag().put("Colors", colorsTag);

            if (!message.name.isEmpty()) {
                stack.setHoverName(Component.literal(message.name));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
