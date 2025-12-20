package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.ArtilleryIndicator;
import com.atsuishio.superbwarfare.item.FiringParameters;
import com.atsuishio.superbwarfare.item.FiringParametersKt;
import com.atsuishio.superbwarfare.tools.SoundTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public enum SetFiringParametersMessage implements CustomPacketPayload {
    INSTANCE;
    public static final Type<SetFiringParametersMessage> TYPE = new Type<>(Mod.loc("set_firing_parameters"));

    public static final StreamCodec<ByteBuf, SetFiringParametersMessage> STREAM_CODEC = StreamCodec.unit(SetFiringParametersMessage.INSTANCE);

    public static void handler(final IPayloadContext context) {
        Player player = context.player();
        ItemStack stack = player.getOffhandItem();
        ItemStack mainStack = player.getMainHandItem();
        boolean lookAtEntity = false;
        Entity lookingEntity = TraceTool.findLookingEntity(player, 520);

            BlockHitResult result = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getViewVector(1).scale(512)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            BlockPos hitPos = result.getBlockPos();

        if (lookingEntity != null && !player.isShiftKeyDown()) {
            lookAtEntity = true;
        }
        if (stack.is(ModItems.FIRING_PARAMETERS.get())) {
            var parameters = FiringParametersKt.getFiringParameters(stack);
            var isDepressed = parameters.isDepressed();
            var radius = parameters.radius();

                if (lookAtEntity) {
                    FiringParametersKt.setFiringParameters(stack, new FiringParameters.Parameters(lookingEntity.blockPosition(), radius, isDepressed));
                } else {
                    FiringParametersKt.setFiringParameters(stack, new FiringParameters.Parameters(hitPos, radius, isDepressed));
                }

            var pos = FiringParametersKt.getFiringParameters(stack).pos();

            player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.target_pos")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("[" + pos.getX()
                            + "," + pos.getY()
                            + "," + pos.getZ()
                            + "]")), true);
        }

            if (mainStack.getItem() instanceof ArtilleryIndicator indicator) {
                BlockPos pos;
                if (lookAtEntity) {
                    pos = BlockPos.containing(lookingEntity.getBoundingBox().getCenter());
                } else {
                    pos = hitPos;
                }
                var parameters = FiringParametersKt.getFiringParameters(mainStack);
                var isDepressed = parameters.isDepressed();
                var radius = parameters.radius();

            FiringParametersKt.setFiringParameters(mainStack, new FiringParameters.Parameters(pos, radius, isDepressed));

            player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.target_pos")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("[" + pos.getX()
                            + "," + pos.getY()
                            + "," + pos.getZ()
                            + "]")), true);
            SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_IN.get(), 2, 1);

            indicator.setTarget(mainStack, player);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
