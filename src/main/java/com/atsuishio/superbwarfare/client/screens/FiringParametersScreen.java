package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

// TODO 完成这个screen
@OnlyIn(Dist.CLIENT)
public class FiringParametersScreen extends Screen {

    private static final ResourceLocation TEXTURE = Mod.loc("textures/gui/firing_parameters.png");
    public static final Component TITLE = Component.translatable("item.superbwarfare.firing_parameters");

    private ItemStack stack;
    private final InteractionHand hand;

    public EditBox posX;
    public EditBox posY;
    public EditBox posZ;
    public EditBox radius;

    protected int imageWidth = 94;
    protected int imageHeight = 126;

    public FiringParametersScreen(ItemStack stack, InteractionHand hand) {
        super(TITLE);
        this.stack = stack;
        this.hand = hand;
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderPositions(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderBg(pGuiGraphics);
    }

    protected void renderPositions(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        var poseStack = pGuiGraphics.pose();

        poseStack.pushPose();

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        poseStack.rotateAround(Axis.ZP.rotationDegrees(5.5f), i + 41, j + 22, 0);

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
    }

    protected void subInit() {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        this.posX = new EditBox(this.font, i + 41, j + 20, 40, 12, Component.empty());
        this.initEditBox(this.posX);

        this.posY = new EditBox(this.font, i + 40, j + 37, 40, 12, Component.empty());
        this.initEditBox(this.posY);

        this.posZ = new EditBox(this.font, i + 39, j + 54, 40, 12, Component.empty());
        this.initEditBox(this.posZ);

        this.radius = new EditBox(this.font, i + 38, j + 71, 40, 12, Component.empty());
        this.initEditBox(this.radius);
    }

    protected void initEditBox(EditBox editBox) {
        editBox.setCanLoseFocus(true);
        editBox.setTextColor(0x5b4c3c);
        editBox.setTextColorUneditable(0x5b4c3c);
        editBox.setBordered(true);
        editBox.setMaxLength(10);
        this.addWidget(editBox);
        editBox.setEditable(true);
        editBox.setFilter(s -> s.matches("-?\\d*"));
    }
}
