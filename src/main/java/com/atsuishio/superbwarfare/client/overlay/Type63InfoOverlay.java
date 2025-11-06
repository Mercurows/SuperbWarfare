package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.FiringParameters;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import static com.atsuishio.superbwarfare.entity.vehicle.Type63Entity.SHOOT_PITCH;
import static com.atsuishio.superbwarfare.entity.vehicle.Type63Entity.SHOOT_YAW;
import static com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector;
import static com.atsuishio.superbwarfare.tools.RangeTool.calculateLaunchVector;

@OnlyIn(Dist.CLIENT)
public class Type63InfoOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("type_63_info");

    private static final ItemStack AP = new ItemStack(ModItems.MEDIUM_ROCKET_AP.get());
    private static final ItemStack HE = new ItemStack(ModItems.MEDIUM_ROCKET_HE.get());
    private static final ItemStack CM = new ItemStack(ModItems.MEDIUM_ROCKET_CM.get());

    @Override
    public void render(GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        PoseStack poseStack = guiGraphics.pose();

        if (player == null) return;

        var screenWidth = guiGraphics.guiWidth();
        var screenHeight = guiGraphics.guiHeight();
        var partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);

        Entity lookingEntity = TraceTool.findLookingEntity(player, player.entityInteractionRange());

        if (!(lookingEntity instanceof Type63Entity type63Entity)) return;
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.pitch")
                        .append(Component.literal(FormatTool.format2D(type63Entity.getEntityData().get(SHOOT_PITCH), "°"))),
                screenWidth / 2 - 130, screenHeight / 2 - 26, -1, false);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.yaw")
                        .append(Component.literal(FormatTool.format2D(type63Entity.getEntityData().get(SHOOT_YAW), "°"))),
                screenWidth / 2 - 130, screenHeight / 2 - 16, -1, false);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.range")
                        .append(Component.literal(FormatTool.format1D(Math.max((int) RangeTool.getRange(type63Entity.getEntityData().get(SHOOT_PITCH), 10, 0.05), 0) , "m"))),
                screenWidth / 2 - 130, screenHeight / 2 - 6, -1, false);

        var items = type63Entity.getEntityData().get(Type63Entity.LOADED_AMMO);
        for (int i = 0; i < type63Entity.barrel.length; i++) {
            if (OBB.getLookingObb(player, player.entityInteractionRange()) == type63Entity.barrel[i]) {
                int type = items.get(i);

                ItemStack stack = switch (type) {
                    case 0 -> AP;
                    case 1 -> HE;
                    case 2 -> CM;
                    default -> ItemStack.EMPTY;
                };

                Vec3 pos = new Vec3(type63Entity.barrel[i].center());
                Vec3 point = VectorUtil.worldToScreen(pos);

                poseStack.pushPose();
                float x = (float) point.x;
                float y = (float) point.y;

                var component = stack.getHoverName();

                if (stack.isEmpty()) {
                    component = Component.translatable("tips.superbwarfare.barrel_empty");
                    int width = Minecraft.getInstance().font.width(component);

                    poseStack.translate(x - width / 2F, y, 0);
                    guiGraphics.drawString(Minecraft.getInstance().font, component, 0, 0, -1, false);
                } else {
                    int width = Minecraft.getInstance().font.width(component) + 20;

                    poseStack.pushPose();
                    poseStack.translate(x - width / 2F, y, 0);
                    guiGraphics.renderFakeItem(stack, 0, 0);

                    poseStack.translate(20, 4, 0);
                    guiGraphics.drawString(Minecraft.getInstance().font, component, 0, 0, -1, false);
                }

                poseStack.popPose();
            }
        }

        ItemStack stack = player.getOffhandItem();

        if (player.getMainHandItem().getItem() instanceof FiringParameters) {
            stack = player.getMainHandItem();
        }

        if (stack.getItem() instanceof FiringParameters) {
            var parameters = stack.get(ModDataComponents.FIRING_PARAMETERS);
            if (parameters == null) {
                parameters = new FiringParameters.Parameters(new BlockPos(0, 0, 0));
            }
            double targetX = parameters.pos().getX();
            double targetY = parameters.pos().getY() - 1;
            double targetZ = parameters.pos().getZ();
            boolean isDepressed = parameters.isDepressed();

            Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
            Vec3 launchVector = calculateLaunchVector(type63Entity.getShootPos(partialTick), targetPos, 10, 0.05, isDepressed);

            Vec3 vec3 = EntityAnchorArgument.Anchor.EYES.apply(lookingEntity);
            double d0 = (targetPos.x - vec3.x) * 0.2;
            double d2 = (targetPos.z - vec3.z) * 0.2;
            double targetYaw = Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F);

            float angle = 0;

            if (launchVector != null) {
                angle = (float) getXRotFromVector(launchVector);
            } else {
                guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.out_of_range").withStyle(ChatFormatting.RED),
                        screenWidth / 2 + 90, screenHeight / 2 - 26, -1, false);
                return;
            }

            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.target.pitch")
                            .append(Component.literal(FormatTool.format2D(angle, "°"))),
                    screenWidth / 2 + 90, screenHeight / 2 - 26, -1, false);
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.target.yaw")
                            .append(Component.literal(FormatTool.format2D(targetYaw, "°"))),
                    screenWidth / 2 + 90, screenHeight / 2 - 16, -1, false);
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.target_pos")
                            .append(Component.literal(FormatTool.format0D(targetX) + " " + FormatTool.format0D(targetY) + " " + FormatTool.format0D(targetZ))),
                    screenWidth / 2 + 90, screenHeight / 2 - 6, -1, false);

            if (angle < -5 || angle > 60) {
                guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.warn", lookingEntity.getDisplayName()).withStyle(ChatFormatting.RED),
                        screenWidth / 2 + 90, screenHeight / 2 + 4, -1, false);
                if (angle > 60 && !isDepressed) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.ballistics.warn").withStyle(ChatFormatting.RED),
                            screenWidth / 2 + 90, screenHeight / 2 + 14, -1, false);
                }
            }
        }
    }
}
