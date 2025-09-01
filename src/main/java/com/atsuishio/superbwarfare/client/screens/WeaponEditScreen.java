package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.send.EditMessage;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class WeaponEditScreen extends Screen {

    // 六个改装位置，大小128*128
    private static final ResourceLocation BARREL = Mod.loc("textures/gui/attachment/barrel.png");
    private static final ResourceLocation SCOPE = Mod.loc("textures/gui/attachment/scope.png");
    private static final ResourceLocation GRIP = Mod.loc("textures/gui/attachment/grip.png");
    private static final ResourceLocation STOCK = Mod.loc("textures/gui/attachment/stock.png");
    private static final ResourceLocation MAGAZINE = Mod.loc("textures/gui/attachment/magazine.png");
    private static final ResourceLocation AMMO_TYPE = Mod.loc("textures/gui/attachment/ammo_type.png");

    // 配件不可用标识，大小128*128
    private static final ResourceLocation INVALID = Mod.loc("textures/gui/attachment/invalid.png");

    // 按钮，大小64*64
    private static final ResourceLocation BUTTON_LEFT = Mod.loc("textures/gui/attachment/button_left.png");
    private static final ResourceLocation BUTTON_RIGHT = Mod.loc("textures/gui/attachment/button_right.png");
    private static final ResourceLocation BUTTON_LEFT_HOVERED = Mod.loc("textures/gui/attachment/button_left_hovered.png");
    private static final ResourceLocation BUTTON_RIGHT_HOVERED = Mod.loc("textures/gui/attachment/button_right_hovered.png");

    // 标记，大小16*16
    private static final ResourceLocation CHOSEN = Mod.loc("textures/gui/attachment/chosen.png");
    private static final ResourceLocation NOT_CHOSEN = Mod.loc("textures/gui/attachment/not_chosen.png");

    private final ItemStack stack;

    public WeaponEditScreen(ItemStack stack) {
        super(Component.empty());
        this.stack = stack;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderEdit(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    public void renderEdit(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (!(stack.getItem() instanceof GunItem gunItem)) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var itemStack = player.getMainHandItem();
        if (!(itemStack.getItem() instanceof GunItem)) return;
        if (itemStack.getItem() != stack.getItem()) return;

        var pose = pGuiGraphics.pose();

        pose.pushPose();

        pGuiGraphics.fill(this.width - 165, 4, this.width - 4, 110, 0x80000000);
        pGuiGraphics.drawString(this.font, this.stack.getHoverName(), this.width - 161, 6, 0xFFFFFF, false);
        pGuiGraphics.fill(this.width - 163, 16, Math.min(this.width + this.font.width(this.stack.getHoverName()) - 159, this.width - 6), 17, 0xFFFFFFFF);

        int posX1 = this.width - 163;
        int posX2 = this.width - 85;

        int posY1 = 20;
        int posY2 = 50;
        int posY3 = 80;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        RenderHelper.preciseBlit(pGuiGraphics, BARREL, posX1, posY1, 0, 0, 24, 24, 24, 24);
        if (!gunItem.hasCustomBarrel(stack)) {
            RenderHelper.preciseBlit(pGuiGraphics, INVALID, posX1, posY1, 0, 0, 24, 24, 24, 24);
        }

        RenderHelper.preciseBlit(pGuiGraphics, SCOPE, posX2, posY1, 0, 0, 24, 24, 24, 24);
        if (!gunItem.hasCustomScope(stack)) {
            RenderHelper.preciseBlit(pGuiGraphics, INVALID, posX2, posY1, 0, 0, 24, 24, 24, 24);
        }

        RenderHelper.preciseBlit(pGuiGraphics, GRIP, posX1, posY2, 0, 0, 24, 24, 24, 24);
        if (!gunItem.hasCustomGrip(stack)) {
            RenderHelper.preciseBlit(pGuiGraphics, INVALID, posX1, posY2, 0, 0, 24, 24, 24, 24);
        }

        RenderHelper.preciseBlit(pGuiGraphics, STOCK, posX2, posY2, 0, 0, 24, 24, 24, 24);
        if (!gunItem.hasCustomStock(stack)) {
            RenderHelper.preciseBlit(pGuiGraphics, INVALID, posX2, posY2, 0, 0, 24, 24, 24, 24);
        }

        RenderHelper.preciseBlit(pGuiGraphics, MAGAZINE, posX1, posY3, 0, 0, 24, 24, 24, 24);
        if (!gunItem.hasCustomMagazine(stack)) {
            RenderHelper.preciseBlit(pGuiGraphics, INVALID, posX1, posY3, 0, 0, 24, 24, 24, 24);
        }

        var currentData = GunData.from(itemStack);
        RenderHelper.preciseBlit(pGuiGraphics, AMMO_TYPE, posX2, posY3, 0, 0, 24, 24, 24, 24);
        if (currentData.ammoConsumers.size() <= 1) {
            RenderHelper.preciseBlit(pGuiGraphics, INVALID, posX2, posY3, 0, 0, 24, 24, 24, 24);
        } else {
            int size = currentData.ammoConsumers.size();
            float offset = 35f;
            int count = size / 2;

            float tempPos = size % 2 == 0 ? this.width - count * 6 + 1 : this.width - count * 6 - 2;
            for (int i = 0; i < size; i++) {
                RenderHelper.preciseBlit(pGuiGraphics,
                        i == currentData.selectedAmmoType.get() ? CHOSEN : NOT_CHOSEN,
                        tempPos - offset + 6 * i, posY3, 0, 0,
                        4, 4, 4, 4);
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        pose.popPose();
    }

    @Override
    protected void init() {
        super.init();

        int posX1 = this.width - 133;
        int posX2 = this.width - 55;

        int posY1 = 26;
        int posY2 = 56;
        int posY3 = 86;

        this.addRenderableWidget(new EditButton(posX1, posY1, 16, 16, 0, true));
        this.addRenderableWidget(new EditButton(posX1 + 24, posY1, 16, 16, 0, false));

        this.addRenderableWidget(new EditButton(posX2, posY1, 16, 16, 1, true));
        this.addRenderableWidget(new EditButton(posX2 + 24, posY1, 16, 16, 1, false));

        this.addRenderableWidget(new EditButton(posX1, posY2, 16, 16, 2, true));
        this.addRenderableWidget(new EditButton(posX1 + 24, posY2, 16, 16, 2, false));

        this.addRenderableWidget(new EditButton(posX2, posY2, 16, 16, 3, true));
        this.addRenderableWidget(new EditButton(posX2 + 24, posY2, 16, 16, 3, false));

        this.addRenderableWidget(new EditButton(posX1, posY3, 16, 16, 4, true));
        this.addRenderableWidget(new EditButton(posX1 + 24, posY3, 16, 16, 4, false));

        this.addRenderableWidget(new EditButton(posX2, posY3, 16, 16, 5, true));
        this.addRenderableWidget(new EditButton(posX2 + 24, posY3, 16, 16, 5, false));
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pMouseX < this.width - 165 || pMouseY < 4 || pMouseX > this.width - 4 || pMouseY > 110) {
            this.onClose();
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public void onClose() {
        super.onClose();
        ClientEventHandler.onCloseEditScreen();
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == ModKeyMappings.EDIT_MODE.getKey().getValue()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @OnlyIn(Dist.CLIENT)
    class EditButton extends AbstractButton {

        // 0 = barrel, 1 = scope, 2 = grip, 3 = stock, 4 = magazine, 5 = ammoType
        public int type;
        public boolean left;

        public EditButton(int pX, int pY, int pWidth, int pHeight, int type, boolean left) {
            super(pX, pY, pWidth, pHeight, Component.empty());
            this.type = type;
            this.left = left;
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            pGuiGraphics.pose().pushPose();

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

            if (this.isHovered && this.isActive()) {
                pGuiGraphics.blit(this.left ? BUTTON_LEFT_HOVERED : BUTTON_RIGHT_HOVERED, this.getX(), this.getY(),
                        0, 0, 16, 16, 16, 16);
            } else {
                pGuiGraphics.blit(this.left ? BUTTON_LEFT : BUTTON_RIGHT, this.getX(), this.getY(),
                        0, 0, 16, 16, 16, 16);
            }

            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);

            pGuiGraphics.pose().popPose();
        }

        @Override
        public void onPress() {
            if (!this.isActive()) return;
            Mod.PACKET_HANDLER.sendToServer(new EditMessage(this.type, !this.left));
            ClientEventHandler.editModelShake();
        }

        @Override
        public boolean isActive() {
            var stack = WeaponEditScreen.this.stack;
            if (!(stack.getItem() instanceof GunItem gunItem)) return false;
            var data = GunData.from(stack);

            return switch (this.type) {
                case 0 -> gunItem.hasCustomBarrel(stack);
                case 1 -> gunItem.hasCustomScope(stack);
                case 2 -> gunItem.hasCustomGrip(stack);
                case 3 -> gunItem.hasCustomStock(stack);
                case 4 -> gunItem.hasCustomMagazine(stack);
                case 5 -> data.ammoConsumers.size() > 1;
                default -> false;
            };
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
        }
    }
}
