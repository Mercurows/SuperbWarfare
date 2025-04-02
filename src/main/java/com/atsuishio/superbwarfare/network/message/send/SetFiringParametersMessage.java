package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.item.FiringParameters;
import com.atsuishio.superbwarfare.tools.TraceTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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

import java.util.Objects;

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

        var parameters = stack.get(ModDataComponents.FIRING_PARAMETERS);
        var isDepressed = parameters != null && parameters.isDepressed();

        if (lookAtEntity) {
            stack.set(ModDataComponents.FIRING_PARAMETERS, new FiringParameters.Parameters(lookingEntity.blockPosition(), isDepressed));
        } else {
            stack.set(ModDataComponents.FIRING_PARAMETERS, new FiringParameters.Parameters(new BlockPos((int) hitPos.x, (int) hitPos.y, (int) hitPos.z), isDepressed));
        }

        var pos = Objects.requireNonNull(stack.get(ModDataComponents.FIRING_PARAMETERS)).pos();

        player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.target_pos")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("[" + pos.getX()
                        + "," + pos.getY()
                        + "," + pos.getZ()
                        + "]")), true);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
