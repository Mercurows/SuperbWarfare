package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.SoundTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
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

public class SetFiringParametersMessage {

    private final int type;

    public SetFiringParametersMessage(int type) {
        this.type = type;
    }

    public static SetFiringParametersMessage decode(FriendlyByteBuf buffer) {
        return new SetFiringParametersMessage(buffer.readInt());
    }

    public static void encode(SetFiringParametersMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
    }

    public static void handler(SetFiringParametersMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                Player player = context.getSender();

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
                    if (lookAtEntity) {
                        stack.getOrCreateTag().putDouble("TargetX", lookingEntity.getX());
                        stack.getOrCreateTag().putDouble("TargetY", lookingEntity.getY());
                        stack.getOrCreateTag().putDouble("TargetZ", lookingEntity.getZ());
                    } else {
                        stack.getOrCreateTag().putDouble("TargetX", hitPos.x());
                        stack.getOrCreateTag().putDouble("TargetY", hitPos.y());
                        stack.getOrCreateTag().putDouble("TargetZ", hitPos.z());
                    }
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.target_pos").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("[" + stack.getOrCreateTag().getInt("TargetX")
                                    + "," + stack.getOrCreateTag().getInt("TargetY")
                                    + "," + stack.getOrCreateTag().getInt("TargetZ") + "]")), true);
                }

                if (mainStack.is(ModItems.ARTILLERY_INDICATOR.get())) {
                    if (lookAtEntity) {
                        mainStack.getOrCreateTag().putDouble("TargetX", lookingEntity.getX());
                        mainStack.getOrCreateTag().putDouble("TargetY", lookingEntity.getY());
                        mainStack.getOrCreateTag().putDouble("TargetZ", lookingEntity.getZ());
                    } else {
                        mainStack.getOrCreateTag().putDouble("TargetX", hitPos.x());
                        mainStack.getOrCreateTag().putDouble("TargetY", hitPos.y());
                        mainStack.getOrCreateTag().putDouble("TargetZ", hitPos.z());
                    }
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.target_pos").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("[" + mainStack.getOrCreateTag().getInt("TargetX")
                                    + "," + mainStack.getOrCreateTag().getInt("TargetY")
                                    + "," + mainStack.getOrCreateTag().getInt("TargetZ") + "]")), true);

                    SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_IN.get(), 2, 1);

                    ListTag tags = mainStack.getOrCreateTag().getList(TAG_CANNON, Tag.TAG_COMPOUND);
                    for (int i = 0; i < tags.size(); i++) {
                        var tag = tags.getCompound(i);
                        Entity entity = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"));
                        if (entity instanceof MortarEntity mortarEntity) {
                            if (!mortarEntity.setTarget(mainStack)) {
                                player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn").withStyle(ChatFormatting.RED), true);
                            }
                        }
                        if (entity instanceof Mk42Entity mk42Entity) {
                            if (!mk42Entity.setTarget(mainStack, true)) {
                                player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn").withStyle(ChatFormatting.RED), true);
                            }
                        }
                        if (entity instanceof Mle1934Entity mle1934Entity) {
                            if (!mle1934Entity.setTarget(mainStack, true)) {
                                player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.warn").withStyle(ChatFormatting.RED), true);
                            }
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
