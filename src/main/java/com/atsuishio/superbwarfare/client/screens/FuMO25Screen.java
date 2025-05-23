package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.network.message.send.RadarChangeModeMessage;
import com.atsuishio.superbwarfare.network.message.send.RadarSetParametersMessage;
import com.atsuishio.superbwarfare.network.message.send.RadarSetPosMessage;
import com.atsuishio.superbwarfare.network.message.send.RadarSetTargetMessage;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class FuMO25Screen extends AbstractContainerScreen<FuMO25Menu> {

    private static final ResourceLocation TEXTURE = Mod.loc("textures/gui/radar.png");
    private static final ResourceLocation SCAN = Mod.loc("textures/gui/radar_scan.png");

    private BlockPos currentPos = null;
    private Entity currentTarget = null;

    public FuMO25Screen(FuMO25Menu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        imageWidth = 340;
        imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 358, 328);

        // 目标位置
        renderTargets(pGuiGraphics);

        // 扫描盘
        renderScan(pGuiGraphics);

        // 网格线
        renderXLine(pGuiGraphics, i, j);

        // FE
        long energy = FuMO25Screen.this.menu.getEnergy();
        float energyRate = (float) energy / (float) FuMO25BlockEntity.MAX_ENERGY;
        pGuiGraphics.blit(TEXTURE, i + 278, j + 39, 178, 167, (int) (54 * energyRate), 16, 358, 328);

        // 信息显示
        renderInfo(pGuiGraphics);

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private void renderXLine(GuiGraphics guiGraphics, int i, int j) {
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

        guiGraphics.blit(TEXTURE, i + 8, j + 11, 0, 167, 147, 147, 358, 328);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        poseStack.popPose();
    }

    private void renderTargets(GuiGraphics guiGraphics) {
        var entities = FuMO25ScreenHelper.entities;
        if (entities == null || entities.isEmpty()) return;
        var pos = FuMO25ScreenHelper.pos;
        if (pos == null) return;
        if (!FuMO25Screen.this.menu.isPowered()) return;

        int type = (int) FuMO25Screen.this.menu.getFuncType();
        int range = type == 1 ? FuMO25BlockEntity.MAX_RANGE : FuMO25BlockEntity.DEFAULT_RANGE;

        var poseStack = guiGraphics.pose();
        poseStack.pushPose();

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        int centerX = i + 81;
        int centerY = j + 84;

        for (var entity : entities) {
            double moveX = (entity.getX() - pos.getX()) / range * 74;
            double moveZ = (entity.getZ() - pos.getZ()) / range * 74;

            RenderHelper.preciseBlit(guiGraphics, TEXTURE, (float) (centerX + moveX), (float) (centerY + moveZ),
                    233, 167, 4, 4, 358, 328);
        }

        poseStack.popPose();
    }

    private void renderScan(GuiGraphics guiGraphics) {
        if (FuMO25Screen.this.menu.getEnergy() <= 0) return;
        if (!FuMO25Screen.this.menu.isPowered()) return;

        var poseStack = guiGraphics.pose();
        poseStack.pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        poseStack.rotateAround(Axis.ZP.rotationDegrees(System.currentTimeMillis() % 36000000 / 30f), i + 9 + 145 / 2f, j + 12 + 145 / 2f, 0);

        guiGraphics.blit(SCAN, i + 9, j + 12, 0, 0, 145, 145, 145, 145);

        poseStack.popPose();
    }

    private void renderInfo(GuiGraphics guiGraphics) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        if (this.currentPos != null) {
            guiGraphics.drawString(this.font, Component.translatable("des.superbwarfare.fumo_25.current_pos",
                    "[" + currentPos.getX() + ", " + currentPos.getY() + ", " + currentPos.getZ() + "]"), i + 173, j + 13, 0xffffff);
        }

        if (this.currentTarget != null) {
            StringBuilder sb = new StringBuilder();
            if (currentTarget.getDisplayName() != null) {
                sb.append(currentTarget.getDisplayName().getString());
            }
            if (currentTarget instanceof LivingEntity living) {
                sb.append(" (HP: ").append(FormatTool.format1D(living.getHealth()))
                        .append("/").append(FormatTool.format1D(living.getMaxHealth())).append(")");
            } else if (currentTarget instanceof VehicleEntity vehicle) {
                sb.append(" (HP: ").append(FormatTool.format1D(vehicle.getHealth()))
                        .append("/").append(FormatTool.format1D(vehicle.getMaxHealth())).append(")");
            }

            guiGraphics.drawString(this.font, Component.translatable("des.superbwarfare.fumo_25.current_target", sb.toString()),
                    i + 173, j + 24, 0xffffff);
        }

        int type = (int) FuMO25Screen.this.menu.getFuncType();
        var component = switch (type) {
            case 1 -> Component.translatable("des.superbwarfare.fumo_25.type_1");
            case 2 -> Component.translatable("des.superbwarfare.fumo_25.type_2");
            case 3 -> Component.translatable("des.superbwarfare.fumo_25.type_3");
            default -> Component.translatable("des.superbwarfare.fumo_25.type_0");
        };
        if (type != 0) {
            component = component.append(Component.literal(" " + FuMO25Screen.this.menu.getTime() / 20 + "s"));
        }
        guiGraphics.drawString(this.font, component, i + 173, j + 43, 0xffffff);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        var entities = FuMO25ScreenHelper.entities;
        if (entities == null || entities.isEmpty()) return super.mouseClicked(pMouseX, pMouseY, pButton);
        var pos = FuMO25ScreenHelper.pos;
        if (pos == null) return super.mouseClicked(pMouseX, pMouseY, pButton);
        if (pButton != 0) return super.mouseClicked(pMouseX, pMouseY, pButton);
        if (!FuMO25Screen.this.menu.isPowered()) return super.mouseClicked(pMouseX, pMouseY, pButton);

        int type = (int) FuMO25Screen.this.menu.getFuncType();
        int range = type == 1 ? FuMO25BlockEntity.MAX_RANGE : FuMO25BlockEntity.DEFAULT_RANGE;

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        int centerX = i + 81;
        int centerY = j + 84;

        for (var entity : entities) {
            double moveX = (entity.getX() - pos.getX()) / range * 74;
            double moveZ = (entity.getZ() - pos.getZ()) / range * 74;

            if (pMouseX >= centerX + moveX && pMouseX <= centerX + moveX + 4 && pMouseY >= centerY + moveZ && pMouseY <= centerY + moveZ + 4) {
                PacketDistributor.sendToServer(new RadarSetPosMessage(entity.getOnPos()));
                this.currentPos = entity.getOnPos();
                this.currentTarget = entity;
                return true;
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.pose().pushPose();
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.pose().popPose();
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics pGuiGraphics, int pX, int pY) {
        super.renderTooltip(pGuiGraphics, pX, pY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("des.superbwarfare.charging_station.energy", FuMO25Screen.this.menu.getEnergy(),
                FuMO25BlockEntity.MAX_ENERGY));

        if ((pX - i) >= 278 && (pX - i) <= 332 && (pY - j) >= 39 && (pY - j) <= 55) {
            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY);
        }
    }

    // 本方法留空
    @Override
    protected void renderLabels(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 33;
        this.titleLabelY = 5;
        this.inventoryLabelX = 105;
        this.inventoryLabelY = 128;

        this.currentPos = null;
        this.currentTarget = null;

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        LockButton lockButton = new LockButton(i + 304, j + 61);
        this.addRenderableWidget(lockButton);

        ModeButton widerButton = new ModeButton(i + 171, j + 61, 1);
        this.addRenderableWidget(widerButton);

        ModeButton glowButton = new ModeButton(i + 201, j + 61, 2);
        this.addRenderableWidget(glowButton);

        ModeButton guideButton = new ModeButton(i + 231, j + 61, 3);
        this.addRenderableWidget(guideButton);
    }

    @OnlyIn(Dist.CLIENT)
    class LockButton extends AbstractButton {

        public LockButton(int pX, int pY) {
            super(pX, pY, 29, 15, Component.empty());
        }

        @Override
        public void onPress() {
            if (FuMO25Screen.this.menu.getFuncType() == 3 && FuMO25Screen.this.menu.getSlot(0).getItem().isEmpty()) {
                if (FuMO25Screen.this.currentTarget == null) return;
                PacketDistributor.sendToServer(new RadarSetTargetMessage(FuMO25Screen.this.currentTarget.getUUID()));
            } else {
                PacketDistributor.sendToServer(new RadarSetParametersMessage((byte) 0));
            }
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            if (FuMO25Screen.this.menu.getFuncType() == 3 && FuMO25Screen.this.menu.getSlot(0).getItem().isEmpty()) {
                pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 148, this.isHovered() ? 311 : 295, 29, 15, 358, 328);
            } else {
                pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 148, this.isHovered() ? 183 : 167, 29, 15, 358, 328);
            }
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class ModeButton extends AbstractButton {

        private final int mode;

        public ModeButton(int pX, int pY, int mode) {
            super(pX, pY, 29, 15, Component.empty());
            this.mode = mode;
        }

        @Override
        public void onPress() {
            PacketDistributor.sendToServer(new RadarChangeModeMessage((byte) this.mode));
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 148, this.isHovered() ? 183 + this.mode * 32 : 167 + this.mode * 32,
                    29, 15, 358, 328);
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
        }
    }
}
