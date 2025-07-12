package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.FiringParameters;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.atsuishio.superbwarfare.item.ArtilleryIndicator.TAG_CANNON;

public record FiringParametersEditMessage(
        int x, int y, int z,
        int radius, boolean isDepressed, boolean mainHand
) implements CustomPacketPayload {
    public static final Type<FiringParametersEditMessage> TYPE = new Type<>(Mod.loc("firing_parameters_edit"));

    public static final StreamCodec<ByteBuf, FiringParametersEditMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, FiringParametersEditMessage::x,
            ByteBufCodecs.INT, FiringParametersEditMessage::y,
            ByteBufCodecs.INT, FiringParametersEditMessage::z,
            ByteBufCodecs.INT, FiringParametersEditMessage::radius,
            ByteBufCodecs.BOOL, FiringParametersEditMessage::isDepressed,
            ByteBufCodecs.BOOL, FiringParametersEditMessage::mainHand,
            FiringParametersEditMessage::new
    );

    public static void handler(FiringParametersEditMessage message, final IPayloadContext context) {
        var player = context.player();

        ItemStack stack = message.mainHand ? player.getMainHandItem() : player.getOffhandItem();
        if (!stack.is(ModItems.FIRING_PARAMETERS.get()) && !stack.is(ModItems.ARTILLERY_INDICATOR.get())) return;

        var parameters = new FiringParameters.Parameters(new BlockPos(message.x, message.y, message.z), message.radius, message.isDepressed);
        stack.set(ModDataComponents.FIRING_PARAMETERS, parameters);

        ListTag tags = NBTTool.getTag(stack).getList(TAG_CANNON, Tag.TAG_COMPOUND);
        for (int i = 0; i < tags.size(); i++) {
            var tag = tags.getCompound(i);
            Entity entity = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"));
            if (entity instanceof MortarEntity mortarEntity) {
                if (!mortarEntity.setTarget(stack)) {
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn").withStyle(ChatFormatting.RED), true);
                }
            }
            if (entity instanceof Mk42Entity mk42Entity) {
                if (!mk42Entity.setTarget(stack)) {
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.mk_42.warn").withStyle(ChatFormatting.RED), true);
                }
            }
            if (entity instanceof Mle1934Entity mle1934Entity) {
                if (!mle1934Entity.setTarget(stack)) {
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.mle_1934.warn").withStyle(ChatFormatting.RED), true);
                }
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
