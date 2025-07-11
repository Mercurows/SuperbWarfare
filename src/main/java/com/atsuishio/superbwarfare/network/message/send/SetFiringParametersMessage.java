package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.FiringParameters;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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

import static com.atsuishio.superbwarfare.item.ArtilleryIndicator.TAG_CANNON;

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
        Vec3 hitPos = result.getLocation();

        if (lookingEntity != null) {
            lookAtEntity = true;
        }
        if (stack.is(ModItems.FIRING_PARAMETERS.get())) {

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

        if (mainStack.is(ModItems.ARTILLERY_INDICATOR.get())) {
            BlockPos pos;
            if (lookAtEntity) {
                pos = lookingEntity.blockPosition();
            } else {
                pos = new BlockPos((int) hitPos.x, (int) hitPos.y, (int) hitPos.z);
            }
            mainStack.set(ModDataComponents.FIRING_PARAMETERS, new FiringParameters.Parameters(pos, false));

            player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.target_pos")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("[" + pos.getX()
                            + "," + pos.getY()
                            + "," + pos.getZ()
                            + "]")), true);
            SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_IN.get(), 2, 1);

            ListTag tags = NBTTool.getTag(mainStack).getList(TAG_CANNON, Tag.TAG_COMPOUND);
            for (int i = 0; i < tags.size(); i++) {
                var tag = tags.getCompound(i);
                Entity entity = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"));
                if (entity instanceof MortarEntity mortarEntity) {
                    if (!mortarEntity.setTarget(mainStack)) {
                        player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn").withStyle(ChatFormatting.RED), true);
                    }
                }
                if (entity instanceof Mk42Entity mk42Entity) {
                    if (!mk42Entity.setTarget(mainStack)) {
                        player.displayClientMessage(Component.translatable("tips.superbwarfare.mk_42.warn").withStyle(ChatFormatting.RED), true);
                    }
                }
                if (entity instanceof Mle1934Entity mle1934Entity) {
                    if (!mle1934Entity.setTarget(mainStack)) {
                        player.displayClientMessage(Component.translatable("tips.superbwarfare.mle_1934.warn").withStyle(ChatFormatting.RED), true);
                    }
                }
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
