package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
public class RedTriangleOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("red_triangle");

    private static final ResourceLocation TRIANGLE = Mod.loc("textures/screens/red_triangle.png");

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = guiGraphics.pose();

        Player player = mc.player;
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModItems.RPG.get())) return;
        if (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player))
            return;

        Entity idf = SeekTool.seekLivingEntity(player, player.level(), 128, 6);
        if (idf == null) return;
        Vec3 playerVec = new Vec3(Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), player.xo, player.getX()), Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), player.yo + player.getEyeHeight(), player.getEyeY()), Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), player.zo, player.getZ()));
        double distance = idf.position().distanceTo(playerVec);
        Vec3 pos = new Vec3(Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), idf.xo, idf.getX()), Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), idf.yo + idf.getEyeHeight() + 0.5 + 0.07 * distance, idf.getEyeY() + 0.5 + 0.07 * distance), Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), idf.zo, idf.getZ()));
        Vec3 point = RenderHelper.worldToScreen(pos, playerVec);
        if (point == null) return;

        poseStack.pushPose();
        float x = (float) point.x;
        float y = (float) point.y;

        RenderHelper.preciseBlit(guiGraphics, TRIANGLE, x - 4, y - 4, 8, 8, 0, 0, 8, 8, 8, 8);

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        poseStack.popPose();
    }
}
