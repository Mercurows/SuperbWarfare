package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.GunType;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

@OnlyIn(Dist.CLIENT)
public class GrenadeLauncherOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_grenade_launcher";

    private static final ResourceLocation POINT = Mod.loc("textures/screens/point.png");
    private static final ResourceLocation SHOTGUN_HUD = Mod.loc("textures/screens/shotgun_hud.png");
    private static final ResourceLocation REX = Mod.loc("textures/screens/rex.png");

    private static float scopeScale = 1f;

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Player player = gui.getMinecraft().player;

        if (player != null && ClientEventHandler.isEditing)
            return;
        if (player != null && player.getVehicle() instanceof ArmedVehicleEntity iArmedVehicle && iArmedVehicle.banHand(player))
            return;
        if (!shouldRenderCrossHair(player)) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        var data = GunData.from(stack);
        if (data.get(GunProp.GUN_TYPE) != GunType.CURVED_LAUNCHER) return;

        guiGraphics.pose().pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        if (data.get(GunProp.PROJECTILE_AMOUNT) > 1) {
            double spread = ClientEventHandler.gunSpread + 1 * ClientEventHandler.firePos;
            float deltaFrame = Minecraft.getInstance().getDeltaFrameTime();
            float moveX = 0;
            float moveY = 0;

            if (DisplayConfig.FLOAT_CROSS_HAIR.get() && player.getVehicle() == null) {
                moveX = (float) (-6 * ClientEventHandler.turnRot[1] - (player.isSprinting() ? 10 : 6) * ClientEventHandler.movePosX);
                moveY = (float) (-6 * ClientEventHandler.turnRot[0] + 6 * (float) ClientEventHandler.velocityY - (player.isSprinting() ? 10 : 6) * ClientEventHandler.movePosY - 0.25 * ClientEventHandler.firePos);
            }

            scopeScale = (float) Mth.lerp(0.5F * deltaFrame, scopeScale, 1 + 1.5f * spread);
            float minLength = (float) Math.min(screenWidth, screenHeight);
            float scaledMinLength = Math.min((float) screenWidth / minLength, (float) screenHeight / minLength) * 0.012f * scopeScale;
            float finLength = Mth.floor(minLength * scaledMinLength);
            float finPosX = ((screenWidth - finLength) / 2) + moveX;
            float finPosY = ((screenHeight - finLength) / 2) + moveY;

            preciseBlit(guiGraphics, POINT, screenWidth / 2f - 7.5f + moveX, screenHeight / 2f - 7.5f + moveY, 0, 0, 16, 16, 16, 16);
            preciseBlit(guiGraphics, SHOTGUN_HUD, finPosX, finPosY, 0, 0.0F, finLength, finLength, finLength, finLength);
        } else {
            guiGraphics.blit(REX, screenWidth / 2 - 16, screenHeight / 2 - 16, 0, 0, 32, 32, 32, 32);
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        guiGraphics.pose().popPose();
    }

    private static boolean shouldRenderCrossHair(Player player) {
        if (player == null) return false;
        return !player.isSpectator()
                && (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || (player.isPassenger() && player.getVehicle() instanceof CannonEntity))
                && !ClientEventHandler.zoom;
    }
}
