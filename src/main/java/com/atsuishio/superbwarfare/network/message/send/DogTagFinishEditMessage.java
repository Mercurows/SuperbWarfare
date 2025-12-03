package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DogTagFinishEditMessage {

    private final short[][] colors;
    private final String name;
    private final boolean mainHand;

    public DogTagFinishEditMessage(short[][] colors, String name, boolean mainHand) {
        this.colors = colors;
        this.name = name;
        this.mainHand = mainHand;
    }

    public static void encode(DogTagFinishEditMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(Mth.clamp(message.colors.length, 0, 16));
        for (short[] color : message.colors) {
            buffer.writeVarInt(Mth.clamp(color.length, 0, 16));
            for (short c : color) {
                buffer.writeShort(c);
            }
        }
        buffer.writeUtf(message.name);
        buffer.writeBoolean(message.mainHand);
    }

    public static DogTagFinishEditMessage decode(FriendlyByteBuf buffer) {
        int rows = buffer.readVarInt();
        if (rows < 0 || rows > 16) {
            throw new IllegalArgumentException("Invalid row count: " + rows);
        }

        short[][] colors = new short[rows][];
        for (int i = 0; i < colors.length; i++) {
            int columns = buffer.readVarInt();
            if (columns < 0 || columns > 16) {
                throw new IllegalArgumentException("Invalid column count: " + columns);
            }

            colors[i] = new short[columns];
            for (int j = 0; j < colors[i].length; j++) {
                colors[i][j] = buffer.readShort();
            }
        }
        String name = buffer.readUtf();
        boolean mainHand = buffer.readBoolean();
        return new DogTagFinishEditMessage(colors, name, mainHand);
    }

    public static void handler(DogTagFinishEditMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = ctx.get().getSender();
            if (serverPlayer == null) return;

            ItemStack stack = message.mainHand ? serverPlayer.getMainHandItem() : serverPlayer.getOffhandItem();
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
