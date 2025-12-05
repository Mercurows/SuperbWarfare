package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.ArtilleryIndicator;
import com.atsuishio.superbwarfare.item.FiringParameters;
import com.atsuishio.superbwarfare.item.FiringParametersKt;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class DroneFireMessage {

    private final Vector3f pos;

    public DroneFireMessage(Vector3f pos) {
        this.pos = pos;
    }

    public static DroneFireMessage decode(FriendlyByteBuf buffer) {
        return new DroneFireMessage(buffer.readVector3f());
    }

    public static void encode(DroneFireMessage message, FriendlyByteBuf buffer) {
        buffer.writeVector3f(message.pos);
    }

    public static void handler(DroneFireMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();

            if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
                DroneEntity drone = EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone"));
                if (drone != null) {
                    if (player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get()) || player.getOffhandItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
                        ItemStack offStack = player.getOffhandItem();

                        var parameters = FiringParametersKt.getFiringParameters(offStack);
                        var isDepressed = false;
                        var radius = 0;
                        isDepressed = parameters.isDepressed();
                        radius = parameters.radius();

                        FiringParametersKt.setFiringParameters(offStack, new FiringParameters.Parameters(new BlockPos((int) message.pos.x, (int) message.pos.y, (int) message.pos.z), radius, isDepressed));

                        player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.target_pos").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal("[" + message.pos.x()
                                        + "," + message.pos.y()
                                        + "," + message.pos.z() + "]")), true);

                        SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_IN.get(), 2, 1);

                        if (offStack.getItem() instanceof ArtilleryIndicator indicator) {
                            indicator.setTarget(offStack, player);
                        }
                    } else {
                        drone.fire = true;
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
