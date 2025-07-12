package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.ArtilleryIndicator;
import com.atsuishio.superbwarfare.item.FiringParameters;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record DroneFireMessage(Vector3f pos) implements CustomPacketPayload {

    public static final Type<DroneFireMessage> TYPE = new Type<>(Mod.loc("drone_fire"));

    public static final StreamCodec<ByteBuf, DroneFireMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VECTOR3F,
            DroneFireMessage::pos,
            DroneFireMessage::new
    );

    public static void handler(DroneFireMessage message, final IPayloadContext context) {
        Player player = context.player();
        ItemStack stack = player.getMainHandItem();
        var mainTag = NBTTool.getTag(stack);

        if (stack.is(ModItems.MONITOR.get()) && mainTag.getBoolean("Using") && mainTag.getBoolean("Linked")) {
            DroneEntity drone = EntityFindUtil.findDrone(player.level(), mainTag.getString("LinkedDrone"));
            if (drone != null) {
                if (player.getOffhandItem().is(ModItems.FIRING_PARAMETERS.get()) || player.getOffhandItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
                    ItemStack offStack = player.getOffhandItem();

                    var parameters = offStack.get(ModDataComponents.FIRING_PARAMETERS);
                    var isDepressed = false;
                    var radius = 0;
                    if (parameters != null) {
                        isDepressed = parameters.isDepressed();
                        radius = parameters.radius();
                    }

                    offStack.set(ModDataComponents.FIRING_PARAMETERS, new FiringParameters.Parameters(new BlockPos((int) message.pos.x, (int) message.pos.y, (int) message.pos.z), radius, isDepressed));

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
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
