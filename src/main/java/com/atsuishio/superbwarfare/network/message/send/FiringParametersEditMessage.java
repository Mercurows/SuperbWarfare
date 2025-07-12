package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.item.ArtilleryIndicator.TAG_CANNON;

public class FiringParametersEditMessage {

    private final int posX;
    private final int posY;
    private final int posZ;
    private final int radius;
    private final boolean isDepressed;
    private final boolean mainHand;

    public FiringParametersEditMessage(int posX, int posY, int posZ, int radius, boolean isDepressed, boolean mainHand) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.radius = radius;
        this.isDepressed = isDepressed;
        this.mainHand = mainHand;
    }

    public static void encode(FiringParametersEditMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.posX);
        buffer.writeInt(message.posY);
        buffer.writeInt(message.posZ);
        buffer.writeInt(message.radius);
        buffer.writeBoolean(message.isDepressed);
        buffer.writeBoolean(message.mainHand);
    }

    public static FiringParametersEditMessage decode(FriendlyByteBuf buffer) {
        return new FiringParametersEditMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handler(FiringParametersEditMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = ctx.get().getSender();
            if (serverPlayer == null) return;

            ItemStack stack = message.mainHand ? serverPlayer.getMainHandItem() : serverPlayer.getOffhandItem();
            if (!stack.is(ModItems.FIRING_PARAMETERS.get()) && !stack.is(ModItems.ARTILLERY_INDICATOR.get())) return;

            stack.getOrCreateTag().putInt("TargetX", message.posX);
            stack.getOrCreateTag().putInt("TargetY", message.posY);
            stack.getOrCreateTag().putInt("TargetZ", message.posZ);
            stack.getOrCreateTag().putInt("Radius", message.radius);
            stack.getOrCreateTag().putBoolean("IsDepressed", message.isDepressed);

            ListTag tags = stack.getOrCreateTag().getList(TAG_CANNON, Tag.TAG_COMPOUND);
            for (int i = 0; i < tags.size(); i++) {
                var tag = tags.getCompound(i);
                Entity entity = EntityFindUtil.findEntity(serverPlayer.level(), tag.getString("UUID"));
                if (entity instanceof MortarEntity mortarEntity) {
                    if (!mortarEntity.setTarget(stack)) {
                        serverPlayer.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn").withStyle(ChatFormatting.RED), true);
                    }
                }
                if (entity instanceof Mk42Entity mk42Entity) {
                    if (!mk42Entity.setTarget(stack)) {
                        serverPlayer.displayClientMessage(Component.translatable("tips.superbwarfare.mk_42.warn").withStyle(ChatFormatting.RED), true);
                    }
                }
                if (entity instanceof Mle1934Entity mle1934Entity) {
                    if (!mle1934Entity.setTarget(stack)) {
                        serverPlayer.displayClientMessage(Component.translatable("tips.superbwarfare.mle_1934.warn").withStyle(ChatFormatting.RED), true);
                    }
                }
            }

        });
        ctx.get().setPacketHandled(true);
    }
}
