package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.screens.component.AssembleButton;
import com.atsuishio.superbwarfare.client.screens.component.CategoryButton;
import com.atsuishio.superbwarfare.client.screens.component.PageButton;
import com.atsuishio.superbwarfare.client.screens.component.RecipeButton;
import com.atsuishio.superbwarfare.init.ModRecipes;
import com.atsuishio.superbwarfare.menu.VehicleAssemblingMenu;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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

    public static final ResourceLocation TEXTURE = Mod.loc("textures/gui/vehicle_assembling_table.png");
    public static final int IMAGE_SIZE = 324;
    public static final int PAGE_SIZE = 9;

    private final Map<VehicleAssemblingRecipe.Category, List<ResourceLocation>> recipes = Maps.newLinkedHashMap();

    private VehicleAssemblingRecipe.Category currentCategory = VehicleAssemblingRecipe.Category.LAND;
    @Nullable
    private List<ResourceLocation> currentRecipes = new ArrayList<>();
    @Nullable
    private VehicleAssemblingRecipe currentRecipe = null;
    @Nullable
    private Int2IntArrayMap materialCount;
    private int pageIndex = 0;

    public VehicleAssemblingScreen(VehicleAssemblingMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        imageWidth = 322;
        imageHeight = 181;
        this.initRecipes();
        this.pageIndex = 0;
        this.currentRecipe = this.getRecipeById(this.currentRecipes == null || this.currentRecipes.isEmpty() ? null : this.currentRecipes.get(0));
        this.calculateMaterialCount(this.currentRecipe);
    }

    @Override
    protected void init() {
        super.init();
        this.initRecipes();
        this.clearWidgets();

        int posX = (this.width - this.imageWidth) / 2;
        int posY = (this.height - this.imageHeight) / 2;

        this.addCategoryButtons(posX, posY);
        this.addRecipeButtons(posX, posY);
        this.addPageButtons(posX, posY);
        this.addAssembleButton(posX, posY);
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
        this.currentRecipes = this.recipes.get(this.currentCategory);
    }

    public void addCategoryButtons(int posX, int posY) {
        int i = 0;
        for (var category : VehicleAssemblingRecipe.Category.values()) {
            CategoryButton button = new CategoryButton(posX, posY + 2 + i * 23, category, b -> {
                this.currentCategory = category;
                this.currentRecipes = this.recipes.get(category);
                this.currentRecipe = this.getRecipeById(this.currentRecipes == null || this.currentRecipes.isEmpty() ? null : this.currentRecipes.get(0));
                this.pageIndex = 0;
                this.calculateMaterialCount(this.currentRecipe);
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

        this.renderables.stream().filter(w -> w instanceof RecipeButton)
                .forEach(w -> ((RecipeButton) w).renderTooltips(stack -> guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY)));
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
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
            // TODO recipe
//            Recipe<?> recipe = recipeManager.byKey(recipeId).orElse(null);
//            if (recipe instanceof VehicleAssemblingRecipe assemblingRecipe) {
//                return assemblingRecipe;
//            }
        }
        return null;
    }

    public void calculateMaterialCount(@Nullable VehicleAssemblingRecipe recipe) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || recipe == null) return;

        var ingredients = recipe.getInputs();
        int size = ingredients.size();
        this.materialCount = new Int2IntArrayMap(size);

        for (int i = 0; i < size; ++i) {
            var ingredient = ingredients.get(i);
            int count = 0;

            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && ingredient.getIngredient().test(stack)) {
                    count += stack.getCount();
                }
            }

            this.materialCount.put(i, count);
        }
    }

    public void addRecipeButtons(int posX, int posY) {
        if (this.currentRecipes != null && !this.currentRecipes.isEmpty()) {
            for (int i = 0; i < 9; i++) {
                int index = i + this.pageIndex * PAGE_SIZE;
                if (index >= this.currentRecipes.size()) break;

                ResourceLocation id = this.currentRecipes.get(index);
                var recipe = this.getRecipeById(id);
                if (recipe == null) break;

                RecipeButton button = this.addRenderableWidget(new RecipeButton(posX + 26, posY + 21 + i * 17, recipe.getResult().getResult(), (b) -> {
                    this.currentRecipe = recipe;
                    this.calculateMaterialCount(recipe);
                    this.init();
                }));
                // TODO recipe
//            if (this.currentRecipe != null && recipe.getId().equals(this.currentRecipe.getId())) {
//                button.setSelected(true);
//            }
            }
        }
    }

    public void addPageButtons(int posX, int posY) {
        PageButton left = this.addRenderableWidget(new PageButton(posX + 70, posY + 4, true, b -> {
            this.pageIndex = Math.max(0, this.pageIndex - 1);
            this.init();
        }));
        PageButton right = this.addRenderableWidget(new PageButton(posX + 97, posY + 4, false, b -> {
            if (this.currentRecipes != null && !this.currentRecipes.isEmpty()) {
                this.pageIndex = Math.min((this.currentRecipes.size() - 1) / PAGE_SIZE, this.pageIndex + 1);
                this.init();
            }
        }));
        if (this.currentRecipes != null && !this.currentRecipes.isEmpty()) {
            left.active = this.pageIndex > 0;
            right.active = this.pageIndex < (this.currentRecipes.size() - 1) / PAGE_SIZE;
        } else {
            left.active = false;
            right.active = false;
        }
    }

    public void addAssembleButton(int posX, int posY) {
        this.addRenderableWidget(new AssembleButton(posX + 272, posY + 163, b -> {
            if (this.currentRecipe == null || this.materialCount == null) return;

            var inputs = this.currentRecipe.getInputs();
            int size = inputs.size();

            for (int i = 0; i < size; ++i) {
                if (i >= this.materialCount.size()) {
                    return;
                }

                int hasCount = this.materialCount.get(i);
                int needCount = inputs.get(i).getCount();
                boolean isCreative = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative();
                if (hasCount < needCount && !isCreative) {
                    return;
                }
            }
            // TODO getId
//            PacketDistributor.sendToServer(new AssembleVehicleMessage(this.currentRecipe.getId(), this.menu.containerId));
        }));
    }

    public void finishAssembling() {
        if (this.currentRecipe != null) {
            this.calculateMaterialCount(this.currentRecipe);
        }
        this.init();
    }
}
