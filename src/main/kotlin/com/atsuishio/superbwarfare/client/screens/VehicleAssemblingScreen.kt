package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.ContainerBlock.Companion.getEntityTranslationKey
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.ValueAnimator
import com.atsuishio.superbwarfare.client.screens.component.*
import com.atsuishio.superbwarfare.compat.jei.JeiCompatHolder.hasJEI
import com.atsuishio.superbwarfare.compat.jei.SbwJEIPlugin
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.inventory.menu.VehicleAssemblingMenu
import com.atsuishio.superbwarfare.network.message.send.AssembleVehicleMessage
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe
import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.phys.Vec2
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import kotlin.math.max
import kotlin.math.min

/**
 * Code based on TaC-Z
 */
@OnlyIn(Dist.CLIENT)
class VehicleAssemblingScreen(pMenu: VehicleAssemblingMenu, pPlayerInventory: Inventory, pTitle: Component) :
    AbstractContainerScreen<VehicleAssemblingMenu>(pMenu, pPlayerInventory, pTitle) {

    private val recipes: MutableMap<VehicleAssemblingRecipe.Category?, MutableList<ResourceLocation?>?> =
        Maps.newLinkedHashMap()

    private var currentCategory = VehicleAssemblingRecipe.Category.LAND
    private var currentRecipes: MutableList<ResourceLocation?>? = ArrayList()
    private var currentRecipe: RecipeHolder<VehicleAssemblingRecipe>? = null
    private var materialCount: Int2IntArrayMap? = null
    private var pageIndex = 0

    private var entityNameCache = ""
    private var entityCache: Entity? = null

    override fun init() {
        super.init()
        this.initRecipes()
        this.clearWidgets()

        val posX = (this.width - this.imageWidth) / 2
        val posY = (this.height - this.imageHeight) / 2

        this.addCategoryButtons(posX, posY)
        this.addRecipeButtons(posX, posY)
        this.addPageButtons(posX, posY)
        this.addAssembleButton(posX, posY)
        this.addScaleButtons(posX, posY)
    }

    fun initRecipes() {
        this.recipes.clear()

        val level = clientLevel ?: return

        val recipeManager = level.recipeManager
        val recipeList = recipeManager.getAllRecipesFor(ModRecipes.VEHICLE_ASSEMBLING_TYPE.get())

        for (recipe in recipeList) {
            this.recipes.computeIfAbsent(recipe.value().category) { _ -> Lists.newArrayList() }!!.add(recipe.id())
        }
        this.currentRecipes = this.recipes[this.currentCategory]
    }

    fun addCategoryButtons(posX: Int, posY: Int) {
        for ((i, category) in VehicleAssemblingRecipe.Category.entries.toTypedArray().withIndex()) {
            val button = CategoryButton(posX, posY + 21 + i * 23, category) { _ ->
                this.currentCategory = category
                this.currentRecipes = this.recipes[category]
                this.currentRecipe =
                    this.getRecipeById(if (this.currentRecipes == null || this.currentRecipes!!.isEmpty()) null else this.currentRecipes!![0])
                this.pageIndex = 0
                this.calculateMaterialCount(this.currentRecipe)
                this.init()
            }
            if (this.currentCategory == category) {
                button.setSelected(true)
            }
            this.addRenderableWidget(button)
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        this.renderTooltip(guiGraphics, mouseX, mouseY)

        val currentRecipe = this.currentRecipe
        if (currentRecipe != null) {
            this.renderModel(currentRecipe, guiGraphics)
            this.renderRecipeInfo(currentRecipe, guiGraphics, mouseX, mouseY)
            guiGraphics.drawString(
                this.font,
                Component.translatable(
                    "container.superbwarfare.vehicle_assembling_table.count",
                    currentRecipe.value().result.getResult().count
                ),
                this.leftPos + 214,
                this.topPos + 164,
                5592405,
                false
            )
        }

        if (this.currentRecipes?.isNotEmpty() ?: false) {
            this.renderIngredients(guiGraphics, mouseX, mouseY)
        }

        this.renderables.stream().filter { w: Renderable? -> w is RecipeButton || w is CategoryButton }
            .forEach { w: Renderable? ->
                if (w is RecipeButton) {
                    w.renderTooltips(guiGraphics, mouseX, mouseY)
                }
                if (w is CategoryButton) {
                    w.renderTooltips(guiGraphics, mouseX, mouseY)
                }
            }
    }

    override fun renderBg(pGuiGraphics: GuiGraphics, pPartialTick: Float, pMouseX: Int, pMouseY: Int) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        pGuiGraphics.blit(TEXTURE, i, j, 0f, 0f, this.imageWidth, this.imageHeight, IMAGE_SIZE, IMAGE_SIZE)
    }

    // 本方法留空
    override fun renderLabels(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {}

    private fun getRecipeById(recipeId: ResourceLocation?): RecipeHolder<VehicleAssemblingRecipe>? {
        if (recipeId == null) return null
        val level = clientLevel
        if (level != null) {
            val recipeManager = level.recipeManager
            val recipe = recipeManager.byKey(recipeId).orElse(null) ?: return null
            if (recipe.value() is VehicleAssemblingRecipe) {
                @Suppress("unchecked_cast")
                return recipe as RecipeHolder<VehicleAssemblingRecipe>
            }
        }
        return null
    }

    fun calculateMaterialCount(holder: RecipeHolder<VehicleAssemblingRecipe>?) {
        val player = Minecraft.getInstance().player
        if (player == null || holder == null) return
        val recipe = holder.value()

        val ingredients = recipe.inputs
        val size = ingredients.size
        this.materialCount = Int2IntArrayMap(size)

        for (i in 0..<size) {
            val ingredient = ingredients[i]
            var count = 0

            for (stack in player.getInventory().items) {
                if (!stack.isEmpty && ingredient.ingredient.test(stack)) {
                    count += stack.count
                }
            }

            this.materialCount!!.put(i, count)
        }
    }

    fun addRecipeButtons(posX: Int, posY: Int) {
        val currentRecipes = this.currentRecipes
        if (!currentRecipes.isNullOrEmpty()) {
            for (i in 0..8) {
                val index = i + this.pageIndex * PAGE_SIZE
                if (index >= currentRecipes.size) break

                val id = currentRecipes[index]
                val recipe = this.getRecipeById(id) ?: break

                val button = this.addRenderableWidget(
                    RecipeButton(
                        posX + 26,
                        posY + 21 + i * 17,
                        recipe.value().result.getResult()
                    ) { _ ->
                        this.currentRecipe = recipe
                        this.calculateMaterialCount(recipe)
                        this.init()
                    }
                )
                if (recipe == this.currentRecipe) {
                    button.setSelected(true)
                }
            }
        }
    }

    private fun renderIngredients(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val inputs = this.currentRecipe?.value()?.inputs ?: return

        val x = (this.width - this.imageWidth) / 2
        val y = (this.height - this.imageHeight) / 2

        for (i in 0..2) {
            for (j in 0..3) {
                val index = i * 4 + j
                if (index >= inputs.size) return

                val posX = x + 215 + j * 34
                val posY = y + 118 + i * 14

                val input = inputs[index]
                val ingredient = input.ingredient
                val items = ingredient.getItems()
                if (items.size == 0) continue

                val itemIndex = (System.currentTimeMillis() / 1000L).toInt() % items.size
                val itemStack: ItemStack = items[itemIndex]!!

                val pose = guiGraphics.pose()

                pose.pushPose()
                pose.scale(0.8f, 0.8f, 1f)
                guiGraphics.renderFakeItem(itemStack, (posX * 1.25f).toInt(), (posY * 1.25f).toInt())
                pose.popPose()

                if (mouseX >= posX && mouseY >= posY && mouseX < posX + 16 * 0.8f && mouseY < posY + 16 * 0.8f) {
                    guiGraphics.renderTooltip(this.font, itemStack, mouseX, mouseY)
                }

                pose.pushPose()
                pose.scale(0.5f, 0.5f, 1f)
                pose.translate(0f, 0f, 200f)

                val count = input.count
                val player = localPlayer
                if (player != null && player.isCreative()) {
                    val text = Component.literal("$count/∞")
                    guiGraphics.drawString(this.font, text, (posX + 14) * 2, (posY + 8) * 2, 0x2C3141, false)
                } else {
                    var hasCount = 0
                    val materialCount = this.materialCount
                    if (materialCount != null && index < materialCount.size) {
                        hasCount = materialCount.get(index)
                    }
                    val color = if (hasCount >= count) 0x2C3141 else 0xf44d61
                    val text: Component = Component.literal("$count/$hasCount")
                    guiGraphics.drawString(this.font, text, (posX + 14) * 2, (posY + 8) * 2, color, false)
                }
                pose.popPose()
            }
        }
    }

    private val scaleAnimator = ValueAnimator(300, DEFAULT_MODEL_SCALE)
        .animation(AnimationCurves.EASE_OUT_EXPO) as ValueAnimator<Float>

    private val modelPosAnimator = ValueAnimator(300, Vec2(DEFAULT_MODEL_X.toFloat(), DEFAULT_MODEL_Y.toFloat()))
        .animation(AnimationCurves.EASE_OUT_EXPO) as ValueAnimator<Vec2>

    init {
        imageWidth = 356
        imageHeight = 181
        this.initRecipes()
        this.pageIndex = 0
        this.currentRecipe =
            this.getRecipeById(if (this.currentRecipes == null || this.currentRecipes!!.isEmpty()) null else this.currentRecipes!![0])
        this.calculateMaterialCount(this.currentRecipe)
    }

    override fun mouseDragged(pMouseX: Double, pMouseY: Double, pButton: Int, pDragX: Double, pDragY: Double): Boolean {
        if (pMouseX >= this.leftPos + 114 && pMouseX <= this.leftPos + 354 && pMouseY >= this.topPos && pMouseY <= this.topPos + 99) {
            val newVec = modelPosAnimator.newValue()
            val posX =
                Mth.clamp(newVec.x + pDragX, (DEFAULT_MODEL_X - 200).toDouble(), (DEFAULT_MODEL_X + 200).toDouble())
            val posY =
                Mth.clamp(newVec.y + pDragY, (DEFAULT_MODEL_Y - 150).toDouble(), (DEFAULT_MODEL_Y + 150).toDouble())
            modelPosAnimator.update(Vec2(posX.toFloat(), posY.toFloat()))
            return true
        }
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)
    }

    override fun mouseScrolled(pMouseX: Double, pMouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (pMouseX >= this.leftPos + 26 && pMouseX <= this.leftPos + 106 && pMouseY >= this.topPos + 21 && pMouseY <= this.topPos + 175) {
            if (scrollY > 0) {
                this.pageIndex = max(0, this.pageIndex - 1)
            } else {
                if (this.currentRecipes != null && !this.currentRecipes!!.isEmpty()) {
                    this.pageIndex = min((this.currentRecipes!!.size - 1) / PAGE_SIZE, this.pageIndex + 1)
                }
            }

            this.init()
            return true
        }
        if (pMouseX >= this.leftPos + 114 && pMouseX <= this.leftPos + 354 && pMouseY >= this.topPos && pMouseY <= this.topPos + 99) {
            val targetScale: Float
            if (scrollY > 0) {
                targetScale = min(
                    scaleAnimator.lerp(
                        scaleAnimator.oldValue(),
                        scaleAnimator.newValue(),
                        System.currentTimeMillis()
                    ) + 20, MAX_MODEL_SCALE
                )
            } else {
                targetScale = max(
                    scaleAnimator.lerp(
                        scaleAnimator.oldValue(),
                        scaleAnimator.newValue(),
                        System.currentTimeMillis()
                    ) - 20, MIN_MODEL_SCALE
                )
            }

            scaleAnimator.update(targetScale)
            scaleAnimator.beginForward(System.currentTimeMillis())

            return true
        }
        return super.mouseScrolled(pMouseX, pMouseY, scrollX, scrollY)
    }

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        val list = this.getIngredientAreas()
        if (list.isNotEmpty() && pMouseX >= this.leftPos + 214 && pMouseY >= this.topPos + 117 && pMouseX <= this.leftPos + 350 && pMouseY <= this.topPos + 160) {
            if (hasJEI()) {
                val ingredientArea = list.stream().filter { area -> area.contains(pMouseX, pMouseY) }.findFirst()
                if (ingredientArea.isPresent) {
                    val items = ingredientArea.get().ingredient!!.getItems()
                    val itemIndex = (System.currentTimeMillis() / 1000L).toInt() % items.size
                    SbwJEIPlugin.showRecipes(items[itemIndex]!!)
                    return true
                }
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton)
    }

    fun addPageButtons(posX: Int, posY: Int) {
        val left = this.addRenderableWidget(PageButton(posX + 95, posY - 1, true) { _ ->
            this.pageIndex = max(0, this.pageIndex - 1)
            this.init()
        })
        val currentRecipes = this.currentRecipes
        val right = this.addRenderableWidget(PageButton(posX + 103, posY - 1, false) { _ ->
            if (!currentRecipes.isNullOrEmpty()) {
                this.pageIndex = min((currentRecipes.size - 1) / PAGE_SIZE, this.pageIndex + 1)
                this.init()
            }
        })
        if (!currentRecipes.isNullOrEmpty()) {
            left.active = this.pageIndex > 0
            right.active = this.pageIndex < (currentRecipes.size - 1) / PAGE_SIZE
        } else {
            left.active = false
            right.active = false
        }
    }

    fun addAssembleButton(posX: Int, posY: Int) {
        val currentRecipe = this.currentRecipe
        val materialCount = this.materialCount
        this.addRenderableWidget(AssembleButton(posX + 295, posY + 163, Button.OnPress { _ ->
            if (currentRecipe == null || materialCount == null) return@OnPress
            val inputs = currentRecipe.value().inputs
            val size = inputs.size

            for (i in 0..<size) {
                if (i >= materialCount.size) {
                    return@OnPress
                }

                val hasCount = materialCount.get(i)
                val needCount = inputs[i].count
                val player = localPlayer
                val isCreative = player != null && player.isCreative()
                if (hasCount < needCount && !isCreative) {
                    return@OnPress
                }
            }
            sendPacketToServer(AssembleVehicleMessage(currentRecipe.id(), this.menu.containerId))
        }))
    }

    fun finishAssembling() {
        if (this.currentRecipe != null) {
            this.calculateMaterialCount(this.currentRecipe)
        }
        this.init()
    }

    fun addScaleButtons(posX: Int, posY: Int) {
        this.addRenderableWidget(
            ScaleButton(posX + 324, posY + 90, 149, 182) { _ ->
                val time = System.currentTimeMillis()
                scaleAnimator.update(DEFAULT_MODEL_SCALE)
                scaleAnimator.beginForward(time)
                modelPosAnimator.update(Vec2(DEFAULT_MODEL_X.toFloat(), DEFAULT_MODEL_Y.toFloat()))
                modelPosAnimator.beginForward(time)
            }
        )
        this.addRenderableWidget(
            ScaleButton(posX + 334, posY + 90, 159, 182) { _ ->
                scaleAnimator.update(
                    max(
                        scaleAnimator.lerp(
                            scaleAnimator.oldValue(),
                            scaleAnimator.newValue(),
                            System.currentTimeMillis()
                        ) - 20, MIN_MODEL_SCALE
                    )
                )
                scaleAnimator.beginForward(System.currentTimeMillis())
            }
        )
        this.addRenderableWidget(
            ScaleButton(posX + 344, posY + 90, 169, 182) { _ ->
                scaleAnimator.update(
                    min(
                        scaleAnimator.lerp(
                            scaleAnimator.oldValue(),
                            scaleAnimator.newValue(),
                            System.currentTimeMillis()
                        ) + 20, MAX_MODEL_SCALE
                    )
                )
                scaleAnimator.beginForward(System.currentTimeMillis())
            }
        )
    }

    fun renderModel(holder: RecipeHolder<VehicleAssemblingRecipe>, guiGraphics: GuiGraphics) {
        val mc = Minecraft.getInstance()
        val level = mc.level
        if (level == null) return

        RenderHelper.markGuiRenderTimestamp()
        val stack = holder.value().result.getResult()
        var renderEntity: Entity? = null

        if (stack.`is`(ModItems.CONTAINER.get())) {
            val data = stack.get(DataComponents.BLOCK_ENTITY_DATA)
            val tag = data?.copyTag()

            if (tag != null && tag.contains("EntityType")) {
                val key = tag.getString("EntityType")
                if (entityNameCache == key && entityCache != null) {
                    renderEntity = entityCache
                } else {
                    renderEntity = EntityType.byString(key)
                        .map { it.create(level) }
                        .orElse(null)
                    if (renderEntity != null) {
                        entityNameCache = key
                        entityCache = renderEntity

                        scaleAnimator.update(DEFAULT_MODEL_SCALE)
                        modelPosAnimator.update(Vec2(DEFAULT_MODEL_X.toFloat(), DEFAULT_MODEL_Y.toFloat()))
                    }
                }
            }
        }

        if (renderEntity == null) {
            renderDefaultItemModel(stack)
        } else {
            renderEntityModel(guiGraphics, renderEntity)
        }
    }

    private fun renderDefaultItemModel(stack: ItemStack) {
        val rotationPeriod = 8f
        val width = 240
        val height = 99
        val rotPitch = 15f

        val window = Minecraft.getInstance().window
        val windowGuiScale = window.guiScale
        val scissorX = ((this.leftPos + 114) * windowGuiScale).toInt()
        val scissorY = (window.height - (this.topPos + height) * windowGuiScale).toInt()
        val scissorW = (width * windowGuiScale).toInt()
        val scissorH = (height * windowGuiScale).toInt()
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH)

        Minecraft.getInstance().textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false)
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS)
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        val posestack = RenderSystem.getModelViewStack()
        posestack.pushMatrix()
        val oldVec = modelPosAnimator.oldValue()
        val newVec = modelPosAnimator.newValue()
        val xOffset = modelPosAnimator.lerp(oldVec.x, newVec.x, System.currentTimeMillis())
        val yOffset = modelPosAnimator.lerp(oldVec.y, newVec.y, System.currentTimeMillis())
        posestack.translate(this.leftPos + xOffset, this.topPos + yOffset - 20, 200f)
        posestack.translate(8.0f, 8.0f, 0.0f)
        posestack.scale(1f, -1f, 1f)
        val currentScale =
            scaleAnimator.lerp(scaleAnimator.oldValue(), scaleAnimator.newValue(), System.currentTimeMillis())
        posestack.scale(currentScale, currentScale, currentScale)

        val rot =
            (System.currentTimeMillis() % ((rotationPeriod * 1000f).toInt()).toLong()).toFloat() * (360f / (rotationPeriod * 1000f))

        posestack.rotate(Axis.XP.rotationDegrees(rotPitch))
        posestack.rotate(Axis.YP.rotationDegrees(rot))
        RenderSystem.applyModelViewMatrix()
        val tmpPose = PoseStack()
        val bufferSource = Minecraft.getInstance().renderBuffers().bufferSource()
        Lighting.setupForFlatItems()

        Minecraft.getInstance().itemRenderer.renderStatic(
            stack,
            ItemDisplayContext.FIXED,
            15728880,
            OverlayTexture.NO_OVERLAY,
            tmpPose,
            bufferSource,
            null,
            0
        )

        bufferSource.endBatch()
        RenderSystem.enableDepthTest()
        Lighting.setupFor3DItems()
        posestack.popMatrix()
        RenderSystem.applyModelViewMatrix()
        RenderSystem.disableScissor()
    }

    private fun renderEntityModel(guiGraphics: GuiGraphics, renderEntity: Entity?) {
        if (renderEntity == null) return

        val posestack = guiGraphics.pose()

        val width = 240
        val height = 99

        val window = Minecraft.getInstance().window
        val windowGuiScale = window.guiScale

        val scissorX = ((this.leftPos + 114) * windowGuiScale).toInt()
        val scissorY = (window.height - (this.topPos + height) * windowGuiScale).toInt()
        val scissorW = (width * windowGuiScale).toInt()
        val scissorH = (height * windowGuiScale).toInt()
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH)

        posestack.pushPose()
        val oldVec = modelPosAnimator.oldValue()
        val newVec = modelPosAnimator.newValue()
        val xOffset = modelPosAnimator.lerp(oldVec.x, newVec.x, System.currentTimeMillis())
        val yOffset = modelPosAnimator.lerp(oldVec.y, newVec.y, System.currentTimeMillis())
        posestack.translate(this.leftPos + xOffset, this.topPos + yOffset, 50f)
        val currentScale =
            scaleAnimator.lerp(scaleAnimator.oldValue(), scaleAnimator.newValue(), System.currentTimeMillis())
        posestack.scale(currentScale, currentScale, -currentScale)

        val size = renderEntity.boundingBox.getSize().toFloat()
        val resizeScale = 1f / max(size, 1.25f)
        posestack.scale(resizeScale, resizeScale, resizeScale)

        Lighting.setupForEntityInInventory()
        val entityrenderdispatcher = Minecraft.getInstance().entityRenderDispatcher

        val rotationPeriod = 12f
        val rotPitch = 195f
        val rot =
            (System.currentTimeMillis() % ((rotationPeriod * 1000f).toInt()).toLong()).toFloat() * (360f / (rotationPeriod * 1000f))

        posestack.mulPose(Axis.XP.rotationDegrees(rotPitch))
        posestack.mulPose(Axis.YP.rotationDegrees(rot))

        entityrenderdispatcher.setRenderShadow(false)
        entityrenderdispatcher.render(
            renderEntity,
            0.0,
            0.0,
            0.0,
            0f,
            1f,
            posestack,
            guiGraphics.bufferSource(),
            15728880
        )
        guiGraphics.flush()
        entityrenderdispatcher.setRenderShadow(true)
        posestack.popPose()
        Lighting.setupFor3DItems()
        RenderSystem.disableScissor()
    }

    fun renderRecipeInfo(
        holder: RecipeHolder<VehicleAssemblingRecipe>,
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int
    ) {
        val stack = holder.value().result.getResult()

        var renderItemName = true
        if (stack.`is`(ModItems.CONTAINER.get())) {
            val data = stack.get(DataComponents.BLOCK_ENTITY_DATA)
            val tag = data?.copyTag()
            if (tag != null && tag.contains("EntityType")) {
                val key = tag.getString("EntityType")
                val entityType = EntityType.byString(key).orElse(null)
                if (entityType != null) {
                    this.renderContainerInfo(key, guiGraphics, mouseX, mouseY)
                    renderItemName = false
                }
            }
        }

        val pose = guiGraphics.pose()
        pose.pushPose()

        pose.scale(0.75f, 0.75f, 1.0f)

        if (renderItemName) {
            RenderHelper.renderScrollingString(
                guiGraphics, this.font,
                Component.empty().append(stack.getHoverName()).withStyle(ChatFormatting.UNDERLINE)
                    .withStyle(ChatFormatting.YELLOW),
                0.75f,
                ((this.leftPos + 122) / 0.75f).toInt(), ((this.topPos + 119) / 0.75f).toInt(),
                ((this.leftPos + 198) / 0.75f).toInt(), ((this.topPos + 130) / 0.75f).toInt(),
                0xFFFFFF
            )
        }

        val modName = Component.translatableWithFallback(
            "info." + holder.id().namespace + ".mod_id",
            holder.id().namespace
        )
        val modInfo = Component.translatable(
            "container.superbwarfare.mod_info",
            modName.withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.AQUA)
        )

        RenderHelper.renderScrollingString(
            guiGraphics, this.font,
            modInfo,
            0.75f,
            ((this.leftPos + 122) / 0.75f).toInt(), ((this.topPos + 167) / 0.75f).toInt(),
            ((this.leftPos + 198) / 0.75f).toInt(), ((this.topPos + 178) / 0.75f).toInt(),
            0xFFFFFF
        )

        pose.popPose()
    }

    private fun renderContainerInfo(typeName: String, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val pose = guiGraphics.pose()

        val key = getEntityTranslationKey(typeName) ?: return
        if (typeName.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size < 2) return

        val info =
            Component.translatableWithFallback("info." + typeName.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[0] + "." + typeName.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[1],
                Component.translatable("info.superbwarfare.no_info").string)
        val infoComponents = this.font.split(FormattedText.of(info.string), 100)

        pose.pushPose()
        pose.scale(0.75f, 0.75f, 1.0f)

        val hoverName = Component.translatable(key).withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.YELLOW)
        RenderHelper.renderScrollingString(
            guiGraphics, this.font,
            hoverName,
            0.75f,
            ((this.leftPos + 122) / 0.75f).toInt(), ((this.topPos + 119) / 0.75f).toInt(),
            ((this.leftPos + 198) / 0.75f).toInt(), ((this.topPos + 130) / 0.75f).toInt(),
            0xFFFFFF
        )

        guiGraphics.enableScissor(this.leftPos + 120, this.topPos + 129, this.leftPos + 198, this.topPos + 165)
        for (j in infoComponents.indices) {
            val cachedComponent = (if (j > 3) Component.literal("...").getVisualOrderText() else infoComponents[j])
            guiGraphics.drawString(
                this.font,
                cachedComponent,
                ((this.leftPos + 122) / 0.75f).toInt(),
                ((this.topPos + 129 + j * 7.5f) / 0.75f).toInt(),
                0x2C3141,
                false
            )
        }
        guiGraphics.disableScissor()

        pose.popPose()

        if (mouseX >= this.leftPos + 120 && mouseX <= this.leftPos + 200 && mouseY >= this.topPos + 117 && mouseY <= this.topPos + 175) {
            guiGraphics.renderTooltip(
                this.font,
                this.font.split(FormattedText.of(info.string), 200),
                mouseX,
                mouseY
            )
        }
    }

    fun getCurrentRecipe(): VehicleAssemblingRecipe? {
        return this.currentRecipe?.value
    }

    fun getIngredientAreas(): MutableList<IngredientArea> {
        val areas: MutableList<IngredientArea> = ArrayList()
        val currentRecipe = this.currentRecipe
        if (currentRecipe != null) {
            val inputs = currentRecipe.value().inputs
            for (i in 0..2) {
                for (j in 0..3) {
                    val index = i * 4 + j
                    if (index >= inputs.size) return areas
                    val input = inputs[index]
                    val ingredient = input.ingredient
                    val items = ingredient.getItems()
                    if (items.size == 0) continue
                    val x = this.leftPos + 215 + j * 34
                    val y = this.topPos + 118 + i * 14
                    areas.add(IngredientArea(ingredient, x.toDouble(), y.toDouble(), 12.8, 12.8))
                }
            }
        }
        return areas
    }

    @JvmRecord
    data class IngredientArea(
        val ingredient: Ingredient?,
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double
    ) {
        fun contains(mouseX: Double, mouseY: Double): Boolean {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        }
    }

    companion object {
        val TEXTURE: ResourceLocation = loc("textures/gui/vehicle_assembling_table.png")
        const val IMAGE_SIZE: Int = 356
        const val PAGE_SIZE: Int = 9

        const val DEFAULT_MODEL_SCALE: Float = 50f
        const val MIN_MODEL_SCALE: Float = 10f
        const val MAX_MODEL_SCALE: Float = 200f

        const val DEFAULT_MODEL_X: Int = 234
        const val DEFAULT_MODEL_Y: Int = 80
    }
}