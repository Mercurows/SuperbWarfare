package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.mojang.math.Axis;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// TODO 完成这个screen
@OnlyIn(Dist.CLIENT)
public class FiringParametersScreen extends Screen {

    private static final ResourceLocation TEXTURE = Mod.loc("textures/gui/firing_parameters.png");

    private ItemStack stack;
    private final InteractionHand hand;

    public EditBox posX;
    public EditBox posY;
    public EditBox posZ;
    public EditBox radius;

    public boolean isDepressed;

    protected int imageWidth = 94;
    protected int imageHeight = 126;

    public FiringParametersScreen(ItemStack stack, InteractionHand hand) {
        super(GameNarrator.NO_TITLE);
        this.stack = stack;
        this.hand = hand;
        if (!stack.isEmpty()) {
            this.isDepressed = stack.getOrCreateTag().getBoolean("IsDepressed");
        }
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        this.renderBg(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderPositions(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    protected void renderPositions(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        var poseStack = pGuiGraphics.pose();

        poseStack.pushPose();

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        poseStack.rotateAround(Axis.ZP.rotationDegrees(5f), i + 41, j + 22, 0);

        this.posX.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.posY.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.posZ.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.radius.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        poseStack.popPose();
    }

    protected void renderBg(GuiGraphics pGuiGraphics) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 140, 140);
    }

    @Override
    protected void init() {
        super.init();
        this.subInit();

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        var modeButton = new ModeButton(i + 12, j + 89, 35, 20);
        this.addRenderableWidget(modeButton);

        var doneButton = new DoneButton(i + 50, j + 94, 23, 14);
        this.addRenderableWidget(doneButton);
    }

    protected void subInit() {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        this.posX = new EditBox(this.font, i + 44, j + 20, 40, 12, Component.empty());
        this.initEditBox(this.posX);

        this.posY = new EditBox(this.font, i + 43, j + 37, 40, 12, Component.empty());
        this.initEditBox(this.posY);

        this.posZ = new EditBox(this.font, i + 42, j + 54, 40, 12, Component.empty());
        this.initEditBox(this.posZ);

        this.radius = new EditBox(this.font, i + 41, j + 71, 40, 12, Component.empty());
        this.initEditBox(this.radius);
    }

    protected void initEditBox(EditBox editBox) {
        editBox.setCanLoseFocus(true);
        editBox.setTextColor(0xb29f7c);
        editBox.setTextColorUneditable(0x5b4c3c);
        editBox.setBordered(false);
        editBox.setMaxLength(10);
        this.addWidget(editBox);
        editBox.setEditable(true);
        editBox.setFilter(s -> s.matches("-?\\d*"));
    }

    @OnlyIn(Dist.CLIENT)
    class ModeButton extends AbstractButton {

        public ModeButton(int pX, int pY, int pWidth, int pHeight) {
            super(pX, pY, pWidth, pHeight, Component.empty());
        }

        @Override
        public void onPress() {
            FiringParametersScreen.this.isDepressed = !FiringParametersScreen.this.isDepressed;
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            boolean isDepressed = FiringParametersScreen.this.isDepressed;
            pGuiGraphics.blit(TEXTURE, this.getX(), isDepressed ? this.getY() + 10 : this.getY(), 96, isDepressed ? 37 : 16,
                    35, isDepressed ? 10 : 20, 140, 140);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class DoneButton extends AbstractButton {

        public DoneButton(int pX, int pY, int pWidth, int pHeight) {
            super(pX, pY, pWidth, pHeight, Component.empty());
        }

        @Override
        public void onPress() {

        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            if (this.isHovered) {
                pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 95, 1, 23, 14, 140, 140);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        }
    }
}
