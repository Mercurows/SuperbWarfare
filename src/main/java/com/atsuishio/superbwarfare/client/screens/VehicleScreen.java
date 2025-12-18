package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleContainerType;
import com.atsuishio.superbwarfare.menu.VehicleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

// TODO 完成渲染
@OnlyIn(Dist.CLIENT)
public class VehicleScreen extends AbstractContainerScreen<VehicleMenu> {

    private static final ResourceLocation MINI = Mod.loc("textures/gui/vehicle/inventory/mini.png");
    private static final ResourceLocation SMALL = Mod.loc("textures/gui/vehicle/inventory/small.png");
    private static final ResourceLocation MEDIUM = Mod.loc("textures/gui/vehicle/inventory/medium.png");
    private static final ResourceLocation LARGE = Mod.loc("textures/gui/vehicle/inventory/large.png");
    private static final ResourceLocation HUGE = Mod.loc("textures/gui/vehicle/inventory/huge.png");

    private static final ResourceLocation INVENTORY = Mod.loc("textures/gui/vehicle/inventory/player_inventory.png");
    private static final ResourceLocation INVENTORY_UPGRADE = Mod.loc("textures/gui/vehicle/inventory/player_inventory_upgrade.png");

    private final VehicleContainerType type;
    private final boolean hasUpgradeSlots;

    public static final int X_OFFSET = 8;
    public static final int INVENTORY_TEXTURE_SIZE = 256;

    public VehicleScreen(VehicleMenu pMenu, Inventory pPlayerInventory, Component pTitle, VehicleContainerType type) {
        super(pMenu, pPlayerInventory, pTitle);
        this.type = type;
        this.hasUpgradeSlots = pMenu.hasUpgradeSlots();

        this.imageWidth = switch (type) {
            default -> 222;
            case HUGE -> 320;
        };
        this.imageHeight = switch (type) {
            default -> 132;
            case HUGE -> 132;
        } + 90;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        var texture = switch (type) {
            default -> null;
            case MINI -> MINI;
            case SMALL -> SMALL;
            case MEDIUM -> MEDIUM;
            case LARGE -> LARGE;
            case HUGE -> HUGE;
        };
        if (texture == null) return;

        int size = this.type == VehicleContainerType.HUGE ? 328 : 256;
        pGuiGraphics.blit(texture, i + X_OFFSET, j, 0, 0, this.imageWidth, this.imageHeight, size, size);

        int x = i + X_OFFSET + (this.menu.getContainerCols() - 9) / 2 * 18;

        if (this.hasUpgradeSlots) {
            pGuiGraphics.blit(INVENTORY_UPGRADE, x - 24, j + this.imageHeight - 90, 0, 0, 198, 90, INVENTORY_TEXTURE_SIZE, INVENTORY_TEXTURE_SIZE);
        } else {
            pGuiGraphics.blit(INVENTORY, x, j + this.imageHeight - 90, 0, 0, 175, 90, INVENTORY_TEXTURE_SIZE, INVENTORY_TEXTURE_SIZE);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 15;
        this.titleLabelY = 5;
        this.inventoryLabelX = (this.menu.getContainerCols() - 9) / 2 * 18 + this.titleLabelX;
        this.inventoryLabelY = 128;
    }
}
