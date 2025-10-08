package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public class RedTriangleOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_red_triangle";

    private static final ResourceLocation TRIANGLE = Mod.loc("textures/screens/red_triangle.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = gui.getMinecraft();
        PoseStack poseStack = guiGraphics.pose();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        Player player = mc.player;
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (stack.is(ModItems.RPG.get()) && GunData.from(stack).selectedAmmoType.get() == 0) {
            if (player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player))
                return;

            Entity idf = SeekTool.seekLivingEntity(player, 128, 6);
            if (idf == null) return;
            double distance = idf.position().distanceTo(cameraPos);
            Vec3 pos = new Vec3(Mth.lerp(partialTick, idf.xo, idf.getX()), Mth.lerp(partialTick, idf.yo + idf.getEyeHeight() + 0.5 + 0.07 * distance, idf.getEyeY() + 0.5 + 0.07 * distance), Mth.lerp(partialTick, idf.zo, idf.getZ()));
            Vec3 point = VectorUtil.worldToScreen(pos);

            poseStack.pushPose();
            float x = (float) point.x;
            float y = (float) point.y;
            RenderHelper.blit(poseStack, TRIANGLE, x - 4, y - 4, 0, 0, 8, 8, 8, 8, -65536);
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            poseStack.popPose();

        }
    }
}
