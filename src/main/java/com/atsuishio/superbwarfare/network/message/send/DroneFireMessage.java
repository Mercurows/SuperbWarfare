package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.item.ArtilleryIndicator.TAG_CANNON;

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
                        if (player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get()) || player.getOffhandItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
                            ItemStack offStack = player.getOffhandItem();
                            boolean lookAtEntity = false;

                            Entity lookingEntity = SeekTool.seekLivingEntity(drone, drone.level(), 512, 2);

                            BlockHitResult result = player.level().clip(new ClipContext(drone.getEyePosition(), drone.getEyePosition().add(drone.getLookAngle().scale(512)),
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

                            SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_IN.get(), 2, 1);

                            if (offStack.is(ModItems.ARTILLERY_INDICATOR.get())) {

                                ListTag tags = offStack.getOrCreateTag().getList(TAG_CANNON, Tag.TAG_COMPOUND);
                                for (int i = 0; i < tags.size(); i++) {
                                    var tag = tags.getCompound(i);
                                    Entity entity = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"));
                                    if (entity instanceof MortarEntity mortarEntity) {
                                        if (!mortarEntity.setTarget(offStack)) {
                                            player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn").withStyle(ChatFormatting.RED), true);
                                        }
                                    }
                                    if (entity instanceof Mk42Entity mk42Entity) {
                                        if (!mk42Entity.setTarget(offStack, true)) {
                                            player.displayClientMessage(Component.translatable("tips.superbwarfare.mk_42.warn").withStyle(ChatFormatting.RED), true);
                                        }
                                    }
                                    if (entity instanceof Mle1934Entity mle1934Entity) {
                                        if (!mle1934Entity.setTarget(offStack, true)) {
                                            player.displayClientMessage(Component.translatable("tips.superbwarfare.mle_1934.warn").withStyle(ChatFormatting.RED), true);
                                        }
                                    }
                                }
                            }
                        } else {
                            drone.fire = true;
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
