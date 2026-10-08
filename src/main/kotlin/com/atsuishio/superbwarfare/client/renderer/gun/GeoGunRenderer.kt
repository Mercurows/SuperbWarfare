package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.client.ClientRenderHandler
import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.gun.GeoGunAnimationInstance
import com.atsuishio.superbwarfare.client.charm.CharmRuntime
import com.atsuishio.superbwarfare.client.charm.CharmSnapshot
import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.overlay.OverlayTraceHandler
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoReadout
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.ARM_ANCHOR_FADE_TICKS
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.CUSTOM_HAND_GUARD_BONE
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.EDIT_FOCUS_Z_OFFSET
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.FLARE_BONE
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.MERGE_BLENDER
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.MUZZLE_BONE
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.OEM_HAND_GUARD_BONE
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.OEM_MUZZLE_BONE
import com.atsuishio.superbwarfare.client.renderer.laser.LaserSightCapture
import com.atsuishio.superbwarfare.client.renderer.laser.LaserSightRenderer
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeStencilRenderHelper
import com.atsuishio.superbwarfare.compat.acceleratedrendering.AcceleratedRenderingCompat
import com.atsuishio.superbwarfare.compat.oculus.OculusCompat
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.attachment.*
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.magazineLevel
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ShieldRuntime
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.resource.ModelResource
import com.atsuishio.superbwarfare.resource.gun.DefaultGunResource
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.resource.gun.pojo.BuiltinScopeInfo
import com.atsuishio.superbwarfare.resource.gun.pojo.ItemDisplayInfo
import com.atsuishio.superbwarfare.resource.model.AttachmentModelReloadListener
import com.atsuishio.superbwarfare.script.GunScriptManager
import com.atsuishio.superbwarfare.tools.BedrockBoneCoordinateTool
import com.atsuishio.superbwarfare.tools.RenderDistanceHelper
import com.atsuishio.superbwarfare.tools.localPlayer
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.firstperson.FirstPersonParticleSystem
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.CameraStateCache
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.renderer.AbstractGeoItemRendererV2
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneDefinition
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
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.client.event.ViewportEvent
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import java.lang.Math
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

    // 当前正在渲染的本地玩家第一人称手
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

    data class AttachmentRenderData(
        val model: BedrockAttachmentModel,
        val texture: ResourceLocation,
        val definition: AttachmentDefinition,
    )

    override fun createAnimationInstance(stack: ItemStack, entity: Entity?): IFPAnimationInstance {
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

    /**
     * 本帧的副武器"跟随运动"结果（[resolveSubWeaponFollowPose]），由 `renderModel` 算一次、
     * `renderRegisteredAttachments` 复用；每帧开头清空（它绑定的是"这一帧用的是哪个模型/哪只手"）。
     */
    private var subWeaponFollow: SubWeaponFollowPose? = null

    /**
     * 本帧的副武器换弹手臂锚点（[resolveSubWeaponHandAnchors]）：换弹时手臂该待在哪儿。
     *
     * 每帧开头清空，在附件渲染窗口里填、在窗口外读；优先级高于 [deployedArmAnchors]，空表 = 这一路没有话说。
     */
    private var subWeaponHandAnchors: Map<HumanoidArm, Matrix4f> = emptyMap()

    /** [subWeaponHandAnchors] 的来源标识（换弹 clip 名），只用于判断"来源换了没有"以决定要不要淡入 */
    private var subWeaponAnchorKey: String? = null

    /** 本帧的部署期间手臂锚点（[resolveDeployedSubWeaponArmAnchors]）；每帧开头清空，空表 = 没有在部署副武器 */
    private var deployedArmAnchors: Map<HumanoidArm, Matrix4f> = emptyMap()

    /** 本帧部署接管的来源标识（`"idle:<clip名>"`），只用于判断"来源换了没有"以决定要不要淡入 */
    private var deployedArmKey: String? = null

    /**
     * 上一次真正画出去的手臂锚点，以及它的来源标识与淡入进度
     */
    private var armAnchorSourceKey: String? = null
    private var armAnchorFade = 1f
    private val armAnchorFadeFrom = EnumMap<HumanoidArm, Matrix4f>(HumanoidArm::class.java)
    private val armAnchorShown = EnumMap<HumanoidArm, Matrix4f>(HumanoidArm::class.java)

    /**
     * 本帧的 hip 位形基准（`idle_view`，或部署副武器时它在副武器上的替代）。
     *
     * `null` = 照旧用模型自己的 `idle_view`（没装副武器、副武器没有这支骨骼、或者不是第一人称）。
     * ⚠ 只有 [updateSubWeaponIdleView] 会写它、每帧只写一次；消费者（[computeViewTransform] /
     * [computeEditFocusOffset]）只读不推进 —— 那两个函数每帧被调两次，写进去淡入会走成两倍速。
     */
    private var idleViewAnchor: Matrix4f? = null

    /**
     * 每只手的副武器 `idle_view` 淡入淡出状态（[updateSubWeaponIdleView]），与手臂锚点同形同速。
     *
     * ⚠ 按 [InteractionHand] 分开存：同一把枪两手各拿一支时命中的是同一个渲染器实例，一帧会跑两遍。
     */
    private val idleViewFades = mutableMapOf<InteractionHand, IdleViewFade>()

    /** [idleViewFades] 的一份：来源标识、进度，以及"上一帧真正画出去的那个基准"。 */
    private data class IdleViewFade(
        /** `"<挂点骨骼>@<附件模型路径>"`；`null` = 主武器自己的 `idle_view` */
        var key: String? = null,
        /** 0 → 1；1 = 已经到位 */
        var fade: Float = 1f,
        /** 切换那一刻屏幕上原本那个基准（上一帧画出去的），插值的起点 */
        var from: Matrix4f? = null,
        /** 上一次真正画出去的基准，供下一次切换当起点 */
        var shown: Matrix4f? = null
    )

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

        if (transformType.firstPerson() || transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            LaserSightCapture.beginFrame()
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
        subWeaponHandAnchors = emptyMap()
        subWeaponAnchorKey = null
        deployedArmAnchors = emptyMap()
        deployedArmKey = null
        idleViewAnchor = null
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

            // 必须早于 `updateEditFocus`（聚焦偏移相对同一个基准算）与 `applyFirstPersonPositioningTransform`，且每帧只能调一次
            updateSubWeaponIdleView(stack, model, hand)

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
            // 枪自带瞄具（BuiltinScope）也参与同一个压缩，否则它会用 0.75 而配件用自己那一档，
            // 切来切去枪身的推进距离对不上。
            val zoomLengthScale = scopeRender?.scopeMode?.zoomLengthScale
                ?: resource.builtinScope?.zoomLengthScale
                ?: 0.75f
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
        // 加长护木：把枪口那几根骨骼搬到护木末端。⚠ 必须夹在下面那句
        // `resolveMuzzleAttachmentMuzzleTransform`（本帧唯一一次采样 `muzzle_pos`）之前，
        // 也要排在所有 `apply*` 之后 —— 骨骼得先摆到本帧的最终姿态上再搬。
        renderBarrelExtension(stack, model)
        val attachmentRender = resolveMuzzleAttachmentRender(stack)
        val attachmentMuzzleTransform = attachmentRender?.let {
            resolveMuzzleAttachmentMuzzleTransform(stack, model, it)
        }
        val muzzleFlashScale = resolveMuzzleAttachmentMuzzleFlashScale(stack)

        val canStencil = transformType.firstPerson()
                && !OculusCompat.isRenderingShadowPass()
                && bufferSource is MultiBufferSource.BufferSource
        val stencilScope = if (canStencil) findStencilScope(stack, model) else null
        var gunCulled = false
        var builtinScopeActive = false
        // 装了加速渲染时，下面这一整段（瞄具 + 枪身 + 收尾）都要让 AR 走**原版管线**：
        // AR 会把顶点收进缓存、推迟到这一帧的 flush 才画，而模板蒙版要求"写模板 → 裁镜身 →
        // 画遮罩与分划"三步按顺序真正落到帧缓冲上 —— 只要中间有一步被推迟，语义就不成立了。
        // 关掉加速之后这些几何立刻出图，顺序与本模组不含 AR 时**逐字一致**。
        // 代价只有"瞄准时这一小段几何不参与加速"；不瞄的时候一切照旧。
        // AR 内部是栈语义（push / pop），所以这里的 try / finally 是配平且可嵌套的。
        val vanillaAcceleration = AcceleratedRenderingCompat.shouldAccelerate
        if (vanillaAcceleration) {
            AcceleratedRenderingCompat.beginVanillaAcceleration()
        }
        try {
            if (stencilScope != null) {
                handledScopeAttachment = stencilScope.attachmentId
                poseStack.pushPose()
                mulPoseWithNormal(poseStack, stencilScope.slotTransform)
                // 命中提示 HUD 要挂在分划上就得知道分划画在屏幕哪儿。采样点只能在这里：
                // poseStack 已经乘上这个瞄具的槽位变换（相机空间），分划模型也还挂在同一份姿态上
                submitScopeReticleSample(poseStack, stencilScope)
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
            } else if (canStencil) {
                // **枪自带瞄具**（geo 里有 `ocular` 的枪，目前只有 igla_9k38）。
                // 装了镜配件就轮不到它，见 [findBuiltinScope]；两条路径互斥，同一帧只可能走一条。
                val builtinScope = findBuiltinScope(stack, model, resource)
                if (builtinScope != null) {
                    builtinScopeActive = true

                    // ⚠ 这里**不做任何额外变换** —— 用的就是下面画枪身那一个 poseStack。
                    // 窗口、准星、被剔除的枪身三者靠共用同一份变换才咬得死，Z 轴压缩量怎么变都不会错位。
                    poseStack.pushPose()
                    model.builtinScopeRenderer.renderWithStencil(
                        poseStack,
                        bufferSource,
                        RenderType.entityTranslucent(texture),
                        BedrockModelRenderTypes.polyMeshCutout(texture),
                        packedLight,
                        builtinScope
                    )
                    poseStack.popPose()

                    // 与上面配件那一支逐字相同的三行：接下来画的枪身只在窗口**之外**留下。
                    // 那圈几何体本身就是黑色外框，仓库里没有任何 2D 遮罩贴图。
                    ScopeStencilRenderHelper.enableItemEntityStencilTest()
                    RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF)
                    RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
                    gunCulled = true
                }
            }

            // 部署副武器期间的手臂接管（[resolveDeployedSubWeaponArmAnchors]）。
            // ⚠ 采样点必须尽可能晚：挂点取自本帧最终骨骼，排在 `applyPose` / `applyCameraShake` /
            // 脚本回调之后，否则手臂跟的是没被压缩过的后坐、瞄准射击时手会离枪约 0.27 方格
            if (transformType.firstPerson()) {
                resolveDeployedSubWeaponArmAnchors(stack, model, handForContext(transformType))
            }

            renderAttachments(stack, model, transformType, poseStack, bufferSource, packedLight, packedOverlay)

            // 内置瞄具的准星板：**任何时候**都要藏（腰射、GUI、第三人称、展示框全都走这里），
            // 否则 `renderToBuffer` 会把它那块 200×200 的透明大板当普通几何体画出来 —— 看上去
            // 就是一块十字准星飘在枪前方半空中。见 [BuiltinGunScopeRenderer.hideDivisionBones]。
            val savedDivisionBones = model.builtinScopeRenderer.hideDivisionBones()

            // `ocular` / `ocular_ring` 则是另一回事：它们只在**走模板那一路**时才需要藏 —— 上面那一遍
            // 已经画过它们了，`renderBoneImmediate` 会临时把 `visible` 打开再还原，所以这里不藏的话会在
            // `GL_EQUAL 0` 的剔除区间里被**再画一遍**（配件侧由 `renderRemaining` 的隐藏逻辑承担同一职责）。
            // 腰射时它们是枪上真实存在的镜筒本体，要照常画出来。
            // ⚠ 这一对必须自己配对，即使在正常帧里收尾的 `model.resetPose()` 也会把 `visible` 复位
            // （`BoneState.reset()` 里有 `visible = true`）：藏与还原之间夹着 `renderToBuffer`，
            // **中途抛异常**就会跳过那个复位，共享实例上的这几根骨骼会一直留在 hidden 直到下次资源重载。
            val savedOcularBones = if (builtinScopeActive) model.builtinScopeRenderer.hideOcularBones() else null
            try {
                model.renderToBuffer(
                    poseStack, bufferSource, texture, packedLight, packedOverlay,
                    resolveGunAmmoReadout(stack, resource),
                    // ⚠ 必须在 `renderAttachments` 之后算：副武器换弹那份锚点是在里面填的，优先级更高
                    resolveArmAnchorsForDraw(model, transformType),
                    // 同目录下的 `<贴图名>_e.png`，没有就返回 null（绝大多数枪都是这样），枪照旧只画一遍。
                    // 按最终选中的贴图推，所以 LOD 贴图会自动去找 `gun_lod/` 里的 `_e`
                    GunEmissiveTextures.get(texture)
                )
            } finally {
                // 可见性写在共享模型实例上，中间抛异常也必须还原
                if (savedOcularBones != null) model.builtinScopeRenderer.restoreOcularBones(savedOcularBones)
                model.builtinScopeRenderer.restoreDivisionBones(savedDivisionBones)
            }

            // 激光束：本地玩家第一人称 + 第三人称右手（出光口已在 `renderAttachments` 里采完）。
            //
            // ⚠ 位置就在**模板窗口内部**（上面那句 `GL_EQUAL 0` 还生效，收尾的
            // `finishStencilCulling` 排在下面）：高倍镜（`scope`）里枪身与出光的那件配件都被
            // 从镜内剔掉，光束跟着一起被剔掉才是对的 —— 否则镜片里会剩下一截看不出从哪来的亮管。
            // 镜外那部分照旧由已经写进深度缓冲的枪身 / 镜筒按深度遮挡。
            //
            // ⚠ 必须**留在上面那个 `vanillaAcceleration` 块内部**：光束的格式（`ITEM_ENTITY_TARGET`
            // 那一族）正好落在 AR 会加速的那批格式里 —— 走出这个块，它的顶点会被 AR 推迟到帧尾，
            // 那时模板状态早就换了，光束就会整根盖到镜筒里外。
            if (transformType.firstPerson() || transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                LaserSightRenderer.render(poseStack, bufferSource, partialTick)
            }
        } finally {
            if (vanillaAcceleration) {
                AcceleratedRenderingCompat.endVanillaAcceleration()
            }
        }

        if (transformType.firstPerson()) {
            val hand = handForContext(transformType)
            val animation = FirstPersonRenderHandler.getActiveAnimationInstance(hand) as? GeoGunAnimationInstance

            // 副武器开火期间（部署中，或副武器那一支开火动画正在播）才解析它的枪口骨骼：谁被切出来，火就归谁
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

            // 枪口焰刚从这个挂点钻出来，顺手把同一个挂点交给子弹：
            // 每帧交一份"枪管现在指向哪儿"，开火窗口里再交一份"虚拟出膛点"的偏移
            submitMuzzleSample(poseStack.last().pose(), model, stack, subWeaponFlare, attachmentMuzzleTransform)

            ShellCasingFxRenderer.render(poseStack, model, stack, hand, bufferSource, packedLight)

            val transforms = lastBoneTransforms.getOrPut(hand) { mutableMapOf() }
            // 副武器开火时枪口定位点改挂副武器模型自己的枪口，且不注册枪口配件的 `MUZZLE_BONE`
            // （`resolveMuzzleLocator` 优先取它，留着会把榴弹的枪口烟吸到枪管前端去）
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
        // 瞄具模板的裁切窗口在这里结束（里面那句 `endBatch` 把枪身与光束整批落盘）
        finishStencilCulling(bufferSource)
        model.resetPose()
    }

    private fun captureLaserSight(
        transformType: ItemDisplayContext,
        poseMatrix: Matrix4f,
        attachmentModel: BedrockAttachmentModel,
        definition: AttachmentDefinition,
        slot: AttachmentType,
        stack: ItemStack,
    ) {
        val info = definition.laser ?: return

        val firstPerson = transformType.firstPerson()
        if (firstPerson) {
            if (localFirstPersonHand == null) return
        } else if (transformType != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return
        }
        // 阴影 pass 里画激光只会往阴影贴图里写颜色（与吊坠那边同一个判据）
        if (OculusCompat.isRenderingShadowPass()) return

        val data = from(stack)
        LaserSightCapture.capture(
            info = info,
            colorRgb = info.resolveColorRgb(data.attachment.getLaserColor(slot)),
            firstPerson = firstPerson,
            poseMatrix = poseMatrix,
            locatorTransform = attachmentModel.getLocatorTransform(info.locator),
        )
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
        renderBulletChain(stack, model)
        renderProjectileBone(stack, model)
        renderScopeMount(stack, model)
        renderScopeAttachment(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
        renderStock(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
        renderGripHandGuard(stack, model)
        renderGripAttachment(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
        renderOemScope(stack, model)
        renderOemMuzzle(stack, model)
        renderMuzzleAttachment(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
        renderRegisteredAttachments(
            stack,
            model,
            transformType,
            poseStack,
            bufferSource,
            packedLight,
            packedOverlay,
            handForContext(transformType)
        )
    }

    /**
     * 注册表驱动的通用配件渲染
     */
    open fun renderRegisteredAttachments(
        stack: ItemStack,
        model: GeoGunModel,
        transformType: ItemDisplayContext,
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

            // 副武器自己的换弹动画（由宿主枪的动画实例推进）：取姿态、应用、复位。
            // 挂点变换取自宿主枪（不含附件姿态），姿态在挂点之内、按附件模型自己的骨骼应用
            val subWeaponAnimation = if (slot.type == AttachmentType.SUBWEAPON) {
                FirstPersonRenderHandler.getActiveAnimationInstance(hand) as? GeoGunAnimationInstance
            } else {
                null
            }
            val clipPose = subWeaponAnimation?.subWeaponReloadPose()
            // clip 里的平移是"相对绑定姿势的偏移"，必须先与绑定姿势混合再套，否则绑定不为 0 的骨骼会被送到父节点原点；
            // 换弹时还要把姿态里的 `root` 通道摘掉，整枪的位移已经由宿主枪的 `root` 承担
            val subWeaponPose = if (clipPose != null && subWeaponFollow?.modelPath == modelPath) {
                withoutBone(clipPose, attachmentModel.baseModel.getIndex(ATTACHMENT_ROOT_BONE))
            } else {
                clipPose
            }

            // 吊坠摆动：只有本地玩家自己的第一人称才推进物理（阴影 pass 不推进，免得一帧推进两次）
            val charm = slot.type == AttachmentType.CHARM &&
                    localFirstPersonHand != null &&
                    !OculusCompat.isRenderingShadowPass()

            // 挂点变换 + 配件自己的基础旋转，合成一份；手臂锚点也必须用它，否则手臂会从配件上脱开
            val mount = Matrix4f(mountTransform)
            attachmentRotation(definition)?.let { mount.mul(it) }

            poseStack.pushPose()
            mulPoseWithNormal(poseStack, mount)
            var charmSnapshot: CharmSnapshot? = null
            var bipodSnapshot: BipodSnapshot? = null
            var shieldHidden = false
            try {
                if (subWeaponPose != null) {
                    attachmentModel.applyPose(BLENDER.blend(attachmentModel.getBindPose(), subWeaponPose))
                }
                // 下挂脚架：配件自带 `bipod_l` / `bipod_r` 时，按卧姿架设进度把它们向后翻下去
                // （进度的纯函数，没有跨帧状态）；放在 `applyPose` 之后，好让快照/还原严格配对
                if (definition.hasBipod) {
                    bipodSnapshot = BipodDeploy.apply(attachmentModel, scriptBipodProgress(stack).toFloat())
                }
                // 手臂锚点要在姿态还在实例上时取（下面 `finally` 里就 `resetPose()` 了），
                // 由 `GeoGunModel.renderHands` 用它代替宿主枪的同名骨骼
                if (subWeaponPose != null) {
                    subWeaponHandAnchors = resolveSubWeaponHandAnchors(attachmentModel, mount)
                    subWeaponAnchorKey = subWeaponAnimation?.subWeaponReloadClipName
                }
                // 摆动姿态必须在这之前写进骨骼（它改的是 `string` / `charm` 两根骨骼），`renderToBuffer` 只是照着画
                if (charm) {
                    charmSnapshot = CharmRuntime.apply(
                        attachmentModel,
                        definition,
                        boneName,
                        poseStack.last().pose(),
                        cameraRotationInverse(),
                        hand
                    )
                }
                // 激光出光口
                captureLaserSight(
                    transformType,
                    poseStack.last().pose(),
                    attachmentModel,
                    definition,
                    slot.type,
                    stack,
                )
                // 枪盾：耐久空的破损态藏掉盾面骨骼
                shieldHidden = definition.shield != null && hideBrokenShield(attachmentModel, stack)
                attachmentModel.renderToBuffer(
                    poseStack, bufferSource, texture, packedLight, packedOverlay,
                    null, attachmentReadout(stack, definition, hand)
                )
            } finally {
                // 附件模型实例是全局共享的，写进去的姿态必须还原；
                // 快照还原要排在 `resetPose()` 之前，否则快照里带的副武器姿态会被写回共享实例
                if (shieldHidden) attachmentModel.setBoneVisible(AttachmentSlots.Bones.SHIELD, true)
                BipodDeploy.revert(attachmentModel, bipodSnapshot)
                if (charmSnapshot != null) CharmRuntime.revert(attachmentModel, charmSnapshot)
                if (subWeaponPose != null) attachmentModel.resetPose()
            }
            poseStack.popPose()
        }
    }

    /** 枪盾耐久为空时藏掉盾面骨骼，返回是否真的藏了（调用方负责还原） */
    private fun hideBrokenShield(model: BedrockAttachmentModel, stack: ItemStack): Boolean {
        val broken = ShieldRuntime.of(from(stack)).any { it.charge <= 0.0 }
        if (!broken) return false

        model.setBoneVisible(AttachmentSlots.Bones.SHIELD, false)
        return true
    }

    open fun renderMagazine(stack: ItemStack, model: GeoGunModel) {
        model.showMagazineBone(resolveMagazineBone(stack))
    }

    /**
     * 按剩余弹量显示弹链上的子弹（`bullet_1`……）：模型里把弹链子弹按 `bullet_1` 起编号即可，
     * 打到只剩几发就自动藏掉几发；`HIDE_BULLET_CHAIN` 动作点之后整条弹链换新、重新画满。
     */
    open fun renderBulletChain(stack: ItemStack, model: GeoGunModel) {
        val data = from(stack)
        val freshBelt = data.reloading() && !data.hideBulletChain.get()
        model.showBulletChainBones(data.ammo.get(), freshBelt)
    }

    /** 只显示当前弹种对应的弹丸骨骼，让枪里画出来的那发子弹跟着弹种切换。 */
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
            definition.ammoBar, definition.textShow
        )
    }

    /** 本帧的弹药读数：弹药条要用的余弹比例，与弹药文字要用的余弹数。 */
    protected open fun resolveAmmoReadout(stack: ItemStack, data: ScopeRenderData): AmmoReadout {
        return resolveAmmoReadout(stack, data.ammoBar, data.textShow)
    }

    /**
     * 本帧的弹药读数：弹药条要用的余弹比例，与弹药文字要用的余弹数。
     *
     * 没配弹药显示时提前返回空读数 —— [GunProp.MAGAZINE] 是一次完整的属性修改链解析，
     * 必须挡在 [from] 之前，否则每一把不显示弹药数的枪与配件都要每帧白算一遍。
     */
    protected open fun resolveAmmoReadout(
        stack: ItemStack,
        bars: List<AmmoBarEntry>,
        texts: List<AmmoTextEntry>,
        range: Int = AmmoTextEntry.NO_RANGE,
    ): AmmoReadout {
        if (bars.isEmpty() && texts.isEmpty()) return AmmoReadout()

        val gun = from(stack)
        val count = gun.ammo.get()
        val magazine = gun.get(GunProp.MAGAZINE)
        // 没有可用弹匣：弹药条按满算而不是除以零（能量武器、背包弹药等 `MAGAZINE <= 0` 的枪因此显示满条 + "0"）
        if (magazine <= 0) return AmmoReadout(bars, texts, 1f, count, range)
        return AmmoReadout(
            bars,
            texts,
            (count.toFloat() / magazine.toFloat()).coerceIn(0f, 1f),
            count,
            range
        )
    }

    /**
     * 配件弹药条 / 弹药文字 / 测距仪读数
     */
    private fun attachmentReadout(
        stack: ItemStack,
        definition: AttachmentDefinition,
        hand: InteractionHand,
    ): AmmoReadout {
        val texts = definition.textShow
        val range = if (hand == localFirstPersonHand && texts.any { it.usesRange }) {
            this.measureRange(localPlayer)
        } else {
            AmmoTextEntry.NO_RANGE
        }
        return resolveAmmoReadout(stack, definition.ammoBar, texts, range)
    }

    open fun measureRange(player: Player?): Int {
        if (player == null) return AmmoTextEntry.NO_RANGE

        val lookingEntity = OverlayTraceHandler.maxRangeEntity
        if (lookingEntity is VehicleEntity) return AmmoTextEntry.NO_RANGE
        if (lookingEntity != null) return player.distanceTo(lookingEntity).roundToInt()

        val result = OverlayTraceHandler.playerViewBlockResult ?: return AmmoTextEntry.NO_RANGE
        val blockRange = player.eyePosition.distanceTo(result.location)
        if (blockRange > MAX_SCOPE_RANGE) return AmmoTextEntry.NO_RANGE
        return blockRange.roundToInt()
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

    /**
     * 查询自带的瞄具
     */
    private fun findBuiltinScope(
        stack: ItemStack,
        model: GeoGunModel,
        resource: DefaultGunResource
    ): BuiltinScopeInfo? {
        val info = resource.builtinScope ?: return null
        if (from(stack).attachment.id(AttachmentType.SCOPE) != null) return null
        if (!model.builtinScopeRenderer.available) return null
        if (ClientEventHandler.zoomTime <= SCOPE_STENCIL_START_PROGRESS) return null
        return info
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
        mulAttachmentRotation(poseStack, definition)
        attachmentModel.renderToBuffer(
            poseStack, bufferSource, texture, packedLight, packedOverlay,
            null, resolveAmmoReadout(stack, definition.ammoBar, definition.textShow)
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

    /**
     * 护木的"原厂 / 导轨"二选一：判定见 [shouldShowCustomHandGuard]，成立就换成
     * [CUSTOM_HAND_GUARD_BONE]、藏起 [OEM_HAND_GUARD_BONE]；模型里没有 `custom_hand_guard`
     * 骨骼的枪不受影响。
     */
    open fun renderGripHandGuard(stack: ItemStack, model: GeoGunModel) {
        val customBone = model.getBone(CUSTOM_HAND_GUARD_BONE) ?: return
        val showCustom = shouldShowCustomHandGuard(stack, model)
        customBone.visible = showCustom
        model.getBone(OEM_HAND_GUARD_BONE)?.visible = !showCustom
    }

    /**
     * 本帧是否渲染"带导轨的那支护木"（[CUSTOM_HAND_GUARD_BONE]，同时藏掉 [OEM_HAND_GUARD_BONE]）。
     *
     * 两个开关各管一套触发条件，任一成立就换：
     * - `Attachments.GripHandGuard`（历史行为）：装了**握把**、**下导轨件**或**下挂副武器**；
     * - `Attachments.RailHandGuard`：**四面导轨（下 / 上 / 左 / 右）任一件**或**下挂副武器** ——
     *   Vector 这类枪的握把并不长在那支护木上，靠"装握把"永远换不出它，只能看导轨。
     *
     * 只读配件数据与枪 json，是纯函数，所以 [renderBarrelExtension] 可以在渲染流程更早的位置
     * 单独问一次，不必等 [renderGripHandGuard] 跑过（那之前 `visible` 还是上一帧的值）。
     */
    open fun shouldShowCustomHandGuard(stack: ItemStack, model: GeoGunModel): Boolean {
        if (model.getBone(CUSTOM_HAND_GUARD_BONE) == null) return false
        val gun = from(stack)
        val info = GunResource.compute(stack).attachmentInfo
        val subWeapon = findSubWeapon(gun) != null
        val legacyTrigger = info.gripHandGuard &&
                (gun.attachment.has(AttachmentType.GRIP) || gun.attachment.has(AttachmentType.LOWER_RAIL) || subWeapon)
        val railTrigger = info.railHandGuard && (RAIL_SLOTS.any { gun.attachment.has(it) } || subWeapon)
        return legacyTrigger || railTrigger
    }

    /**
     * 加长枪管：[CUSTOM_HAND_GUARD_BONE] 里带了 `new_flare` / `new_muzzle_pos` 这对"延长段"锚点
     * （Vector）时，把那支护木当成枪管的延长件 ——
     * 枪口焰锚点 [FLARE_BONE] 与枪口挂点 [MUZZLE_BONE] 搬到 `new_flare` / `new_muzzle_pos`，
     * 原厂火帽 [OEM_MUZZLE_BONE] 一起搬到延长后的枪口（它的绑定位置本来就与 `muzzle_pos` 重合）。
     *
     * 搬的是**骨骼自己的变换**，所以后面所有按名字取全局变换的地方（枪口配件挂载、枪口焰、
     * 枪口烟、脚本查询）全都跟着走，不需要各自再挑一次骨骼名。
     *
     * ⚠ 必须在 `renderModel` 里那句 `resolveMuzzleAttachmentMuzzleTransform` **之前**调用 ——
     * 它是本帧唯一一次采样 `muzzle_pos` 全局变换的地方，晚了枪口配件还挂在旧枪口上。
     *
     * ⚠ 写的是共享骨骼实例，由每帧收尾的 `model.resetPose()` 复位（与同文件里那些 `visible`
     * 写入同一套约定：不要自己再存一份、也不要在中途还原）。
     */
    open fun renderBarrelExtension(stack: ItemStack, model: GeoGunModel) {
        if (!shouldShowCustomHandGuard(stack, model)) return
        if (model.getIndex(NEW_FLARE_BONE) < 0 || model.getIndex(NEW_MUZZLE_BONE) < 0) return
        retargetBone(model, FLARE_BONE, NEW_FLARE_BONE)
        retargetBone(model, MUZZLE_BONE, NEW_MUZZLE_BONE)
        retargetBone(model, OEM_MUZZLE_BONE, NEW_MUZZLE_BONE)
    }

    /**
     * 把 [fromName] 这根骨骼的**局部变换**改写成"它的全局变换等于 [toName] 的全局变换"。
     *
     * 两根骨骼的父链可以完全不同（`flare` 挂在 `root` 下，`new_flare` 挂在 `custom_hand_guard` 下），
     * 所以先把目标全局变换换算到 [fromName] 父节点的空间里得到目标局部变换 `L`，再按运行时那套
     * 「平移（骨骼单位）→ 绕 pivot 旋转」的表示反解：局部位移 `p` 处的局部变换是
     * `T(pos) · T(p) · R · S · T(-p)`，要让它的线性部分等于 `L`，平移只能取 `pos = L(p) - p`
     * （`L` 的旋转部分原样当 `R`，缩放不动）。
     *
     * 名字取不到、或者骨骼带**折叠父变换**（那层变换夹在平移与 pivot 之间，上面的式子不成立）
     * 时原样不动。
     */
    private fun retargetBone(model: GeoGunModel, fromName: String, toName: String) {
        val fromIndex = model.getIndex(fromName)
        val toIndex = model.getIndex(toName)
        if (fromIndex < 0 || toIndex < 0) return
        val from = model.getBone(fromIndex) ?: return
        val definition = from.definition()
        if (definition.foldedParentTransform() != null) return

        val parentIndex = from.parentIndex()
        val parentGlobal = if (parentIndex < 0) Matrix4f() else Matrix4f(model.getGlobalTransform(parentIndex))
        val target = parentGlobal.invert().mul(model.getGlobalTransform(toIndex))

        val pivot = Vector3f(definition.pivotX(), definition.pivotY(), definition.pivotZ())
        val position = target.transformPosition(Vector3f(pivot)).sub(pivot)
        from.x = position.x * 16f
        from.y = position.y * 16f
        from.z = position.z * 16f

        // 两边绑定旋转一致时这里一次都不会进（Vector 的 `flare` / `new_flare` 都是绕 Y 转 -90°），
        // 保留是为了父链上真的带了旋转时不至于是错的
        val rotation = target.getNormalizedRotation(Quaternionf())
        if (!from.rotation.equals(rotation, 1.0E-4f)) {
            from.rotation.set(rotation)
            // euler 用 **Matrix4f** 那一份反解：烘焙时的约定是 euler = (-rotX, -rotY, +rotZ)
            // 按 Z → Y → X 合成（`TreeBedrockModelBaker`），而 `Matrix4f.getEulerAnglesZYX`
            // 正是它的逆（Quaternionf 上那个同名方法在 1.10.5 里对不上，别用）
            val euler = Matrix4f().rotation(rotation).getEulerAnglesZYX(Vector3f())
            from.rotationInEuler.set(euler.x, euler.y, euler.z)
        }
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
            Matrix4f(mountTransform).mul(resolveMuzzleAttachmentLocalTransform(stack))
        )
        mulAttachmentRotation(poseStack, definition)
        attachmentModel.renderToBuffer(
            poseStack, bufferSource, texture, packedLight, packedOverlay,
            null, resolveAmmoReadout(stack, definition.ammoBar, definition.textShow)
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
        val hasMuzzleAttachment = data.attachment.id(AttachmentType.MUZZLE) != null
                || data.attachment.get(AttachmentType.MUZZLE) != 0
        bone.visible = !hasMuzzleAttachment
    }

    open fun renderOemScope(stack: ItemStack, model: GeoGunModel) {
        val bone = model.getBone(OEM_SCOPE_BONE) ?: return
        val data = from(stack)
        val hasScope = data.attachment.id(AttachmentType.SCOPE) != null
                || data.attachment.get(AttachmentType.SCOPE) != 0
        bone.visible = !hasScope
    }

    open fun renderMuzzleAttachment(
        stack: ItemStack,
        model: GeoGunModel,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val (attachmentModel, texture, definition) = resolveMuzzleAttachmentRender(stack) ?: return
        val boneName = resolveMuzzleAttachmentBone(stack) ?: return
        val mountTransform = model.getGlobalTransform(boneName) ?: return

        poseStack.pushPose()
        mulPoseWithNormal(poseStack, Matrix4f(mountTransform))
        mulAttachmentRotation(poseStack, definition)
        attachmentModel.renderToBuffer(
            poseStack, bufferSource, texture, packedLight, packedOverlay,
            null, resolveAmmoReadout(stack, definition.ammoBar, definition.textShow)
        )
        poseStack.popPose()
    }

    open fun resolveMuzzleAttachmentRender(stack: ItemStack): AttachmentRenderData? {
        val data = from(stack)
        val attachmentId = data.attachment.id(AttachmentType.MUZZLE) ?: return null
        val definition = AttachmentDefinition.from(attachmentId) ?: return null
        val modelPath = definition.model ?: return null
        val texture = definition.texture ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        return AttachmentRenderData(attachmentModel, texture, definition)
    }

    open fun resolveMuzzleAttachmentMuzzleFlashScale(stack: ItemStack): Float {
        val data = from(stack)
        val attachmentId = data.attachment.id(AttachmentType.MUZZLE) ?: return 1.0f
        return AttachmentDefinition.from(attachmentId)?.muzzleFlashScale?.coerceAtLeast(0f) ?: 1.0f
    }

    open fun resolveMuzzleAttachmentBone(stack: ItemStack): String? {
        val data = from(stack)
        val attachmentId = data.attachment.id(AttachmentType.MUZZLE) ?: return null
        return AttachmentDefinition.from(attachmentId)?.bone
    }

    open fun resolveMuzzleAttachmentLocalTransform(stack: ItemStack): Matrix4f {
        val data = from(stack)
        val offset = data.attachment.getOffset(AttachmentType.MUZZLE)
        val rotation = data.attachment.getRotation(AttachmentType.MUZZLE).toFloat()
        return Matrix4f()
            .translate(0f, 0f, offset.toFloat())
            .rotateZ(Mth.DEG_TO_RAD * rotation)
    }

    open fun resolveMuzzleAttachmentMuzzleTransform(
        stack: ItemStack,
        model: GeoGunModel,
        renderData: AttachmentRenderData
    ): Matrix4f? {
        val attachmentMuzzle = renderData.model.getGlobalTransform(MUZZLE_BONE) ?: return null
        val boneName = resolveMuzzleAttachmentBone(stack) ?: return null
        val mountTransform = model.getGlobalTransform(boneName) ?: return null
        return Matrix4f(mountTransform)
            .mul(resolveMuzzleAttachmentLocalTransform(stack))
            .mul(attachmentMuzzle)
    }

    /**
     * 副武器换弹：把副武器动画 `root` 通道里的整体运动反推到**宿主枪**身上，让整枪跟着一起动。
     *
     * 设挂点在宿主枪 `root` 空间里的变换为 `M`（取 bind pose）、副武器动画的 `root` 通道为 `A`，
     * 要叠加到宿主枪 `root` 上的就是 `D = M · A · M⁻¹`；附件那一侧不需要补偿，
     * 只要在渲染时把它的 `root` 通道摘掉（[withoutBone]）即可。
     *
     * 只在副武器被切出来且正在换弹时生效，其余情况（含所有量解析不到时）返回 `null`。
     * ⚠ 姿态的平移是 Bedrock 单位、`…GlobalTransform` 是方块，两头都要显式换算（[localMatrixOfPose] / [poseTransformOf]）。
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
        val gunRootBone = model.getBone(gunRootIndex) ?: return null
        val gunRoot = gunRootBone.getLocalTransform()

        // 挂点取 bind pose：只取决于静态父子关系，不会和"刚给 root 加的 D"互相纠缠
        val mountBind = model.getBindGlobalTransform(boneName) ?: return null
        val gunRootBind = model.getBindGlobalTransform(GUN_ROOT_BONE) ?: return null

        // M = 挂点在宿主枪 root 空间里的变换（方块）
        val mount = Matrix4f(gunRootBind).invert().mul(mountBind)

        // A = 附件动画的 `root` 通道，换算到方块空间
        val rootLocal = localMatrixOfPose(attachmentModel.baseModel.bone(rootIndex), attachmentRoot)

        // D = M · A · M⁻¹：把附件空间里的整体运动换算成宿主枪空间里的整体运动
        val worldOffset = Matrix4f(mount).mul(rootLocal).mul(Matrix4f(mount).invert())

        // 主武器 root 的修正 G → D · G：这里只算、不写进模型实例，
        // 否则本帧后续所有从实例读出来的骨骼都会带上这份偏移
        val newGunRootLocal = Matrix4f(worldOffset).mul(gunRoot)
        val newGunRoot = poseTransformOf(gunRootIndex, gunRootBone, newGunRootLocal) ?: return null

        return SubWeaponFollowPose(modelPath, singleBonePose(newGunRoot))
    }

    /** [resolveSubWeaponFollowPose] 的产物 */
    data class SubWeaponFollowPose(
        /** 这份修正属于哪个附件模型；渲染时用它确认"这个槽位的附件就是被修正的那一个" */
        val modelPath: ResourceLocation,
        /** 只含宿主枪 `root` 一根骨骼的修正姿态，由调用方 merge 进渲染姿态 */
        val gunRoot: Pose,
    )

    /** 姿态变换（`BoneTransform`）→ 模型空间的局部矩阵（**方块**，Bedrock 单位要除 16）。 */
    private fun localMatrixOfPose(definition: BoneDefinition, transform: BoneTransform): Matrix4f {
        val matrix = Matrix4f().translate(transform.translation().div(16f, Vector3f()))
        val rotation = transform.rotation().asQuaternion()
        val scale = transform.scale()
        if (definition.rotateAroundPivot()) {
            matrix.translate(definition.pivotX(), definition.pivotY(), definition.pivotZ())
            matrix.rotate(rotation).scale(scale)
            matrix.translate(-definition.pivotX(), -definition.pivotY(), -definition.pivotZ())
        } else {
            matrix.rotate(rotation).scale(scale)
        }
        return matrix
    }

    /**
     * [localMatrixOfPose] 的逆：模型空间局部矩阵（方块）→ 姿态变换（Bedrock 单位）。
     *
     * 只在"枢轴在原点 + 无绑定旋转 + 无折叠父变换"时无损，不满足就返回 `null` 让调用方退回原有渲染。
     */
    private fun poseTransformOf(boneIndex: Int, bone: BoneState, local: Matrix4f): BoneTransform? {
        val definition = bone.definition()
        if (definition.rotateAroundPivot() &&
            (definition.pivotX() != 0f || definition.pivotY() != 0f || definition.pivotZ() != 0f)
        ) return null
        if (definition.bindRotation().angle() > 1.0E-4f) return null
        if (definition.foldedParentTransform() != null) return null

        val rotation = local.getUnnormalizedRotation(Quaternionf())
        return BoneTransform(
            boneIndex,
            local.getTranslation(Vector3f()).mul(16f),
            ZYXRotationView(rotation.getEulerAnglesZYX(Vector3f())),
            local.getScale(Vector3f()),
        )
    }

    /**
     * 从姿态里摘掉某一根骨骼，其余原样保留。
     *
     * 用在副武器换弹时：附件的 `root` 通道已由宿主枪的 `D` 承担，附件这边再套一次就会被推离枪身。
     */
    private fun withoutBone(pose: Pose, boneIndex: Int): Pose {
        if (boneIndex < 0) return pose
        val builder = ArrayPoseBuilder()
        for (transform in pose.boneTransforms) {
            if (transform.boneIndex() != boneIndex) builder.addBoneTransform(transform)
        }
        return builder.toPose()
    }

    /** 从姿态里取出指定骨骼下标的那一条变换（`Pose` 只有遍历这一个读接口）。 */
    private fun Pose.findTransform(boneIndex: Int): BoneTransform? =
        boneTransforms.firstOrNull { it.boneIndex() == boneIndex }

    /** 构造一个"只含某一根骨骼"的姿态，供 [MERGE_BLENDER] 覆盖到别的姿态上 */
    private fun singleBonePose(transform: BoneTransform): Pose {
        val builder = ArrayPoseBuilder()
        builder.addBoneTransform(transform)
        return builder.toPose()
    }

    /**
     * 副武器换弹时手臂该挂到哪儿：把附件模型自己的 `lefthand_pos` / `righthand_pos` 乘上挂点，
     * 交给 `GeoGunModel.renderHands` 代替宿主枪的同名骨骼。附件里没有的骨骼会被跳过，
     * 调用方退回宿主枪的那一根。
     *
     * ⚠ 必须在附件实例还带着换弹姿态的时候取（调用点在 `applyPose` 与 `resetPose` 之间）。
     */
    private fun resolveSubWeaponHandAnchors(
        attachment: BedrockAttachmentModel,
        mountTransform: Matrix4f
    ): Map<HumanoidArm, Matrix4f> {
        val anchors = EnumMap<HumanoidArm, Matrix4f>(HumanoidArm::class.java)
        for ((arm, bone) in SUB_WEAPON_HAND_BONES) {
            val local = attachment.getGlobalTransform(bone) ?: continue
            anchors[arm] = Matrix4f(mountTransform).mul(local)
        }
        return anchors
    }

    /**
     * 部署期间的手臂接管：副武器被切出来之后，手臂就挂在**副武器自己**的
     * `lefthand_pos` / `righthand_pos` 上，直到把它切回去（这样换弹进出的那一次换源不存在硬切）。
     *
     * 准入条件只有 [ActiveGun.isDeployed]；宿主枪自己换弹 / 近战 / 改装时让位（[GunAnimationState.takesHandAway]）。
     * 姿态来自副武器数据里的 `Animation.Idle`（[GeoGunAnimationInstance.subWeaponIdlePose]），
     * ⚠ 挂点要取本帧最终骨骼，附件模型的姿态写入必须与 `resetPose()` 配平（实例是全局共享的）。
     */
    private fun resolveDeployedSubWeaponArmAnchors(stack: ItemStack, model: GeoGunModel, hand: InteractionHand) {
        val data = from(stack)

        if (!ActiveGun.isDeployed(data, true)) return

        val animation = FirstPersonRenderHandler.getActiveAnimationInstance(hand) as? GeoGunAnimationInstance
        // 宿主枪自己换弹/近战 → 让位。状态还没解析出来（等同没有主武器动画）时照旧接管。
        if (animation?.currentGunState?.takesHandAway == true) return

        val (slot, definition) = findSubWeapon(data) ?: return
        val modelPath = definition.model ?: return
        val boneName = AttachmentSlots.mountBoneOf(slot, definition) ?: return
        val pose = animation?.subWeaponIdlePose() ?: return
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return
        val mountTransform = model.getGlobalTransform(boneName) ?: return

        try {
            // 动画文件里的平移是"相对绑定姿势的偏移"，不先垫绑定就会把绑定不为 0 的骨骼送到父节点原点
            attachmentModel.applyPose(BLENDER.blend(attachmentModel.getBindPose(), pose))
            deployedArmAnchors = resolveSubWeaponHandAnchors(attachmentModel, mountTransform)
            deployedArmKey = "idle:${animation.subWeaponIdleClipName ?: ""}"
        } finally {
            attachmentModel.resetPose()
        }
    }

    /**
     * 本帧真正要交给 `GeoGunModel.renderHands` 的手臂锚点
     */
    private fun resolveArmAnchorsForDraw(
        model: GeoGunModel,
        transformType: ItemDisplayContext
    ): Map<HumanoidArm, Matrix4f> {
        if (!transformType.firstPerson()) return emptyMap()

        val reloading = subWeaponHandAnchors.isNotEmpty()
        val sourceKey = when {
            reloading -> "reload:${subWeaponAnchorKey ?: ""}"
            deployedArmAnchors.isNotEmpty() -> deployedArmKey
            else -> null
        }
        val target = if (reloading) subWeaponHandAnchors else deployedArmAnchors

        // 来源换了就从"上一帧真正画出去的那个矩阵"开始插值；一律从 0 开始，
        // 因为非部署时这条路径根本不画（记录永远是空的），跳过淡入就等于每次切出副武器都瞬移
        if (sourceKey != armAnchorSourceKey) {
            armAnchorSourceKey = sourceKey
            armAnchorFadeFrom.clear()
            if (armAnchorShown.isNotEmpty()) armAnchorFadeFrom.putAll(armAnchorShown)
            armAnchorFade = 0f
        } else {
            val delta = Minecraft.getInstance().deltaFrameTime.coerceIn(0f, MAX_FRAME_DELTA_TICKS)
            armAnchorFade = (armAnchorFade + delta / ARM_ANCHOR_FADE_TICKS).coerceAtMost(1f)
        }

        if (sourceKey == null && armAnchorFade >= 1f) {
            // 淡出结束：清掉记录，免得"换了一把枪再切出副武器"的淡入从上一把枪的手位起步
            armAnchorShown.clear()
            return emptyMap()
        }

        val shown = EnumMap<HumanoidArm, Matrix4f>(HumanoidArm::class.java)
        for ((arm, boneName) in SUB_WEAPON_HAND_BONES) {
            // 配件没有的骨骼（`righthand_pos` 目前只有手持模型有）退回宿主枪自己那一根，
            // 显式算出来是为了让它也能参与插值
            val gunBone = model.getGlobalTransform(boneName)?.let { Matrix4f(it) }
            val goal = target[arm] ?: gunBone ?: continue
            // 起点：上一帧真正画出去的那个矩阵；没有就退回宿主枪自己的骨骼（还没接管过时手所在的位置）
            val from = armAnchorFadeFrom[arm] ?: gunBone
            shown[arm] = if (from == null || armAnchorFade >= 1f) {
                Matrix4f(goal)
            } else {
                fadeAnchor(from, goal, armAnchorFade)
            }
        }

        armAnchorShown.clear()
        armAnchorShown.putAll(shown)
        return shown
    }

    /**
     * 两个锚点之间的插值：平移线性、旋转 `nlerp`、缩放线性，用 `smoothstep` 缓入缓出。
     *
     * ⚠ 缩放必须一起插：附件骨骼可以带 `scale` 通道（副武器的 `lefthand` 就 key 了 `[1, 1.5, 1]`），
     * 丢掉缩放会让换弹进出时光画回原长，成为唯一的跳变源。
     */
    private fun fadeAnchor(from: Matrix4f, to: Matrix4f, progress: Float): Matrix4f {
        val t = progress * progress * (3f - 2f * progress)
        val translation = from.getTranslation(Vector3f()).lerp(to.getTranslation(Vector3f()), t)
        val rotation = from.getUnnormalizedRotation(Quaternionf())
            .nlerp(to.getUnnormalizedRotation(Quaternionf()), t)
        val scale = from.getScale(Vector3f()).lerp(to.getScale(Vector3f()), t)
        return Matrix4f().translationRotateScale(translation, rotation, scale)
    }

    /**
     * 副武器模型自己的 `flare` 骨骼在枪姿态空间里的变换，让副武器开火时的枪口焰与枪口烟挂在**它的**枪口上。
     *
     * 组合方式与渲染那条路径一致（挂点骨骼 × 配件模型里的骨骼）；取不到时返回 `null`，
     * 调用方不会退回宿主枪的 `flare`，而是干脆不画这一簇火焰。
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

    /** 副武器开火时的枪口焰缩放：与枪口配件共用配件 json 里的 `MuzzleFlashScale` 字段。 */
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
     * 渲染的 `stack` 是不是**本地玩家自己手里**那把枪（主手）。
     *
     * 掉落在地上的、摆在展示框里的、别人手里的枪都是别的对象，好办；麻烦的是第一人称：
     * `FirstPersonRenderHandler` 渲染时传下来的是动画实例持有的那份 stack
     * （`GeoGunAnimationInstance.currentItem()`），而它每个客户端 tick 才由 `updateItem` 刷新一次，
     * 换枪过渡期间渲染的更是上一个实例里的旧对象。服务端每次同步手持槽（开枪改弹药、热量、
     * 各种计时器）都会把客户端手上的 ItemStack **换成新对象**（见 `GunData.DATA_CACHE` 的注释），
     * 于是同步之后到下一次 `updateItem` 之间的那几帧里身份对不上——只看对象身份的脚本会以为
     * 枪不在手上，脚架被压回 bind 姿态、下一帧又展开，第一人称看到的就是在收起/展开之间横跳。
     * [GeoGunAnimationInstance.shouldSpin] 早就为同一个坑改成比物品类型了。
     *
     * 所以分两种情况：对象身份成立（第三人称与正常的第一人称帧）直接用；
     * 第一人称下额外接受"渲染的是本地玩家主手 + 同一种物品"。第一人称入口只会画本地玩家自己的手，
     * 所以这个放宽不会波及世界上的同型号枪——掉落物/展示框/别人手里走的是普通物品渲染，
     * [localFirstPersonHand] 为 `null`，仍然一律判否。
     *
     * 换枪动画期间渲染的是旧 stack：换了另一种枪时物品对不上、直接判否而不是平滑过渡，
     * 与之前的行为一致，可以接受。
     *
     * 凡是读**客户端全局状态**（只描述本地玩家自己的视角，不写在枪自己的 tag 里）的脚本钩子都要过这一关，
     * 否则世界上每一把同型号枪都会跟着本地玩家的动作一起动。逐物品的属性（如 [scriptHeat]）不需要。
     */
    private fun isLocalPlayerGun(stack: ItemStack): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        val held = player.mainHandItem
        return held === stack
                || (localFirstPersonHand == InteractionHand.MAIN_HAND
                && !held.isEmpty
                && held.item === stack.item)
    }

    /**
     * 脚架展开进度：0 为收起，1 为完全展开。
     *
     * 直接复用 `bipod_view` 定位点用的 [ClientEventHandler.bipodViewTime]，这样子骨骼的翻转与
     * 卧姿视角过渡天然同步，脚本里不需要自己再做一次插值。
     *
     * 但它描述的是**本地玩家自己**的持枪视角过渡，是客户端全局的一份状态，所以只有他手里那把枪
     * 能读到，别的枪一律返回 0、也就是保持收起——否则世界上每一把同型号枪都会跟着本地玩家的卧姿
     * 一起展开。判定见 [isLocalPlayerGun]。
     */
    open fun scriptBipodProgress(stack: ItemStack): Double {
        return if (isLocalPlayerGun(stack)) ClientEventHandler.bipodViewTime else 0.0
    }

    /**
     * 瞄准推进度：0 为腰射，1 为完全瞄准，就是 [ClientEventHandler.zoomTime] 的**线性**原值。
     *
     * ⚠ 不要再对它套 `aimingProgress`（EASE_IN_OUT_QUINT）：[GeoGunRenderer] 内部读同一个量时套了
     * 曲线，脚本里写的门槛（"0.3 之后才出现"）要按这里的原值来定。
     *
     * 和 [scriptBipodProgress] 同理，它描述的是本地玩家自己的视角过渡，只有他手里那把枪能读到，
     * 别的枪一律返回 0——否则本地玩家一按瞄准键，世界上每一把同型号枪都会跟着亮起来。
     */
    open fun scriptZoomTime(stack: ItemStack): Double {
        return if (isLocalPlayerGun(stack)) ClientEventHandler.zoomTime else 0.0
    }

    /**
     * 单调推进的游戏时间，单位 **tick**（`level.gameTime` 加上本帧的 `frameTime` 做帧间插值，
     * 20 tick = 1 秒）。给脚本算"随时间匀速自转"这类效果用。
     *
     * 之所以给的是时间轴而不是像 [scriptHeat] 那样的逐帧增量：自转角度算成时间的函数就不需要任何记忆，
     * 于是既不用担心顶层变量是全世界同型号枪共用的一份，也不用担心 JsState 按 ItemStack 对象身份
     * 记忆会在每次服务端同步（换对象）时被清零，还顺带免疫"同一帧被画几次就走几倍"。
     *
     * 时间是**世界时间**：单人游戏暂停时它停住，自转也跟着停，符合直觉。
     */
    open fun scriptGameTime(): Double {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return 0.0
        return level.gameTime + mc.frameTime.toDouble()
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

        val multiply = 1 - Mth.clamp(GunResource.compute(stack).zoomingTranslateMultiply, 0f, 1f)

        var rotationScale = (1f - 0.5f * zoomTime * multiply).coerceAtLeast(0.05f)
        var rotationScaleX = (1f - 0.97f * zoomTime * multiply).coerceAtLeast(0.05f)
        var rotationScaleY = (1f - 0.97f * zoomTime * multiply).coerceAtLeast(0.05f)
        var rotationScaleZ = (1f - 0.7f * zoomTime * multiply).coerceAtLeast(0.05f)
        var positionScale = (1f - 0.95f * zoomTime * multiply).coerceAtLeast(0.05f)
        var positionScaleX = (1f - 0.95f * zoomTime * multiply).coerceAtLeast(0.05f)
        var positionScaleZ = (1f - 0.96f * zoomTime * multiply).coerceAtLeast(0.05f)

        if (!data.reloading()) {
            rotationScale = (1f - 0.5f * zoomTime * multiply).coerceAtLeast(0.05f)
            rotationScaleX = (1f - 0.55f * zoomTime * multiply).coerceAtLeast(0.05f)
            rotationScaleY = (1f - 0.2f * zoomTime * multiply).coerceAtLeast(0.05f)
            rotationScaleZ = (1f - 0.2f * zoomTime * multiply).coerceAtLeast(0.05f)
            positionScale = (1f - 0.4f * zoomTime * multiply).coerceAtLeast(0.05f)
            positionScaleX = (1f - 0.5f * zoomTime * multiply).coerceAtLeast(0.05f)
            positionScaleZ = (1f - 0.82f * zoomTime * multiply).coerceAtLeast(0.05f)
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
     * 采一份**分划骨骼（`division`）在屏幕上的位置**交给命中提示 HUD
     * （[ClientRenderHandler.scopeReticleScreen]，消费方 `CrossHairOverlay`）。
     *
     * ## 为什么要它
     * 开火动画（还有代码后坐、枪身晃动）拧的是**枪**，座在枪上的分划跟着被拧走；而命中提示一直钉在
     * 屏幕正中。连射时玩家看到的是"分划在这儿、命中提示在那儿"两个点各画各的。把提示挪到分划骨骼上，
     * 两者就重新咬合 —— 这也和准星的弹道偏移同源：分划本来就在告诉玩家"枪现在指着哪儿"。
     *
     * ## 锚点 = `division` 骨骼**关节**（pivot）的当前位形
     * 骨骼名取自 `ScopeMode.divisionBone()`（多档镜是 `division_<index>`）。
     *
     * 写法是 `getGlobalTransform(name).transformPosition(Vector3f(0f, 0f, 0f))` —— **骨骼本地原点**，
     * 也就是那份变换的平移列。树模型（SBM v2）里几何是按"**相对自己的 pivot**"烘出来的
     * （`TreeBedrockModelBaker.createCubes` 里 `(origin − absolutePivot)/16`），骨骼局部变换是
     * `T(pos/16)·R·S`（`TreeBoneDefinition.rotateAroundPivot()` 恒为 `false`，根本没有 pivot 项），
     * 而 `pos` 取的是 `bind = absolutePivot − parentAbsolutePivot` 逐级套上去的 ——
     * 于是本地原点的像就是 pivot 的当前位形，旋转也是绕它转的。
     *
     * ⚠ **不要**喂 `getBonePivot(name)`：那是 pivot 在**模型空间**的绝对坐标，而这份矩阵期望的是
     * pivot **相对**坐标（本地原点）。两者相加等于把 pivot 的偏移算两遍，锚点会漂到
     * `translation + p`（bind 姿态下正好是 pivot 的**两倍**）。`scope_pu` 就是被这个坑中的：
     * 它的 pivot 在 (0, 0.2845, −1.3873) —— 分划面就在 1.39 格处 —— 于是提示被顶到光轴上方
     * 0.28 格、深度翻倍，投到屏幕上就是镜筒上沿那一带。
     * 另外这也解释了为什么"改 pivot 修不好"：改 pivot 时**分划几何跟着一起挪**（几何按 pivot 相对烘），
     * 而锚点挪的是两倍，越改越偏。
     *
     * ⚠ 这个点是**骨骼的关节**，不保证落在分划图案的几何中心上：美术给 `division` 摆 pivot 时有一半
     * （eotech / red dot / ranger / bruiser / okp_7）摆在了模型原点，ACOG 摆在 z = −1.55 而分划面在
     * −6.18，sniper 摆在 −27.93。要让提示正好压在准星上，得改模型的 pivot —— 这是美术侧的活，
     * 代码这边按"division 骨骼在哪儿，提示就画在哪儿"执行。
     *
     * ## 为什么这里能直接给出像素
     * 用的是**采样这一刻生效的投影矩阵**（`RenderSystem.getProjectionMatrix()`）——分划就是它画出来的，
     * 手部 pass 那个固定基准 FOV 已经在里面了。存像素而不是存骨骼位姿，消费方就不必再挑 fov 或矩阵
     * （对比 [submitMuzzleSample] 存向量、由世界 pass 那边补 FOV 的做法）。
     *
     * @param poseStack 已经乘上这个瞄具**槽位变换**的 pose stack —— 也就是画分划时的那一份
     * （相机空间 = 附件模型空间的外层）
     * @param scope 本帧真正在走模板的那一档瞄具，锚点骨骼名取自它的 `divisionBone()`
     */
    private fun submitScopeReticleSample(poseStack: PoseStack, scope: ScopeRenderData) {
        // 分划是这个时候才开始画的：没画出来就没有"分划位置"可言（腰射、还没抬到位）。
        // 这里只是**不采样**，样本会自己按 TTL 过期；"松开右键那几帧"由消费方把关（同一个常量）。
        if (ClientEventHandler.zoomTime < BedrockAttachmentModel.DIVISION_MIN_ZOOM) return

        val boneName = scope.scopeMode.divisionBone()
        val boneTransform = scope.model.getGlobalTransform(boneName) ?: return

        val screen = BedrockBoneCoordinateTool.firstPersonBonePointToScreen(
            poseStack,
            boneTransform,
            // 骨骼本地原点 = pivot 的当前位形（几何就是按"相对 pivot"烘的，见上面的 KDoc）。
            // 这里**不能**换成模型空间那根 pivot 绝对坐标，否则等于算两遍偏移。
            Vector3f(),
            RenderSystem.getProjectionMatrix(),
            Minecraft.getInstance().window.guiScaledWidth,
            Minecraft.getInstance().window.guiScaledHeight
        )

        // 分划骨骼自己都在屏幕外时不给样本：命中提示跟着飞出屏幕，等于把命中反馈弄丢了，
        // 还不如退回屏幕中心（样本过期 → 消费方走原逻辑）
        if (screen == null || !screen.visible) return

        ClientRenderHandler.scopeReticleScreen = Vector2f(screen.x, screen.y)
    }

    private fun submitMuzzleSample(
        poseMatrix: Matrix4f,
        model: GeoGunModel,
        stack: ItemStack,
        subWeaponFlare: Matrix4f?,
        attachmentMuzzleTransform: Matrix4f?,
    ) {
        val flareTransform = MuzzleFlashRenderer.resolveFlareTransform(
            model,
            stack,
            attachmentMuzzleTransform,
            subWeaponFlare
        ) ?: run {
            ClientRenderHandler.muzzleDirection = null
            return
        }
        val muzzleTransform = Matrix4f(poseMatrix).mul(flareTransform)
        val axis = muzzleTransform.transformDirection(0f, 0f, -1f, Vector3f())
        val axisScale = axis.length()
        if (axisScale < 1e-6f) {
            ClientRenderHandler.muzzleDirection = null
        } else {
            // 相机空间 → 世界轴。两份矩阵都只有旋转，单位向量换算过去还是单位向量
            val worldAxis = cameraRotationInverse().transformDirection(
                axis.x / axisScale, axis.y / axisScale, axis.z / axisScale, Vector3f()
            )
            ClientRenderHandler.muzzleDirection =
                Vec3(worldAxis.x.toDouble(), worldAxis.y.toDouble(), worldAxis.z.toDouble())
        }

        if (!MuzzleFlashRenderer.isFiring()) return

        // 枪口在相机空间里的位置
        val muzzle = muzzleTransform.getTranslation(Vector3f())

        val adjusted = BedrockBoneCoordinateTool.adjustViewDepthForFov(
            Vec3(muzzle.x.toDouble(), muzzle.y.toDouble(), muzzle.z.toDouble()),
            ClientEventHandler.handFov.toFloat(),
            ClientEventHandler.fov.toFloat()
        )

        // `PoseStack.translate` 加的是**世界轴**上的位移（相机旋转在更外层），所以把这段偏移转到世界轴
        val offset = cameraRotationInverse().transformPosition(
            Vector3f(adjusted.x.toFloat(), adjusted.y.toFloat(), adjusted.z.toFloat())
        )
        ClientRenderHandler.bulletRenderOffset =
            Vec3(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())
    }

    /** 把 0..1 的瞄准进度换算成真正用于插值的值 */
    private fun aimingProgress(rawProgress: Double): Float =
        AnimationCurves.EASE_IN_OUT_QUINT.apply(rawProgress.coerceIn(0.0, 1.0)).toFloat()

    open fun computeViewTransform(
        model: GeoGunModel,
        stack: ItemStack,
        scopeRender: ScopeRenderData? = null,
        hand: InteractionHand = InteractionHand.MAIN_HAND
    ): Matrix4f? {
        // 基准取自 [idleViewAnchor]（`renderModel` 每帧算一次），部署副武器期间它是副武器自己的 `idle_view`；
        // 为空就退回模型自己的 `idle_view`。⚠ 这里只读不推进，本函数每帧会被调两次
        val idleViewTransform = idleViewAnchor ?: model.getGlobalTransform(IDLE_VIEW_BONE) ?: return null
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

        // 瞄准位形的优先顺序：① 副武器自己的 `iron_view`（只在副武器被切出来时才可能取到）
        // ② 宿主枪的瞄具分划 ③ 宿主枪的机瞄
        val deployed = playerDeployedSubWeapon(stack)
        val ironViewTransform = (if (deployed) resolveSubWeaponAimTransform(stack, model) else null)
            ?: scopeViewTransform(scopeRender, hand)
            ?: model.getGlobalTransform(IRON_VIEW_BONE)
            ?: return hipViewTransform
        return blendViewTransform(hipViewTransform, Matrix4f(ironViewTransform), zoom)
    }

    /**
     * 本地玩家当前操控的是不是这把枪上的副武器（部署状态由服务端写在主手枪的状态里）。
     */
    private fun playerDeployedSubWeapon(stack: ItemStack): Boolean {
        if (localPlayer == null) return false
        val gun = from(stack)
        return ActiveGun.isDeployed(gun, true)
    }

    /** 副武器自己的瞄准位形：附件模型的 `iron_view`（约定骨骼，无配置字段）× 挂点骨骼 */
    open fun resolveSubWeaponAimTransform(stack: ItemStack, model: GeoGunModel): Matrix4f? {
        val (slot, definition) = findSubWeapon(from(stack)) ?: return null

        val modelPath = definition.model ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        val boneName = AttachmentSlots.mountBoneOf(slot, definition) ?: return null

        val aimTransform = attachmentModel.getGlobalTransform(SubWeaponInfo.VIEW_BONE) ?: return null
        val mountTransform = model.getBindGlobalTransform(boneName) ?: return null

        return Matrix4f(mountTransform).mul(aimTransform)
    }

    /**
     * 副武器自己的 hip 位形：附件模型静态的 `idle_view` 骨骼 × 挂点 bind 变换；
     * 与 [resolveSubWeaponAimTransform] 成对，缺失时同样返回 `null` 让调用方回退。
     */
    open fun resolveSubWeaponIdleTransform(stack: ItemStack, model: GeoGunModel): Matrix4f? {
        val (slot, definition) = findSubWeapon(from(stack)) ?: return null

        val modelPath = definition.model ?: return null
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return null
        val boneName = AttachmentSlots.mountBoneOf(slot, definition) ?: return null

        val idleTransform = attachmentModel.getGlobalTransform(SubWeaponInfo.IDLE_VIEW_BONE) ?: return null
        val mountTransform = model.getBindGlobalTransform(boneName) ?: return null

        return Matrix4f(mountTransform).mul(idleTransform)
    }

    /**
     * [updateSubWeaponIdleView] 用的来源标识（`"<挂点骨骼>@<附件模型路径>"`）：
     * 换掉副武器时基准也会变，那一下同样该淡过去；取不到副武器返回 `null`。
     */
    private fun subWeaponIdleViewKey(stack: ItemStack): String? {
        val (slot, definition) = findSubWeapon(from(stack)) ?: return null
        val modelPath = definition.model ?: return null
        val boneName = AttachmentSlots.mountBoneOf(slot, definition) ?: return null
        return "$boneName@$modelPath"
    }

    /**
     * 推进并解析本帧的 hip 位形基准（[idleViewAnchor]）：宿主枪自己的 `idle_view`，
     * 或正在部署的那把副武器的（[resolveSubWeaponIdleTransform]），两者之间用
     * [ARM_ANCHOR_FADE_TICKS] 与 [fadeAnchor] 同一个 `smoothstep` 抹平。
     *
     * ⚠ 每帧只调一次（`renderModel` 的第一人称块里）：[computeViewTransform] 每帧会被调两次。
     * ⚠ 收尾判据是 [IdleViewFade.fade] 而不是 [IdleViewFade.key]：来源刚变成 `null` 的那一帧是"淡出开始"。
     */
    private fun updateSubWeaponIdleView(stack: ItemStack, model: GeoGunModel, hand: InteractionHand) {
        val subWeaponIdle = if (playerDeployedSubWeapon(stack)) {
            resolveSubWeaponIdleTransform(stack, model)
        } else {
            null
        }
        val mainIdle = model.getGlobalTransform(IDLE_VIEW_BONE)

        if (subWeaponIdle == null && mainIdle == null) {
            // 宿主枪的 `idle_view` 也没有（这种枪不存在，只是别让它变成 NPE）：交回 `null`
            idleViewFades.remove(hand)
            return
        }

        val state = idleViewFades.getOrPut(hand) { IdleViewFade() }
        val key = if (subWeaponIdle != null) subWeaponIdleViewKey(stack) else null
        val target = subWeaponIdle ?: mainIdle!!

        if (key != state.key) {
            // 来源换了：从上一帧画出去的基准起步，没有记录就从宿主枪自己的位形起步（淡入从原地开始）
            state.key = key
            state.from = state.shown?.let { Matrix4f(it) } ?: mainIdle?.let { Matrix4f(it) }
            state.fade = 0f
        } else {
            val delta = Minecraft.getInstance().deltaFrameTime.coerceIn(0f, MAX_FRAME_DELTA_TICKS)
            state.fade = (state.fade + delta / ARM_ANCHOR_FADE_TICKS).coerceAtMost(1f)
        }

        if (key == null && state.fade >= 1f) {
            // 淡出结束：交 `null` 走老路径，清掉记录（留着会让下一次淡入从上一把枪的视点起步）
            state.shown = null
            return
        }

        val from = state.from
        val shown = if (from == null || state.fade >= 1f) {
            // 到位就直接用目标矩阵，不留残差
            Matrix4f(target)
        } else {
            val t = state.fade * state.fade * (3f - 2f * state.fade)
            blendViewTransform(from, target, t)
        }
        state.shown = Matrix4f(shown)
        idleViewAnchor = shown
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
     * 返回当前正在编辑的配件槽位对应的定位骨骼名（`AttachmentSlots` 登记），未选中或未支持时返回 null。
     * 弹药类型不是槽位、另行处理，[model] 用来挑出模型实际拥有的那根骨骼。
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
     * 返回改装聚焦的目标偏移（模型空间，相对本帧的 hip 位形基准）：聚焦点取当前编辑配件的定位点往
     * Z 轴负方向偏移 [EDIT_FOCUS_Z_OFFSET]，未选中配件时返回浮动预览的鼠标平移偏移。
     *
     * ⚠ 基准必须与 [computeViewTransform] 加回去的那个一致（都用 [idleViewAnchor]），否则部署副武器时相机会偏。
     */
    private fun computeEditFocusOffset(model: GeoGunModel): Vector3f? {
        if (!ClientEventHandler.isEditing) return null
        val boneName = attachmentFocusBone(model) ?: return computeUnfocusedPanOffset()

        val idleView = idleViewAnchor ?: model.getGlobalTransform(IDLE_VIEW_BONE) ?: return null
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

    /**
     * 配件数据里的基础三轴旋转（[AttachmentDefinition.rotation]）矩阵；没写或全 `0` 时返回 `null`。
     *
     * ⚠ 要乘在挂点变换**之后**（`mount.mul(rotation)`），字段含义是"挂上去之后再整件转一下"；
     * 换算与模型加载时一致：X、Y 取负、Z 不取负，按 `ZYX` 顺序合成。
     */
    fun attachmentRotation(definition: AttachmentDefinition): Matrix4f? {
        val rotation = definition.rotation ?: return null
        if (rotation.isIdentity) return null

        return Matrix4f().rotate(
            Quaternionf().rotateZYX(
                rotation.z * Mth.DEG_TO_RAD,
                -rotation.y * Mth.DEG_TO_RAD,
                -rotation.x * Mth.DEG_TO_RAD,
            )
        )
    }

    /** [attachmentRotation] 直接乘进 `PoseStack` 的写法，给不需要那份矩阵本身的那几条渲染路径用 */
    fun mulAttachmentRotation(poseStack: PoseStack, definition: AttachmentDefinition) {
        attachmentRotation(definition)?.let { mulPoseWithNormal(poseStack, it) }
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
        /** 枪上装着的副武器：`(槽位, 配件定义)`，没装返回 `null` */
        private fun findSubWeapon(data: GunData): Pair<AttachmentSlot, AttachmentDefinition>? {
            for (slot in AttachmentSlots.ALL) {
                val attachmentId = data.attachment.id(slot.type) ?: continue
                val definition = AttachmentDefinition.from(attachmentId) ?: continue
                if (definition.subWeapon != null) return slot to definition
            }
            return null
        }

        /**
         * 部署中的副武器有没有自己的瞄准位形（附件模型里有没有 `iron_view`）。
         *
         * 位形回退到宿主枪时倍率也必须跟着用宿主枪的，所以 FOV 那一侧（`ClientEventHandler.onFovUpdate`）
         * 与渲染这边要问同一个判据；它不吃 `GeoGunModel` 是因为 FOV 侧只有枪的数据。
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

        /** 加长护木里那对"延长段"锚点，见 [renderBarrelExtension] */
        private const val NEW_FLARE_BONE = "new_flare"
        private const val NEW_MUZZLE_BONE = "new_muzzle_pos"

        /** 四面导轨的槽位，见 [shouldShowCustomHandGuard] */
        private val RAIL_SLOTS = listOf(
            AttachmentType.LOWER_RAIL,
            AttachmentType.UPPER_RAIL,
            AttachmentType.LEFT_RAIL,
            AttachmentType.RIGHT_RAIL
        )

        /** **主武器**模型里的整体骨骼。副武器换弹时"反推"过来的运动加在它上面（见 [resolveSubWeaponFollowPose]） */
        private const val GUN_ROOT_BONE = "root"

        /** **配件**模型里的整体骨骼。美术的换弹动画把"整把武器在手里怎么动"写在这一根上 */
        private const val ATTACHMENT_ROOT_BONE = "root"

        /** 副武器换弹时接管手臂的两根骨骼（**附件**模型里的）；取不到就退回宿主枪那一根 */
        private val SUB_WEAPON_HAND_BONES = listOf(
            HumanoidArm.LEFT to "lefthand_pos",
            HumanoidArm.RIGHT to "righthand_pos",
        )

        /**
         * 手臂锚点换来源时的淡入淡出时长，单位 tick（3 tick ≈ 0.15 秒），见 [resolveArmAnchorsForDraw]。
         */
        private const val ARM_ANCHOR_FADE_TICKS = 3f

        /** 单帧步进的上限（tick）：不夹住的话卡顿时淡入淡出会一帧走完，看起来还是硬切 */
        private const val MAX_FRAME_DELTA_TICKS = 0.8f

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

        private const val MAX_SCOPE_RANGE = 500.0

        private val BLENDER: EulerAdditiveBlender =
            SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
        private val MERGE_BLENDER = NoAllocMergeBlender()
    }
}
