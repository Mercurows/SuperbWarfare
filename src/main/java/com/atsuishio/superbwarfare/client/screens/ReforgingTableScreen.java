package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.menu.ReforgingTableMenu;
import com.atsuishio.superbwarfare.network.message.send.GunReforgeMessage;
import com.atsuishio.superbwarfare.network.message.send.SetPerkLevelMessage;
import com.atsuishio.superbwarfare.perk.Perk;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ReforgingTableScreen extends AbstractContainerScreen<ReforgingTableMenu> {

    private static final ResourceLocation TEXTURE = Mod.loc("textures/gui/reforging_table.png");

    public ReforgingTableScreen(ReforgingTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        imageWidth = 176;
        imageHeight = 177;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 200, 200);
        RenderSystem.disableBlend();
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        var ammoPerkLevel = ReforgingTableScreen.this.menu.ammoPerkLevel.get();
        var funcPerkLevel = ReforgingTableScreen.this.menu.funcPerkLevel.get();
        var damagePerkLevel = ReforgingTableScreen.this.menu.damagePerkLevel.get();

        if (ammoPerkLevel > 0) {
            renderNumber(pGuiGraphics, this.leftPos + 136, this.topPos + 31, 1, 178, ammoPerkLevel);
        }

        if (funcPerkLevel > 0) {
            renderNumber(pGuiGraphics, this.leftPos + 146, this.topPos + 31, 1, 184, funcPerkLevel);
        }

        if (damagePerkLevel > 0) {
            renderNumber(pGuiGraphics, this.leftPos + 156, this.topPos + 31, 1, 190, damagePerkLevel);
        }

        var upgradePoint = ReforgingTableScreen.this.menu.upgradePoint.get();
        renderNumber(pGuiGraphics, this.leftPos + 43, this.topPos + 20, 51, 178, upgradePoint);

        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    private void renderNumber(GuiGraphics guiGraphics, int x, int y, int u, int v, int number) {
        int g = number / 10;
        int s = number % 10;
        guiGraphics.blit(TEXTURE, x, y, u + 5 * g, v, 5, 5, 200, 200);
        guiGraphics.blit(TEXTURE, x + 4, y, u + 5 * s, v, 5, 5, 200, 200);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 2;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 85;

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        ReforgeButton button = new ReforgeButton(i + 124, j + 70);
        UpgradeButton ammoUpgrade = new UpgradeButton(i + 98, j + 21, Perk.Type.AMMO);
        DowngradeButton ammoDowngrade = new DowngradeButton(i + 69, j + 21, Perk.Type.AMMO);
        UpgradeButton funcUpgrade = new UpgradeButton(i + 98, j + 41, Perk.Type.FUNCTIONAL);
        DowngradeButton funcDowngrade = new DowngradeButton(i + 69, j + 41, Perk.Type.FUNCTIONAL);
        UpgradeButton damageUpgrade = new UpgradeButton(i + 98, j + 61, Perk.Type.DAMAGE);
        DowngradeButton damageDowngrade = new DowngradeButton(i + 69, j + 61, Perk.Type.DAMAGE);

        this.addRenderableWidget(button);
        this.addRenderableWidget(ammoUpgrade);
        this.addRenderableWidget(ammoDowngrade);
        this.addRenderableWidget(funcUpgrade);
        this.addRenderableWidget(funcDowngrade);
        this.addRenderableWidget(damageUpgrade);
        this.addRenderableWidget(damageDowngrade);
    }

    @OnlyIn(Dist.CLIENT)
    static class ReforgeButton extends AbstractButton {

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), this.isHovered() ? 81 : 51, 184, 29, 15, 200, 200);
        }

        public ReforgeButton(int pX, int pY) {
            super(pX, pY, 40, 16, Component.empty());
        }


        @Override
        public void onPress() {
            PacketDistributor.sendToServer(new GunReforgeMessage(0));
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    class UpgradeButton extends AbstractButton {
        public Perk.Type type;

        public UpgradeButton(int pX, int pY, Perk.Type type) {
            super(pX, pY, 9, 9, Component.empty());
            this.type = type;
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 187, this.isHovered() ? 10 : 0, 9, 9, 200, 200);
        }

        @Override
        public void onPress() {
            if (ReforgingTableScreen.this.menu.getPerkItemBySlot(type) == ItemStack.EMPTY) {
                return;
            }
            switch (type) {
                case AMMO -> {
                    if (ReforgingTableScreen.this.menu.ammoPerkLevel.get() >= ReforgingTableMenu.MAX_PERK_LEVEL) {
                        return;
                    }
                }
                case FUNCTIONAL -> {
                    if (ReforgingTableScreen.this.menu.funcPerkLevel.get() >= ReforgingTableMenu.MAX_PERK_LEVEL) {

                        return;
                    }
                }
                case DAMAGE -> {
                    if (ReforgingTableScreen.this.menu.damagePerkLevel.get() >= ReforgingTableMenu.MAX_PERK_LEVEL) {
                        return;
                    }
                }
            }

            PacketDistributor.sendToServer(new SetPerkLevelMessage(type.ordinal(), true));
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {

        }
    }

    @OnlyIn(Dist.CLIENT)
    class DowngradeButton extends AbstractButton {
        public Perk.Type type;

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 177, this.isHovered() ? 10 : 0, 9, 9, 200, 200);
        }

        public DowngradeButton(int pX, int pY, Perk.Type type) {
            super(pX, pY, 12, 12, Component.empty());
            this.type = type;
        }

        @Override
        public void onPress() {
            if (ReforgingTableScreen.this.menu.getPerkItemBySlot(type) == ItemStack.EMPTY) {
                return;
            }
            switch (type) {
                case AMMO -> {
                    if (ReforgingTableScreen.this.menu.ammoPerkLevel.get() <= 1) {
                        return;
                    }
                }
                case FUNCTIONAL -> {
                    if (ReforgingTableScreen.this.menu.funcPerkLevel.get() <= 1) {
                        return;
                    }
                }
                case DAMAGE -> {
                    if (ReforgingTableScreen.this.menu.damagePerkLevel.get() <= 1) {
                        return;
                    }
                }
            }

            PacketDistributor.sendToServer(new SetPerkLevelMessage(type.ordinal(), false));
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
        }
    }
}