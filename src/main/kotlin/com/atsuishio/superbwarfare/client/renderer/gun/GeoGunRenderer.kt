package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.gun.GeoGunAnimationInstance
import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoReadout
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.EDIT_FOCUS_Z_OFFSET
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.MERGE_BLENDER
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.subWeaponHasOwnAimPose
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeStencilRenderHelper
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
import com.atsuishio.superbwarfare.tools.ActiveGun
import com.atsuishio.superbwarfare.tools.RenderDistanceHelper
import com.atsuishio.superbwarfare.tools.deltaFrameTime
import com.atsuishio.superbwarfare.tools.localPlayer
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.firstperson.FirstPersonParticleSystem
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.CameraStateCache
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.renderer.AbstractGeoItemRendererV2
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState
import com.maydaymemory.mae.basic.*
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.NoAllocMergeBlender
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
import net.neoforged.neoforge.client.event.ViewportEvent
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
        entity: Entity?,
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

    /**
     * 本帧的副武器"跟随运动"结果（[resolveSubWeaponFollowPose]），由 `renderModel` 算一次、
     * `renderRegisteredAttachments` 复用。
     *
     * 必须是**每次 renderModel 开头清空**的：它绑定了"这一帧用的是哪个主武器模型/哪只手"，
     * 跨帧留着会把上一帧的修正套到这一帧（两只手、或者第三人称那一遍）上。
     */
    private var subWeaponFollow: SubWeaponFollowPose? = null

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
        subWeaponFollow = null
        if (transformType.firstPerson()) {
            val hand = handForContext(transformType)
            val pose = FirstPersonRenderHandler.getActiveAnimationInstance(hand)?.cachedPose
            // 副武器换弹：让**主武器（含玩家手臂）**跟着副武器动画的 `root` 运动走（§9.8.7）。
            // ⚠ 必须在 `applyPose` **之前**算：它会直接改写主武器 `root` 那根骨骼。
            val follow = resolveSubWeaponFollowPose(stack, model)
            subWeaponFollow = follow

            // 把"只含主武器 root"的修正盖到动画姿态上；没有修正时逐字走原来的路径
            val renderPose = when {
                pose == null -> follow?.gunRoot
                follow == null -> pose
                else -> MERGE_BLENDER.blend(listOf(pose, follow.gunRoot))
            }
            if (renderPose != null) {
                model.applyPose(BLENDER.blend(model.getBindPose(), renderPose))
            }

            applyCameraShake(stack, model, hand)

            updateEditFocus(model)

            val scopeRender = resolveScopeAttachmentRender(stack, model)
            applyFirstPersonPositioningTransform(poseStack, model, stack, scopeRender, hand)

            val sprintOffset = resource.sprintOffset
            ClientEventHandler.gunRootMoveV2(
                poseStack,
                sprintOffset.x,
                sprintOffset.y,
                sprintOffset.z,
                resource.useCustomSprintAnimation
            )

            val shootRecoil = resource.shootRecoil
            ClientEventHandler.handleShootAnimationV2(
                poseStack,
                shootRecoil.offset.x, shootRecoil.offset.y, shootRecoil.offset.z,
                shootRecoil.rotation.x, shootRecoil.rotation.y, shootRecoil.rotation.z,
                shootRecoil.zoomRate, shootRecoil.speed
            )

            val zoomPivot = computeViewTransform(model, stack, scopeRender, hand)?.let {
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
//                && !OculusCompat.isRenderingShadowPass()
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
            val hand = handForContext(transformType)
            val animation = FirstPersonRenderHandler.getActiveAnimationInstance(hand) as? GeoGunAnimationInstance

            // 副武器开火期间（四期：**部署中**，或副武器那一支开火动画正在播）才解析它的枪口骨骼。
            // 判据从三期的"枪口焰窗口 + 是否播了副武器专属 clip"简化成一条：**谁被切出来，火就归谁**
            // （§11.10.5-⑦）—— 更简单也更准，不会出现"部署着、但这一发播的是宿主 `fire`，
            // 于是火喷在枪管上"的错位。
            val subWeaponFire = ActiveGun.isDeployed(from(stack), true) ||
                    ClientEventHandler.subWeaponFireRotTimer > 0.0 ||
                    animation?.isSubWeaponFire() == true
            val subWeaponFlare = if (subWeaponFire) resolveSubWeaponFlareTransform(stack, model) else null
            val subWeaponFlashScale = if (subWeaponFire) resolveSubWeaponMuzzleFlashScale(stack) else 1.0f

            MuzzleFlashRenderer.render(
                poseStack,
                model,
                stack,
                bufferSource,
                attachmentMuzzleTransform,
                muzzleFlashScale,
                subWeaponFlare,
                subWeaponFlashScale
            )

            ShellCasingFxRenderer.render(poseStack, model, stack, hand, bufferSource, packedLight)

            val transforms = lastBoneTransforms.getOrPut(hand) { mutableMapOf() }
            // 副武器开火时，动画里的枪口定位点（`flare`）改挂副武器模型自己的枪口，
            // 而且**不注册**枪口配件的 `MUZZLE_BONE` —— `resolveMuzzleLocator` 优先取它，
            // 留着会把榴弹的枪口烟吸到枪管前端去（同时装了消音器时尤其明显）。
            val subWeaponMuzzle = subWeaponFlare?.takeIf { animation?.isSubWeaponFire() == true }
            if (subWeaponMuzzle != null) {
                transforms[FLARE_BONE] = Matrix4f(subWeaponMuzzle)
            } else {
                for (boneName in listOf(FLARE_BONE, MUZZLE_FLASH_BONE)) {
                    model.getGlobalTransform(boneName)?.let { transforms[boneName] = Matrix4f(it) }
                }
                attachmentMuzzleTransform?.let { transforms[MUZZLE_BONE] = Matrix4f(it) }
            }
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
        renderRegisteredAttachments(
            stack,
            model,
            poseStack,
            bufferSource,
            packedLight,
            packedOverlay,
            handForContext(transformType)
        )
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
        packedOverlay: Int,
        hand: InteractionHand = InteractionHand.MAIN_HAND
    ) {
        val data = from(stack)

        for (slot in AttachmentSlots.ALL) {
            if (slot.renderMode != AttachmentRenderMode.GENERIC) continue

            val attachmentId = data.attachment.id(slot.type) ?: continue
            val definition = AttachmentDefinition.from(attachmentId) ?: continue
            val modelPath = definition.model ?: continue
            val texture = definition.texture ?: continue

            val boneName = AttachmentSlots.mountBoneOf(slot, definition) ?: continue

            val mountTransform = model.getGlobalTransform(boneName) ?: continue
            val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: continue

            // 四期：副武器**自己**的换弹动画（§9.8.7）。它由宿主的动画实例推进
            // （附件模型实例是全局共享的，状态不能挂在它身上），这里只负责取姿态、应用、复位。
            //
            // ⚠ **注意挂点变换与套姿态的先后**：挂点变换取自**宿主枪**模型（不含附件姿态），
            // 姿态是在挂点矩阵**之内**、按**附件模型自己的骨骼**应用的，两者不会互相污染。
            // 附件姿态是"绝对局部姿态"（`BoneTreeInstance.applyPose` 直接写 `BoneState`，不做混合），
            // 所以 keyframe 必须写**该骨骼的绑定值**：几何上"位置 0 / 旋转 0"等价于停在 pivot、
            // 保持绑定旋转，**但前提是那根骨骼的绑定旋转本来就是 0** —— 一旦给一根带绑定旋转的骨骼
            // （比如本附件模型的 `iron_view`，绑定旋转 `[4.5, 0, 8.5]`）写了 `0`，它就会被从绑定
            // 姿势上推开。那正是"动画一播就错位"的成因。
            // 没被 key 的骨骼保持绑定姿势；`resetPose()` 在 finally 里还原，漏了会串到别的枪上。
            //
            // ⚠⚠ **副武器换弹时这里用的是 [subWeaponFollow] 里那份"抵消过"的姿态**，不是原始姿态：
            // 主武器的 root 已经被 `D` 顶走了（见 [resolveSubWeaponFollowPose]），附件这一侧必须
            // 左乘 `D⁻¹` 才能继续贴在挂点上，否则下挂筒会被推离枪身两倍的距离。
            val subWeaponPose = if (slot.type == AttachmentType.SUBWEAPON) {
                subWeaponFollow?.takeIf { it.modelPath == modelPath }?.attachmentPose
                    ?: (FirstPersonRenderHandler.getActiveAnimationInstance(hand) as? GeoGunAnimationInstance)
                        ?.subWeaponReloadPose()
            } else {
                null
            }

            poseStack.pushPose()
            mulPoseWithNormal(poseStack, Matrix4f(mountTransform))
            try {
                if (subWeaponPose != null) attachmentModel.applyPose(subWeaponPose)
                attachmentModel.renderToBuffer(
                    poseStack, bufferSource, texture, packedLight, packedOverlay,
                    null, resolveAmmoReadout(stack, definition.effectiveAmmoBar(), definition.effectiveTextShow())
                )
            } finally {
                if (subWeaponPose != null) attachmentModel.resetPose()
            }
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
        val data = from(stack)
        val candidates = data.projectileBoneNames()
        if (candidates.isEmpty()) return

        model.showProjectileBone(data.get(GunProp.PROJECTILE_BONE), candidates)
    }

    open fun renderScopeMount(stack: ItemStack, model: GeoGunModel) {
        val bone = model.getBone(CUSTOM_SCOPE_MOUNT_BONE) ?: return
        val data = from(stack)
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
        val data = from(stack)
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

        val gun = from(stack)
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
//            if (!OculusCompat.endBatch(bufferSource)) {
                bufferSource.endBatch()
//            }
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
        val data = from(stack)
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
        val data = from(stack)
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
        val gripInstalled = from(stack).attachment.has(AttachmentType.GRIP)
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
        val data = from(stack)
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
        val data = from(stack)
        val hasBarrelAttachment = data.attachment.id(AttachmentType.BARREL) != null
                || data.attachment.get(AttachmentType.BARREL) != 0
        bone.visible = !hasBarrelAttachment
    }

    open fun renderOemScope(stack: ItemStack, model: GeoGunModel) {
        val bone = model.getBone(OEM_SCOPE_BONE) ?: return
        val data = from(stack)
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
        val data = from(stack)
        val attachmentId = data.attachment.id(AttachmentType.BARREL) ?: return null
        val definition = AttachmentDefinition.from(attachmentId) ?: return null
        val modelPath = definition.model ?: return null
        val texture = definition.texture ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        return AttachmentRenderData(attachmentModel, texture, definition)
    }

    open fun resolveBarrelAttachmentMuzzleFlashScale(stack: ItemStack): Float {
        val data = from(stack)
        val attachmentId = data.attachment.id(AttachmentType.BARREL) ?: return 1.0f
        return AttachmentDefinition.from(attachmentId)?.muzzleFlashScale?.coerceAtLeast(0f) ?: 1.0f
    }

    open fun resolveBarrelAttachmentBone(stack: ItemStack): String? {
        val data = from(stack)
        val attachmentId = data.attachment.id(AttachmentType.BARREL) ?: return null
        return AttachmentDefinition.from(attachmentId)?.bone
    }

    open fun resolveBarrelAttachmentLocalTransform(stack: ItemStack): Matrix4f {
        val data = from(stack)
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

    /**
     * 【副武器换弹：让**主武器**跟着副武器动】把副武器动画的 `root` 运动"反推"到主武器上。
     *
     * ## 要解决的问题
     *
     * 副武器是挂在主武器骨骼上的**子节点**，正常情况下它只能"在主武器身上动"。
     * 而美术做的换弹动画（原手持形态那把枪的 `animation.gp_25.reload`）里，
     * **整把武器的位移与摇晃写在 `root` 通道上** —— 那是"这把枪在手里怎么动"。
     * 直接放到附件模型上播，就变成"下挂榴弹自己在枪身上甩来甩去"，而主武器纹丝不动，
     * 手（由**主武器**模型渲染，见 `GeoGunModel.renderHands`）也不会跟。
     *
     * ## 做法（不需要改动画文件）
     *
     * 设挂点在主武器骨骼空间里的变换为 `M`（bind pose 下算，稳定、不受姿态影响），
     * 副武器动画的 `root` 通道为 `A`。那么真正该动的是这个量：
     *
     * ```
     * D = M · A · M⁻¹        ← 把"附件空间里的整体运动"换算成"主武器空间里的整体运动"
     * ```
     *
     * 于是：
     * - **主武器**的 `root` 左乘 `D` → 整枪（含手臂、其它配件、枪口焰）一起跟着动；
     * - **副武器**的 `root` 左乘 `D⁻¹` → 抵消掉子节点侧的同一份位移，
     *   让它**仍然死死贴在挂点上**（`A` 里除 `root` 以外的通道 —— 炮管、扳机、榴弹 —— 照常播）。
     *
     * 两者相乘后在挂点处精确抵消，所以"下挂筒不会从枪身上脱开"，同时"枪和手跟着动画走"。
     * 推导（挂点世界变换记为 `W`，主武器原姿态的 root 记为 `G`）：
     *
     * ```
     * 现在：  attach = W · A             gun = G
     * 目标：  attach = W · A（不动）      gun = M·A·M⁻¹ · G
     * 实现：  gun' = D·G                 attach' = (D·G)·M_bind·(D⁻¹·A)
     *        = D·G·M_bind·D⁻¹·A = (D·M_bind·D⁻¹)·(G·M_bind⁻¹)·(M_bind·A) = W·A  ✅
     * ```
     *
     * ## 边界
     *
     * - 只在"副武器被切出来 + 正在换弹"时生效（[GeoGunAnimationInstance.subWeaponReloadPose] 非空）；
     * - `A` 是单位阵时 `D` 也是单位阵 → **完全无副作用**，绝大部分帧走的就是这条；
     * - 挂点骨骼 / `root` 骨骼 / 附件模型任一解析不到 → 返回 `null`，退回原有渲染；
     * - 返回的 [SubWeaponFollowPose.gunRoot] 由调用方 merge 到主武器姿态上，
     *   [SubWeaponFollowPose.attachmentPose] 则要在渲染副武器附件模型时**代替**原始姿态使用。
     */
    open fun resolveSubWeaponFollowPose(stack: ItemStack, model: GeoGunModel): SubWeaponFollowPose? {
        val (slot, definition) = findSubWeapon(from(stack)) ?: return null

        val modelPath = definition.model ?: return null
        val boneName = AttachmentSlots.mountBoneOf(slot, definition) ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null

        // 本帧的副武器换弹姿态（换弹之外恒为 null，这条路径也就整个跳过）
        val attachmentPose = (FirstPersonRenderHandler.getActiveAnimationInstance(InteractionHand.MAIN_HAND)
                as? GeoGunAnimationInstance)
            ?.subWeaponReloadPose() ?: return null

        val rootIndex = attachmentModel.baseModel.getIndex(ATTACHMENT_ROOT_BONE)
        if (rootIndex < 0) return null
        val attachmentRoot = attachmentPose.findTransform(rootIndex) ?: return null

        val gunRootIndex = model.getIndex(GUN_ROOT_BONE)
        if (gunRootIndex < 0) return null
        val gunRoot = model.getBone(gunRootIndex)?.getLocalTransform() ?: return null

        // ⚠ 用 **bind pose** 的挂点变换：它只取决于骨骼静态父子关系，不受主武器姿态影响，
        // 因此可以缓存、也不会和"我们刚给 root 加的 D"互相纠缠（用 posed 版本会自反馈）。
        val mountBind = model.getBindGlobalTransform(boneName) ?: return null
        val gunRootBind = model.getBindGlobalTransform(GUN_ROOT_BONE) ?: return null

        // M = 挂点在主武器空间里的变换
        val mount = Matrix4f(gunRootBind).invert().mul(mountBind)
        val mountInv = Matrix4f(mount).invert()

        val rootLocal = matrixOf(attachmentRoot)
        // D = M · A · M⁻¹：把附件空间里的整体运动换算成主武器空间里的整体运动
        val worldOffset = Matrix4f(mount).mul(rootLocal).mul(mountInv)

        // 副武器 root：A → D⁻¹ · A（抵消子节点侧的同一份位移，让它留在挂点上）
        val cancelLocal = Matrix4f(worldOffset).invert().mul(rootLocal)
        val attachmentRootFinal = MERGE_BLENDER.blend(
            listOf(attachmentPose, singleBonePose(rootIndex, cancelLocal))
        )

        // 玩家手臂：**不由副武器接管**（附件的手部骨骼不参与手臂渲染）。
        // 理由见 `SubWeaponFollowPose` 的说明：手臂位形读的是**主武器模型**的 `lefthand_pos`，
        // 附件模型那根同名骨骼既不渲染手臂、也被主动隐藏；而现有换弹动画是对着手持形态的
        // 骨架做的，直接搬过来会让手消失、光照错乱。

        // 炮弹：**不做任何修正**（试过的换基版方向反了、更差，已连同死代码一起删掉）。
        // 记录与下一步建议见上面那段注释、以及设计文档 §11.11.7.3-C。

        // 主武器 root 的修正：G → D · G。这里**只算**、不写进模型实例 ——
        // 真正的"主武器跟着动"由 `renderModel` 把 [SubWeaponFollowPose.gunRoot] 叠加到渲染姿态上完成；
        // 在这里改实例只会让本帧后续所有从实例读出来的骨骼都带上这份偏移。
        val newGunRootLocal = Matrix4f(worldOffset).mul(gunRoot)

        return SubWeaponFollowPose(
            modelPath,
            singleBonePose(gunRootIndex, newGunRootLocal),
            attachmentRootFinal,
        )
    }

    /**
     * 炮弹位置：**当前不做任何修正**。
     *
     * 这里曾经有一个 `resolveProjectilePositionCorrection`（把炮弹的位移从 `root` 空间换基到
     * 父骨骼空间）。实测它**方向搞反** —— 修正后炮弹"偏上偏后"，比不修更差，所以撤掉了，
     * 连同它需要的 `BedrockAttachmentModel.getBindGlobalTransform` 一起删干净。
     *
     * 留着这段注释而不是留一段死代码，是因为它的价值只在"别再照那个思路试一遍"：
     *
     * | 事实 | 说明 |
     * |---|---|
     * | 炮弹**旋转是对的** | 需求方实测确认，不要动旋转那部分 |
     * | 位置**偏上偏后** | 换基修正没能修好，方向反而反了 |
     * | position 关键帧是**偏移** | `BoneState.x/y/z` 语义（相对绑定位置），不是绝对值 |
     * | `projectile` 挂在 **`root`** 下 | **不是 `tube` 的子节点** —— 所以 `gun`/`tube` 跟着 `root` 转时炮弹不跟着转 |
     *
     * 下一步不要继续猜变换：先量**"静止（绑定姿势）时炮弹相对 `tube` 偏了多少"**，
     * 有偏差量之后需要什么变换是一步能算出来的。完整记录见设计文档 §11.11.7.3-C。
     */

    /** [resolveSubWeaponFollowPose] 的产物，见那个函数的说明 */
    data class SubWeaponFollowPose(
        /** 这份修正属于哪个附件模型；渲染时用它确认"这个槽位的附件就是被修正的那一个" */
        val modelPath: ResourceLocation,
        /**
         * 只含主武器 `root` 一根骨骼的修正姿态。
         *
         * ⚠ 调用方必须在**渲染姿态**里 merge 它（`renderModel` 就是这么做的）。
         * 本函数**不会**把 `D` 留在模型实例上 —— 量完手部位形就还原了，
         * 否则这一帧后续所有从实例读出来的骨骼都会带上这份偏移。
         */
        val gunRoot: Pose,
        /** 抵消过的副武器换弹姿态；渲染附件模型时**代替**原始姿态使用 */
        val attachmentPose: Pose,
    )

    /** 从 `BoneTransform` 取出它的局部变换矩阵（平移 → 旋转 → 缩放，与 `BoneState` 同一套组合顺序） */
    private fun matrixOf(transform: BoneTransform): Matrix4f =
        Matrix4f()
            .translate(transform.translation())
            .rotate(transform.rotation().asQuaternion())
            .scale(transform.scale())

    /**
     * 从姿态里取出**指定骨骼下标**的那一条变换。
     *
     * `Pose` 只有 `getBoneTransforms()` 这一个读接口（没有按下标取的方法），所以这里遍历一次；
     * 一帧里最多两次调用、骨骼数量级是几十，可以忽略。
     */
    private fun Pose.findTransform(boneIndex: Int): BoneTransform? =
        getBoneTransforms().firstOrNull { it.boneIndex() == boneIndex }

    /**
     * 把 [matrix] 写回某根骨骼。
     *
     * `BoneTreeInstance.applyPose` 就是直接写这几个字段，`Pose` 接口**只能读不能写**，
     * 所以"给主武器 root 加一个偏移"这件事没别的做法。
     *
     * ⚠ 欧拉角取 **ZYX**（与 `BoneState` / `ZYXRotationView` / `BLENDER` 同一套约定），
     * 不能图省事只看四元数：姿态的应用顺序在这套引擎里是按欧拉角定的，
     * 换一套顺序会让 Z 轴之外的分量转错方向。
     */
    private fun writeLocalTransform(bone: BoneState, matrix: Matrix4f) {
        val translation = matrix.getTranslation(Vector3f())
        val scale = matrix.getScale(Vector3f())
        val rotation = matrix.getUnnormalizedRotation(Quaternionf())

        bone.x = translation.x
        bone.y = translation.y
        bone.z = translation.z
        bone.rotation.set(rotation)
        bone.rotationInEuler.set(rotation.getEulerAnglesZYX(Vector3f()))
        bone.xScale = scale.x
        bone.yScale = scale.y
        bone.zScale = scale.z
    }

    /** 构造一个"只含某一根骨骼"的姿态，供 [MERGE_BLENDER] 覆盖到别的姿态上 */
    private fun singleBonePose(boneIndex: Int, matrix: Matrix4f): Pose {
        val builder = ArrayPoseBuilder()
        builder.addBoneTransform(
            BoneTransform(
                boneIndex,
                matrix.getTranslation(Vector3f()),
                ZYXRotationView(matrix.getUnnormalizedRotation(Quaternionf()).getEulerAnglesZYX(Vector3f())),
                matrix.getScale(Vector3f()),
            )
        )
        return builder.toPose()
    }

    /**
     * 副武器**模型自己**的 `flare` 骨骼在枪姿态空间里的变换。
     *
     * 副武器（下挂榴弹发射器这类）开火时，枪口焰与动画关键帧里的枪口烟都该挂在**它的**枪口上，
     * 而不是主武器的 `flare`：主武器的枪口在另一头，挂上去就成了"枪管前端在喷火、下挂筒在下面发射"。
     *
     * 组合方式与渲染那一条路径完全一致（挂点骨骼 × 配件模型里的骨骼变换），
     * 所以只要配件画得出来，这里就取得动。取不到时返回 `null`
     * ——调用方**不会**因此退回主武器的 `flare`，而是干脆不画这一簇火焰。
     *
     * ⚠ 副武器换弹时主武器的 `root` 会在**渲染姿态**里被 [resolveSubWeaponFollowPose] 顶走，
     * 那一帧这里读到的挂点变换会**跟着偏移** —— 这是对的（枪口焰该跟着枪走）。
     */
    open fun resolveSubWeaponFlareTransform(stack: ItemStack, model: GeoGunModel): Matrix4f? {
        val (slot, definition) = findSubWeapon(from(stack)) ?: return null

        val modelPath = definition.model ?: return null
        val boneName = AttachmentSlots.mountBoneOf(slot, definition) ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        val mountTransform = model.getGlobalTransform(boneName) ?: return null
        val flareTransform = attachmentModel.getGlobalTransform(FLARE_BONE) ?: return null

        return Matrix4f(mountTransform).mul(flareTransform)
    }

    /**
     * 副武器开火时的枪口焰缩放：配件自己的 `MuzzleFlashScale`。
     *
     * 与枪口配件共用同一个字段（同一个 POJO），所以"下挂榴弹的火焰比步枪大一圈"这种调整
     * 写在副武器配件的 json 里即可，不必再开一个只对这一处生效的字段。
     */
    open fun resolveSubWeaponMuzzleFlashScale(stack: ItemStack): Float =
        findSubWeapon(from(stack))?.second?.muzzleFlashScale?.coerceAtLeast(0f) ?: 1.0f

    open fun resolveMagazineBone(stack: ItemStack): String {
        return when (from(stack).magazineLevel()) {
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
        val data = from(stack)
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
        return from(stack).heat.get()
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
        if (stack.item !is GunItem) return
        if (localPlayer == null) return
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
        stack: ItemStack,
        scopeRender: ScopeRenderData? = null,
        hand: InteractionHand = InteractionHand.MAIN_HAND
    ) {
        val viewTransform = computeViewTransform(model, stack, scopeRender, hand) ?: return
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
        stack: ItemStack,
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

        // 瞄准位形的优先顺序（四期，§9.8.6）：
        //   ① 副武器附件模型自己的 `iron_view`   —— 只有**副武器被切出来**时才可能取到
        //   ② 宿主枪的瞄具分划（装了瞄具才有）
        //   ③ 宿主枪的机瞄
        // 装了红点的枪切到副武器时，玩家眼睛贴在副武器上、但红点分划还在枪身上 ——
        // 这时取宿主枪的 `scope_view` 反而是对的（副武器是下挂件，它自己的瞄具就在枪身中段）。
        val deployed = playerDeployedSubWeapon(stack)
        val ironViewTransform = (if (deployed) resolveSubWeaponAimTransform(stack, model) else null)
            ?: scopeViewTransform(scopeRender, hand)
            ?: model.getGlobalTransform(IRON_VIEW_BONE)
            ?: return hipViewTransform
        return blendViewTransform(hipViewTransform, Matrix4f(ironViewTransform), zoom)
    }

    /**
     * 本地玩家当前操控的是不是这把枪上的副武器。
     *
     * 用 `ActiveGun`（而不是 `player.mainHandItem`）：部署状态就写在主手那把枪的枪械状态里，
     * 由服务端写好同步过来（§9.8.10）。
     */
    private fun playerDeployedSubWeapon(stack: ItemStack): Boolean {
        if (localPlayer == null) return false
        val gun = from(stack)
        return ActiveGun.isDeployed(gun, true)
    }

    /**
     * 副武器**自己**的瞄具位形；没有就用 `null` 让调用方继续往下找（四期，§9.8.6）。
     *
     * 骨骼名是**约定**（`SubWeaponInfo.VIEW_BONE` = `iron_view`），**没有配置字段** ——
     * 骨骼名是模型作者与渲染器之间的约定，多一个可覆盖字段只会多一个写错的地方。
     *
     * 取的顺序：附件模型自己的 `iron_view` × 挂点骨骼。`idle_view` 是**主武器**的持枪位形，
     * 副武器没有它也不需要它（它跟着挂点走）。
     *
     * 附件模型里没有 `iron_view` 时返回 `null`，瞄准位形回退到宿主枪的瞄具/机瞄
     * —— 视觉上是"整枪抬到机瞄位、榴弹筒跟着上去"，可接受。**给附件模型加一支
     * `iron_view` 骨骼即可生效，代码一个字都不用改**（`sub_weapon_gp_25` 已经这么做了）。
     * 届时 [subWeaponHasOwnAimPose] 也会跟着变成 `true`，倍率自动改用副武器自己的 ——
     * 两条路径共用同一个判据，不会出现"位形换源了、倍率还留在主武器"。
     *
     * ⚠ **挂点必须取[绑定][GeoGunModel.getBindGlobalTransform]变换，不能取当帧的动画变换**，理由见函数内注释。
     * 这也是本仓库对"相机锚点"的统一口径：瞄具走 `ScopeRenderData.bindSlotTransform`
     * （[scopeViewTransform]），副武器走这里，两者都不跟着动画动；差异只在**绘制**时才用动画变换。
     *
     * 只有在**副武器被切出来**时才该用它（`ActiveGun`）：主武器自己还挂在枪上时，
     * 瞄准位形当然还是主武器的。
     */
    open fun resolveSubWeaponAimTransform(stack: ItemStack, model: GeoGunModel): Matrix4f? {
        val (slot, definition) = findSubWeapon(from(stack)) ?: return null

        val modelPath = definition.model ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        val boneName = AttachmentSlots.mountBoneOf(slot, definition) ?: return null

        val aimTransform = attachmentModel.getGlobalTransform(SubWeaponInfo.VIEW_BONE) ?: return null
        // 挂点骨骼是宿主枪 `root` 的后代（ak_12 是 `sub_weapon_pos <- positioning2 <- root`），
        // 而 `fire_sub_weapon` 动的正是宿主枪的 `root`（position 峰值 4.7、rotation 峰值 7.4°）。
        // 用当帧动画变换当锚点 = 相机跟着后坐一起走 → 模型被反变换回屏幕原位，
        // 整把枪相对屏幕一动不动，开火动画"幅度特别小"甚至看不见。
        // 取绑定变换后，锚点固定在枪身静止姿态上，后坐就正常显示（与瞄具、与本枪机瞄一致）。
        val mountTransform = model.getBindGlobalTransform(boneName) ?: return null

        return Matrix4f(mountTransform).mul(aimTransform)
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
        /**
         * 枪上装着的副武器：`(槽位, 配件定义)`；没装返回 `null`。
         *
         * 放在伴生对象里是为了让 [subWeaponHasOwnAimPose] 也能用 —— 那个判据要被 FOV 那一侧
         * （`ClientEventHandler.onFovUpdate`）以静态形式调用，而它手上没有渲染器实例。
         */
        private fun findSubWeapon(data: GunData): Pair<AttachmentSlot, AttachmentDefinition>? {
            for (slot in AttachmentSlots.ALL) {
                val attachmentId = data.attachment.id(slot.type) ?: continue
                val definition = AttachmentDefinition.from(attachmentId) ?: continue
                if (definition.subWeapon != null) return slot to definition
            }
            return null
        }

        /**
         * 部署中的副武器**有没有自己的瞄准位形** —— 也就是 `resolveSubWeaponAimTransform` 会不会给出结果。
         *
         * | 副武器有自己的 `iron_view` | 位形 | 倍率 |
         * |---|---|---|
         * | 有 | 副武器模型自己的 `iron_view` | 副武器自己的 `Zoom` |
         * | 没有（当前 GP-25 就是这种） | 宿主枪的 `scope_view` / `iron_view` | **宿主枪的** `Zoom`（含它装的瞄具倍率） |
         *
         * 后一行是四期返修补上的：位形回退到了主武器的 4 倍镜，倍率却还读副武器那份（默认 1），
         * 就成了"眼睛贴着 4 倍镜、FOV 却是 1 倍"。
         *
         * 之所以单独拆成一个**不吃 `GeoGunModel`** 的函数：FOV 那一侧手上只有枪的数据，
         * 拿不到渲染中的模型；而"附件模型里有没有 `iron_view`"这件事必须**两处问同一个人**，
         * 否则位形换源了、倍率还留在原处。
         */
        @JvmStatic
        fun subWeaponHasOwnAimPose(gun: GunData): Boolean {
            val definition = findSubWeapon(gun)?.second ?: return false
            val modelPath = definition.model ?: return false
            val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return false
            return attachmentModel.getGlobalTransform(SubWeaponInfo.VIEW_BONE) != null
        }

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

        /** **主武器**模型里的整体骨骼。副武器换弹时"反推"过来的运动加在它上面（见 [resolveSubWeaponFollowPose]） */
        private const val GUN_ROOT_BONE = "root"

        /** **配件**模型里的整体骨骼。美术的换弹动画把"整把武器在手里怎么动"写在这一根上 */
        private const val ATTACHMENT_ROOT_BONE = "root"

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

        /**
         * 按骨骼下标叠加姿态，**上层覆盖下层**（与 `GeoGunAnimationInstance.MERGE_BLENDER` 同一套）。
         *
         * 副武器换弹的"跟随运动"（见 [resolveSubWeaponFollowPose]）用它把"只含枪模型 `root` 一根骨骼"
         * 的修正姿态盖到主武器的动画姿态上：没被修正碰到的骨骼原样保留，所以主武器自己的 idle/run
         * 照常播。
         */
        private val MERGE_BLENDER = NoAllocMergeBlender()
    }
}
