package com.atsuishio.superbwarfare.client.animation

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.api.event.RenderPlayerArmEvent
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.SmartTextureBrightener
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ClientEventHandler.activeThermalImaging
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.player.PlayerRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.player.PlayerModelPart
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Matrix3f
import org.joml.Matrix4f
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.core.animatable.model.CoreGeoBone
import software.bernie.geckolib.core.animation.AnimationProcessor
import software.bernie.geckolib.util.RenderUtils
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

@Deprecated("Geckolib will be removed since 0.8.10, use Simple Bedrock Model instead")
object AnimationHelper {

    @JvmField
    var lerpTimer: Float = 0f

    @JvmStatic
    fun renderPartOverBone(
        model: ModelPart,
        bone: GeoBone,
        stack: PoseStack,
        buffer: VertexConsumer,
        packedLightIn: Int,
        packedOverlayIn: Int,
        alpha: Float
    ) {
        renderPartOverBone(model, bone, stack, buffer, packedLightIn, packedOverlayIn, 1.0f, 1.0f, 1.0f, alpha)
    }

    @JvmStatic
    fun renderPartOverBone(
        model: ModelPart,
        bone: GeoBone,
        stack: PoseStack,
        buffer: VertexConsumer,
        packedLightIn: Int,
        packedOverlayIn: Int,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        setupModelFromBone(model, bone)
        model.render(stack, buffer, packedLightIn, packedOverlayIn, r, g, b, a)
    }

    @JvmStatic
    fun setupModelFromBone(model: ModelPart, bone: GeoBone) {
        model.setPos(bone.pivotX, bone.pivotY, bone.pivotZ)
        model.xRot = 0.0f
        model.yRot = 0.0f
        model.zRot = 0.0f
    }

    @JvmStatic
    fun renderPartOverBone2(
        model: ModelPart,
        bone: GeoBone,
        stack: PoseStack,
        buffer: VertexConsumer,
        packedLightIn: Int,
        packedOverlayIn: Int,
        alpha: Float
    ) {
        renderPartOverBone2(model, bone, stack, buffer, packedLightIn, packedOverlayIn, 1.0f, 1.0f, 1.0f, alpha)
    }

    @JvmStatic
    fun renderPartOverBone2(
        model: ModelPart,
        bone: GeoBone,
        stack: PoseStack,
        buffer: VertexConsumer,
        packedLightIn: Int,
        packedOverlayIn: Int,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        setupModelFromBone2(model, bone)
        model.render(stack, buffer, packedLightIn, packedOverlayIn, r, g, b, a)
    }

    @JvmStatic
    fun setupModelFromBone2(model: ModelPart, bone: GeoBone) {
        model.setPos(bone.pivotX, bone.pivotY + 7, bone.pivotZ)
        model.xRot = 0.0f
        model.yRot = 180 * Mth.DEG_TO_RAD
        model.zRot = 180 * Mth.DEG_TO_RAD
    }

    @JvmStatic
    fun handleShellsAnimation(animationProcessor: AnimationProcessor<*>, x: Float, y: Float) {
        val shell1 = animationProcessor.getBone("shell1")
        val shell2 = animationProcessor.getBone("shell2")
        val shell3 = animationProcessor.getBone("shell3")
        val shell4 = animationProcessor.getBone("shell4")
        val shell5 = animationProcessor.getBone("shell5")

        ClientEventHandler.handleShells(x, y, shell1, shell2, shell3, shell4, shell5)
    }

    @JvmStatic
    fun handleReloadShakeAnimation(
        stack: ItemStack,
        main: CoreGeoBone,
        camera: CoreGeoBone,
        roll: Float,
        pitch: Float
    ) {
        if (GunData.from(stack).reload.time() > 0) {
            main.rotX *= roll
            main.rotY *= roll
            main.rotZ *= roll
            main.posX *= pitch
            main.posY *= pitch
            main.posZ *= pitch
            camera.rotX *= roll
            camera.rotY *= roll
            camera.rotZ *= roll
        }
    }

    @JvmStatic
    fun handleShootFlare(
        name: String,
        stack: PoseStack,
        itemStack: ItemStack,
        bone: GeoBone,
        buffer: MultiBufferSource,
        packedLightIn: Int
    ) {
        if (itemStack.item !is GunItem) return

        val gunResource = GunResource.from(itemStack).compute()
        if (gunResource.flarePosition != null) {
            handleShootFlare(
                name, stack, itemStack, bone, buffer, packedLightIn,
                gunResource.flarePosition.x, gunResource.flarePosition.y,
                gunResource.flarePosition.z, gunResource.flareSize.toDouble()
            )
        }
    }

    @JvmStatic
    fun handleShootFlare(
        name: String,
        stack: PoseStack,
        itemStack: ItemStack,
        bone: GeoBone,
        buffer: MultiBufferSource,
        packedLightIn: Int,
        x: Double,
        y: Double,
        z: Double,
        size: Double
    ) {
        val data = GunData.from(itemStack)

        if (name == "flare" && ClientEventHandler.fireRotTimer > 0 && ClientEventHandler.fireRotTimer < 0.3 && data.attachment.get(AttachmentType.BARREL) != 2) {
            bone.scaleX = (size + 0.8 * size * (Math.random() - 0.5)).toFloat()
            bone.scaleY = (size + 0.8 * size * (Math.random() - 0.5)).toFloat()
            bone.rotZ = (0.5 * (Math.random() - 0.5)).toFloat()

            var height = 0f

            if ((data.attachment.get(AttachmentType.SCOPE) == 2 || data.attachment.get(AttachmentType.SCOPE) == 3) && ClientEventHandler.zoom) {
                height = -0.07f
            }

            stack.pushPose()
            stack.translate(x, y + 0.02 + height, -z)
            RenderUtils.translateMatrixToBone(stack, bone)
            RenderUtils.translateToPivotPoint(stack, bone)
            RenderUtils.rotateMatrixAroundBone(stack, bone)
            RenderUtils.scaleMatrixForBone(stack, bone)
            RenderUtils.translateAwayFromPivotPoint(stack, bone)
            val pose = stack.last()
            val poseMatrix = pose.pose()
            val normalMatrix = pose.normal()
            val consumer = buffer.getBuffer(ModRenderTypes.MUZZLE_FLASH_TYPE.apply(Mod.loc("textures/particle/flare.png")))
            vertex(consumer, poseMatrix, normalMatrix, packedLightIn, 0f, 0f, 0, 1)
            vertex(consumer, poseMatrix, normalMatrix, packedLightIn, 1f, 0f, 1, 1)
            vertex(consumer, poseMatrix, normalMatrix, packedLightIn, 1f, 1f, 1, 0)
            vertex(consumer, poseMatrix, normalMatrix, packedLightIn, 0f, 1f, 0, 0)
            stack.popPose()

            lerpTimer = Mth.lerp(
                Minecraft.getInstance().partialTick.toDouble(),
                lerpTimer.toDouble(),
                (ClientEventHandler.fireRotTimer * 0.667f)
            ).toFloat()
        }
    }

    @JvmStatic
    fun handleShootSmoke(
        stack: PoseStack,
        bone: GeoBone,
        buffer: MultiBufferSource,
        packedLightIn: Int,
        x: Double,
        y: Double,
        z: Double,
        height: Double
    ) {
        stack.pushPose()
        stack.translate(x, y + height - 0.03, -z)
        RenderUtils.translateMatrixToBone(stack, bone)
        RenderUtils.translateToPivotPoint(stack, bone)
        RenderUtils.rotateMatrixAroundBone(stack, bone)
        RenderUtils.scaleMatrixForBone(stack, bone)
        RenderUtils.translateAwayFromPivotPoint(stack, bone)
        val pose = stack.last()

        val poseMatrix = pose.pose()
        val normalMatrix = pose.normal()

        stack.scale(3f + lerpTimer * 20f, 3f + lerpTimer * 20f, 1f)

        val consumer = buffer.getBuffer(RenderType.entityTranslucent(Mod.loc("textures/particle/shoot_smoke.png")))
        vertexSmoke(consumer, poseMatrix, normalMatrix, packedLightIn, 0f - 0.15f - lerpTimer, 0f, 0, 1, lerpTimer.toDouble())
        vertexSmoke(consumer, poseMatrix, normalMatrix, packedLightIn, 1f - 0.15f - lerpTimer, 0f, 1, 1, lerpTimer.toDouble())
        vertexSmoke(consumer, poseMatrix, normalMatrix, packedLightIn, 1f - 0.15f - lerpTimer, 1f, 1, 0, lerpTimer.toDouble())
        vertexSmoke(consumer, poseMatrix, normalMatrix, packedLightIn, 0f - 0.15f - lerpTimer, 1f, 0, 0, lerpTimer.toDouble())

        stack.popPose()
    }

    @JvmStatic
    fun handleShootSmoke2(
        stack: PoseStack,
        bone: GeoBone,
        buffer: MultiBufferSource,
        packedLightIn: Int,
        x: Double,
        y: Double,
        z: Double,
        height: Double
    ) {
        stack.pushPose()
        stack.translate(x, y + height - 0.03, -z)
        RenderUtils.translateMatrixToBone(stack, bone)
        RenderUtils.translateToPivotPoint(stack, bone)
        RenderUtils.rotateMatrixAroundBone(stack, bone)
        RenderUtils.scaleMatrixForBone(stack, bone)
        RenderUtils.translateAwayFromPivotPoint(stack, bone)
        val pose = stack.last()

        val poseMatrix = pose.pose()
        val normalMatrix = pose.normal()

        stack.scale(3f + lerpTimer * 20f, 3f + lerpTimer * 20f, 1f)

        val consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(Mod.loc("textures/particle/shoot_smoke2.png")))
        vertexSmoke(consumer, poseMatrix, normalMatrix, packedLightIn, 0f + 0.15f + lerpTimer, 0f, 0, 1, lerpTimer.toDouble())
        vertexSmoke(consumer, poseMatrix, normalMatrix, packedLightIn, 1f + 0.15f + lerpTimer, 0f, 1, 1, lerpTimer.toDouble())
        vertexSmoke(consumer, poseMatrix, normalMatrix, packedLightIn, 1f + 0.15f + lerpTimer, 1f, 1, 0, lerpTimer.toDouble())
        vertexSmoke(consumer, poseMatrix, normalMatrix, packedLightIn, 0f + 0.15f + lerpTimer, 1f, 0, 0, lerpTimer.toDouble())

        stack.popPose()
    }

    private fun vertexSmoke(
        pConsumer: VertexConsumer,
        pPose: Matrix4f,
        pNormal: Matrix3f,
        pLightmapUV: Int,
        pX: Float,
        pY: Float,
        pU: Int,
        pV: Int,
        time: Double
    ) {
        pConsumer.vertex(pPose, pX - 0.5F, pY - 0.5F, 0f)
            .color(255, 255, 255, (96 - 40 * time).toInt())
            .uv(pU.toFloat(), pV.toFloat())
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(pLightmapUV)
            .normal(pNormal, 0F, 1F, 0F)
            .endVertex()
    }

    private fun vertex(
        pConsumer: VertexConsumer,
        pPose: Matrix4f,
        pNormal: Matrix3f,
        pLightmapUV: Int,
        pX: Float,
        pY: Float,
        pU: Int,
        pV: Int
    ) {
        pConsumer.vertex(pPose, pX - 0.5F, pY - 0.5F, 0f)
            .color(255, 255, 255, 255)
            .uv(pU.toFloat(), pV.toFloat())
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(pLightmapUV)
            .normal(pNormal, 0F, 1F, 0F)
            .endVertex()
    }

    @JvmStatic
    fun handleZoomCrossHair(
        currentBuffer: MultiBufferSource,
        renderType: RenderType,
        boneName: String,
        stack: PoseStack,
        bone: GeoBone,
        buffer: MultiBufferSource,
        x: Double,
        y: Double,
        z: Double,
        size: Float,
        r: Int,
        g: Int,
        b: Int,
        a: Int,
        name: String,
        hasBlackPart: Boolean
    ) {
        if (boneName == "cross" && ClientEventHandler.zoomPos > 0.1) {
            stack.pushPose()
            stack.translate(x, y, -z)
            RenderUtils.translateMatrixToBone(stack, bone)
            RenderUtils.translateToPivotPoint(stack, bone)
            RenderUtils.rotateMatrixAroundBone(stack, bone)
            RenderUtils.scaleMatrixForBone(stack, bone)
            RenderUtils.translateAwayFromPivotPoint(stack, bone)
            val pose = stack.last()
            val poseMatrix = pose.pose()
            val normalMatrix = pose.normal()
            var tex = Mod.loc("textures/crosshair/$name.png")

            val alpha = (3 * Mth.clamp(ClientEventHandler.zoomTime - 0.34, 0.0, 1.0) * 255).toInt()

            val blackAlpha = if (hasBlackPart) alpha else (0.12 * alpha).toInt()

            var rr = r
            var gg = g
            var bb = b
            var aa = a

            if (activeThermalImaging) {
                rr = 255
                gg = 255
                bb = 255
                aa = 255
                tex = SmartTextureBrightener.getSmartBrightenedTexture(tex, 10f)
            }

            val blackPart = buffer.getBuffer(RenderType.entityTranslucentEmissive(tex))
            vertexRGB(blackPart, poseMatrix, normalMatrix, 0f, 0f, 0, 1, rr, gg, bb, blackAlpha, size)
            vertexRGB(blackPart, poseMatrix, normalMatrix, size, 0f, 1, 1, rr, gg, bb, blackAlpha, size)
            vertexRGB(blackPart, poseMatrix, normalMatrix, size, size, 1, 0, rr, gg, bb, blackAlpha, size)
            vertexRGB(blackPart, poseMatrix, normalMatrix, 0f, size, 0, 0, rr, gg, bb, blackAlpha, size)

            val consumer = buffer.getBuffer(ModRenderTypes.MUZZLE_FLASH_TYPE.apply(tex))
            vertexRGB(consumer, poseMatrix, normalMatrix, 0f, 0f, 0, 1, rr, gg, bb, aa, size)
            vertexRGB(consumer, poseMatrix, normalMatrix, size, 0f, 1, 1, rr, gg, bb, aa, size)
            vertexRGB(consumer, poseMatrix, normalMatrix, size, size, 1, 0, rr, gg, bb, aa, size)
            vertexRGB(consumer, poseMatrix, normalMatrix, 0f, size, 0, 0, rr, gg, bb, aa, size)

            stack.popPose()
        }
        currentBuffer.getBuffer(renderType)
    }

    private fun vertexRGB(
        pConsumer: VertexConsumer,
        pPose: Matrix4f,
        pNormal: Matrix3f,
        pX: Float,
        pY: Float,
        pU: Int,
        pV: Int,
        r: Int,
        g: Int,
        b: Int,
        a: Int,
        size: Float
    ) {
        pConsumer.vertex(pPose, pX - 0.5F * size, pY - 0.5F * size, 0f)
            .color(r, g, b, a)
            .uv(pU.toFloat(), pV.toFloat())
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(pNormal, 0F, 1F, 0F)
            .endVertex()
    }

    @JvmStatic
    fun renderArms(
        localPlayer: LocalPlayer?,
        transformType: ItemDisplayContext?,
        stack: PoseStack,
        name: String,
        bone: GeoBone,
        currentBuffer: MultiBufferSource,
        renderType: RenderType,
        packedLightIn: Int,
        useOldHandRender: Boolean
    ) {
        if (transformType == null || !transformType.firstPerson()) {
            return
        }

        val mc = Minecraft.getInstance()

        if (localPlayer == null) {
            return
        }

        val playerRenderer = mc.entityRenderDispatcher.getRenderer(localPlayer) as PlayerRenderer
        val model = playerRenderer.model
        stack.pushPose()
        RenderUtils.translateMatrixToBone(stack, bone)
        RenderUtils.translateToPivotPoint(stack, bone)
        RenderUtils.rotateMatrixAroundBone(stack, bone)
        RenderUtils.scaleMatrixForBone(stack, bone)
        RenderUtils.translateAwayFromPivotPoint(stack, bone)

        val arm = if ("Lefthand" == name) HumanoidArm.LEFT else HumanoidArm.RIGHT
        val renderPlayerArmEvent = RenderPlayerArmEvent(
            localPlayer, transformType, stack, arm, bone,
            currentBuffer, renderType, packedLightIn, useOldHandRender
        )
        if (FORGE_BUS.post(renderPlayerArmEvent)) {
            currentBuffer.getBuffer(renderType) // 用来重置 Render Type，防止后续渲染出错
            stack.popPose()
            return
        }

        val loc = localPlayer.skinTextureLocation
        val armBuilder = currentBuffer.getBuffer(RenderType.entitySolid(loc))
        val sleeveBuilder = currentBuffer.getBuffer(RenderType.entityTranslucent(loc))

        val overlayTexture = if (activeThermalImaging) OverlayTexture.pack(15, 10) else OverlayTexture.NO_OVERLAY

        var effectivePackedLight = packedLightIn
        if (activeThermalImaging) {
            effectivePackedLight = LightTexture.FULL_BRIGHT
        }

        if (arm == HumanoidArm.LEFT) {
            if (!model.leftArm.visible) {
                model.leftArm.visible = true
            }
            if (!model.leftSleeve.visible && mc.options.isModelPartEnabled(PlayerModelPart.LEFT_SLEEVE)) {
                model.leftSleeve.visible = true
            }

            stack.translate(-1.0f * CustomGunRenderer.SCALE_RECIPROCAL, 2.0f * CustomGunRenderer.SCALE_RECIPROCAL, 0.0f)
            if (useOldHandRender) {
                renderPartOverBone(model.leftArm, bone, stack, armBuilder, effectivePackedLight, overlayTexture, 1f)
                renderPartOverBone(model.leftSleeve, bone, stack, sleeveBuilder, effectivePackedLight, overlayTexture, 1f)
            } else {
                renderPartOverBone2(model.leftArm, bone, stack, armBuilder, effectivePackedLight, overlayTexture, 1f)
                renderPartOverBone2(model.leftSleeve, bone, stack, sleeveBuilder, effectivePackedLight, overlayTexture, 1f)
            }
        } else {
            if (!model.rightArm.visible) {
                model.rightArm.visible = true
            }
            if (!model.rightSleeve.visible && mc.options.isModelPartEnabled(PlayerModelPart.RIGHT_SLEEVE)) {
                model.rightSleeve.visible = true
            }

            stack.translate(CustomGunRenderer.SCALE_RECIPROCAL, 2.0f * CustomGunRenderer.SCALE_RECIPROCAL, 0.0f)
            if (useOldHandRender) {
                renderPartOverBone(model.rightArm, bone, stack, armBuilder, effectivePackedLight, overlayTexture, 1f)
                renderPartOverBone(model.rightSleeve, bone, stack, sleeveBuilder, effectivePackedLight, overlayTexture, 1f)
            } else {
                renderPartOverBone2(model.rightArm, bone, stack, armBuilder, effectivePackedLight, overlayTexture, 1f)
                renderPartOverBone2(model.rightSleeve, bone, stack, sleeveBuilder, effectivePackedLight, overlayTexture, 1f)
            }
        }

        currentBuffer.getBuffer(renderType) // 用来重置 Render Type，防止后续渲染出错
        stack.popPose()
    }
}
