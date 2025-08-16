package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.client.screens.component.AssembleButton;
import com.atsuishio.superbwarfare.client.screens.component.CategoryButton;
import com.atsuishio.superbwarfare.client.screens.component.PageButton;
import com.atsuishio.superbwarfare.client.screens.component.RecipeButton;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModRecipes;
import com.atsuishio.superbwarfare.menu.VehicleAssemblingMenu;
import com.atsuishio.superbwarfare.network.message.send.AssembleVehicleMessage;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
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
    private float modelScale = 50f;

    private String entityNameCache = "";
    private Entity entityCache = null;

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
        this.addScaleButtons(posX, posY);
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
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (this.currentRecipe != null) {
            this.renderModel(this.currentRecipe, guiGraphics);
        }

        this.renderables.stream().filter(w -> w instanceof RecipeButton)
                .forEach(w -> ((RecipeButton) w).renderTooltips(guiGraphics, mouseX, mouseY));
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
                if (this.currentRecipe != null && recipe.getId().equals(this.currentRecipe.getId())) {
                    button.setSelected(true);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (pMouseX >= this.leftPos + 26 && pMouseX <= this.leftPos + 106 && pMouseY >= this.topPos + 21 && pMouseY <= this.topPos + 175) {
            if (pDelta > 0) {
                this.pageIndex = Math.max(0, this.pageIndex - 1);
            } else {
                if (this.currentRecipes != null && !this.currentRecipes.isEmpty()) {
                    this.pageIndex = Math.min((this.currentRecipes.size() - 1) / PAGE_SIZE, this.pageIndex + 1);
                }
            }

            this.init();
            return true;
        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
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
            Mod.PACKET_HANDLER.sendToServer(new AssembleVehicleMessage(this.currentRecipe.getId(), this.menu.containerId));
        }));
    }

    public void finishAssembling() {
        if (this.currentRecipe != null) {
            this.calculateMaterialCount(this.currentRecipe);
        }
        this.init();
    }

    public void addScaleButtons(int posX, int posY) {
        this.addRenderableWidget(new ImageButton(posX + 290, posY + 90, 9, 9, 149, 182, 10,
                TEXTURE, IMAGE_SIZE, IMAGE_SIZE,
                b -> this.modelScale = 50));
        this.addRenderableWidget(new ImageButton(posX + 300, posY + 90, 9, 9, 159, 182, 10,
                TEXTURE, IMAGE_SIZE, IMAGE_SIZE,
                b -> this.modelScale = Math.max(this.modelScale - 20, 10)));
        this.addRenderableWidget(new ImageButton(posX + 310, posY + 90, 9, 9, 169, 182, 10,
                TEXTURE, IMAGE_SIZE, IMAGE_SIZE,
                b -> this.modelScale = Math.min(this.modelScale + 20, 150)));
    }

    public void renderModel(VehicleAssemblingRecipe recipe, GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) return;

        RenderHelper.markGuiRenderTimestamp();
        ItemStack stack = recipe.getResult().getResult();
        Entity renderEntity = null;

        if (stack.is(ModItems.CONTAINER.get())) {
            CompoundTag tag = BlockItem.getBlockEntityData(stack);
            typeFlag:
            if (tag != null && tag.contains("EntityType")) {
                String key = tag.getString("EntityType");
                if (entityNameCache.equals(key) && entityCache != null) {
                    renderEntity = entityCache;
                } else {
                    renderEntity = EntityType.byString(key)
                            .map(type -> type.create(level))
                            .orElse(null);
                    if (renderEntity == null) break typeFlag;

                    entityNameCache = key;
                    entityCache = renderEntity;
                }
            }
        }

        if (renderEntity == null) {
            renderDefaultItemModel(stack);
        } else {
            renderEntityModel(guiGraphics, renderEntity);
        }
    }

    @SuppressWarnings("deprecation")
    private void renderDefaultItemModel(ItemStack stack) {
        float rotationPeriod = 8.0F;
        int xPos = this.leftPos + 200;
        int yPos = this.topPos + 50;
        int startX = this.leftPos + 125;
        int startY = this.topPos + 15;
        int width = 128;
        int height = 85;
        float rotPitch = 15.0F;
        Window window = Minecraft.getInstance().getWindow();
        double windowGuiScale = window.getGuiScale();
        int scissorX = (int) (startX * windowGuiScale);
        int scissorY = (int) (window.getHeight() - (startY + height) * windowGuiScale);
        int scissorW = (int) (width * windowGuiScale);
        int scissorH = (int) (height * windowGuiScale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        Minecraft.getInstance().textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.translate((float) xPos, (float) yPos, 200.0F);
        posestack.translate(8.0, 8.0, 0.0);
        posestack.scale(1.0F, -1.0F, 1.0F);
        posestack.scale(this.modelScale, this.modelScale, this.modelScale);

        float rot = (float) (System.currentTimeMillis() % (long) ((int) (rotationPeriod * 1000.0F))) * (360.0F / (rotationPeriod * 1000.0F));

        posestack.mulPose(Axis.XP.rotationDegrees(rotPitch));
        posestack.mulPose(Axis.YP.rotationDegrees(rot));
        RenderSystem.applyModelViewMatrix();
        PoseStack tmpPose = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Lighting.setupForFlatItems();

        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, tmpPose, bufferSource, null, 0);

        bufferSource.endBatch();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableScissor();
    }

    private void renderEntityModel(GuiGraphics guiGraphics, Entity renderEntity) {
        if (renderEntity == null) return;

        PoseStack posestack = guiGraphics.pose();

        // TODO 正确调整渲染的角度和大小
        int posX = this.leftPos + 220;
        int posY = this.topPos + 80;
        int width = 208;
        int height = 99;

        Window window = Minecraft.getInstance().getWindow();
        double windowGuiScale = window.getGuiScale();

        int scissorX = (int) ((this.leftPos + 114) * windowGuiScale);
        int scissorY = (int) (window.getHeight() - (this.topPos + height) * windowGuiScale);
        int scissorW = (int) (width * windowGuiScale);
        int scissorH = (int) (height * windowGuiScale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

        posestack.pushPose();
        posestack.translate(posX, posY, 50.0D);
        posestack.scale(this.modelScale, this.modelScale, -this.modelScale);

        float size = (float) renderEntity.getBoundingBox().getSize();
        posestack.scale(1f / size, 1f / size, 1f / size);

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        float rotationPeriod = 8.0F;
        float rotPitch = 195F;
        float rot = (float) (System.currentTimeMillis() % (long) ((int) (rotationPeriod * 1000.0F))) * (360.0F / (rotationPeriod * 1000.0F));

        posestack.mulPose(Axis.XP.rotationDegrees(rotPitch));
        posestack.mulPose(Axis.YP.rotationDegrees(rot));

        entityrenderdispatcher.setRenderShadow(false);
        entityrenderdispatcher.render(renderEntity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, posestack, guiGraphics.bufferSource(), 15728880);
        guiGraphics.flush();
        entityrenderdispatcher.setRenderShadow(true);
        posestack.popPose();
        Lighting.setupFor3DItems();
        RenderSystem.disableScissor();
    }
}
