package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.gun.GeoGunAnimationInstance
import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.EDIT_FOCUS_Z_OFFSET
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.magazineLevel
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.resource.ModelResource
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.resource.gun.pojo.ItemDisplayInfo
import com.atsuishio.superbwarfare.resource.model.AttachmentModelReloadListener
import com.atsuishio.superbwarfare.script.GunScriptManager
import com.atsuishio.superbwarfare.tools.RenderDistanceHelper
import com.atsuishio.superbwarfare.tools.deltaFrameTime
import com.atsuishio.superbwarfare.tools.mulPoseMatrix
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.firstperson.FirstPersonParticleSystem
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.CameraStateCache
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.renderer.AbstractGeoItemRendererV2
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.YXZRotationView
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.client.event.ViewportEvent
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import java.util.*
import kotlin.math.roundToInt

open class GeoGunRenderer : AbstractGeoItemRendererV2() {

    protected val capturedRenderPose = mutableMapOf<InteractionHand, Matrix4f>()
    protected val lastBoneTransforms = mutableMapOf<InteractionHand, MutableMap<String, Matrix4f>>()
    protected val muzzleEmitterLocators =
        mutableMapOf<InteractionHand, MutableMap<ParticleEmitterInstance, String>>()

    override fun createAnimationInstance(stack: ItemStack, entity: Entity): IFPAnimationInstance {
        return GeoGunAnimationInstance(stack, entity, InteractionHand.MAIN_HAND)
    }

    override fun createAnimationInstance(
        stack: ItemStack,
        entity: Entity,
        hand: InteractionHand
    ): IFPAnimationInstance {
        return GeoGunAnimationInstance(stack, entity, hand)
    }

    override fun getSlotTexture(stack: ItemStack): ResourceLocation? {
        val resource = GunResource.compute(stack)
        val slotIcon = resource.slotIcon.ifEmpty { null } ?: return null
        return ResourceLocation.tryParse(slotIcon)
    }

    override fun hasModel(stack: ItemStack): Boolean {
        val modelResource = GunResource.compute(stack).getModel()
        return GeoGunModel.create(modelResource) != null
    }

    override fun applyLevelCameraAnimation(
        event: ViewportEvent.ComputeCameraAngles,
        stack: ItemStack,
        animateRot: Quaternionf,
        partialTicks: Float
    ) {
        // Avoid the Euler -> Quaternion -> Euler roundtrip while idle/aiming:
        // at +/-90 degrees pitch it remaps yaw into roll.
        if (Mth.abs(animateRot.x()) < 1e-5f &&
            Mth.abs(animateRot.y()) < 1e-5f &&
            Mth.abs(animateRot.z()) < 1e-5f &&
            Mth.abs(animateRot.w() - 1f) < 1e-5f
        ) {
            return
        }

        val raw = YXZRotationView(
            Vector3f(
                Mth.DEG_TO_RAD * event.pitch,
                Mth.DEG_TO_RAD * event.yaw,
                Mth.DEG_TO_RAD * event.roll
            )
        ).asQuaternion()
        val combined = Quaternionf(raw).mul(animateRot)
        val euler = YXZRotationView(combined).asEulerAngle()

        event.yaw = Mth.RAD_TO_DEG * euler.y()
        event.pitch = Mth.RAD_TO_DEG * euler.x()
        event.roll = -Mth.RAD_TO_DEG * euler.z()
    }

    override fun applyItemInHandCameraAnimation(
        poseStack: PoseStack,
        stack: ItemStack,
        animateRot: Quaternionf,
        partialTicks: Float
    ) {
        poseStack.mulPose(animateRot)
    }

    override fun renderFirstPerson(
        player: LocalPlayer,
        stack: ItemStack,
        transformType: ItemDisplayContext,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        partialTick: Float
    ) {
        render(stack, transformType, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, partialTick)
    }

    override fun beforeRender(
        poseStack: PoseStack,
        transformType: ItemDisplayContext,
        stack: ItemStack,
        partialTick: Float
    ) {
        val resource = GunResource.compute(stack)
        val modelResource = resource.getModel()
        val boneName = positioningBone(transformType)
        val usesModelBone = !transformType.firstPerson()
                && boneName != null
                && GeoGunModel.create(modelResource)?.getBindGlobalTransform(boneName) != null
        val display = resource.itemDisplay[displayKey(transformType)]
        if (display != null && !usesModelBone) {
            applyItemDisplayTransform(poseStack, display)
        }
        super.beforeRender(poseStack, transformType, stack, partialTick)
    }

    override fun updateParticleEmitterTransforms(
        system: FirstPersonParticleSystem,
        poseStack: PoseStack,
        hand: InteractionHand
    ) {
        capturedRenderPose[hand] = Matrix4f(poseStack.last().pose())
    }

    override fun afterRender(
        poseStack: PoseStack,
        transformType: ItemDisplayContext,
        stack: ItemStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        partialTick: Float
    ) {
        super.afterRender(poseStack, transformType, stack, bufferSource, packedLight, partialTick)
        if (!transformType.firstPerson()) return
        spawnAndBindMuzzleParticles(poseStack, stack, handForContext(transformType))
    }

    override fun renderModel(
        poseStack: PoseStack,
        transformType: ItemDisplayContext,
        stack: ItemStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int,
        partialTick: Float
    ) {
        if (transformType.firstPerson()) {
            lastBoneTransforms[handForContext(transformType)]?.clear()
        }

        val resource = GunResource.compute(stack)
        val modelResource = resource.getModel()

        val useLod = !transformType.firstPerson()
                && DisplayConfig.ENABLE_GUN_LOD.get()
                && !RenderDistanceHelper.isInGui()
        val model = if (useLod) {
            GeoGunModel.create(modelResource, 1)
        } else {
            GeoGunModel.create(modelResource)
        } ?: return

        val texture = if (useLod) {
            modelResource.getLODTexture(1)
        } else {
            modelResource.texture
        } ?: return

        model.renderHand = transformType.firstPerson()
        if (transformType.firstPerson()) {
            val hand = handForContext(transformType)
            val pose = FirstPersonRenderHandler.getActiveAnimationInstance(hand)?.cachedPose
            if (pose != null) {
                model.applyPose(BLENDER.blend(model.getBindPose(), pose))
            }

            applyCameraShake(stack, model, hand)

            updateEditFocus(model)

            applyFirstPersonPositioningTransform(poseStack, model)

            val sprintOffset = resource.sprintOffset
            ClientEventHandler.gunRootMoveV2(poseStack, sprintOffset.x, sprintOffset.y, sprintOffset.z, false)

            val shootRecoil = resource.shootRecoil
            ClientEventHandler.handleShootAnimationV2(
                poseStack,
                shootRecoil.offset.x, shootRecoil.offset.y, shootRecoil.offset.z,
                shootRecoil.rotation.x, shootRecoil.rotation.y, shootRecoil.rotation.z,
                shootRecoil.zoomRate, shootRecoil.speed
            )

            val zoomPivot = computeViewTransform(model)?.let {
                val pivot = Vector3f()
                it.getTranslation(pivot)
                pivot
            }
            if (zoomPivot != null) {
                poseStack.translate(zoomPivot.x, zoomPivot.y, zoomPivot.z)
            }
            poseStack.scale(1f, 1f, 1f - 0.25f * ClientEventHandler.zoomTime.toFloat())
            if (zoomPivot != null) {
                poseStack.translate(-zoomPivot.x, -zoomPivot.y, -zoomPivot.z)
            }
        }
        applyCustomAnimations(stack, model, transformType, partialTick)
        applyCustomAnimationsByScript(stack, model, transformType, partialTick)
        if (!transformType.firstPerson()) {
            applyModelBonePositioning(poseStack, model, modelResource, transformType)
        }
        val attachmentRender = resolveBarrelAttachmentRender(stack)
        val attachmentMuzzleTransform = attachmentRender?.let {
            resolveBarrelAttachmentMuzzleTransform(stack, model, it)
        }
        val muzzleFlashScale = resolveBarrelAttachmentMuzzleFlashScale(stack)
        renderAttachments(stack, model, transformType, poseStack, bufferSource, packedLight, packedOverlay)
        model.renderToBuffer(poseStack, bufferSource, texture, packedLight, packedOverlay)
        if (transformType.firstPerson()) {
            MuzzleFlashRenderer.render(
                poseStack,
                model,
                stack,
                bufferSource,
                attachmentMuzzleTransform,
                muzzleFlashScale
            )

            val hand = handForContext(transformType)
            ShellCasingFxRenderer.render(poseStack, model, stack, hand, bufferSource, packedLight)

            val transforms = lastBoneTransforms.getOrPut(handForContext(transformType)) { mutableMapOf() }
            for (boneName in listOf(FLARE_BONE, MUZZLE_FLASH_BONE)) {
                model.getGlobalTransform(boneName)?.let { transforms[boneName] = Matrix4f(it) }
            }
            attachmentMuzzleTransform?.let { transforms[MUZZLE_BONE] = Matrix4f(it) }
        }
        model.resetPose()
    }

    open fun renderAttachments(
        stack: ItemStack,
        model: GeoGunModel,
        transformType: ItemDisplayContext,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        renderMagazine(stack, model)
        renderStock(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
        renderGripHandGuard(stack, model)
        renderGripAttachment(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
        renderBarrelAttachment(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
    }

    open fun renderMagazine(stack: ItemStack, model: GeoGunModel) {
        model.showMagazineBone(resolveMagazineBone(stack))
    }

    open fun renderStock(
        stack: ItemStack,
        model: GeoGunModel,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val definition = resolveStockDefinition(stack)
        if (definition == null) {
            model.showStockBone(GeoGunModel.OEM_STOCK_STANDARD_BONE)
            return
        }

        if (definition.usesGunStock) {
            model.showStockBone(
                definition.bone ?: GeoGunModel.OEM_STOCK_STANDARD_BONE
            )
            return
        }

        model.showStockBone(
            GeoGunModel.CUSTOM_STOCK_ADAPTER_BONE,
            GeoGunModel.CUSTOM_STOCK_ADAPTER_BONE
        )
        renderStockAttachment(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
    }

    open fun resolveStockDefinition(stack: ItemStack): AttachmentDefinition? {
        val data = GunData.from(stack)
        val attachmentId = data.attachment.id(AttachmentType.STOCK) ?: return null
        return AttachmentDefinition.from(attachmentId)
    }

    open fun renderStockAttachment(
        stack: ItemStack,
        model: GeoGunModel,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val (attachmentModel, texture) = resolveStockAttachmentRender(stack) ?: return
        val mountTransform = model.getGlobalTransform(GeoGunModel.CUSTOM_STOCK_ADAPTER_BONE) ?: return

        poseStack.pushPose()
        poseStack.mulPoseMatrix(Matrix4f(mountTransform))
        attachmentModel.renderToBuffer(poseStack, bufferSource, texture, packedLight, packedOverlay)
        poseStack.popPose()
    }

    open fun resolveStockAttachmentRender(stack: ItemStack): Pair<BedrockAttachmentModel, ResourceLocation>? {
        val data = GunData.from(stack)
        val attachmentId = data.attachment.id(AttachmentType.STOCK) ?: return null
        val definition = AttachmentDefinition.from(attachmentId) ?: return null
        if (definition.usesGunStock) return null
        val modelPath = definition.model ?: return null
        val texture = definition.texture ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        return Pair(attachmentModel, texture)
    }

    open fun renderGripHandGuard(stack: ItemStack, model: GeoGunModel) {
        val customBone = model.getBone(CUSTOM_HAND_GUARD_BONE) ?: return
        val gripInstalled = GunData.from(stack).attachment.has(AttachmentType.GRIP)
        val showCustom = gripInstalled && GunResource.compute(stack).attachmentInfo.gripHandGuard
        customBone.visible = showCustom
        model.getBone(OEM_HAND_GUARD_BONE)?.visible = !showCustom
    }

    open fun renderGripAttachment(
        stack: ItemStack,
        model: GeoGunModel,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val (attachmentModel, texture) = resolveGripAttachmentRender(stack) ?: return
        val boneName = resolveGripAttachmentBone(stack)
        val mountTransform = model.getGlobalTransform(boneName) ?: return

        poseStack.pushPose()
        poseStack.mulPoseMatrix(Matrix4f(mountTransform))
        attachmentModel.renderToBuffer(poseStack, bufferSource, texture, packedLight, packedOverlay)
        poseStack.popPose()
    }

    open fun resolveGripAttachmentRender(stack: ItemStack): Pair<BedrockAttachmentModel, ResourceLocation>? {
        val data = GunData.from(stack)
        val attachmentId = data.attachment.id(AttachmentType.GRIP) ?: return null
        val definition = AttachmentDefinition.from(attachmentId) ?: return null
        val modelPath = definition.model ?: return null
        val texture = definition.texture ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        return Pair(attachmentModel, texture)
    }

    open fun resolveGripAttachmentBone(stack: ItemStack): String {
        return GRIP_BONE
    }

    open fun renderBarrelAttachment(
        stack: ItemStack,
        model: GeoGunModel,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val (attachmentModel, texture) = resolveBarrelAttachmentRender(stack) ?: return
        val boneName = resolveBarrelAttachmentBone(stack) ?: return
        val mountTransform = model.getGlobalTransform(boneName) ?: return

        poseStack.pushPose()
        poseStack.mulPoseMatrix(Matrix4f(mountTransform))
        attachmentModel.renderToBuffer(poseStack, bufferSource, texture, packedLight, packedOverlay)
        poseStack.popPose()
    }

    open fun resolveBarrelAttachmentRender(stack: ItemStack): Pair<BedrockAttachmentModel, ResourceLocation>? {
        val data = GunData.from(stack)
        val attachmentId = data.attachment.id(AttachmentType.BARREL) ?: return null
        val definition = AttachmentDefinition.from(attachmentId) ?: return null
        val modelPath = definition.model ?: return null
        val texture = definition.texture ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        return Pair(attachmentModel, texture)
    }

    open fun resolveBarrelAttachmentMuzzleFlashScale(stack: ItemStack): Float {
        val data = GunData.from(stack)
        val attachmentId = data.attachment.id(AttachmentType.BARREL) ?: return 1.0f
        return AttachmentDefinition.from(attachmentId)?.muzzleFlashScale?.coerceAtLeast(0f) ?: 1.0f
    }

    open fun resolveBarrelAttachmentBone(stack: ItemStack): String? {
        val data = GunData.from(stack)
        val attachmentId = data.attachment.id(AttachmentType.BARREL) ?: return null
        return AttachmentDefinition.from(attachmentId)?.bone
    }

    open fun resolveBarrelAttachmentMuzzleTransform(
        stack: ItemStack,
        model: GeoGunModel,
        renderData: Pair<BedrockAttachmentModel, ResourceLocation>
    ): Matrix4f? {
        val attachmentMuzzle = renderData.first.getGlobalTransform(MUZZLE_BONE) ?: return null
        val boneName = resolveBarrelAttachmentBone(stack) ?: return null
        val mountTransform = model.getGlobalTransform(boneName) ?: return null
        return Matrix4f(mountTransform).mul(attachmentMuzzle)
    }

    open fun resolveMagazineBone(stack: ItemStack): String {
        return when (GunData.from(stack).magazineLevel()) {
            1 -> GeoGunModel.MAGAZINE_EXTEND_BONE
            2 -> GeoGunModel.MAGAZINE_EXTEND_PRO_BONE
            else -> GeoGunModel.MAGAZINE_STANDARD_BONE
        }
    }

    open fun applyCustomAnimations(
        stack: ItemStack,
        model: GeoGunModel,
        transformType: ItemDisplayContext,
        partialTick: Float
    ) {
    }

    open fun applyCustomAnimationsByScript(
        stack: ItemStack,
        model: GeoGunModel,
        transformType: ItemDisplayContext,
        partialTick: Float
    ) {
        val script = GunResource.compute(stack).getScript() ?: return
        GunScriptManager.invokeTransform(script, stack, model, transformType, partialTick, this)
    }

    open fun spawnAndBindMuzzleParticles(
        poseStack: PoseStack,
        stack: ItemStack,
        hand: InteractionHand
    ) {
        val resource = GunResource.compute(stack)
        if (!resource.hasSmoke) return

        val basePose = capturedRenderPose[hand] ?: return
        val boneTransforms = lastBoneTransforms[hand] ?: return
        if (boneTransforms.isEmpty()) return

        val animation = FirstPersonRenderHandler.getActiveAnimationInstance(hand) as? GeoGunAnimationInstance ?: return
        val system = FirstPersonRenderHandler.getParticleSystem()
        val newEmitters = ArrayList<ParticleEmitterInstance>()
        val locators = muzzleEmitterLocators.getOrPut(hand) { WeakHashMap() }

        for (data in animation.consumePendingParticles()) {
            val definition = ParticleDefinitionLoader.getInstance().getDefinition(data.effect()) ?: continue
            val emitter = system.addEmitter(definition, hand)
            val smoke = resource.smoke
            val shotRandom = Math.random().toFloat()
            val sizeJitter = 1f - smoke.randomSize + shotRandom * 2f * smoke.randomSize
            val growthJitter = 1f - smoke.randomGrowth + shotRandom * 2f * smoke.randomGrowth
            val lifetimeJitter = 1f - smoke.randomLifetime + shotRandom * 2f * smoke.randomLifetime
            val speedJitter = 1f - smoke.randomSpeed + shotRandom * 2f * smoke.randomSpeed
            val countJitter = 1f - smoke.randomCount + shotRandom * 2f * smoke.randomCount
            val opacityJitter = 1f - smoke.randomOpacity + shotRandom * 2f * smoke.randomOpacity

            emitter.setVariable("smoke_size", smoke.size * sizeJitter)
            emitter.setVariable("smoke_growth", smoke.growth * growthJitter)
            emitter.setVariable("smoke_lifetime", smoke.lifetime * lifetimeJitter)
            emitter.setVariable("smoke_speed", smoke.speed * speedJitter)
            emitter.setVariable(
                "smoke_count",
                (smoke.count * countJitter).roundToInt().coerceAtLeast(1).toFloat()
            )
            emitter.setVariable("smoke_opacity", smoke.opacity * opacityJitter)
            emitter.setVariable("smoke_drag", resource.smoke.drag)
            val locator = resolveMuzzleLocator(data, boneTransforms)
            if (locator == null) {
                emitter.setRemoved(true)
                continue
            }
            locators[emitter] = locator
            newEmitters += emitter
        }

        if (locators.isEmpty()) return

        val basePoseInv = Matrix4f(basePose).invert()
        val cameraRotationInv = cameraRotationInverse()
        val gunViewPose = poseStack.last().pose()

        for ((emitter, locator) in locators) {
            val boneTransform = boneTransforms[locator] ?: continue
            val muzzleView = Matrix4f(gunViewPose).mul(boneTransform)
            emitter.setEmitterTransform(
                Matrix4f(basePoseInv).mul(muzzleView),
                Matrix4f(cameraRotationInv).mul(muzzleView)
            )
        }

        for (emitter in newEmitters) {
            emitter.tick(0f)
        }

        locators.keys.removeIf { it.isFinished }
    }

    open fun resolveMuzzleLocator(
        data: ParticleEffectData,
        boneTransforms: Map<String, Matrix4f>
    ): String? {
        if (boneTransforms.containsKey(MUZZLE_BONE)) {
            return MUZZLE_BONE
        }
        val locator = data.locator()
        if (locator.isNotBlank() && boneTransforms.containsKey(locator)) {
            return locator
        }
        if (boneTransforms.containsKey(FLARE_BONE)) {
            return FLARE_BONE
        }
        if (boneTransforms.containsKey(MUZZLE_FLASH_BONE)) {
            return MUZZLE_FLASH_BONE
        }
        return null
    }

    open fun cameraRotationInverse(): Matrix4f {
        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        return Matrix4f()
            .rotationX(Mth.DEG_TO_RAD * camera.xRot)
            .rotateY(Mth.DEG_TO_RAD * (camera.yRot + 180f))
            .rotateZ(CameraStateCache.getCameraRollRadians())
            .invert()
    }

    open fun handForContext(transformType: ItemDisplayContext): InteractionHand {
        return if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            InteractionHand.OFF_HAND
        } else {
            InteractionHand.MAIN_HAND
        }
    }

    open fun applyCameraShake(stack: ItemStack, model: GeoGunModel, hand: InteractionHand) {
        val animation = FirstPersonRenderHandler.getActiveAnimationInstance(hand) ?: return
        val camera = model.getCameraBone()
        if (camera == null) {
            animation.cameraRotation = Quaternionf()
            return
        }

        val strength = DisplayConfig.WEAPON_SCREEN_SHAKE.get().toFloat() / 100f
        if (strength <= 0f) {
            animation.cameraRotation = Quaternionf()
            return
        }

        val zoomTime = ClientEventHandler.zoomTime.coerceIn(0.0, 1.0).toFloat()
        val rotationScale = (1f - 0.9f * zoomTime).coerceAtLeast(0.05f)
        val positionScale = (1f - 0.8f * zoomTime).coerceAtLeast(0.05f)

        val main = model.getRootBone()
        main?.let { bone ->
            val boneEuler = Vector3f(bone.rotationInEuler).mul(rotationScale)
            bone.rotation.set(Quaternionf().rotateZYX(boneEuler.z, boneEuler.y, boneEuler.x))
            bone.rotationInEuler.set(boneEuler)
            bone.x *= positionScale
            bone.y *= positionScale
            bone.z *= positionScale
        }

        val cameraEuler = Vector3f(camera.rotationInEuler).mul(rotationScale).mul(-strength)
        animation.cameraRotation = Quaternionf().rotateZYX(cameraEuler.z, cameraEuler.y, cameraEuler.x)
    }

    open fun applyFirstPersonPositioningTransform(poseStack: PoseStack, model: GeoGunModel) {
        val viewTransform = computeViewTransform(model) ?: return
        poseStack.mulPoseMatrix(viewTransform.invert())
    }

    open fun computeViewTransform(model: GeoGunModel): Matrix4f? {
        val idleViewTransform = model.getGlobalTransform(IDLE_VIEW_BONE) ?: return null

        val zoom = AnimationCurves.EASE_IN_OUT_QUINT
            .apply(ClientEventHandler.zoomTime.coerceIn(0.0, 1.0))
            .toFloat()

        val focusOffset = ClientEventHandler.editFocusOffset
        if (focusOffset.lengthSquared() > 1e-8f && zoom <= 0f) {
            val idlePos = Vector3f()
            idleViewTransform.getTranslation(idlePos)
            val translation = Vector3f(idlePos).add(focusOffset)
            var rotation = Quaternionf()
            idleViewTransform.getNormalizedRotation(rotation)
            val yaw = ClientEventHandler.editFocusYaw
            val pitch = ClientEventHandler.editFocusPitch
            if (Mth.abs(pitch) > 1e-5f || Mth.abs(yaw) > 1e-5f) {
                rotation = Quaternionf().rotateY(yaw).rotateX(pitch).mul(rotation)
            }
            val scale = Vector3f()
            idleViewTransform.getScale(scale)
            return Matrix4f()
                .translation(translation)
                .rotate(rotation)
                .scale(scale)
        }

        if (zoom <= 0f) {
            return Matrix4f(idleViewTransform)
        }
        val ironViewTransform = model.getGlobalTransform(IRON_VIEW_BONE)
            ?: return Matrix4f(idleViewTransform)
        return blendViewTransform(Matrix4f(idleViewTransform), Matrix4f(ironViewTransform), zoom)
    }

    /**
     * 返回当前正在编辑的配件槽位对应的定位骨骼名；未支持或未选中时返回 null。
     * 目前支持枪托与弹匣。
     */
    open fun attachmentFocusBone(): String? {
        return when (ClientEventHandler.editingAttachmentType) {
            0 -> MUZZLE_BONE
            1 -> SCOPE_BONE
            2 -> GRIP_BONE
            3 -> STOCK_BONE
            4 -> MAGAZINE_BONE
            5 -> MAGAZINE_BONE
            else -> null
        }
    }

    /**
     * 每帧将 [com.atsuishio.superbwarfare.event.ClientEventHandler.editFocusOffset] 向
     * 改装聚焦目标偏移平滑插值，实现槽位切换时的缓动过渡。
     */
    private fun updateEditFocus(model: GeoGunModel) {
        val desired = computeEditFocusOffset(model) ?: Vector3f()
        val desiredYaw = computeEditFocusYaw()
        val desiredPitch = computeEditFocusPitch()
        val delta = Minecraft.getInstance().deltaFrameTime.coerceAtMost(0.5f)
        val focusing = attachmentFocusBone() != null
        val panning = ClientEventHandler.isEditing && !focusing

        if (focusing) {
            // 聚焦配件时重置回退缓动时长，供之后 ESC 返回预览使用
            ClientEventHandler.editFocusReturnTime = EDIT_FOCUS_RETURN_TIME
        } else if (panning && ClientEventHandler.editFocusReturnTime > 0f) {
            ClientEventHandler.editFocusReturnTime =
                (ClientEventHandler.editFocusReturnTime - delta).coerceAtLeast(0f)
        }

        val smoothing = when {
            panning && ClientEventHandler.editFocusReturnTime > 0f -> EDIT_FOCUS_RETURN_SMOOTHING
            panning -> UNFOCUSED_PAN_SMOOTHING
            else -> EDIT_FOCUS_SMOOTHING
        }
        val t = (smoothing * delta).coerceIn(0f, 1f)
        ClientEventHandler.editFocusOffset.lerp(desired, t)
        ClientEventHandler.editFocusYaw = Mth.lerp(t, ClientEventHandler.editFocusYaw, desiredYaw)
        ClientEventHandler.editFocusPitch = Mth.lerp(t, ClientEventHandler.editFocusPitch, desiredPitch)
    }

    /**
     * 返回改装聚焦的目标偏移（相对 IDLE_VIEW_BONE，模型空间）：聚焦点为当前编辑配件定位点的
     * 绝对坐标往 Z 轴负方向偏移 [EDIT_FOCUS_Z_OFFSET] 单位，避免视角卡进模型。
     * 未选中配件时返回浮动预览的鼠标平移偏移；未处于改装状态或对应骨骼不存在时返回 null。
     */
    private fun computeEditFocusOffset(model: GeoGunModel): Vector3f? {
        if (!ClientEventHandler.isEditing) return null
        val boneName = attachmentFocusBone() ?: return computeUnfocusedPanOffset()

        val idleView = model.getGlobalTransform(IDLE_VIEW_BONE) ?: return null
        val attachment = model.getGlobalTransform(boneName) ?: return null

        val idlePos = Vector3f()
        idleView.getTranslation(idlePos)
        val attachmentPos = Vector3f()
        attachment.getTranslation(attachmentPos)

        // 世界坐标：配件定位点往 Z 轴方向偏移，不使用配件的局部坐标
        val focusPos = Vector3f(attachmentPos.x, attachmentPos.y, attachmentPos.z + EDIT_FOCUS_Z_OFFSET)
        return focusPos.sub(idlePos)
    }

    /**
     * 未聚焦配件时的浮动预览偏移：以屏幕中心为原点，根据鼠标位置动态平移视角定位点的 XY，
     * 使视角跟随鼠标移动，便于查看超出屏幕范围的长枪。
     */
    private fun computeUnfocusedPanOffset(): Vector3f {
        val mc = Minecraft.getInstance()
        val window = mc.window
        val x = doubleArrayOf(0.0)
        val y = doubleArrayOf(0.0)
        GLFW.glfwGetCursorPos(window.window, x, y)

        val nx = (x[0] / window.width * 2.0 - 1.0).coerceIn(-1.0, 1.0)
        val ny = (y[0] / window.height * 2.0 - 1.0).coerceIn(-1.0, 1.0)

        return Vector3f(
            (nx * UNFOCUSED_PAN_RANGE).toFloat(),
            (-ny * UNFOCUSED_PAN_RANGE).toFloat() * 0.5f,
            0f
        )
    }

    /**
     * 返回未聚焦浮动预览绕 Y 轴的旋转角（弧度）：以屏幕中心为原点，鼠标越靠右整体越向逆时针
     * 方向旋转，越靠左越向顺时针旋转，避免视角平移时卡进模型。聚焦或非改装状态下返回 0。
     */
    private fun computeEditFocusYaw(): Float {
        if (!ClientEventHandler.isEditing || attachmentFocusBone() != null) return 0f

        val mc = Minecraft.getInstance()
        val window = mc.window
        val x = doubleArrayOf(0.0)
        val y = doubleArrayOf(0.0)
        GLFW.glfwGetCursorPos(window.window, x, y)

        val nx = (x[0] / window.width * 2.0 - 1.0).coerceIn(-1.0, 1.0)
        return (-nx * UNFOCUSED_PAN_YAW).toFloat()
    }

    /**
     * 返回未聚焦浮动预览绕 X 轴的旋转角（弧度）：以屏幕中心为原点，鼠标越靠上整体越向俯视
     * 方向旋转，越靠下越向仰视方向旋转。聚焦或非改装状态下返回 0。
     */
    private fun computeEditFocusPitch(): Float {
        if (!ClientEventHandler.isEditing || attachmentFocusBone() != null) return 0f

        val mc = Minecraft.getInstance()
        val window = mc.window
        val x = doubleArrayOf(0.0)
        val y = doubleArrayOf(0.0)
        GLFW.glfwGetCursorPos(window.window, x, y)

        val ny = (y[0] / window.height * 2.0 - 1.0).coerceIn(-1.0, 1.0)
        return (-ny * UNFOCUSED_PAN_PITCH).toFloat()
    }

    open fun blendViewTransform(from: Matrix4f, to: Matrix4f, t: Float): Matrix4f {
        val translation = Vector3f()
        val toTranslation = Vector3f()
        from.getTranslation(translation)
        to.getTranslation(toTranslation)
        translation.lerp(toTranslation, t)

        val rotation = Quaternionf()
        val toRotation = Quaternionf()
        from.getNormalizedRotation(rotation)
        to.getNormalizedRotation(toRotation)
        rotation.slerp(toRotation, t)

        val scale = Vector3f()
        val toScale = Vector3f()
        from.getScale(scale)
        to.getScale(toScale)
        scale.lerp(toScale, t)

        return Matrix4f()
            .translation(translation)
            .rotate(rotation)
            .scale(scale)
    }

    open fun applyItemDisplayTransform(poseStack: PoseStack, display: ItemDisplayInfo) {
        val translation = display.translation
        poseStack.translate(translation[0] / 16f, translation[1] / 16f, translation[2] / 16f)

        val rotation = display.rotation
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation[0]))
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation[1]))
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation[2]))

        val scale = display.scale
        poseStack.scale(scale[0], scale[1], scale[2])
    }

    open fun positioningBone(transformType: ItemDisplayContext): String? {
        return when (transformType) {
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND -> THIRDPERSON_HAND_BONE

            ItemDisplayContext.GROUND -> GROUND_BONE
            ItemDisplayContext.FIXED -> FIXED_BONE
            else -> null
        }
    }

    open fun applyModelBonePositioning(
        poseStack: PoseStack,
        model: GeoGunModel,
        modelResource: ModelResource,
        transformType: ItemDisplayContext
    ) {
        val boneName = positioningBone(transformType) ?: return
        val transform = model.getBindGlobalTransform(boneName)
            ?: GeoGunModel.create(modelResource)?.getBindGlobalTransform(boneName)
            ?: return
        poseStack.mulPoseMatrix(Matrix4f(transform).invert())
    }

    open fun displayKey(transformType: ItemDisplayContext): String {
        return when (transformType) {
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND -> "firstperson_righthand"
            ItemDisplayContext.FIRST_PERSON_LEFT_HAND -> "firstperson_lefthand"
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND -> "thirdperson_righthand"
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND -> "thirdperson_lefthand"
            ItemDisplayContext.GUI -> "gui"
            ItemDisplayContext.GROUND -> "ground"
            ItemDisplayContext.HEAD -> "head"
            ItemDisplayContext.FIXED -> "fixed"
            else -> ""
        }
    }

    companion object {
        // Bone Positions
        private const val IDLE_VIEW_BONE = "idle_view"
        private const val IRON_VIEW_BONE = "iron_view"
        private const val MUZZLE_BONE = "muzzle_pos"
        private const val GRIP_BONE = "grip_pos"
        private const val MAGAZINE_BONE = "magazine_pos"
        private const val SCOPE_BONE = "scope_pos"
        private const val STOCK_BONE = "stock_pos"
        private const val THIRDPERSON_HAND_BONE = "thirdperson_hand"
        private const val GROUND_BONE = "ground"
        private const val FIXED_BONE = "fixed"
        private const val FLARE_BONE = "flare"
        private const val MUZZLE_FLASH_BONE = "muzzle_flash"
        private const val CUSTOM_HAND_GUARD_BONE = "custom_hand_guard"
        private const val OEM_HAND_GUARD_BONE = "oem_hand_guard"

        private const val EDIT_FOCUS_Z_OFFSET = 0.8f
        private const val EDIT_FOCUS_SMOOTHING = 1f
        private const val EDIT_FOCUS_RETURN_SMOOTHING = 3f
        private const val EDIT_FOCUS_RETURN_TIME = 0.6f
        private const val UNFOCUSED_PAN_RANGE = 0.13f
        private const val UNFOCUSED_PAN_SMOOTHING = 12f
        private const val UNFOCUSED_PAN_YAW = 0.6f
        private const val UNFOCUSED_PAN_PITCH = 0.3f

        private val BLENDER: EulerAdditiveBlender =
            SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }
}
