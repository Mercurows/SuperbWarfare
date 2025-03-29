package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
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

public record DroneFireMessage(int msgType) implements CustomPacketPayload {
    public static final Type<DroneFireMessage> TYPE = new Type<>(Mod.loc("drone_fire"));

    public static final StreamCodec<ByteBuf, DroneFireMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            DroneFireMessage::msgType,
            DroneFireMessage::new
    );

    public static void handler(DroneFireMessage message, final IPayloadContext context) {
        Player player = context.player();
        ItemStack stack = player.getMainHandItem();
        var tag = NBTTool.getTag(stack);

        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            DroneEntity drone = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"));
            if (drone == null) return;

            if (!player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get())) {
                drone.fire = true;
            } else {
                boolean lookAtEntity = false;

                Entity lookingEntity = SeekTool.seekLivingEntity(drone, drone.level(), 512, 2);

                BlockHitResult result = drone.level().clip(new ClipContext(drone.getEyePosition(), drone.getEyePosition().add(drone.getViewVector(1).scale(512)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, drone));
                Vec3 hitPos = result.getLocation();

                if (lookingEntity != null) {
                    lookAtEntity = true;
                }

                ItemStack offStack = player.getOffhandItem();
                var offTag = NBTTool.getTag(offStack);
                if (lookAtEntity) {
                    offTag.putDouble("TargetX", lookingEntity.getX());
                    offTag.putDouble("TargetY", lookingEntity.getY());
                    offTag.putDouble("TargetZ", lookingEntity.getZ());
                } else {
                    offTag.putDouble("TargetX", hitPos.x());
                    offTag.putDouble("TargetY", hitPos.y());
                    offTag.putDouble("TargetZ", hitPos.z());
                }
                NBTTool.saveTag(offStack, offTag);

                player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.target_pos")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("["
                                + offTag.getInt("TargetX")
                                + "," + offTag.getInt("TargetY")
                                + "," + offTag.getInt("TargetZ")
                                + "]")), true);
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
