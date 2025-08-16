package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.ContainerBlock;
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
import com.atsuishio.superbwarfare.tools.animation.AnimationCurves;
import com.atsuishio.superbwarfare.tools.animation.ValueAnimator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.phys.Vec2;
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
    public static final int IMAGE_SIZE = 356;
    public static final int PAGE_SIZE = 9;

    public static final float DEFAULT_MODEL_SCALE = 50f;
    public static final float MIN_MODEL_SCALE = 10f;
    public static final float MAX_MODEL_SCALE = 200f;

    public static final int DEFAULT_MODEL_X = 234;
    public static final int DEFAULT_MODEL_Y = 80;

    private final Map<VehicleAssemblingRecipe.Category, List<ResourceLocation>> recipes = Maps.newLinkedHashMap();

    private VehicleAssemblingRecipe.Category currentCategory = VehicleAssemblingRecipe.Category.LAND;
    @Nullable
    private List<ResourceLocation> currentRecipes = new ArrayList<>();
    @Nullable
    private VehicleAssemblingRecipe currentRecipe = null;
    @Nullable
    private Int2IntArrayMap materialCount;
    private int pageIndex = 0;

    private String entityNameCache = "";
    private Entity entityCache = null;

    public VehicleAssemblingScreen(VehicleAssemblingMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        imageWidth = 356;
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

        guiGraphics.drawString(this.font, Component.translatable("container.superbwarfare.vehicle_assembling_table.information"), this.leftPos + 120, this.topPos + 105, 5592405, false);
        guiGraphics.drawString(this.font, Component.translatable("container.superbwarfare.vehicle_assembling_table.ingredient"), this.leftPos + 214, this.topPos + 105, 5592405, false);

        if (this.currentRecipe != null) {
            this.renderModel(this.currentRecipe, guiGraphics);
            this.renderRecipeInfo(this.currentRecipe, guiGraphics);
            guiGraphics.drawString(this.font, Component.translatable("container.superbwarfare.vehicle_assembling_table.count", this.currentRecipe.getResult().getResult().getCount()), this.leftPos + 214, this.topPos + 164, 5592405, false);
        }

        if (this.currentRecipes != null && !this.currentRecipes.isEmpty()) {
            this.renderIngredients(guiGraphics, mouseX, mouseY);
        }

        this.renderables.stream().filter(w -> w instanceof RecipeButton || w instanceof CategoryButton)
                .forEach(w -> {
                    if (w instanceof RecipeButton recipeButton) {
                        recipeButton.renderTooltips(guiGraphics, mouseX, mouseY);
                    }
                    if (w instanceof CategoryButton categoryButton) {
                        categoryButton.renderTooltips(guiGraphics, mouseX, mouseY);
                    }
                });
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

    private void renderIngredients(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.currentRecipe == null) return;
        var inputs = this.currentRecipe.getInputs();

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                int index = i * 4 + j;
                if (index >= inputs.size()) return;

                int posX = x + 215 + j * 34;
                int posY = y + 118 + i * 14;

                var input = inputs.get(index);
                var ingredient = input.getIngredient();
                var items = ingredient.getItems();
                if (items.length == 0) continue;

                int itemIndex = (int) (System.currentTimeMillis() / 1000L) % items.length;
                var itemStack = items[itemIndex];

                var pose = guiGraphics.pose();

                pose.pushPose();
                pose.scale(0.8F, 0.8F, 1.0F);
                guiGraphics.renderFakeItem(itemStack, (int) (posX * 1.25f), (int) (posY * 1.25f));
                pose.popPose();

                if (mouseX >= posX && mouseY >= posY && mouseX < posX + 16 * 0.8f && mouseY < posY + 16 * 0.8f) {
                    guiGraphics.renderTooltip(this.font, itemStack, mouseX, mouseY);
                }

                pose.pushPose();
                pose.scale(0.5F, 0.5F, 1.0F);
                pose.translate(0.0F, 0.0F, 200.0F);

                int count = input.getCount();
                if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative()) {
                    Component text = Component.literal(count + "/∞");
                    guiGraphics.drawString(this.font, text, (posX + 14) * 2, (posY + 8) * 2, 0x9dffa5, false);
                } else {
                    int hasCount = 0;
                    if (this.materialCount != null && index < this.materialCount.size()) {
                        hasCount = this.materialCount.get(index);
                    }
                    int color = hasCount >= count ? 0x80ff58 : 0xf44d61;
                    Component text = Component.literal(count + "/" + hasCount);
                    guiGraphics.drawString(this.font, text, (posX + 14) * 2, (posY + 8) * 2, color, false);
                }
                pose.popPose();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private final ValueAnimator<Float> scaleAnimator = (ValueAnimator<Float>) new ValueAnimator<>(300, DEFAULT_MODEL_SCALE)
            .animation(AnimationCurves.EASE_OUT_EXPO);

    @SuppressWarnings("unchecked")
    private final ValueAnimator<Vec2> modelPosAnimator = (ValueAnimator<Vec2>) new ValueAnimator<>(300, new Vec2(DEFAULT_MODEL_X, DEFAULT_MODEL_Y))
            .animation(AnimationCurves.EASE_OUT_EXPO);

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if (pMouseX >= this.leftPos + 114 && pMouseX <= this.leftPos + 354 && pMouseY >= this.topPos && pMouseY <= this.topPos + 99) {
            var newVec = modelPosAnimator.newValue();
            var posX = Mth.clamp(newVec.x + pDragX, DEFAULT_MODEL_X - 200, DEFAULT_MODEL_X + 200);
            var posY = Mth.clamp(newVec.y + pDragY, DEFAULT_MODEL_Y - 150, DEFAULT_MODEL_Y + 150);
            modelPosAnimator.update(new Vec2((float) posX, (float) posY));
            return true;
        }
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
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
        if (pMouseX >= this.leftPos + 114 && pMouseX <= this.leftPos + 354 && pMouseY >= this.topPos && pMouseY <= this.topPos + 99) {
            float targetScale;
            if (pMouseY > 0) {
                targetScale = Math.min(scaleAnimator.lerp(scaleAnimator.oldValue(), scaleAnimator.newValue(), System.currentTimeMillis()) + 20, MAX_MODEL_SCALE);
            } else {
                targetScale = Math.max(scaleAnimator.lerp(scaleAnimator.oldValue(), scaleAnimator.newValue(), System.currentTimeMillis()) - 20, MIN_MODEL_SCALE);
            }

            scaleAnimator.update(targetScale);
            scaleAnimator.beginForward(System.currentTimeMillis());

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
        this.addRenderableWidget(new AssembleButton(posX + 306, posY + 163, b -> {
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
        this.addRenderableWidget(new ImageButton(posX + 324, posY + 90, 9, 9, 149, 182, 10,
                TEXTURE, IMAGE_SIZE, IMAGE_SIZE,
                b -> {
                    long time = System.currentTimeMillis();
                    scaleAnimator.update(DEFAULT_MODEL_SCALE);
                    scaleAnimator.beginForward(time);
                    modelPosAnimator.update(new Vec2(DEFAULT_MODEL_X, DEFAULT_MODEL_Y));
                    modelPosAnimator.beginForward(time);
                }));
        this.addRenderableWidget(new ImageButton(posX + 334, posY + 90, 9, 9, 159, 182, 10,
                TEXTURE, IMAGE_SIZE, IMAGE_SIZE,
                b -> {
                    scaleAnimator.update(Math.max(scaleAnimator.lerp(scaleAnimator.oldValue(), scaleAnimator.newValue(), System.currentTimeMillis()) - 20, MIN_MODEL_SCALE));
                    scaleAnimator.beginForward(System.currentTimeMillis());
                }));
        this.addRenderableWidget(new ImageButton(posX + 344, posY + 90, 9, 9, 169, 182, 10,
                TEXTURE, IMAGE_SIZE, IMAGE_SIZE,
                b -> {
                    scaleAnimator.update(Math.min(scaleAnimator.lerp(scaleAnimator.oldValue(), scaleAnimator.newValue(), System.currentTimeMillis()) + 20, MAX_MODEL_SCALE));
                    scaleAnimator.beginForward(System.currentTimeMillis());
                }));
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
        int width = 240;
        int height = 99;
        float rotPitch = 15.0F;

        Window window = Minecraft.getInstance().getWindow();
        double windowGuiScale = window.getGuiScale();
        int scissorX = (int) ((this.leftPos + 114) * windowGuiScale);
        int scissorY = (int) (window.getHeight() - (this.topPos + height) * windowGuiScale);
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
        var oldVec = modelPosAnimator.oldValue();
        var newVec = modelPosAnimator.newValue();
        var xOffset = modelPosAnimator.lerp(oldVec.x, newVec.x, System.currentTimeMillis());
        var yOffset = modelPosAnimator.lerp(oldVec.y, newVec.y, System.currentTimeMillis());
        posestack.translate(this.leftPos + xOffset, this.topPos + yOffset - 20, 200.0F);
        posestack.translate(8.0, 8.0, 0.0);
        posestack.scale(1.0F, -1.0F, 1.0F);
        var currentScale = scaleAnimator.lerp(scaleAnimator.oldValue(), scaleAnimator.newValue(), System.currentTimeMillis());
        posestack.scale(currentScale, currentScale, currentScale);

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

        int width = 240;
        int height = 99;

        Window window = Minecraft.getInstance().getWindow();
        double windowGuiScale = window.getGuiScale();

        int scissorX = (int) ((this.leftPos + 114) * windowGuiScale);
        int scissorY = (int) (window.getHeight() - (this.topPos + height) * windowGuiScale);
        int scissorW = (int) (width * windowGuiScale);
        int scissorH = (int) (height * windowGuiScale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

        posestack.pushPose();
        var oldVec = modelPosAnimator.oldValue();
        var newVec = modelPosAnimator.newValue();
        var xOffset = modelPosAnimator.lerp(oldVec.x, newVec.x, System.currentTimeMillis());
        var yOffset = modelPosAnimator.lerp(oldVec.y, newVec.y, System.currentTimeMillis());
        posestack.translate(this.leftPos + xOffset, this.topPos + yOffset, 50.0D);
        var currentScale = scaleAnimator.lerp(scaleAnimator.oldValue(), scaleAnimator.newValue(), System.currentTimeMillis());
        posestack.scale(currentScale, currentScale, -currentScale);

        float size = (float) renderEntity.getBoundingBox().getSize();
        float resizeScale = 1f / Math.max(size, 1.25f);
        posestack.scale(resizeScale, resizeScale, resizeScale);

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        float rotationPeriod = 12.0F;
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

    public void renderRecipeInfo(VehicleAssemblingRecipe recipe, GuiGraphics guiGraphics) {
        ItemStack stack = recipe.getResult().getResult();

        boolean renderItemName = true;
        if (stack.is(ModItems.CONTAINER.get())) {
            CompoundTag tag = BlockItem.getBlockEntityData(stack);
            if (tag != null && tag.contains("EntityType")) {
                String key = tag.getString("EntityType");
                var entityType = EntityType.byString(key).orElse(null);
                if (entityType != null) {
                    this.renderContainerInfo(key, guiGraphics);
                    renderItemName = false;
                }
            }
        }

        var pose = guiGraphics.pose();
        pose.pushPose();

        pose.scale(0.75f, 0.75f, 1.0f);

        if (renderItemName) {
            RenderHelper.renderScrollingString(guiGraphics, this.font,
                    Component.empty().append(stack.getHoverName()).withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.YELLOW),
                    0.75f,
                    (int) ((this.leftPos + 122) / 0.75f), (int) ((this.topPos + 119) / 0.75f),
                    (int) ((this.leftPos + 198) / 0.75f), (int) ((this.topPos + 130) / 0.75f),
                    0xFFFFFF);
        }

        var modName = Component.translatableWithFallback("info." + recipe.getId().getNamespace() + ".mod_id", recipe.getId().getNamespace());
        var modInfo = Component.translatable("container.superbwarfare.mod_info", modName.withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.AQUA));

        RenderHelper.renderScrollingString(guiGraphics, this.font,
                modInfo,
                0.75f,
                (int) ((this.leftPos + 122) / 0.75f), (int) ((this.topPos + 167) / 0.75f),
                (int) ((this.leftPos + 198) / 0.75f), (int) ((this.topPos + 178) / 0.75f),
                0xFFFFFF);

        pose.popPose();
    }

    private void renderContainerInfo(String typeName, GuiGraphics guiGraphics) {
        var pose = guiGraphics.pose();

        String key = ContainerBlock.getEntityTranslationKey(typeName);
        if (key == null) return;
        if (typeName.split(":").length < 2) return;

        pose.pushPose();
        pose.scale(0.75f, 0.75f, 1.0f);

        var hoverName = Component.translatable(key).withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.YELLOW);
        RenderHelper.renderScrollingString(guiGraphics, this.font,
                hoverName,
                0.75f,
                (int) ((this.leftPos + 122) / 0.75f), (int) ((this.topPos + 119) / 0.75f),
                (int) ((this.leftPos + 198) / 0.75f), (int) ((this.topPos + 130) / 0.75f),
                0xFFFFFF);

        var info = Component.translatable("info." + typeName.split(":")[0] + "." + typeName.split(":")[1]);

        guiGraphics.enableScissor(this.leftPos + 120, this.topPos + 129, this.leftPos + 198, this.topPos + 165);

        // TODO 这里有时候会显示不全，漏一行
        List<FormattedCharSequence> infoComponents = this.font.split(FormattedText.of(info.getString()), 100);
        float height = (infoComponents.size() + 1) * 7.5f;

        if (height > 36) {
            float l = height - 36;
            double rate = (double) Util.getMillis() / 1000.0D;
            double d1 = Math.max((double) l * 0.5D, 3.0D);
            double d2 = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * rate / d1)) / 2.0D + 0.5D;
            double d3 = Mth.lerp(d2, 0.0D, l);
            pose.translate(0, -d3, 0);
        }

        for (int j = 0; j < infoComponents.size(); j++) {
            var cachedComponent = infoComponents.get(j);
            guiGraphics.drawString(this.font, cachedComponent, (int) ((this.leftPos + 122) / 0.75f), (int) ((this.topPos + 129 + j * 7.5f) / 0.75f), 0xFFFFFF);
        }

        guiGraphics.disableScissor();

        pose.popPose();
    }
}
