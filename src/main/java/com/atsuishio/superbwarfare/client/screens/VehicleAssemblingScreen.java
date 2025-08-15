package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModRecipes;
import com.atsuishio.superbwarfare.menu.VehicleAssemblingMenu;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class VehicleAssemblingScreen extends AbstractContainerScreen<VehicleAssemblingMenu> {

    private static final ResourceLocation TEXTURE = Mod.loc("textures/gui/vehicle_assembling_table.png");
    private static final int IMAGE_SIZE = 324;

    private final Map<VehicleAssemblingRecipe.Category, List<ResourceLocation>> recipes = Maps.newLinkedHashMap();

    private List<ResourceLocation> currentRecipes = new ArrayList<>();
    private VehicleAssemblingRecipe.Category currentCategory = VehicleAssemblingRecipe.Category.LAND;

    public VehicleAssemblingScreen(VehicleAssemblingMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        imageWidth = 322;
        imageHeight = 178;
    }

    @Override
    protected void init() {
        super.init();
        this.initRecipes();
        this.addCategoryButtons();
    }

    public void initRecipes() {
        this.recipes.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        var recipeList = recipeManager.getAllRecipesFor(ModRecipes.VEHICLE_ASSEMBLING_TYPE.get());

//        for (var recipe : recipeList) {
//            this.recipes.computeIfAbsent(recipe.getCategory(), k -> Lists.newArrayList()).add(recipe.getId());
//        }
    }

    public void addCategoryButtons() {
        int posX = (this.width - this.imageWidth) / 2;
        int posY = (this.height - this.imageHeight) / 2 + 2;

        int i = 0;
        for (var category : VehicleAssemblingRecipe.Category.values()) {
            CategoryButton button = new CategoryButton(posX, posY + i * 23, category, b -> {
                this.currentCategory = category;
                this.init();
            });
            if (this.currentCategory.equals(category)) {
                button.setSelected(true);
            }
            this.addRenderableWidget(button);
            i++;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, IMAGE_SIZE, IMAGE_SIZE);

        this.renderRecipes(pGuiGraphics, pPartialTick, pMouseX, pMouseY);
    }

    public void renderRecipes(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        var recipes = this.recipes.get(this.currentCategory);
        if (recipes == null) return;

        for (var recipe : recipes) {
            guiGraphics.drawString(Minecraft.getInstance().font, recipe.getPath(), i + 10, j, 0xFFFFFF);
        }
    }

    // 本方法留空
    @Override
    protected void renderLabels(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    }

    public class CategoryButton extends Button {

        public VehicleAssemblingRecipe.Category category;
        private boolean isSelected = false;

        public CategoryButton(int x, int y, VehicleAssemblingRecipe.Category category, Button.OnPress onPress) {
            super(x, y, 20, 22, Component.literal("114"), onPress, DEFAULT_NARRATION);
            this.category = category;
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            pGuiGraphics.pose().pushPose();
            RenderSystem.enableDepthTest();

            if (VehicleAssemblingScreen.this.currentCategory != this.category) {
                this.isSelected = false;
            }

            if (this.isSelected) {
                pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 179, 179, 23, this.height, IMAGE_SIZE, IMAGE_SIZE);
            } else {
                pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 179, 202, 20, this.height, IMAGE_SIZE, IMAGE_SIZE);
            }

            pGuiGraphics.pose().popPose();
        }

        @Override
        public void onPress() {
            this.isSelected = true;
            this.onPress.onPress(this);
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
        }
    }
}
