package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.ChargingStationBlockEntity;
import com.atsuishio.superbwarfare.menu.ChargingStationMenu;
import com.atsuishio.superbwarfare.network.message.send.ShowChargingRangeMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class ChargingStationScreen extends AbstractContainerScreen<ChargingStationMenu> {

    private static final ResourceLocation TEXTURE = Mod.loc("textures/gui/charging_station.png");

    public ChargingStationScreen(ChargingStationMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);

        long fuelTick = ChargingStationScreen.this.menu.getFuelTick();
        long maxFuelTick = ChargingStationScreen.this.menu.getMaxFuelTick();
        long energy = ChargingStationScreen.this.menu.getEnergy();

        if (maxFuelTick == 0) {
            maxFuelTick = ChargingStationBlockEntity.DEFAULT_FUEL_TIME;
        }

        // Fuel
        float fuelRate = (float) fuelTick / (float) maxFuelTick;
        pGuiGraphics.blit(TEXTURE, i + 45, j + 51 - (int) (13 * fuelRate), 177, 14 - (int) (13 * fuelRate), 13, (int) (13 * fuelRate));

        // Energy
        float energyRate = (float) energy / (float) ChargingStationBlockEntity.MAX_ENERGY;
        pGuiGraphics.blit(TEXTURE, i + 80, j + 70 - (int) (54 * energyRate),
                177, 17, 16, (int) (54 * energyRate));
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics pGuiGraphics, int pX, int pY) {
        super.renderTooltip(pGuiGraphics, pX, pY);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("des.superbwarfare.charging_station.energy", ChargingStationScreen.this.menu.getEnergy(),
                ChargingStationBlockEntity.MAX_ENERGY));

        if ((pX - i) >= 80 && (pX - i) <= 96 && (pY - j) >= 16 && (pY - j) <= 70) {
            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class ShowRangeButton extends AbstractButton {

        @Override
        protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            this.setMessage(ChargingStationScreen.this.menu.showRange() ? Component.translatable("container.superbwarfare.charging_station.hide_range") : Component.translatable("container.superbwarfare.charging_station.show_range"));
            super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        public ShowRangeButton(int pX, int pY) {
            super(pX + 7, pY + 55, 33, 14, Component.translatable("container.superbwarfare.charging_station.show_range"));
        }

        @Override
        public void onPress() {
            PacketDistributor.sendToServer(new ShowChargingRangeMessage(!ChargingStationScreen.this.menu.showRange()));
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
        }
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 74;

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        this.addRenderableWidget(new ShowRangeButton(i, j));
    }

}
