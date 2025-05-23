package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.DogTag;
import com.atsuishio.superbwarfare.menu.DogTagEditorMenu;
import com.atsuishio.superbwarfare.network.message.send.DogTagFinishEditMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
public class DogTagEditorScreen extends AbstractContainerScreen<DogTagEditorMenu> {

    private static final ResourceLocation TEXTURE = Mod.loc("textures/gui/dog_tag_editor.png");

    public EditBox name;
    private short currentColor = 0;
    private short[][] icon = new short[16][16];
    public ItemStack stack;
    private boolean init = false;

    public DogTagEditorScreen(DogTagEditorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.stack = pMenu.stack;
        imageWidth = 207;
        imageHeight = 185;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        ItemStack stack = DogTagEditorScreen.this.menu.stack;
        pGuiGraphics.renderItem(stack, i + 18, j + 36);

        var pose = pGuiGraphics.pose();

        pose.pushPose();

        for (int x = 0; x < this.icon.length; x++) {
            for (int y = 0; y < this.icon.length; y++) {
                int num = this.icon[x][y];
                if (num != -1) {
                    var color = ChatFormatting.getById(num);
                    pGuiGraphics.fill(i + 66 + x * 9, j + 44 + y * 9, i + 58 + x * 9, j + 36 + y * 9,
                            getColorFromFormatting(color));
                }
            }
        }

        pose.popPose();
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.name.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        this.drawColor(pMouseX, pMouseY, pButton);
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        this.drawColor(pMouseX, pMouseY, pButton);
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    private void drawColor(double pMouseX, double pMouseY, int pButton) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        if (pMouseX >= i + 57 && pMouseX <= i + 201 && pMouseY >= j + 36 && pMouseY <= j + 179) {
            double posX = pMouseX - i - 57;
            double posY = pMouseY - j - 36;
            if (Math.ceil(posX) % 9 == 0 || Math.ceil(posY) % 9 == 0)
                return;

            int x = (int) Math.floor(posX / 9);
            int y = (int) Math.floor(posY / 9);

            this.icon[Mth.clamp(x, 0, 15)][Mth.clamp(y, 0, 15)] = pButton == 0 ? this.currentColor : -1;
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
//        this.name.tick();
        if (!this.init) {
            if (!this.stack.isEmpty()) {
                this.name.setValue(this.stack.getHoverName().getString());
                this.icon = DogTag.getColors(this.stack);
            }
            this.init = true;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.subInit();

        this.clearColors();

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        for (short k = 0; k < 16; k++) {
            var button = new ColorButton(k, i + 6 + (k % 2) * 22, j + 62 + (k / 2) * 10, 18, 8);
            this.addRenderableWidget(button);
        }
        var eraserButton = new ColorButton((short) -1, i + 17, j + 143, 18, 8);
        this.addRenderableWidget(eraserButton);

        var finishButton = new FinishButton(i + 6, j + 167, 40, 13);
        this.addRenderableWidget(finishButton);
    }

    protected void subInit() {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        this.name = new EditBox(this.font, i + 9, j + 11, 180, 12, Component.empty());
        this.name.setCanLoseFocus(false);
        this.name.setTextColor(-1);
        this.name.setTextColorUneditable(-1);
        this.name.setBordered(false);
        this.name.setMaxLength(30);
        this.name.setResponder(this::onNameChanged);
//        this.name.setValue(this.stack.getHoverName().getString());
        this.addWidget(this.name);
        this.setInitialFocus(this.name);
        this.name.setEditable(true);
    }

    private void onNameChanged(String name) {
        String s = name;
        ItemStack stack = DogTagEditorScreen.this.menu.stack;
        if (!stack.has(DataComponents.CUSTOM_NAME)) {
            s = "";
        }

        if (this.menu.setItemName(s)) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.connection.send(new ServerboundRenameItemPacket(s));
            }
        }
    }

    // 留空
    @Override
    protected void renderLabels(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    }

    public void clearColors() {
        for (var el : this.icon) {
            Arrays.fill(el, (short) -1);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class ColorButton extends AbstractButton {

        short color;

        public ColorButton(short color, int pX, int pY, int pWidth, int pHeight) {
            super(pX, pY, pWidth, pHeight, Component.empty());
            this.color = color;
        }

        @Override
        public void onPress() {
            DogTagEditorScreen.this.currentColor = this.color;
            if (this.color == -1 && Screen.hasShiftDown()) {
                DogTagEditorScreen.this.clearColors();
            }
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            if (this.isHovered || DogTagEditorScreen.this.currentColor == this.color) {
                if (this.color == -1) {
                    pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 19, 186,
                            18, 8, 256, 256);
                } else {
                    pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 0, 186,
                            18, 8, 256, 256);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class FinishButton extends AbstractButton {

        public FinishButton(int pX, int pY, int pWidth, int pHeight) {
            super(pX, pY, pWidth, pHeight, Component.empty());
        }

        @Override
        public void onPress() {
            if (!DogTagEditorScreen.this.init) return;
            var colors = new ArrayList<Short>(DogTagEditorScreen.this.icon.length * DogTagEditorScreen.this.icon[0].length);

            for (var row : DogTagEditorScreen.this.icon) {
                for (var color : row) {
                    colors.add(color);
                }
            }

            PacketDistributor.sendToServer(new DogTagFinishEditMessage(colors, DogTagEditorScreen.this.name.getValue()));
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            if (this.isHovered) {
                pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 0, 195,
                        40, 13, 256, 256);
            }
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
        }
    }

    public static int getColorFromFormatting(ChatFormatting chatFormatting) {
        if (chatFormatting == null) {
            return -1;
        }
        return switch (chatFormatting) {
            case BLACK -> 0xFF000000;
            case DARK_BLUE -> 0xFF0000AA;
            case DARK_GREEN -> 0xFF00AA00;
            case DARK_AQUA -> 0xFF00AAAA;
            case DARK_RED -> 0xFFAA0000;
            case DARK_PURPLE -> 0xFFAA00AA;
            case GOLD -> 0xFFFFAA00;
            case GRAY -> 0xFFAAAAAA;
            case DARK_GRAY -> 0xFF555555;
            case BLUE -> 0xFF5555FF;
            case GREEN -> 0xFF55FF55;
            case AQUA -> 0xFF55FFFF;
            case RED -> 0xFFFF5555;
            case LIGHT_PURPLE -> 0xFFFF55FF;
            case YELLOW -> 0xFFFFFF55;
            case WHITE -> 0xFFFFFFFF;
            default -> -1;
        };
    }
}
