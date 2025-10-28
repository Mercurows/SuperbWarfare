package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.send.FiringParametersEditMessage;
import net.minecraft.ChatFormatting;
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
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ArtilleryIndicatorScreen extends Screen {

    private static final ResourceLocation TEXTURE = Mod.loc("textures/gui/artillery_indicator.png");

    private final ItemStack stack;
    private final InteractionHand hand;

    public EditBox posX;
    public EditBox posY;
    public EditBox posZ;
    public EditBox radius;

    public boolean isDepressed;

    private boolean init = false;

    protected int imageWidth = 176;
    protected int imageHeight = 84;

    public ArtilleryIndicatorScreen(ItemStack stack, InteractionHand hand) {
        super(Component.translatable("item.superbwarfare.artillery_indicator"));
        this.stack = stack;
        this.hand = hand;
        if (!stack.isEmpty()) {
            this.isDepressed = stack.getOrCreateTag().getBoolean("IsDepressed");
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.init) {
            if (!this.stack.isEmpty()) {
                this.posX.setValue("" + stack.getOrCreateTag().getInt("TargetX"));
                this.posY.setValue("" + stack.getOrCreateTag().getInt("TargetY"));
                this.posZ.setValue("" + stack.getOrCreateTag().getInt("TargetZ"));
                this.radius.setValue("" + Math.max(0, stack.getOrCreateTag().getInt("Radius")));
            }
            this.init = true;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        this.renderBg(pGuiGraphics, pMouseX, pMouseY);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderPositions(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    protected void renderPositions(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        var poseStack = pGuiGraphics.pose();

        poseStack.pushPose();

        this.posX.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.posY.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.posZ.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.radius.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        poseStack.popPose();
    }

    protected void renderBg(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        if (pMouseX >= i + 98 && pMouseX <= i + 162 && pMouseY >= j + 19 && pMouseY <= j + 49) {
            pGuiGraphics.renderTooltip(this.font,
                    this.isDepressed ?
                            Component.translatable("tips.superbwarfare.mortar.target_pos.depressed_trajectory").withStyle(ChatFormatting.WHITE) :
                            Component.translatable("tips.superbwarfare.mortar.target_pos.lofted_trajectory").withStyle(ChatFormatting.WHITE),
                    pMouseX, pMouseY);
        }

        pGuiGraphics.drawString(this.font, this.title, i + 6, j + 6, 4210752, false);
    }

    @Override
    protected void init() {
        super.init();
        this.subInit();

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        var modeButton = new ModeButton(i + 99, j + 19, 64, 30);
        this.addRenderableWidget(modeButton);

        var doneButton = new DoneButton(i + 113, j + 54, 48, 15);
        this.addRenderableWidget(doneButton);
    }

    protected void subInit() {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        this.posX = new EditBox(this.font, i + 24, j + 20, 60, 12, Component.empty());
        this.initEditBox(this.posX);

        this.posY = new EditBox(this.font, i + 24, j + 33, 60, 12, Component.empty());
        this.initEditBox(this.posY);

        this.posZ = new EditBox(this.font, i + 24, j + 46, 60, 12, Component.empty());
        this.initEditBox(this.posZ);

        this.radius = new EditBox(this.font, i + 24, j + 59, 20, 12, Component.empty());
        this.initEditBox(this.radius);
        this.radius.setMaxLength(2);
        this.radius.setFilter(s -> s.matches("\\d*"));
    }

    protected void initEditBox(EditBox editBox) {
        editBox.setCanLoseFocus(true);
        editBox.setTextColor(-1);
        editBox.setTextColorUneditable(-1);
        editBox.setBordered(false);
        editBox.setMaxLength(9);
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
            ArtilleryIndicatorScreen.this.isDepressed = !ArtilleryIndicatorScreen.this.isDepressed;
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            boolean isDepressed = ArtilleryIndicatorScreen.this.isDepressed;
            pGuiGraphics.blit(TEXTURE, this.getX(), isDepressed ? this.getY() + 14 : this.getY(), 177, isDepressed ? 62 : 33,
                    61, isDepressed ? 14 : 28, 256, 256);
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    class DoneButton extends AbstractButton {

        public DoneButton(int pX, int pY, int pWidth, int pHeight) {
            super(pX, pY, pWidth, pHeight, Component.empty());
        }

        @Override
        public void onPress() {
            if (!ArtilleryIndicatorScreen.this.init) return;
            if (ArtilleryIndicatorScreen.this.minecraft != null) {
                ArtilleryIndicatorScreen.this.minecraft.setScreen(null);
            }
            NetworkRegistry.PACKET_HANDLER.sendToServer(
                    new FiringParametersEditMessage(
                            getEditBoxValue(ArtilleryIndicatorScreen.this.posX.getValue()),
                            getEditBoxValue(ArtilleryIndicatorScreen.this.posY.getValue()),
                            getEditBoxValue(ArtilleryIndicatorScreen.this.posZ.getValue()),
                            Math.max(0, getEditBoxValue(ArtilleryIndicatorScreen.this.radius.getValue())),
                            ArtilleryIndicatorScreen.this.isDepressed,
                            ArtilleryIndicatorScreen.this.hand == InteractionHand.MAIN_HAND
                    )
            );
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 177, this.isHovered ? 16 : 0, 48, 15, 256, 256);
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
        }

        public int getEditBoxValue(String value) {
            if (value.equals("-")) return 0;
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
