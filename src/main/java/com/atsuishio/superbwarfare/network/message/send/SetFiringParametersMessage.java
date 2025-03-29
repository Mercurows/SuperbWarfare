package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetFiringParametersMessage(int msgType) implements CustomPacketPayload {
    public static final Type<SetFiringParametersMessage> TYPE = new Type<>(Mod.loc("set_firing_parameters"));

    public static final StreamCodec<ByteBuf, SetFiringParametersMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SetFiringParametersMessage::msgType,
            SetFiringParametersMessage::new
    );

    public static void handler(SetFiringParametersMessage message, final IPayloadContext context) {
        Player player = context.player();
        ItemStack stack = player.getOffhandItem();
        boolean lookAtEntity = false;
        Entity lookingEntity = TraceTool.findLookingEntity(player, 520);

        BlockHitResult result = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getViewVector(1).scale(512)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 hitPos = result.getLocation();

        if (lookingEntity != null) {
            lookAtEntity = true;
        }

        var tag = NBTTool.getTag(stack);
        if (lookAtEntity) {
            tag.putDouble("TargetX", lookingEntity.getX());
            tag.putDouble("TargetY", lookingEntity.getY());
            tag.putDouble("TargetZ", lookingEntity.getZ());
        } else {
            tag.putDouble("TargetX", hitPos.x());
            tag.putDouble("TargetY", hitPos.y());
            tag.putDouble("TargetZ", hitPos.z());
        }
        NBTTool.saveTag(stack, tag);

        player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.target_pos")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("[" + tag.getInt("TargetX")
                        + "," + tag.getInt("TargetY")
                        + "," + tag.getInt("TargetZ")
                        + "]")), true);

    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
