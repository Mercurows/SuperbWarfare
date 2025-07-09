package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.network.message.receive.ResetCameraTypeMessage;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.ItemNBTTool;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class Monitor extends Item {

    public static final String LINKED = "Linked";
    public static final String LINKED_DRONE = "LinkedDrone";
//    public static final String DRONE_UUID = "DroneUUID";

    public Monitor() {
        super(new Properties().stacksTo(1));
    }

    public static void link(ItemStack itemstack, String id) {
        ItemNBTTool.setBoolean(itemstack, LINKED, true);
        itemstack.getOrCreateTag().putString(LINKED_DRONE, id);
    }

    public static void disLink(ItemStack itemstack, Player player) {
        ItemNBTTool.setBoolean(itemstack, LINKED, false);
        itemstack.getOrCreateTag().putString(LINKED_DRONE, "none");
        if (player instanceof ServerPlayer serverPlayer) {
            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), ResetCameraTypeMessage.INSTANCE);
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();

        if (!ItemNBTTool.getBoolean(stack, LINKED, false)) {
            return super.use(level, player, hand);
        }

        if (stack.getOrCreateTag().getBoolean("Using")) {
            stack.getOrCreateTag().putBoolean("Using", false);
            if (level.isClientSide) {
                if (ClientEventHandler.lastCameraType != null) {
                    Minecraft.getInstance().options.setCameraType(ClientEventHandler.lastCameraType);
                }
            }
        } else {
            stack.getOrCreateTag().putBoolean("Using", true);
            if (level.isClientSide) {
                ClientEventHandler.lastCameraType = Minecraft.getInstance().options.getCameraType();
                Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        }

        DroneEntity drone = EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString(LINKED_DRONE));
        this.resetDroneData(drone);

        return super.use(level, player, hand);
//        ItemStack stack = player.getItemInHand(hand);
//        if (!player.isShiftKeyDown()) {
//            CompoundTag tag = stack.getOrCreateTag();
//            if (DronesTool.hasInstanceOf(player)) return InteractionResultHolder.fail(stack);
//            if (!tag.contains(DRONE_UUID)) return InteractionResultHolder.fail(stack);
//            player.startUsingItem(hand);
//        }
//        return InteractionResultHolder.fail(stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(super.getAttributeModifiers(slot, stack));
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Item modifier", 2d, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Item modifier", -2.4, AttributeModifier.Operation.ADDITION));
            return builder.build();
        }

        return super.getAttributeModifiers(slot, stack);
    }

//    @Override
//    public int getUseDuration(ItemStack stack) {
//        return 72000;
//    }

    public static void getDronePos(ItemStack itemstack, Vec3 vec3) {
        itemstack.getOrCreateTag().putDouble("PosX", vec3.x);
        itemstack.getOrCreateTag().putDouble("PosY", vec3.y);
        itemstack.getOrCreateTag().putDouble("PosZ", vec3.z);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> list, TooltipFlag flag) {
        if (!stack.getOrCreateTag().contains(LINKED_DRONE) || stack.getOrCreateTag().getString(LINKED_DRONE).equals("none"))
            return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        if (!stack.getOrCreateTag().contains("PosX") || !stack.getOrCreateTag().contains("PosY") || !stack.getOrCreateTag().contains("PosZ"))
            return;

        Vec3 droneVec = new Vec3(stack.getOrCreateTag().getDouble("PosX"), stack.getOrCreateTag().getDouble("PosY"), stack.getOrCreateTag().getDouble("PosZ"));

        list.add(Component.translatable("des.superbwarfare.monitor",
                FormatTool.format1D(player.position().distanceTo(droneVec), "m")).withStyle(ChatFormatting.GRAY));
        list.add(Component.literal("X: " + FormatTool.format1D(droneVec.x) +
                " Y: " + FormatTool.format1D(droneVec.y) +
                " Z: " + FormatTool.format1D(droneVec.z)
        ));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(itemstack, world, entity, slot, selected);
        DroneEntity drone = EntityFindUtil.findDrone(entity.level(), itemstack.getOrCreateTag().getString(LINKED_DRONE));

        if (!selected) {
            if (itemstack.getOrCreateTag().getBoolean("Using")) {
                itemstack.getOrCreateTag().putBoolean("Using", false);
                if (entity.level().isClientSide) {
                    if (ClientEventHandler.lastCameraType != null) {
                        Minecraft.getInstance().options.setCameraType(ClientEventHandler.lastCameraType);
                    }
                }
            }
            this.resetDroneData(drone);
        } else if (drone == null) {
            if (itemstack.getOrCreateTag().getBoolean("Using")) {
                itemstack.getOrCreateTag().putBoolean("Using", false);
                if (entity.level().isClientSide) {
                    if (ClientEventHandler.lastCameraType != null) {
                        Minecraft.getInstance().options.setCameraType(ClientEventHandler.lastCameraType);
                    }
                }
            }
        }
    }

//    @Nullable
//    public static UUID getDroneUUID(Player player) {
//        if (player == null) return null;
//        if (player.getMainHandItem().is(ModItems.MONITOR.get())) {
//            CompoundTag tag = player.getMainHandItem().getOrCreateTag();
//            if (tag.contains(DRONE_UUID)) {
//                return tag.getUUID(DRONE_UUID);
//            }
//        }
//        return null;
//    }
}
