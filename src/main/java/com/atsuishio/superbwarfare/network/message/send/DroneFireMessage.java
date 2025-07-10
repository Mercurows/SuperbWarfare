package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.FiringParameters;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import static com.atsuishio.superbwarfare.item.ArtilleryIndicator.TAG_CANNON;

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
                    if (parameters != null) {
                        isDepressed = parameters.isDepressed();
                    }

                    offStack.set(ModDataComponents.FIRING_PARAMETERS, new FiringParameters.Parameters(new BlockPos((int) message.pos.x, (int) message.pos.y, (int) message.pos.z), isDepressed));

                    player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.target_pos").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("[" + message.pos.x()
                                    + "," + message.pos.y()
                                    + "," + message.pos.z() + "]")), true);

                    SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_IN.get(), 2, 1);

                    if (offStack.is(ModItems.ARTILLERY_INDICATOR.get())) {
                        ListTag tags = NBTTool.getTag(offStack).getList(TAG_CANNON, Tag.TAG_COMPOUND);
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

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
