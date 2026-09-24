package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.gun.GeoGunAnimationInstance
import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoReadout
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.EDIT_FOCUS_Z_OFFSET
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeStencilRenderHelper
import com.atsuishio.superbwarfare.compat.oculus.OculusCompat
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.attachment.*
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.magazineLevel
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.resource.ModelResource
import com.atsuishio.superbwarfare.resource.gun.DefaultGunResource
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.resource.gun.pojo.ItemDisplayInfo
import com.atsuishio.superbwarfare.resource.model.AttachmentModelReloadListener
import com.atsuishio.superbwarfare.script.GunScriptManager
import com.atsuishio.superbwarfare.tools.RenderDistanceHelper
import com.atsuishio.superbwarfare.tools.localPlayer
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
import com.mojang.blaze3d.systems.RenderSystem
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
import net.minecraftforge.client.event.ViewportEvent
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import java.util.*
import kotlin.math.roundToInt

open class GeoGunRenderer : AbstractGeoItemRendererV2() {

    protected val capturedRenderPose = mutableMapOf<InteractionHand, Matrix4f>()
    protected val lastBoneTransforms = mutableMapOf<InteractionHand, MutableMap<String, Matrix4f>>()
    protected val muzzleEmitterLocators =
        mutableMapOf<InteractionHand, MutableMap<ParticleEmitterInstance, String>>()

    private var handledScopeAttachment: ResourceLocation? = null
    private var gunStencilCulling = false
    private val scopeViewSmoothing = mutableMapOf<InteractionHand, ScopeViewSmoothState>()

    /**
     * 当前正在渲染的本地玩家第一人称手；不是第一人称渲染时为 `null`。
     *
     * 只有这个入口能确定"这一帧画的是本地玩家自己的手"：第三人称、掉落物、展示框、别人手里的枪
     * 走的是普通物品渲染，画的是别人的枪。脚本需要区分这两者时用它，见 [scriptBipodProgress]。
     */
    private var localFirstPersonHand: InteractionHand? = null

    private data class ScopeViewSmoothState(
        var modeIndex: Int = -1,
        var source: Matrix4f = Matrix4f(),
        var target: Matrix4f = Matrix4f(),
        var current: Matrix4f = Matrix4f(),
        var progress: Float = 1.0f
    )

    data class ScopeRenderData(
        val model: BedrockAttachmentModel,
        val texture: ResourceLocation,
        val scopeMode: ScopeMode,
        val scopeModeIndex: Int,
        val companionSightMode: ScopeMode? = null,
        val attachmentId: ResourceLocation,
        val slotTransform: Matrix4f,
        val bindSlotTransform: Matrix4f,
        // 弹药显示配置；实际的余弹数与比例在渲染调用点按需计算，避免每次解析配件都走一遍 PMC
        val ammoBar: List<AmmoBarEntry> = emptyList(),
        val textShow: List<AmmoTextEntry> = emptyList()
    )

    /**
     * A resolved attachment model ready to be drawn: the model and texture to use, plus the
     * definition they came from, which the ammo display configuration is read off.
     *
     * The definition travels with the model so the render path does not have to look it up a second
     * time — [AttachmentDefinition.from] is a map lookup, but the ammo readout needs the definition's
     * `AmmoBar` / `TextShow` on every frame the attachment is visible.
     */
    data class AttachmentRenderData(
        val model: BedrockAttachmentModel,
        val texture: ResourceLocation,
        val definition: AttachmentDefinition,
    )

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
        val absolutePitch = Mth.abs(Mth.wrapDegrees(event.pitch))

        // At +/-90 degrees pitch the YXZ Euler decomposition is singular. Fold
        // animated yaw into roll instead of changing event.yaw, otherwise the
        // player's look input gets trapped at the pole while the animation plays.
        if (absolutePitch >= VERTICAL_PITCH_START) {
            val animatedEuler = YXZRotationView(animateRot).asEulerAngle()
            val animatedYaw = Mth.RAD_TO_DEG * animatedEuler.y()
            val animatedRoll = Mth.RAD_TO_DEG * animatedEuler.z()
            val positivePitch = event.pitch >= 0f
            event.pitch = Mth.clamp(event.pitch + Mth.RAD_TO_DEG * animatedEuler.x(), -90f, 90f)
            event.roll = if (positivePitch) {
                event.roll - animatedYaw - animatedRoll
            } else {
                event.roll + animatedYaw - animatedRoll
            }
            return
        }

        if (isIdentity(animateRot)) {
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

    private fun isIdentity(rotation: Quaternionf): Boolean {
        return Mth.abs(rotation.x()) < 1e-5f &&
                Mth.abs(rotation.y()) < 1e-5f &&
                Mth.abs(rotation.z()) < 1e-5f &&
                Mth.abs(Mth.abs(rotation.w()) - 1f) < 1e-5f
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
        // 这个入口只会被本地玩家自己的手调用（`FirstPersonRenderHandler` 挂在 `RenderHandEvent` 上），
        // 所以在这里记下当前手，脚本就能把"自己手里那把枪"和世界上的同型号枪区分开。
        localFirstPersonHand = handForContext(transformType)
        try {
            render(stack, transformType, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, partialTick)
        } finally {
            localFirstPersonHand = null
        }
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
        handledScopeAttachment = null
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

            val scopeRender = resolveScopeAttachmentRender(stack, model)
            applyFirstPersonPositioningTransform(poseStack, model, scopeRender, hand)

            val sprintOffset = resource.sprintOffset
            ClientEventHandler.gunRootMoveV2(poseStack, sprintOffset.x, sprintOffset.y, sprintOffset.z, resource.useCustomSprintAnimation)

            val shootRecoil = resource.shootRecoil
            ClientEventHandler.handleShootAnimationV2(
                poseStack,
                shootRecoil.offset.x, shootRecoil.offset.y, shootRecoil.offset.z,
                shootRecoil.rotation.x, shootRecoil.rotation.y, shootRecoil.rotation.z,
                shootRecoil.zoomRate, shootRecoil.speed
            )

            val zoomPivot = computeViewTransform(model, scopeRender, hand)?.let {
                val pivot = Vector3f()
                it.getTranslation(pivot)
                pivot
            }
            if (zoomPivot != null) {
                poseStack.translate(zoomPivot.x, zoomPivot.y, zoomPivot.z)
            }
            val zoomLengthScale = scopeRender?.scopeMode?.zoomLengthScale ?: 0.75f
            // 与定位点混合共用同一条曲线：以前这里直接用线性的 zoomTime，于是推进节奏和枪的位置对不上。
            // 以 scope_ranger / scope_sniper（zoomLengthScale = 0.3）为例，zoomTime = 0.3 时长度已经缩掉
            // 总压缩量的 30%（实际长度的 21%），而枪才刚走完 10.8% 的路程，看上去是先"缩一下"再"抬上来"；
            // 反过来 zoomTime = 0.7 时枪已到位 89.2%，长度却只缩了 70%，收尾阶段长度还在慢慢变。
            val zoom = aimingProgress(ClientEventHandler.zoomTime)
            poseStack.scale(1f, 1f, 1f - (1f - zoomLengthScale) * zoom)
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

        val canStencil = transformType.firstPerson()
                && !OculusCompat.isRenderingShadowPass()
                && bufferSource is MultiBufferSource.BufferSource
        val stencilScope = if (canStencil) findStencilScope(stack, model) else null
        var gunCulled = false
        if (stencilScope != null) {
            handledScopeAttachment = stencilScope.attachmentId
            poseStack.pushPose()
            mulPoseWithNormal(poseStack, stencilScope.slotTransform)
            stencilScope.model.renderWithStencil(
                poseStack,
                bufferSource as MultiBufferSource.BufferSource,
                stencilScope.texture,
                packedLight,
                partialTick,
                stencilScope.scopeMode,
                stencilScope.companionSightMode,
                resolveAmmoReadout(stack, stencilScope)
            )
            poseStack.popPose()

            if (stencilScope.scopeMode.isScope()) {
                ScopeStencilRenderHelper.enableItemEntityStencilTest()
                RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF)
                RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
                gunCulled = true
            }
        }

        renderAttachments(stack, model, transformType, poseStack, bufferSource, packedLight, packedOverlay)
        model.renderToBuffer(
            poseStack, bufferSource, texture, packedLight, packedOverlay,
            resolveGunAmmoReadout(stack, resource)
        )
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
        gunStencilCulling = gunCulled
        finishStencilCulling(bufferSource)
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
        renderProjectileBone(stack, model)
        renderScopeMount(stack, model)
        renderScopeAttachment(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
        renderStock(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
        renderGripHandGuard(stack, model)
        renderGripAttachment(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
        renderOemScope(stack, model)
        renderOemMuzzle(stack, model)
        renderBarrelAttachment(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
        renderRegisteredAttachments(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
    }

    /**
     * 注册表驱动的通用配件渲染。
     *
     * `AttachmentSlots` 里 [AttachmentRenderMode.GENERIC] 的槽位都走这里：按槽位登记的挂点骨骼
     * （约定骨骼，或配件自己的 `AttachmentDefinition.Bone`）把配件的模型画上去。
     * **新增这类槽位不需要再往 [renderAttachments] 里加一行**，只要在注册表登记一条、
     * 在数据里写好 `Model`/`Texture` 即可。
     */
    open fun renderRegisteredAttachments(
        stack: ItemStack,
        model: GeoGunModel,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val data = GunData.from(stack)

        for (slot in AttachmentSlots.ALL) {
            if (slot.renderMode != AttachmentRenderMode.GENERIC) continue

            val attachmentId = data.attachment.id(slot.type) ?: continue
            val definition = AttachmentDefinition.from(attachmentId) ?: continue
            val modelPath = definition.model ?: continue
            val texture = definition.texture ?: continue

            val boneName = when (val mountBone = slot.mountBone) {
                is AttachmentMountBone.Fixed -> mountBone.name
                is AttachmentMountBone.FromDefinition -> definition.bone ?: mountBone.fallback
                AttachmentMountBone.GunModel -> null
            } ?: continue

            val mountTransform = model.getGlobalTransform(boneName) ?: continue
            val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: continue

            poseStack.pushPose()
            mulPoseWithNormal(poseStack, Matrix4f(mountTransform))
            attachmentModel.renderToBuffer(
                poseStack, bufferSource, texture, packedLight, packedOverlay,
                null, resolveAmmoReadout(stack, definition.effectiveAmmoBar(), definition.effectiveTextShow())
            )
            poseStack.popPose()
        }
    }

    open fun renderMagazine(stack: ItemStack, model: GeoGunModel) {
        model.showMagazineBone(resolveMagazineBone(stack))
    }

    /**
     * Shows the model bones of the loaded ammo type and hides the ones belonging to the ammo types
     * that are not selected, so the round drawn in the weapon follows the ammo switch.
     */
    open fun renderProjectileBone(stack: ItemStack, model: GeoGunModel) {
        val data = GunData.from(stack)
        val candidates = data.projectileBoneNames()
        if (candidates.isEmpty()) return

        model.showProjectileBone(data.get(GunProp.PROJECTILE_BONE), candidates)
    }

    open fun renderScopeMount(stack: ItemStack, model: GeoGunModel) {
        val bone = model.getBone(CUSTOM_SCOPE_MOUNT_BONE) ?: return
        val data = GunData.from(stack)
        val definition = data.attachment.id(AttachmentType.SCOPE)
            ?.let { AttachmentDefinition.from(it) }
        bone.visible = definition != null && definition.requiresRail
    }

    open fun renderScopeAttachment(
        stack: ItemStack,
        model: GeoGunModel,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val data = resolveScopeAttachmentRender(stack, model) ?: return
        if (data.attachmentId == handledScopeAttachment) return

        poseStack.pushPose()
        mulPoseWithNormal(poseStack, data.slotTransform)
        data.model.renderToBuffer(
            poseStack,
            bufferSource,
            data.texture,
            packedLight,
            packedOverlay,
            data.companionSightMode,
            resolveAmmoReadout(stack, data)
        )
        poseStack.popPose()
    }

    open fun resolveScopeAttachmentRender(stack: ItemStack, model: GeoGunModel): ScopeRenderData? {
        val data = GunData.from(stack)
        val attachmentId = data.attachment.id(AttachmentType.SCOPE) ?: return null
        val definition = AttachmentDefinition.from(attachmentId) ?: return null
        val scopeInfo = definition.scopeInfo ?: return null
        val scopeModeIndex = data.attachment.scopeMode(AttachmentType.SCOPE)
        val scopeMode = scopeInfo.mode(scopeModeIndex)
        val companionSightMode = if (scopeMode.isScope()) {
            scopeInfo.modes.firstOrNull { it.isSight() }
        } else {
            null
        }
        val modelPath = definition.model ?: return null
        val texture = definition.texture ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        val boneName = definition.bone ?: SCOPE_BONE
        val mountTransform = model.getGlobalTransform(boneName) ?: return null
        val bindMountTransform = model.getBindGlobalTransform(boneName) ?: return null
        return ScopeRenderData(
            attachmentModel, texture, scopeMode, scopeModeIndex, companionSightMode, attachmentId,
            Matrix4f(mountTransform), Matrix4f(bindMountTransform),
            definition.effectiveAmmoBar(), definition.effectiveTextShow()
        )
    }

    /** This frame's ammo readout for the scope described by [data]. */
    protected open fun resolveAmmoReadout(stack: ItemStack, data: ScopeRenderData): AmmoReadout {
        return resolveAmmoReadout(stack, data.ammoBar, data.textShow)
    }

    /**
     * This frame's ammo readout for a model carrying [bars] and [texts]: the remaining magazine ratio
     * used to squash its ammo bar bones, and the round count its text anchors display.
     *
     * Returns an empty readout when nothing is configured, which is what keeps the
     * [com.atsuishio.superbwarfare.data.gun.GunProp.MAGAZINE] lookup — a full property modifier chain
     * resolve, and one that can rebuild the whole property set after every vanilla stack resync — off
     * the render path of every gun and attachment in the game that shows no ammo display. That is the
     * overwhelming majority of them, so the early return has to stay ahead of [GunData.from].
     */
    protected open fun resolveAmmoReadout(
        stack: ItemStack,
        bars: List<AmmoBarEntry>,
        texts: List<AmmoTextEntry>,
    ): AmmoReadout {
        if (bars.isEmpty() && texts.isEmpty()) return AmmoReadout()

        val gun = GunData.from(stack)
        val count = gun.ammo.get()
        val magazine = gun.get(GunProp.MAGAZINE)
        // No usable magazine: the bar holds at full rather than dividing by zero. Guns whose
        // effective count lives outside `ammo` — energy weapons, backpack-ammo guns, melee-only guns,
        // all of which report MAGAZINE <= 0 — therefore read as a full bar showing "0", so an author
        // should not configure a readout on those.
        if (magazine <= 0) return AmmoReadout(bars, texts, 1f, count)
        return AmmoReadout(
            bars,
            texts,
            (count.toFloat() / magazine.toFloat()).coerceIn(0f, 1f),
            count
        )
    }

    /**
     * This frame's ammo readout for the gun body itself, resolved from the assets-side gun resource.
     *
     * Like the attachment path this returns empty before touching [GunData] when the gun declares no
     * ammo display, so a gun with no `AmmoBar` / `TextShow` never pays for a magazine resolve.
     */
    protected open fun resolveGunAmmoReadout(stack: ItemStack, resource: DefaultGunResource): AmmoReadout {
        return resolveAmmoReadout(stack, resource.ammoBar, resource.textShow)
    }

    private fun findStencilScope(stack: ItemStack, model: GeoGunModel): ScopeRenderData? {
        val data = resolveScopeAttachmentRender(stack, model) ?: return null
        if (!data.model.needsStencil(data.scopeMode)) return null
        // Magnified scopes use the same aiming progress as their ocular rendering.
        if (data.scopeMode.isScope() && ClientEventHandler.zoomTime <= SCOPE_STENCIL_START_PROGRESS) return null
        return data
    }

    private fun finishStencilCulling(bufferSource: MultiBufferSource) {
        if (!gunStencilCulling) return
        gunStencilCulling = false

        if (bufferSource is MultiBufferSource.BufferSource) {
            if (!OculusCompat.endBatch(bufferSource)) {
                bufferSource.endBatch()
            }
        }

        ScopeStencilRenderHelper.disableItemEntityStencilTest()
        RenderSystem.clearStencil(0)
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX)
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

        if (definition.requiresAdapter) {
            model.showStockBone(
                GeoGunModel.CUSTOM_STOCK_ADAPTER_BONE,
                GeoGunModel.CUSTOM_STOCK_ADAPTER_BONE
            )
        } else {
            model.hideAllStockBones()
        }
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
        val (attachmentModel, texture, definition) = resolveStockAttachmentRender(stack) ?: return
        val mountTransform = model.getGlobalTransform(GeoGunModel.CUSTOM_STOCK_ADAPTER_BONE) ?: return

        poseStack.pushPose()
        mulPoseWithNormal(poseStack, Matrix4f(mountTransform))
        attachmentModel.renderToBuffer(
            poseStack, bufferSource, texture, packedLight, packedOverlay,
            null, resolveAmmoReadout(stack, definition.effectiveAmmoBar(), definition.effectiveTextShow())
        )
        poseStack.popPose()
    }

    open fun resolveStockAttachmentRender(stack: ItemStack): AttachmentRenderData? {
        val data = GunData.from(stack)
        val attachmentId = data.attachment.id(AttachmentType.STOCK) ?: return null
        val definition = AttachmentDefinition.from(attachmentId) ?: return null
        if (definition.usesGunStock) return null
        val modelPath = definition.model ?: return null
        val texture = definition.texture ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        return AttachmentRenderData(attachmentModel, texture, definition)
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
        val (attachmentModel, texture, definition) = resolveGripAttachmentRender(stack) ?: return
        val boneName = resolveGripAttachmentBone(stack)
        val mountTransform = model.getGlobalTransform(boneName) ?: return

        poseStack.pushPose()
        mulPoseWithNormal(
            poseStack,
            Matrix4f(mountTransform).mul(resolveBarrelAttachmentLocalTransform(stack))
        )
        attachmentModel.renderToBuffer(
            poseStack, bufferSource, texture, packedLight, packedOverlay,
            null, resolveAmmoReadout(stack, definition.effectiveAmmoBar(), definition.effectiveTextShow())
        )
        poseStack.popPose()
    }

    open fun resolveGripAttachmentRender(stack: ItemStack): AttachmentRenderData? {
        val data = GunData.from(stack)
        val attachmentId = data.attachment.id(AttachmentType.GRIP) ?: return null
        val definition = AttachmentDefinition.from(attachmentId) ?: return null
        val modelPath = definition.model ?: return null
        val texture = definition.texture ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        return AttachmentRenderData(attachmentModel, texture, definition)
    }

    open fun resolveGripAttachmentBone(stack: ItemStack): String {
        return GRIP_BONE
    }

    open fun renderOemMuzzle(stack: ItemStack, model: GeoGunModel) {
        val bone = model.getBone(OEM_MUZZLE_BONE) ?: return
        val data = GunData.from(stack)
        val hasBarrelAttachment = data.attachment.id(AttachmentType.BARREL) != null
                || data.attachment.get(AttachmentType.BARREL) != 0
        bone.visible = !hasBarrelAttachment
    }

    open fun renderOemScope(stack: ItemStack, model: GeoGunModel) {
        val bone = model.getBone(OEM_SCOPE_BONE) ?: return
        val data = GunData.from(stack)
        val hasScope = data.attachment.id(AttachmentType.SCOPE) != null
                || data.attachment.get(AttachmentType.SCOPE) != 0
        bone.visible = !hasScope
    }

    open fun renderBarrelAttachment(
        stack: ItemStack,
        model: GeoGunModel,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val (attachmentModel, texture, definition) = resolveBarrelAttachmentRender(stack) ?: return
        val boneName = resolveBarrelAttachmentBone(stack) ?: return
        val mountTransform = model.getGlobalTransform(boneName) ?: return

        poseStack.pushPose()
        mulPoseWithNormal(poseStack, Matrix4f(mountTransform))
        attachmentModel.renderToBuffer(
            poseStack, bufferSource, texture, packedLight, packedOverlay,
            null, resolveAmmoReadout(stack, definition.effectiveAmmoBar(), definition.effectiveTextShow())
        )
        poseStack.popPose()
    }

    open fun resolveBarrelAttachmentRender(stack: ItemStack): AttachmentRenderData? {
        val data = GunData.from(stack)
        val attachmentId = data.attachment.id(AttachmentType.BARREL) ?: return null
        val definition = AttachmentDefinition.from(attachmentId) ?: return null
        val modelPath = definition.model ?: return null
        val texture = definition.texture ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        return AttachmentRenderData(attachmentModel, texture, definition)
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

    open fun resolveBarrelAttachmentLocalTransform(stack: ItemStack): Matrix4f {
        val data = GunData.from(stack)
        val offset = data.attachment.getOffset(AttachmentType.BARREL)
        val rotation = data.attachment.getRotation(AttachmentType.BARREL).toFloat()
        return Matrix4f()
            .translate(0f, 0f, offset.toFloat())
            .rotateZ(Mth.DEG_TO_RAD * rotation)
    }

    open fun resolveBarrelAttachmentMuzzleTransform(
        stack: ItemStack,
        model: GeoGunModel,
        renderData: AttachmentRenderData
    ): Matrix4f? {
        val attachmentMuzzle = renderData.model.getGlobalTransform(MUZZLE_BONE) ?: return null
        val boneName = resolveBarrelAttachmentBone(stack) ?: return null
        val mountTransform = model.getGlobalTransform(boneName) ?: return null
        return Matrix4f(mountTransform)
            .mul(resolveBarrelAttachmentLocalTransform(stack))
            .mul(attachmentMuzzle)
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

    open fun scriptHasScope(stack: ItemStack): Boolean {
        val data = GunData.from(stack)
        return data.attachment.id(AttachmentType.SCOPE) != null
                || data.attachment.get(AttachmentType.SCOPE) != 0
    }

    /**
     * 脚架展开进度：0 为收起，1 为完全展开。
     *
     * 直接复用 `bipod_view` 定位点用的 [ClientEventHandler.bipodViewTime]，这样子骨骼的翻转与
     * 卧姿视角过渡天然同步，脚本里不需要自己再做一次插值。
     *
     * 但它描述的是**本地玩家自己**的持枪视角过渡，是客户端全局的一份状态，所以只有他手里那把枪
     * 能用它。掉落在地上的、摆在展示框里的、别人手里的枪都拿不到"持有者"来问是否趴着
     * （物品渲染路径只有 [ItemStack]，没有实体），对它们一律返回 0，也就是保持收起——
     * 否则世界上每一把同型号枪都会跟着本地玩家的卧姿一起展开。
     *
     * 判定**不能只看 ItemStack 对象身份**。第三人称、掉落物、展示框、别人手里的枪确实是别的对象，
     * 但第一人称不是：`FirstPersonRenderHandler` 渲染时传下来的是动画实例持有的那份 stack
     * （`GeoGunAnimationInstance.currentItem()`），而它每个客户端 tick 才由 `updateItem` 刷新一次，
     * 换枪过渡期间渲染的更是上一个实例里的旧对象。服务端每次同步手持槽（开枪改弹药、热量、
     * 各种计时器）都会把客户端手上的 ItemStack **换成新对象**（见 `GunData.DATA_CACHE` 的注释），
     * 于是同步之后到下一次 `updateItem` 之间的那几帧里身份对不上，脚本会以为枪不在手上，
     * 脚架被压回 bind 姿态、下一帧又展开——第一人称看到的就是脚架在收起/展开之间来回横跳。
     * [GeoGunAnimationInstance.shouldSpin] 早就为同一个坑改成比物品类型了。
     *
     * 所以这里分两种情况：对象身份成立（第三人称与正常的第一人称帧）直接用；
     * 第一人称下额外接受"渲染的是本地玩家主手 + 同一种物品"。第一人称入口只会画本地玩家自己的手，
     * 所以这个放宽不会波及世界上的同型号枪——掉落物/展示框/别人手里走的是普通物品渲染，
     * [localFirstPersonHand] 为 `null`，仍然一律返回 0。
     *
     * 换枪动画期间渲染的是旧 stack：换了另一种枪时物品对不上、脚架直接收起而不是平滑过渡，
     * 与之前的行为一致，可以接受。
     */
    open fun scriptBipodProgress(stack: ItemStack): Double {
        val player = Minecraft.getInstance().player ?: return 0.0
        val held = player.mainHandItem
        val mine = held === stack
                || (localFirstPersonHand == InteractionHand.MAIN_HAND
                && !held.isEmpty
                && held.item === stack.item)
        return if (mine) ClientEventHandler.bipodViewTime else 0.0
    }

    /**
     * 枪管热量：0 为空，100 为过热阈值（与 `HeatBarOverlay` 的 `heat / 100` 同一刻度）。
     *
     * 和 [scriptBipodProgress] 不同，这里**不**按对象身份限制：热量写在枪自己的 tag 里，是逐物品的属性，
     * 别人手里的、地上躺着的枪读到的都是它自己的热量，而不是本地玩家的。代价是热量只在持有者的 tick 里
     * 自然冷却（`GunEventHandler.reduceHeat`），一把打到过热再丢在地上的枪会一直保持那个热度。
     */
    open fun scriptHeat(stack: ItemStack): Double {
        return GunData.from(stack).heat.get()
    }

    open fun scriptFrameDeltaSeconds(): Float {
        return Minecraft.getInstance().deltaFrameTime.coerceIn(0f, 0.8f)
    }

    open fun applyCustomAnimationsByScript(
        stack: ItemStack,
        model: GeoGunModel,
        transformType: ItemDisplayContext,
        partialTick: Float
    ) {
        val script = GunResource.getDefault(stack).getScript() ?: return
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
        val item = stack.item as? GunItem ?: return
        val player = localPlayer ?: return
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

        val data = from(stack)
        // 与定位点混合、Z 轴长度压缩共用同一条曲线。以前这里读的是线性的 zoomTime，于是姿态收敛跑在
        // 枪到位之前：中段枪身的摆动已经被压平了，位置却还没跟上，衔接处会看出"甩一下"。
        // 卧姿架在脚架上也要求收敛，所以与 bipodViewTime 取较大者；缓动单调，先取 max 再缓动与
        // 各自缓动后取 max 等价。
        val zoomTime = aimingProgress(
            ClientEventHandler.zoomTime.coerceAtLeast(ClientEventHandler.bipodViewTime)
        )

        var rotationScale = (1f - 0.5f * zoomTime).coerceAtLeast(0.05f)
        var rotationScaleX = (1f - 0.97f * zoomTime).coerceAtLeast(0.05f)
        var rotationScaleY = (1f - 0.97f * zoomTime).coerceAtLeast(0.05f)
        var rotationScaleZ = (1f - 0.7f * zoomTime).coerceAtLeast(0.05f)
        var positionScale = (1f - 0.95f * zoomTime).coerceAtLeast(0.05f)
        var positionScaleX = (1f - 0.95f * zoomTime).coerceAtLeast(0.05f)
        var positionScaleZ = (1f - 0.96f * zoomTime).coerceAtLeast(0.05f)

        if (!data.reloading()) {
            rotationScale = (1f - 0.5f * zoomTime).coerceAtLeast(0.05f)
            rotationScaleX = (1f - 0.55f * zoomTime).coerceAtLeast(0.05f)
            rotationScaleY = (1f - 0.2f * zoomTime).coerceAtLeast(0.05f)
            rotationScaleZ = (1f - 0.2f * zoomTime).coerceAtLeast(0.05f)
            positionScale = (1f - 0.4f * zoomTime).coerceAtLeast(0.05f)
            positionScaleX = (1f - 0.5f * zoomTime).coerceAtLeast(0.05f)
            positionScaleZ = (1f - 0.82f * zoomTime).coerceAtLeast(0.05f)
        }

        val main = model.getRootBone()
        main?.let { bone ->
            val boneEuler = Vector3f(bone.rotationInEuler).mul(rotationScaleX, rotationScaleY, rotationScaleZ)
            bone.rotation.set(Quaternionf().rotateZYX(boneEuler.z, boneEuler.y, boneEuler.x))
            bone.rotationInEuler.set(boneEuler)
            bone.x *= positionScale
            bone.y *= positionScaleX
            bone.z *= positionScaleZ
        }

        val cameraEuler = Vector3f(camera.rotationInEuler).mul(rotationScale).mul(-strength)
        animation.cameraRotation = Quaternionf().rotateZYX(cameraEuler.z, cameraEuler.y, cameraEuler.x)
    }

    open fun applyFirstPersonPositioningTransform(
        poseStack: PoseStack,
        model: GeoGunModel,
        scopeRender: ScopeRenderData? = null,
        hand: InteractionHand = InteractionHand.MAIN_HAND
    ) {
        val viewTransform = computeViewTransform(model, scopeRender, hand) ?: return
        mulPoseWithNormal(poseStack, viewTransform.invert())
    }

    /**
     * 把 0..1 的瞄准进度换算成真正用于插值的值。
     *
     * **瞄准过渡里所有按进度推进的量都必须走这里**，否则同一个过渡的不同部分会以不同速度推进：
     * [computeViewTransform] 的定位点混合（平移与旋转）、`renderModel` 里的 Z 轴长度压缩、
     * [applyCameraShake] 的姿态收敛，三者只要有一条用了线性的 `zoomTime`，中段就会看出
     * "长度先缩掉一截 / 姿态先甩到位，枪却还没进来"。
     *
     * [AnimationCurves.EASE_IN_OUT_QUINT] 是单调的，所以对若干个驱动量先取较大者再缓动，
     * 与各自缓动后取较大者等价——[applyCameraShake] 里脚架进度与瞄准进度取 max 就依赖这一点。
     *
     * 两个端点固定（0 → 0、1 → 1），所以换成它不会改变"完全未瞄准"和"完全瞄准"两帧的样子，
     * 只改变中间的推进节奏。
     */
    private fun aimingProgress(rawProgress: Double): Float =
        AnimationCurves.EASE_IN_OUT_QUINT.apply(rawProgress.coerceIn(0.0, 1.0)).toFloat()

    open fun computeViewTransform(
        model: GeoGunModel,
        scopeRender: ScopeRenderData? = null,
        hand: InteractionHand = InteractionHand.MAIN_HAND
    ): Matrix4f? {
        val idleViewTransform = model.getGlobalTransform(IDLE_VIEW_BONE) ?: return null
        val hipViewTransform = bipodViewTransform(model, idleViewTransform)

        val zoom = aimingProgress(ClientEventHandler.zoomTime)

        val focusOffset = ClientEventHandler.editFocusOffset
        if (focusOffset.lengthSquared() > 1e-8f && zoom <= 0f) {
            // 以 hipViewTransform 为基准，保证与未聚焦时的返回值连续（脚架视图退场过程中不会跳变）
            val basePos = Vector3f()
            hipViewTransform.getTranslation(basePos)
            val translation = Vector3f(basePos).add(focusOffset)
            var rotation = Quaternionf()
            hipViewTransform.getNormalizedRotation(rotation)
            val yaw = ClientEventHandler.editFocusYaw
            val pitch = ClientEventHandler.editFocusPitch
            if (Mth.abs(pitch) > 1e-5f || Mth.abs(yaw) > 1e-5f) {
                rotation = Quaternionf().rotateY(yaw).rotateX(pitch).mul(rotation)
            }
            val scale = Vector3f()
            hipViewTransform.getScale(scale)
            return Matrix4f()
                .translation(translation)
                .rotate(rotation)
                .scale(scale)
        }

        if (zoom <= 0f) {
            return hipViewTransform
        }
        val ironViewTransform = scopeViewTransform(scopeRender, hand)
            ?: model.getGlobalTransform(IRON_VIEW_BONE)
            ?: return hipViewTransform
        return blendViewTransform(hipViewTransform, Matrix4f(ironViewTransform), zoom)
    }

    /**
     * 非瞄准视角的脚架视图：脚架功能启用（卧姿 + 枪械或配件带脚架）时，
     * 以 [ClientEventHandler.bipodViewTime] 为进度把 `idle_view` 平滑过渡到模型自带的 `bipod_view`。
     * 模型没有 `bipod_view` 定位点或进度为 0 时保持 `idle_view`。
     */
    private fun bipodViewTransform(model: GeoGunModel, idleViewTransform: Matrix4f): Matrix4f {
        val progress = ClientEventHandler.bipodViewTime
        if (progress <= 0.0) return Matrix4f(idleViewTransform)

        val bipodViewTransform = model.getGlobalTransform(BIPOD_VIEW_BONE)
            ?: return Matrix4f(idleViewTransform)
        if (progress >= 1.0) return Matrix4f(bipodViewTransform)

        val blend = AnimationCurves.EASE_IN_OUT_QUINT
            .apply(progress.coerceIn(0.0, 1.0))
            .toFloat()
        return blendViewTransform(Matrix4f(idleViewTransform), Matrix4f(bipodViewTransform), blend)
    }

    private fun scopeViewTransform(
        scopeRender: ScopeRenderData?,
        hand: InteractionHand
    ): Matrix4f? {
        if (scopeRender == null) return null
        val scopeView = scopeRender.model.getGlobalTransform(scopeRender.scopeMode.viewBone())
            ?: scopeRender.model.getGlobalTransform(SCOPE_VIEW_BONE)
            ?: return null
        val target = Matrix4f(scopeRender.bindSlotTransform).mul(scopeView)
        return smoothScopeView(scopeRender, hand, target)
    }

    private fun smoothScopeView(
        scopeRender: ScopeRenderData,
        hand: InteractionHand,
        target: Matrix4f
    ): Matrix4f {
        val state = scopeViewSmoothing.getOrPut(hand) { ScopeViewSmoothState() }
        if (state.modeIndex != scopeRender.scopeModeIndex) {
            if (state.modeIndex >= 0) {
                state.source = Matrix4f(state.current)
                state.progress = 0.0f
            } else {
                state.source = Matrix4f(target)
                state.progress = 1.0f
            }
            state.target = Matrix4f(target)
            state.modeIndex = scopeRender.scopeModeIndex
        } else {
            state.target = Matrix4f(target)
        }

        if (state.progress < 1.0f) {
            val delta = Minecraft.getInstance().deltaFrameTime.coerceAtMost(0.08f)
            // 指数缓动，与 onFovUpdate 中倍率的 customZoom = Mth.lerp(0.6 * delta, ...) 保持一致，
            // 使主副镜切换时枪械瞄准点与 FOV 倍率以相同速率过渡。
            state.progress = Mth.lerp(SCOPE_VIEW_SMOOTHING * delta, state.progress, 1f)
            if (state.progress >= 0.999f) {
                state.progress = 1f
            }
            state.current = blendViewTransform(state.source, state.target, state.progress)
        } else {
            state.current = Matrix4f(state.target)
        }
        return Matrix4f(state.current)
    }

    /**
     * 返回当前正在编辑的配件槽位对应的定位骨骼名；未支持或未选中时返回 null。
     *
     * 瞄准镜/枪口/握把/枪托/弹匣的定位骨骼都由 `AttachmentSlots` 登记；
     * 弹药类型不是槽位，另行处理。
     *
     * [model] 是正在渲染的模型：弹药槽位有多个候选骨骼，需要靠它挑出模型实际拥有的那个。
     */
    open fun attachmentFocusBone(model: GeoGunModel): String? {
        return when (val target = AttachmentSlots.EDIT_ORDER.getOrNull(ClientEventHandler.editingAttachmentType)) {
            is AttachmentEditTarget.Slot -> target.slot.focusBone
            AttachmentEditTarget.AmmoType -> ammoFocusBone(model)
            null -> null
        }
    }

    /**
     * 弹药槽位的定位骨骼：模型自带 `ammo_pos`（如 m_79）时聚焦到它，否则沿用弹匣的定位骨骼。
     */
    private fun ammoFocusBone(model: GeoGunModel): String {
        return if (model.getIndex(AMMO_BONE) >= 0) AMMO_BONE else MAGAZINE_BONE
    }

    /**
     * 每帧将 [com.atsuishio.superbwarfare.event.ClientEventHandler.editFocusOffset] 向
     * 改装聚焦目标偏移平滑插值，实现槽位切换时的缓动过渡。
     */
    private fun updateEditFocus(model: GeoGunModel) {
        val desired = computeEditFocusOffset(model) ?: Vector3f()
        val desiredYaw = computeEditFocusYaw(model)
        val desiredPitch = computeEditFocusPitch(model)
        val delta = Minecraft.getInstance().deltaFrameTime.coerceAtMost(0.5f)
        val focusing = attachmentFocusBone(model) != null
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
        val boneName = attachmentFocusBone(model) ?: return computeUnfocusedPanOffset()

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
    private fun computeEditFocusYaw(model: GeoGunModel): Float {
        if (!ClientEventHandler.isEditing || attachmentFocusBone(model) != null) return 0f

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
    private fun computeEditFocusPitch(model: GeoGunModel): Float {
        if (!ClientEventHandler.isEditing || attachmentFocusBone(model) != null) return 0f

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
        mulPoseWithNormal(poseStack, Matrix4f(transform).invert())
    }

    fun mulPoseWithNormal(poseStack: PoseStack, matrix: Matrix4f) {
        // PoseStack.mulPoseMatrix only updates pose; SBM geometry also consumes the normal matrix.
        val normal = Matrix3f(matrix).invert().transpose()
        poseStack.last().normal().mul(normal)
        poseStack.last().pose().mul(matrix)
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
        private const val BIPOD_VIEW_BONE = "bipod_view"
        private const val IRON_VIEW_BONE = "iron_view"

        // 槽位相关的定位骨骼名统一登记在 AttachmentSlots.Bones，这里只是给渲染代码用的短别名
        private const val MUZZLE_BONE = AttachmentSlots.Bones.MUZZLE
        private const val GRIP_BONE = AttachmentSlots.Bones.GRIP
        private const val MAGAZINE_BONE = AttachmentSlots.Bones.MAGAZINE
        private const val SCOPE_BONE = AttachmentSlots.Bones.SCOPE
        private const val STOCK_BONE = AttachmentSlots.Bones.STOCK
        private const val AMMO_BONE = "ammo_pos"
        private const val SCOPE_VIEW_BONE = "scope_view"
        private const val SCOPE_VIEW_SMOOTHING = 0.6f
        private const val THIRDPERSON_HAND_BONE = "thirdperson_hand"
        private const val GROUND_BONE = "ground"
        private const val FIXED_BONE = "fixed"
        private const val FLARE_BONE = "flare"
        private const val MUZZLE_FLASH_BONE = "muzzle_flash"
        private const val CUSTOM_HAND_GUARD_BONE = "custom_hand_guard"
        private const val OEM_HAND_GUARD_BONE = "oem_hand_guard"
        private const val OEM_MUZZLE_BONE = "oem_muzzle"
        private const val OEM_SCOPE_BONE = "oem_scope"
        private const val CUSTOM_SCOPE_MOUNT_BONE = "custom_scope_mount"

        private const val SCOPE_STENCIL_START_PROGRESS = 0.2
        private const val EDIT_FOCUS_Z_OFFSET = 0.8f
        private const val EDIT_FOCUS_SMOOTHING = 1f
        private const val EDIT_FOCUS_RETURN_SMOOTHING = 0.8f
        private const val EDIT_FOCUS_RETURN_TIME = 0.6f
        private const val UNFOCUSED_PAN_RANGE = 0.13f
        private const val UNFOCUSED_PAN_SMOOTHING = 12f
        private const val UNFOCUSED_PAN_YAW = 0.6f
        private const val UNFOCUSED_PAN_PITCH = 0.3f
        private const val VERTICAL_PITCH_START = 89.0f

        private val BLENDER: EulerAdditiveBlender =
            SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }
}
