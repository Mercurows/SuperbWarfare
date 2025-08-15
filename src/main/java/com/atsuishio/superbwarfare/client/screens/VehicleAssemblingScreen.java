package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModRecipes;
import com.atsuishio.superbwarfare.menu.VehicleAssemblingMenu;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Code based on TaC-Z
 */
@OnlyIn(Dist.CLIENT)
public class VehicleAssemblingScreen extends AbstractContainerScreen<VehicleAssemblingMenu> {

    private static final ResourceLocation TEXTURE = Mod.loc("textures/gui/vehicle_assembling_table.png");
    private static final int IMAGE_SIZE = 324;

    private final Map<VehicleAssemblingRecipe.Category, List<ResourceLocation>> recipes = Maps.newLinkedHashMap();

    private VehicleAssemblingRecipe.Category currentCategory = VehicleAssemblingRecipe.Category.LAND;
    @Nullable
    private List<ResourceLocation> currentRecipes = new ArrayList<>();
    @Nullable
    private VehicleAssemblingRecipe currentRecipe = null;
    private int scrollOffset = 0;

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
        this.addRecipeButtons();
    }

    public void initRecipes() {
        this.recipes.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        List<VehicleAssemblingRecipe> recipeList = recipeManager.getAllRecipesFor(ModRecipes.VEHICLE_ASSEMBLING_TYPE.get());

        for (var recipe : recipeList) {
            this.recipes.computeIfAbsent(recipe.getCategory(), k -> Lists.newArrayList()).add(recipe.getId());
        }
        this.currentRecipes = this.recipes.get(this.currentCategory);
        this.currentRecipe = this.getRecipeById(this.currentRecipes == null || this.currentRecipes.isEmpty() ? null : this.currentRecipes.get(0));
    }

    public void addCategoryButtons() {
        int posX = (this.width - this.imageWidth) / 2;
        int posY = (this.height - this.imageHeight) / 2 + 2;

        int i = 0;
        for (var category : VehicleAssemblingRecipe.Category.values()) {
            // TODO 这里配方选择有点问题
            CategoryButton button = new CategoryButton(posX, posY + i * 23, category, b -> {
                this.currentCategory = category;
                this.currentRecipes = this.recipes.get(category);
                this.currentRecipe = this.getRecipeById(this.currentRecipes == null || this.currentRecipes.isEmpty() ? null : this.currentRecipes.get(0));

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
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        pGuiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, IMAGE_SIZE, IMAGE_SIZE);
    }

    // 本方法留空
    @Override
    protected void renderLabels(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    }

    @Nullable
    private VehicleAssemblingRecipe getRecipeById(ResourceLocation recipeId) {
        if (recipeId == null) return null;
        if (Minecraft.getInstance().level != null) {
            RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
            Recipe<?> recipe = recipeManager.byKey(recipeId).orElse(null);
            if (recipe instanceof VehicleAssemblingRecipe assemblingRecipe) {
                return assemblingRecipe;
            }
        }
        return null;
    }

    public void addRecipeButtons() {
        if (this.currentRecipes == null || this.currentRecipes.isEmpty()) return;

        int posX = (this.width - this.imageWidth) / 2;
        int posY = (this.height - this.imageHeight) / 2;

        for (int i = 0; i < 9; i++) {
            int index = i + this.scrollOffset;
            if (index >= this.currentRecipes.size()) break;

            ResourceLocation id = this.currentRecipes.get(index);
            var recipe = this.getRecipeById(id);
            if (recipe == null) break;

            RecipeButton button = this.addRenderableWidget(new RecipeButton(posX + 26, posY + 21 + i * 15, recipe.getResult().getResult(), (b) -> {
                this.currentRecipe = recipe;

                this.init();
            }));
            if (this.currentRecipe != null && recipe.getId().equals(this.currentRecipe.getId())) {
                button.setSelected(true);
            }
        }
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

    public class RecipeButton extends Button {

        private final ItemStack stack;
        private boolean isSelected = false;

        public RecipeButton(int x, int y, ItemStack stack, Button.OnPress onPress) {
            super(x, y, 73, 16, Component.literal("114"), onPress, DEFAULT_NARRATION);
            this.stack = stack;
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            if (this.isHovered) {
                pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 13, 196, this.width, this.height, IMAGE_SIZE, IMAGE_SIZE);
            } else {
                pGuiGraphics.blit(TEXTURE, this.getX(), this.getY(), 13, 179, this.width, this.height, IMAGE_SIZE, IMAGE_SIZE);
            }

            pGuiGraphics.renderFakeItem(this.stack, this.getX() + 1, this.getY());
            Component hoverName = this.stack.getHoverName();
            renderScrollingString(pGuiGraphics, Minecraft.getInstance().font, hoverName, this.getX() + 20, this.getY() + 4, this.getX() + 92, this.getY() + 13, 16777215);
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
        }
    }
}
