package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.DogTag;
import com.atsuishio.superbwarfare.network.message.send.DogTagFinishEditMessage;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
public class DogTagEditorScreen extends Screen {

    private static final ResourceLocation TEXTURE = Mod.loc("textures/gui/dog_tag_editor.png");

    public EditBox name;
    private short currentColor = 0;
    private short[][] icon = new short[16][16];

    public ItemStack stack;

    private boolean init = false;

    protected int imageWidth;
    protected int imageHeight;

    @Nullable
    private String itemName;

    public DogTagEditorScreen(ItemStack stack) {
        super(GameNarrator.NO_TITLE);
        this.stack = stack;
        imageWidth = 207;
        imageHeight = 185;
    }

    protected void renderBg(GuiGraphics pGuiGraphics) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        pGuiGraphics.renderItem(stack, i + 18, j + 36);

        var pose = pGuiGraphics.pose();

        pose.pushPose();

        for (int x = 0; x < this.icon.length; x++) {
            for (int y = 0; y < this.icon.length; y++) {
                short num = this.icon[x][y];
                if (num != -1) {
                    pGuiGraphics.fill(i + 66 + x * 9, j + 44 + y * 9, i + 58 + x * 9, j + 36 + y * 9,
                            getColorByNum(num));
                }
            }
        }

        pose.popPose();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        this.renderBg(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.name.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
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
    public void tick() {
        super.tick();
        this.name.tick();
        if (!this.init) {
            if (!this.stack.isEmpty()) {
                this.name.setValue(this.stack.getHoverName().getString());
                this.icon = DogTag.getColors(this.stack);
            }
            this.init = true;
            this.name.setEditable(true);
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
        this.addWidget(this.name);
        this.setInitialFocus(this.name);
        this.name.setEditable(false);
    }

    private void onNameChanged(String name) {
        String s = name;
        if (!stack.hasCustomHoverName() && name.equals(stack.getHoverName().getString())) {
            s = "";
        }

        if (this.setItemName(s)) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.connection.send(new ServerboundRenameItemPacket(s));
            }
        }
    }

    public void clearColors() {
        for (var el : this.icon) {
            Arrays.fill(el, (short) -1);
        }
    }

    @Nullable
    private static String validateName(String pItemName) {
        String s = SharedConstants.filterText(pItemName);
        return s.length() <= 30 ? s : null;
    }

    public boolean setItemName(String pItemName) {
        String s = validateName(pItemName);
        if (s != null && !s.equals(this.itemName)) {
            this.itemName = s;
            if (!this.stack.isEmpty()) {
                if (Util.isBlank(s)) {
                    this.stack.resetHoverName();
                } else {
                    this.stack.setHoverName(Component.literal(s));
                }
            }
            return true;
        } else {
            return false;
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
            if (DogTagEditorScreen.this.minecraft != null) {
                DogTagEditorScreen.this.minecraft.setScreen(null);
            }
            this.updateLocal(DogTagEditorScreen.this.icon, DogTagEditorScreen.this.name.getValue());
            Mod.PACKET_HANDLER.sendToServer(new DogTagFinishEditMessage(DogTagEditorScreen.this.icon, DogTagEditorScreen.this.name.getValue()));
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

        protected void updateLocal(short[][] colors, String name) {
            CompoundTag colorsTag = new CompoundTag();
            for (int i = 0; i < colors.length; i++) {
                int[] color = new int[colors[i].length];
                for (int j = 0; j < colors[i].length; j++) {
                    color[j] = colors[i][j];
                }
                colorsTag.putIntArray("Color" + i, color);
            }
            DogTagEditorScreen.this.stack.getOrCreateTag().put("Colors", colorsTag);

            if (!name.isEmpty()) {
                DogTagEditorScreen.this.stack.setHoverName(Component.literal(name));
            }
        }
    }

    public static int getColorByNum(short num) {
        return switch (num) {
            case 0 -> 0xFF000000;
            case 1 -> 0xFFFFFFFF;
            case 2 -> 0xFF808080;
            case 3 -> 0xFFD42424;
            case 4 -> 0xFFFFAA00;
            case 5 -> 0xFFFFFF00;
            case 6 -> 0xFF3CE03C;
            case 7 -> 0xFF66CCFF;
            case 8 -> 0xFF3A4FFF;
            case 9 -> 0xFFB654FF;
            case 10 -> 0xFF7D5841;
            case 11 -> 0xFFFF97A7;
            case 12 -> 0xFF76945E;
            case 13 -> 0xFFFFC400;
            case 14 -> 0xFF4C425B;
            case 15 -> 0xFFF8E4D0;
            default -> -1;
        };
    }
}
