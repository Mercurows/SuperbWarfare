package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.ContainerBlock.Companion.getEntityTranslationKey
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.ValueAnimator
import com.atsuishio.superbwarfare.client.screens.component.AssembleButton
import com.atsuishio.superbwarfare.client.screens.component.CategoryButton
import com.atsuishio.superbwarfare.client.screens.component.PageButton
import com.atsuishio.superbwarfare.client.screens.component.RecipeButton
import com.atsuishio.superbwarfare.compat.jei.JeiCompatHolder.hasJEI
import com.atsuishio.superbwarfare.compat.jei.SbwJEIPlugin
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.inventory.menu.VehicleAssemblingMenu
import com.atsuishio.superbwarfare.network.message.send.AssembleVehicleMessage
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe
import com.atsuishio.superbwarfare.tools.*
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.ImageButton
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.phys.Vec2
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max
import kotlin.math.min

/**
 * Code based on TaC-Z
 */
@OnlyIn(Dist.CLIENT)
open class VehicleAssemblingScreen(menu: VehicleAssemblingMenu, inventory: Inventory, title: Component) :
    AbstractContainerScreen<VehicleAssemblingMenu>(menu, inventory, title) {
    private val recipes: MutableMap<VehicleAssemblingRecipe.Category, MutableList<ResourceLocation>> = linkedMapOf()

    private var currentCategory = VehicleAssemblingRecipe.Category.LAND
    private var currentRecipes: MutableList<ResourceLocation>? = arrayListOf()
    var currentRecipe: VehicleAssemblingRecipe? = null
        private set
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
            this.recipes.computeIfAbsent(recipe.category) { arrayListOf() }.add(recipe.id)
        }
        this.currentRecipes = this.recipes[this.currentCategory]
    }

    fun addCategoryButtons(posX: Int, posY: Int) {
        for ((i, category) in VehicleAssemblingRecipe.Category.entries.withIndex()) {
            val button = CategoryButton(posX, posY + 21 + i * 23, category) {
                this.currentCategory = category
                this.currentRecipes = this.recipes[category]
                this.currentRecipe = this.getRecipeById(
                    if (this.currentRecipes == null || this.currentRecipes!!.isEmpty()) null
                    else this.currentRecipes!![0]
                )
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
        this.renderBackground(guiGraphics)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        this.renderTooltip(guiGraphics, mouseX, mouseY)

        if (this.currentRecipe != null) {
            this.renderModel(this.currentRecipe!!, guiGraphics)
            this.renderRecipeInfo(this.currentRecipe!!, guiGraphics, mouseX, mouseY)
            guiGraphics.drawString(
                this.font,
                Component.translatable(
                    "container.superbwarfare.vehicle_assembling_table.count",
                    this.currentRecipe!!.result.getResult().count
                ),
                this.leftPos + 214,
                this.topPos + 164,
                5592405,
                false
            )
        }

        if (this.currentRecipes != null && !this.currentRecipes!!.isEmpty()) {
            this.renderIngredients(guiGraphics, mouseX, mouseY)
        }

        this.renderables
            .filter { it is RecipeButton || it is CategoryButton }
            .forEach {
                if (it is RecipeButton) {
                    it.renderTooltips(guiGraphics, mouseX, mouseY)
                }
                if (it is CategoryButton) {
                    it.renderTooltips(guiGraphics, mouseX, mouseY)
                }
            }
    }

    public override fun renderBg(pGuiGraphics: GuiGraphics, pPartialTick: Float, pMouseX: Int, pMouseY: Int) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        pGuiGraphics.blit(TEXTURE, i, j, 0f, 0f, this.imageWidth, this.imageHeight, IMAGE_SIZE, IMAGE_SIZE)
    }

    // 本方法留空
    override fun renderLabels(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
    }

    private fun getRecipeById(recipeId: ResourceLocation?): VehicleAssemblingRecipe? {
        if (recipeId == null) return null
        val level = clientLevel ?: return null

        val recipeManager = level.recipeManager
        val recipe = recipeManager.byKey(recipeId).orElse(null)
        return recipe as? VehicleAssemblingRecipe
    }

    fun calculateMaterialCount(recipe: VehicleAssemblingRecipe?) {
        if (recipe == null) return
        val player = localPlayer ?: return

        val ingredients = recipe.inputs
        val size = ingredients.size
        this.materialCount = Int2IntArrayMap(size)

        for (i in 0..<size) {
            val ingredient = ingredients[i]
            var count = 0

            for (stack in player.inventory.items) {
                if (!stack.isEmpty && ingredient.ingredient.test(stack)) {
                    count += stack.count
                }
            }

            this.materialCount!!.put(i, count)
        }
    }

    fun addRecipeButtons(posX: Int, posY: Int) {
        if (this.currentRecipes != null && !this.currentRecipes!!.isEmpty()) {
            for (i in 0..8) {
                val index: Int = i + this.pageIndex * PAGE_SIZE
                if (index >= this.currentRecipes!!.size) break

                val id = this.currentRecipes!![index]
                val recipe = this.getRecipeById(id) ?: break

                val button = this.addRenderableWidget<RecipeButton>(
                    RecipeButton(
                        posX + 26,
                        posY + 21 + i * 17,
                        recipe.result.getResult()
                    ) {
                        this.currentRecipe = recipe
                        this.calculateMaterialCount(recipe)
                        this.init()
                    }
                )
                if (this.currentRecipe != null && recipe.id == this.currentRecipe!!.id) {
                    button.setSelected(true)
                }
            }
        }
    }

    private fun renderIngredients(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (this.currentRecipe == null) return
        val inputs = this.currentRecipe!!.inputs

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
                if (Minecraft.getInstance().player != null && Minecraft.getInstance().player!!.isCreative()) {
                    val text: Component = Component.literal("$count/∞")
                    guiGraphics.drawString(this.font, text, (posX + 14) * 2, (posY + 8) * 2, 0x2C3141, false)
                } else {
                    var hasCount = 0
                    if (this.materialCount != null && index < this.materialCount!!.size) {
                        hasCount = this.materialCount!!.get(index)
                    }
                    val color = if (hasCount >= count) 0x2C3141 else 0xf44d61
                    val text: Component = Component.literal("$count/$hasCount")
                    guiGraphics.drawString(this.font, text, (posX + 14) * 2, (posY + 8) * 2, color, false)
                }
                pose.popPose()
            }
        }
    }

    @Suppress("unchecked_cast")
    private val scaleAnimator = ValueAnimator(300, DEFAULT_MODEL_SCALE)
        .animation(AnimationCurves.EASE_OUT_EXPO) as ValueAnimator<Float>

    @Suppress("unchecked_cast")
    private val modelPosAnimator = ValueAnimator(300, Vec2(DEFAULT_MODEL_X.toFloat(), DEFAULT_MODEL_Y.toFloat()))
        .animation(AnimationCurves.EASE_OUT_EXPO) as ValueAnimator<Vec2>

    init {
        imageWidth = 356
        imageHeight = 181
        this.initRecipes()
        this.pageIndex = 0
        this.currentRecipe = this.getRecipeById(
            if (this.currentRecipes == null || this.currentRecipes!!.isEmpty()) null else this.currentRecipes!![0]
        )
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

    override fun mouseScrolled(pMouseX: Double, pMouseY: Double, pDelta: Double): Boolean {
        if (pMouseX >= this.leftPos + 26 && pMouseX <= this.leftPos + 106 && pMouseY >= this.topPos + 21 && pMouseY <= this.topPos + 175) {
            if (pDelta > 0) {
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
            if (pDelta > 0) {
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
        return super.mouseScrolled(pMouseX, pMouseY, pDelta)
    }

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        val list = this.ingredientAreas
        if (!list.isEmpty() && pMouseX >= this.leftPos + 214 && pMouseY >= this.topPos + 117 && pMouseX <= this.leftPos + 350 && pMouseY <= this.topPos + 160) {
            if (hasJEI()) {
                val ingredientArea = list.firstOrNull { it.contains(pMouseX, pMouseY) }
                if (ingredientArea != null) {
                    val items = ingredientArea.ingredient!!.getItems()
                    val itemIndex = (System.currentTimeMillis() / 1000L).toInt() % items.size
                    SbwJEIPlugin.showRecipes(items[itemIndex]!!)
                    return true
                }
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton)
    }

    fun addPageButtons(posX: Int, posY: Int) {
        val left =
            this.addRenderableWidget<PageButton>(PageButton(posX + 95, posY - 1, true) {
                this.pageIndex = max(0, this.pageIndex - 1)
                this.init()
            })
        val right =
            this.addRenderableWidget<PageButton>(PageButton(posX + 103, posY - 1, false) {
                if (this.currentRecipes != null && !this.currentRecipes!!.isEmpty()) {
                    this.pageIndex = min((this.currentRecipes!!.size - 1) / PAGE_SIZE, this.pageIndex + 1)
                    this.init()
                }
            })
        if (this.currentRecipes != null && !this.currentRecipes!!.isEmpty()) {
            left.active = this.pageIndex > 0
            right.active = this.pageIndex < (this.currentRecipes!!.size - 1) / PAGE_SIZE
        } else {
            left.active = false
            right.active = false
        }
    }

    fun addAssembleButton(posX: Int, posY: Int) {
        this.addRenderableWidget(AssembleButton(posX + 295, posY + 163) {
            if (this.currentRecipe == null || this.materialCount == null) return@AssembleButton
            val inputs = this.currentRecipe!!.inputs
            val size = inputs.size

            for (i in 0..<size) {
                if (i >= this.materialCount!!.size) {
                    return@AssembleButton
                }

                val hasCount = this.materialCount!!.get(i)
                val needCount = inputs[i].count
                val isCreative = localPlayer != null && localPlayer!!.isCreative
                if (hasCount < needCount && !isCreative) {
                    return@AssembleButton
                }
            }
            sendPacketToServer(AssembleVehicleMessage(this.currentRecipe!!.id, this.menu.containerId))
        })
    }

    fun finishAssembling() {
        if (this.currentRecipe != null) {
            this.calculateMaterialCount(this.currentRecipe)
        }
        this.init()
    }

    fun addScaleButtons(posX: Int, posY: Int) {
        this.addRenderableWidget(
            ImageButton(
                posX + 324, posY + 90, 9, 9, 149, 182, 10,
                TEXTURE, IMAGE_SIZE, IMAGE_SIZE
            ) {
                val time = System.currentTimeMillis()
                scaleAnimator.update(DEFAULT_MODEL_SCALE)
                scaleAnimator.beginForward(time)
                modelPosAnimator.update(Vec2(DEFAULT_MODEL_X.toFloat(), DEFAULT_MODEL_Y.toFloat()))
                modelPosAnimator.beginForward(time)
            }
        )
        this.addRenderableWidget(
            ImageButton(
                posX + 334, posY + 90, 9, 9, 159, 182, 10,
                TEXTURE, IMAGE_SIZE, IMAGE_SIZE
            ) {
                scaleAnimator.update(
                    max(
                        scaleAnimator.lerp(
                            scaleAnimator.oldValue(),
                            scaleAnimator.newValue(),
                            System.currentTimeMillis()
                        ) - 20,
                        MIN_MODEL_SCALE
                    )
                )
                scaleAnimator.beginForward(System.currentTimeMillis())
            }
        )
        this.addRenderableWidget(
            ImageButton(
                posX + 344, posY + 90, 9, 9, 169, 182, 10,
                TEXTURE, IMAGE_SIZE, IMAGE_SIZE
            ) {
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

    fun renderModel(recipe: VehicleAssemblingRecipe, guiGraphics: GuiGraphics) {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return

        RenderDistanceHelper.markGuiRenderTimestamp()
        val stack = recipe.result.getResult()
        var renderEntity: Entity? = null

        if (stack.`is`(ModItems.CONTAINER.get())) {
            val tag = BlockItem.getBlockEntityData(stack)
            if (tag != null && tag.contains("EntityType")) {
                val key = tag.getString("EntityType")
                if (entityNameCache == key && entityCache != null) {
                    renderEntity = entityCache
                } else {
                    renderEntity = EntityType.byString(key)
                        .map { type -> type.create(level) }
                        .orElse(null)
                    if (renderEntity != null) {
                        entityNameCache = key
                        entityCache = renderEntity
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

    @Suppress("DEPRECATION")
    private fun renderDefaultItemModel(stack: ItemStack) {
        val rotationPeriod = 8f
        val width = 240
        val height = 99
        val rotPitch = 15f

        val window = mc.window
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
        posestack.pushPose()
        val oldVec = modelPosAnimator.oldValue()
        val newVec = modelPosAnimator.newValue()
        val xOffset = modelPosAnimator.lerp(oldVec.x, newVec.x, System.currentTimeMillis())
        val yOffset = modelPosAnimator.lerp(oldVec.y, newVec.y, System.currentTimeMillis())
        posestack.translate(this.leftPos + xOffset, this.topPos + yOffset - 20, 200f)
        posestack.translate(8.0, 8.0, 0.0)
        posestack.scale(1f, -1f, 1f)
        val currentScale =
            scaleAnimator.lerp(scaleAnimator.oldValue(), scaleAnimator.newValue(), System.currentTimeMillis())
        posestack.scale(currentScale, currentScale, currentScale)

        val rot =
            (System.currentTimeMillis() % ((rotationPeriod * 1000f).toInt()).toLong()).toFloat() * (360f / (rotationPeriod * 1000f))

        posestack.mulPose(Axis.XP.rotationDegrees(rotPitch))
        posestack.mulPose(Axis.YP.rotationDegrees(rot))
        RenderSystem.applyModelViewMatrix()
        val tmpPose = PoseStack()
        val bufferSource = mc.renderBuffers().bufferSource()
        Lighting.setupForFlatItems()

        mc.itemRenderer.renderStatic(
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
        posestack.popPose()
        RenderSystem.applyModelViewMatrix()
        RenderSystem.disableScissor()
    }

    private fun renderEntityModel(guiGraphics: GuiGraphics, renderEntity: Entity?) {
        if (renderEntity == null) return

        val posestack = guiGraphics.pose()

        val width = 240
        val height = 99

        val window = mc.window
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
        val entityRenderDispatcher = mc.entityRenderDispatcher

        val rotationPeriod = 12f
        val rotPitch = 195f
        val rot =
            (System.currentTimeMillis() % ((rotationPeriod * 1000f).toInt()).toLong()).toFloat() * (360f / (rotationPeriod * 1000f))

        posestack.mulPose(Axis.XP.rotationDegrees(rotPitch))
        posestack.mulPose(Axis.YP.rotationDegrees(rot))

        entityRenderDispatcher.setRenderShadow(false)
        entityRenderDispatcher.render(
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
        entityRenderDispatcher.setRenderShadow(true)
        posestack.popPose()
        Lighting.setupFor3DItems()
        RenderSystem.disableScissor()
    }

    fun renderRecipeInfo(recipe: VehicleAssemblingRecipe, guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val stack = recipe.result.getResult()

        var renderItemName = true
        if (stack.`is`(ModItems.CONTAINER.get())) {
            val tag = BlockItem.getBlockEntityData(stack)
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
            "info." + recipe.id.namespace + ".mod_id",
            recipe.id.namespace
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
            val cachedComponent: FormattedCharSequence =
                (if (j > 3) Component.literal("...").getVisualOrderText() else infoComponents[j])!!
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

    val ingredientAreas: MutableList<IngredientArea>
        get() {
            val areas: MutableList<IngredientArea> = arrayListOf()
            if (this.currentRecipe != null) {
                val inputs = this.currentRecipe!!.inputs
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
