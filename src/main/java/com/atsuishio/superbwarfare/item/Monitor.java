package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.network.message.receive.ResetCameraTypeMessage;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public class Monitor extends Item {

    public static final String LINKED = "Linked";
    public static final String LINKED_DRONE = "LinkedDrone";

    public Monitor() {
        super(new Properties().stacksTo(1));
    }

    public static void link(CompoundTag tag, String id) {
        tag.putBoolean(LINKED, true);
        tag.putString(LINKED_DRONE, id);
    }

    public static void disLink(CompoundTag tag, Player player) {
        tag.putBoolean(LINKED, false);
        tag.putString(LINKED_DRONE, "none");
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ResetCameraTypeMessage(0));
        }
    }

    private void resetDroneData(DroneEntity drone) {
        if (drone == null) return;

        drone.getPersistentData().putBoolean("left", false);
        drone.getPersistentData().putBoolean("right", false);
        drone.getPersistentData().putBoolean("forward", false);
        drone.getPersistentData().putBoolean("backward", false);
        drone.getPersistentData().putBoolean("up", false);
        drone.getPersistentData().putBoolean("down", false);
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        var tag = NBTTool.getTag(stack);

        if (!tag.getBoolean(LINKED)) {
            return super.use(world, player, hand);
        }

        if (tag.getBoolean("Using")) {
            tag.putBoolean("Using", false);
            if (world.isClientSide) {
                if (ClientEventHandler.lastCameraType != null) {
                    Minecraft.getInstance().options.setCameraType(ClientEventHandler.lastCameraType);
                }
            }
        } else {
            tag.putBoolean("Using", true);
            if (world.isClientSide) {
                ClientEventHandler.lastCameraType = Minecraft.getInstance().options.getCameraType();
                Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        }

        NBTTool.saveTag(stack, tag);
        DroneEntity drone = EntityFindUtil.findDrone(player.level(), tag.getString(LINKED_DRONE));
        this.resetDroneData(drone);

        return super.use(world, player, hand);
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        var list = new ArrayList<>(super.getDefaultAttributeModifiers(stack).modifiers());
        list.addAll(List.of(
                new ItemAttributeModifiers.Entry(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 2, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                ),
                new ItemAttributeModifiers.Entry(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
        ));

        return new ItemAttributeModifiers(list, true);
    }

    public static void getDronePos(ItemStack stack, Vec3 vec3) {
        var tag = NBTTool.getTag(stack);
        tag.putDouble("PosX", vec3.x);
        tag.putDouble("PosY", vec3.y);
        tag.putDouble("PosZ", vec3.z);
        NBTTool.saveTag(stack, tag);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        var tag = NBTTool.getTag(stack);
        if (!tag.contains(LINKED_DRONE) || tag.getString(LINKED_DRONE).equals("none"))
            return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        if (!tag.contains("PosX") || !tag.contains("PosY") || !tag.contains("PosZ"))
            return;

        Vec3 droneVec = new Vec3(tag.getDouble("PosX"), tag.getDouble("PosY"), tag.getDouble("PosZ"));

        tooltipComponents.add(Component.translatable("des.superbwarfare.monitor",
                FormatTool.format1D(player.position().distanceTo(droneVec), "m")).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("X: " + FormatTool.format1D(droneVec.x) +
                " Y: " + FormatTool.format1D(droneVec.y) +
                " Z: " + FormatTool.format1D(droneVec.z)
        ));
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        var tag = NBTTool.getTag(stack);
        DroneEntity drone = EntityFindUtil.findDrone(entity.level(), tag.getString(LINKED_DRONE));

        if (!selected) {
            if (tag.getBoolean("Using")) {
                tag.putBoolean("Using", false);
                NBTTool.saveTag(stack, tag);
                if (entity.level().isClientSide) {
                    if (ClientEventHandler.lastCameraType != null) {
                        Minecraft.getInstance().options.setCameraType(ClientEventHandler.lastCameraType);
                    }
                }
            }
            this.resetDroneData(drone);
        } else if (drone == null) {
            if (tag.getBoolean("Using")) {
                tag.putBoolean("Using", false);
                NBTTool.saveTag(stack, tag);
                if (entity.level().isClientSide) {
                    if (ClientEventHandler.lastCameraType != null) {
                        Minecraft.getInstance().options.setCameraType(ClientEventHandler.lastCameraType);
                    }
                }
            }
        }
    }
}
