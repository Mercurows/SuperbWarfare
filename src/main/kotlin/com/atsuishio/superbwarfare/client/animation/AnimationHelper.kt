package com.atsuishio.superbwarfare.client.animation

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.api.event.RenderPlayerArmEvent
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.SmartTextureBrightener.getSmartBrightenedTexture
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ClientEventHandler.activeThermalImaging
import com.atsuishio.superbwarfare.event.ClientEventHandler.handleShells
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.tools.postEvent
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
import org.joml.Matrix4f
import software.bernie.geckolib.animation.AnimationProcessor
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.util.RenderUtil

object AnimationHelper {
    var lerpTimer: Float = 0f

    fun renderPartOverBone(
        model: ModelPart,
        bone: GeoBone,
        stack: PoseStack,
        buffer: VertexConsumer,
        packedLightIn: Int,
        packedOverlayIn: Int
    ) {
        setupModelFromBone(model, bone)
        model.render(stack, buffer, packedLightIn, packedOverlayIn)
    }

    fun setupModelFromBone(model: ModelPart, bone: GeoBone) {
        model.setPos(bone.pivotX, bone.pivotY, bone.pivotZ)
        model.xRot = 0.0f
        model.yRot = 0.0f
        model.zRot = 0.0f
    }

    fun renderPartOverBoneR(
        model: ModelPart,
        bone: GeoBone,
        stack: PoseStack,
        buffer: VertexConsumer,
        packedLightIn: Int,
        packedOverlayIn: Int
    ) {
        renderPartOverBone(model, bone, stack, buffer, packedLightIn, packedOverlayIn)
    }

    fun renderPartOverBone2(
        model: ModelPart,
        bone: GeoBone,
        stack: PoseStack,
        buffer: VertexConsumer,
        packedLightIn: Int,
        packedOverlayIn: Int
    ) {
        setupModelFromBone2(model, bone)
        model.render(stack, buffer, packedLightIn, packedOverlayIn)
    }

    fun setupModelFromBone2(model: ModelPart, bone: GeoBone) {
        model.setPos(bone.pivotX, bone.pivotY + 7, bone.pivotZ)
        model.xRot = 0.0f
        model.yRot = 180 * Mth.DEG_TO_RAD
        model.zRot = 180 * Mth.DEG_TO_RAD
    }

    fun renderPartOverBone2R(
        model: ModelPart,
        bone: GeoBone,
        stack: PoseStack,
        buffer: VertexConsumer,
        packedLightIn: Int,
        packedOverlayIn: Int
    ) {
        setupModelFromBone2R(model, bone)
        model.render(stack, buffer, packedLightIn, packedOverlayIn)
    }

    fun setupModelFromBone2R(model: ModelPart, bone: GeoBone) {
        model.setPos(bone.pivotX, bone.pivotY + 7, bone.pivotZ)
        model.xRot = 180 * Mth.DEG_TO_RAD
        model.yRot = 180 * Mth.DEG_TO_RAD
        model.zRot = 0f
    }

    fun handleShellsAnimation(animationProcessor: AnimationProcessor<*>, x: Float, y: Float) {
        val shell1 = animationProcessor.getBone("shell1")
        val shell2 = animationProcessor.getBone("shell2")
        val shell3 = animationProcessor.getBone("shell3")
        val shell4 = animationProcessor.getBone("shell4")
        val shell5 = animationProcessor.getBone("shell5")

        handleShells(x, y, shell1, shell2, shell3, shell4, shell5)
    }

    fun handleReloadShakeAnimation(stack: ItemStack, main: GeoBone, camera: GeoBone, roll: Float, pitch: Float) {
        val data = from(stack)
        if (data.reload.time() > 0) {
            main.setRotX(roll * main.rotX)
            main.setRotY(roll * main.rotY)
            main.setRotZ(roll * main.rotZ)
            main.setPosX(pitch * main.posX)
            main.setPosY(pitch * main.posY)
            main.setPosZ(pitch * main.posZ)
            camera.setRotX(roll * camera.rotX)
            camera.setRotY(roll * camera.rotY)
            camera.setRotZ(roll * camera.rotZ)
        }
    }


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
                name,
                stack,
                itemStack,
                bone,
                buffer,
                packedLightIn,
                gunResource.flarePosition.x,
                gunResource.flarePosition.y,
                gunResource.flarePosition.z,
                gunResource.flareSize.toDouble()
            )
        }
    }

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
        val data = from(itemStack)

        if (name == "flare" && ClientEventHandler.fireRotTimer > 0 && ClientEventHandler.fireRotTimer < 0.3 && data.attachment.get(
                AttachmentType.BARREL
            ) != 2
        ) {
            bone.setScaleX((size + 0.8 * size * (Math.random() - 0.5)).toFloat())
            bone.setScaleY((size + 0.8 * size * (Math.random() - 0.5)).toFloat())
            bone.setRotZ((0.5 * (Math.random() - 0.5)).toFloat())

            var height = 0f

            if ((data.attachment.get(AttachmentType.SCOPE) == 2 || data.attachment.get(AttachmentType.SCOPE) == 3) && ClientEventHandler.zoom) {
                height = -0.07f
            }

            stack.pushPose()
            stack.translate(x, y + 0.02 + height, -z)
            RenderUtil.translateMatrixToBone(stack, bone)
            RenderUtil.translateToPivotPoint(stack, bone)
            RenderUtil.rotateMatrixAroundBone(stack, bone)
            RenderUtil.scaleMatrixForBone(stack, bone)
            RenderUtil.translateAwayFromPivotPoint(stack, bone)
            val pose = stack.last()
            val vertexConsumer =
                buffer.getBuffer(ModRenderTypes.MUZZLE_FLASH_TYPE.apply(loc("textures/particle/flare.png")))
            vertex(vertexConsumer, pose, packedLightIn, 0f, 0f, 0, 1)
            vertex(vertexConsumer, pose, packedLightIn, 1f, 0f, 1, 1)
            vertex(vertexConsumer, pose, packedLightIn, 1f, 1f, 1, 0)
            vertex(vertexConsumer, pose, packedLightIn, 0f, 1f, 0, 0)
            stack.popPose()

            lerpTimer = Mth.lerp(
                Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(true),
                lerpTimer,
                ClientEventHandler.fireRotTimer.toFloat() * 0.667f
            )

            //            handleShootSmoke(stack, bone, buffer, packedLightIn, x, y, z, height);
//            handleShootSmoke2(stack, bone, buffer, packedLightIn, x, y, z, height);
        }
    }

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
        RenderUtil.translateMatrixToBone(stack, bone)
        RenderUtil.translateToPivotPoint(stack, bone)
        RenderUtil.rotateMatrixAroundBone(stack, bone)
        RenderUtil.scaleMatrixForBone(stack, bone)
        RenderUtil.translateAwayFromPivotPoint(stack, bone)
        val `$$6` = stack.last()

        stack.scale(3f + lerpTimer * 20f, 3f + lerpTimer * 20f, 1f)

        val `$$9` = buffer.getBuffer(RenderType.entityTranslucent(loc("textures/particle/shoot_smoke.png")))
        vertexSmoke(`$$9`, `$$6`, packedLightIn, 0 - 0.15f - lerpTimer, 0f, 0, 1, lerpTimer.toDouble())
        vertexSmoke(`$$9`, `$$6`, packedLightIn, 1 - 0.15f - lerpTimer, 0f, 1, 1, lerpTimer.toDouble())
        vertexSmoke(`$$9`, `$$6`, packedLightIn, 1 - 0.15f - lerpTimer, 1f, 1, 0, lerpTimer.toDouble())
        vertexSmoke(`$$9`, `$$6`, packedLightIn, 0 - 0.15f - lerpTimer, 1f, 0, 0, lerpTimer.toDouble())

        stack.popPose()
    }

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
        RenderUtil.translateMatrixToBone(stack, bone)
        RenderUtil.translateToPivotPoint(stack, bone)
        RenderUtil.rotateMatrixAroundBone(stack, bone)
        RenderUtil.scaleMatrixForBone(stack, bone)
        RenderUtil.translateAwayFromPivotPoint(stack, bone)
        val `$$6` = stack.last()

        stack.scale(3f + lerpTimer * 20f, 3f + lerpTimer * 20f, 1f)

        val `$$9` = buffer.getBuffer(RenderType.entityTranslucentEmissive(loc("textures/particle/shoot_smoke2.png")))
        vertexSmoke(`$$9`, `$$6`, packedLightIn, 0 + 0.15f + lerpTimer, 0f, 0, 1, lerpTimer.toDouble())
        vertexSmoke(`$$9`, `$$6`, packedLightIn, 1 + 0.15f + lerpTimer, 0f, 1, 1, lerpTimer.toDouble())
        vertexSmoke(`$$9`, `$$6`, packedLightIn, 1 + 0.15f + lerpTimer, 1f, 1, 0, lerpTimer.toDouble())
        vertexSmoke(`$$9`, `$$6`, packedLightIn, 0 + 0.15f + lerpTimer, 1f, 0, 0, lerpTimer.toDouble())

        stack.popPose()
    }

    private fun vertexSmoke(
        pConsumer: VertexConsumer,
        pPose: PoseStack.Pose,
        pLightmapUV: Int,
        pX: Float,
        pY: Float,
        pU: Int,
        pV: Int,
        time: Double
    ) {
        pConsumer.addVertex(pPose, pX - 0.5f, pY - 0.5f, 0f)
            .setColor(255, 255, 255, (96 - 40 * time).toInt())
            .setUv(pU.toFloat(), pV.toFloat())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pLightmapUV)
            .setNormal(pPose, 0f, 1f, 0f)
    }

    private fun vertex(
        pConsumer: VertexConsumer,
        pPose: PoseStack.Pose,
        pLightmapUV: Int,
        pX: Float,
        pY: Float,
        pU: Int,
        pV: Int
    ) {
        pConsumer.addVertex(pPose, pX - 0.5f, pY - 0.5f, 0f)
            .setColor(255, 255, 255, 255)
            .setUv(pU.toFloat(), pV.toFloat())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pLightmapUV)
            .setNormal(pPose, 0f, 1f, 0f)
    }

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
        name: String?,
        hasBlackPart: Boolean
    ) {
        var r = r
        var g = g
        var b = b
        var a = a
        if (boneName == "cross" && ClientEventHandler.zoomPos > 0.1) {
            stack.pushPose()
            stack.translate(x, y, -z)
            RenderUtil.translateMatrixToBone(stack, bone)
            RenderUtil.translateToPivotPoint(stack, bone)
            RenderUtil.rotateMatrixAroundBone(stack, bone)
            RenderUtil.scaleMatrixForBone(stack, bone)
            RenderUtil.translateAwayFromPivotPoint(stack, bone)
            val pose = stack.last()
            val `$$7` = pose.pose()

            var tex = loc("textures/crosshair/$name.png")

            a = (3 * Mth.clamp(ClientEventHandler.zoomTime - 0.34, 0.0, 1.0) * 255).toInt()

            val alpha = if (hasBlackPart) a else (0.12 * a).toInt()

            if (activeThermalImaging) {
                r = 255
                g = 255
                b = 255
                a = 255
                tex = getSmartBrightenedTexture(tex, 10f)
            }

            val blackPart = buffer.getBuffer(RenderType.entityTranslucentEmissive(tex))
            vertexRGB(blackPart, `$$7`, pose, 255, 0f, 0f, 0, 1, r, g, b, alpha, size)
            vertexRGB(blackPart, `$$7`, pose, 255, size, 0f, 1, 1, r, g, b, alpha, size)
            vertexRGB(blackPart, `$$7`, pose, 255, size, size, 1, 0, r, g, b, alpha, size)
            vertexRGB(blackPart, `$$7`, pose, 255, 0f, size, 0, 0, r, g, b, alpha, size)

            val `$$9` = buffer.getBuffer(ModRenderTypes.MUZZLE_FLASH_TYPE.apply(tex))
            vertexRGB(`$$9`, `$$7`, pose, 255, 0f, 0f, 0, 1, r, g, b, a, size)
            vertexRGB(`$$9`, `$$7`, pose, 255, size, 0f, 1, 1, r, g, b, a, size)
            vertexRGB(`$$9`, `$$7`, pose, 255, size, size, 1, 0, r, g, b, a, size)
            vertexRGB(`$$9`, `$$7`, pose, 255, 0f, size, 0, 0, r, g, b, a, size)

            stack.popPose()
        }
        currentBuffer.getBuffer(renderType)
    }

    private fun vertexRGB(
        pConsumer: VertexConsumer,
        pPose: Matrix4f,
        pNormal: PoseStack.Pose,
        pLightmapUV: Int,
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
        pConsumer.addVertex(pPose, pX - 0.5f * size, pY - 0.5f * size, 0f)
            .setColor(r, g, b, a)
            .setUv(pU.toFloat(), pV.toFloat())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pLightmapUV)
            .setNormal(pNormal, 0f, 1f, 0f)
    }

    fun renderArms(
        localPlayer: LocalPlayer, transformType: ItemDisplayContext?, stack: PoseStack, name: String?, bone: GeoBone,
        currentBuffer: MultiBufferSource, renderType: RenderType, packedLightIn: Int, useOldHandRender: Boolean
    ) {
        var packedLightIn = packedLightIn
        if (transformType != null && transformType.firstPerson()) {
            val mc = Minecraft.getInstance()
            val playerRenderer = mc.entityRenderDispatcher.getRenderer<LocalPlayer?>(localPlayer) as PlayerRenderer
            val model = playerRenderer.getModel()
            stack.pushPose()
            RenderUtil.translateMatrixToBone(stack, bone)
            RenderUtil.translateToPivotPoint(stack, bone)
            RenderUtil.rotateMatrixAroundBone(stack, bone)
            RenderUtil.scaleMatrixForBone(stack, bone)
            RenderUtil.translateAwayFromPivotPoint(stack, bone)

            val arm = if ("Lefthand" == name) HumanoidArm.LEFT else HumanoidArm.RIGHT
            val renderPlayerArmEvent = RenderPlayerArmEvent(
                localPlayer,
                transformType,
                stack,
                arm,
                bone,
                currentBuffer,
                renderType,
                packedLightIn,
                useOldHandRender
            )
            if (postEvent(renderPlayerArmEvent).isCanceled()) {
                currentBuffer.getBuffer(renderType) // 用来重置 Render Type，防止后续渲染出错
                stack.popPose()
                return
            }

            val loc = localPlayer.skin.texture()
            val overlayTexture = if (activeThermalImaging) OverlayTexture.pack(15, 10) else OverlayTexture.NO_OVERLAY

            if (activeThermalImaging) {
                packedLightIn = LightTexture.FULL_BRIGHT
            }

            if (arm == HumanoidArm.LEFT) {
                if (!model.leftArm.visible) {
                    model.leftArm.visible = true
                }
                if (!model.leftSleeve.visible && mc.options.isModelPartEnabled(PlayerModelPart.LEFT_SLEEVE)) {
                    model.leftSleeve.visible = true
                }

                stack.translate(
                    -1.0f * CustomGunRenderer.SCALE_RECIPROCAL,
                    2.0f * CustomGunRenderer.SCALE_RECIPROCAL,
                    0.0f
                )
                if (useOldHandRender) {
                    renderPartOverBone(
                        model.leftArm,
                        bone,
                        stack,
                        currentBuffer.getBuffer(RenderType.entitySolid(loc)),
                        packedLightIn,
                        overlayTexture
                    )
                    renderPartOverBone(
                        model.leftSleeve,
                        bone,
                        stack,
                        currentBuffer.getBuffer(RenderType.entityTranslucent(loc)),
                        packedLightIn,
                        overlayTexture
                    )
                } else {
                    renderPartOverBone2(
                        model.leftArm,
                        bone,
                        stack,
                        currentBuffer.getBuffer(RenderType.entitySolid(loc)),
                        packedLightIn,
                        overlayTexture
                    )
                    renderPartOverBone2(
                        model.leftSleeve,
                        bone,
                        stack,
                        currentBuffer.getBuffer(RenderType.entityTranslucent(loc)),
                        packedLightIn,
                        overlayTexture
                    )
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
                    renderPartOverBone(
                        model.rightArm,
                        bone,
                        stack,
                        currentBuffer.getBuffer(RenderType.entitySolid(loc)),
                        packedLightIn,
                        overlayTexture
                    )
                    renderPartOverBone(
                        model.rightSleeve,
                        bone,
                        stack,
                        currentBuffer.getBuffer(RenderType.entityTranslucent(loc)),
                        packedLightIn,
                        overlayTexture
                    )
                } else {
                    renderPartOverBone2(
                        model.rightArm,
                        bone,
                        stack,
                        currentBuffer.getBuffer(RenderType.entitySolid(loc)),
                        packedLightIn,
                        overlayTexture
                    )
                    renderPartOverBone2(
                        model.rightSleeve,
                        bone,
                        stack,
                        currentBuffer.getBuffer(RenderType.entityTranslucent(loc)),
                        packedLightIn,
                        overlayTexture
                    )
                }
            }

            currentBuffer.getBuffer(renderType) // 用来重置 Render Type，防止后续渲染出错
            stack.popPose()
        }
    }
}