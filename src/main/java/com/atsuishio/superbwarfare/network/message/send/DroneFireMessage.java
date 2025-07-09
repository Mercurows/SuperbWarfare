package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public enum DroneFireMessage {
    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                Player player = context.getSender();

                ItemStack stack = player.getMainHandItem();

                if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
                    DroneEntity drone = EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone"));
                    if (drone != null) {
                        if (!player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get())) {
                            drone.fire = true;
                        } else {
                            ItemStack offStack = player.getOffhandItem();
                            boolean lookAtEntity = false;

                            Entity lookingEntity = SeekTool.seekLivingEntity(drone, drone.level(), 512, 2);

                            BlockHitResult result = drone.level().clip(new ClipContext(drone.getEyePosition(), drone.getEyePosition().add(drone.getViewVector(1).scale(512)),
                                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, drone));
                            Vec3 hitPos = result.getLocation();

                            if (lookingEntity != null) {
                                lookAtEntity = true;
                            }

                            if (lookAtEntity) {
                                offStack.getOrCreateTag().putDouble("TargetX", lookingEntity.getX());
                                offStack.getOrCreateTag().putDouble("TargetY", lookingEntity.getY());
                                offStack.getOrCreateTag().putDouble("TargetZ", lookingEntity.getZ());
                            } else {
                                offStack.getOrCreateTag().putDouble("TargetX", hitPos.x());
                                offStack.getOrCreateTag().putDouble("TargetY", hitPos.y());
                                offStack.getOrCreateTag().putDouble("TargetZ", hitPos.z());
                            }

                            player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.target_pos").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal("[" + offStack.getOrCreateTag().getInt("TargetX")
                                            + "," + offStack.getOrCreateTag().getInt("TargetY")
                                            + "," + offStack.getOrCreateTag().getInt("TargetZ") + "]")), true);
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
