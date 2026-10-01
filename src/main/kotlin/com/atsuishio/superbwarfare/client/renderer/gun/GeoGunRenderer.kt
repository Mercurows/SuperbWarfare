package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.gun.GeoGunAnimationInstance
import com.atsuishio.superbwarfare.client.charm.CharmRuntime
import com.atsuishio.superbwarfare.client.charm.CharmSnapshot
import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoReadout
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.ARM_ANCHOR_FADE_TICKS
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.CUSTOM_HAND_GUARD_BONE
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.EDIT_FOCUS_Z_OFFSET
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.MERGE_BLENDER
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.OEM_HAND_GUARD_BONE
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.SCOPE_STENCIL_START_PROGRESS
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer.Companion.findSubWeapon
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
import com.atsuishio.superbwarfare.resource.gun.pojo.BuiltinScopeInfo
import com.atsuishio.superbwarfare.resource.gun.pojo.ItemDisplayInfo
import com.atsuishio.superbwarfare.resource.model.AttachmentModelReloadListener
import com.atsuishio.superbwarfare.script.GunScriptManager
import com.atsuishio.superbwarfare.tools.ActiveGun
import com.atsuishio.superbwarfare.tools.RenderDistanceHelper
import com.atsuishio.superbwarfare.tools.deltaFrameTime
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
     * `renderRegisteredAttachments` 复用。
     *
     * 必须是**每次 renderModel 开头清空**的：它绑定了"这一帧用的是哪个主武器模型/哪只手"，
     * 跨帧留着会把上一帧的修正套到这一帧（两只手、或者第三人称那一遍）上。
     */
    private var subWeaponFollow: SubWeaponFollowPose? = null

    /**
     * 本帧的副武器换弹**手臂锚点**（[resolveSubWeaponHandAnchors]）：副武器换弹时手臂该待在哪儿，
     * 由附件模型自己的 `lefthand_pos`/`righthand_pos` 给。
     *
     * 与 [subWeaponFollow] 一样每帧开头清空，并且**必须在附件渲染窗口里填、在窗口外读**
     * —— 渲染顺序是 `renderRegisteredAttachments`（填）→ 合并（[resolveArmAnchorsForDraw]）→
     * `model.renderToBuffer`（读，画手臂）。
     *
     * 它现在只是"**换弹期间的覆盖**"，优先级高于 [deployedArmAnchors]（部署接管）；
     * 空表 = "这一路没有话说"，由部署接管或主武器自己的骨骼决定手臂位置。
     */
    private var subWeaponHandAnchors: Map<HumanoidArm, Matrix4f> = emptyMap()

    /**
     * [subWeaponHandAnchors] 的来源标识（换弹 clip 名），只用于判断"来源换了没有"以决定要不要淡入。
     *
     * 它**每帧在 `renderModel` 之外**（`renderRegisteredAttachments` 里）才被填上，所以只能在
     * 那一处连同锚点一起写 —— 见 [resolveArmAnchorsForDraw] 里 `"reload:…"` 那个前缀。
     */
    private var subWeaponAnchorKey: String? = null

    /**
     * 本帧的**部署期间手臂锚点**（[resolveDeployedSubWeaponArmAnchors]）：副武器被 G 键切出来
     * 之后，手臂就挂在**副武器自己**的 `lefthand_pos`/`righthand_pos` 上，直到再切回去。
     *
     * 与 [subWeaponFollow] 一样每帧开头清空；空表 = "没有在部署副武器，手臂照旧归主武器管"。
     */
    private var deployedArmAnchors: Map<HumanoidArm, Matrix4f> = emptyMap()

    /** 本帧部署接管的**来源标识**（`"idle:<clip名>"`），只用于判断"来源换了没有"以决定要不要淡入 */
    private var deployedArmKey: String? = null

    /**
     * **上一次真正画出去**的手臂锚点，以及它的来源标识与淡入进度。
     *
     * 这三个是**跨帧**字段（上面几个都是每帧清空的），因为"来源切换"这件事本身是跨帧的：
     * 主武器自己换弹时手要去抓弹匣、换完再回到配件上，装卸配件时手也会换地方，这些切换都不该是硬跳。
     * 来源标识一变就把"上一帧画的那个矩阵"记进 [armAnchorFadeFrom]，用 [armAnchorFade] 在
     * [ARM_ANCHOR_FADE_TICKS] 内插值过去。
     *
     * ⚠ **为什么放在渲染器上还算安全**：这套锚点只在 `renderHand`（即 `transformType.firstPerson()`）
     * 时才解析，而第一人称只会画**本地玩家自己**的手 —— 第三人称、掉落物、展示框、别人手里的枪
     * 都走不到这里（`GeoGunModel.renderToBuffer` 里 `if (renderHand)` 那一道）。
     * 与 [subWeaponFollow] / [subWeaponHandAnchors] 依赖的是同一条性质。
     * 另外"是否部署了副武器"（[ActiveGun.isDeployed]）本身也只对**本地玩家**成立。
     */
    private var armAnchorSourceKey: String? = null
    private var armAnchorFade = 1f
    private val armAnchorFadeFrom = EnumMap<HumanoidArm, Matrix4f>(HumanoidArm::class.java)
    private val armAnchorShown = EnumMap<HumanoidArm, Matrix4f>(HumanoidArm::class.java)

    /**
     * 本帧的 **hip 位形基准**（`idle_view`，或部署副武器时它在副武器上的替代）。
     *
     * `null` = "照旧用模型自己的 `idle_view`"，也就是**改动前逐字的那条老路径**：没装副武器、
     * 副武器模型里没有这支骨骼（[resolveSubWeaponIdleTransform] 返回 `null`）、
     * 或者这一帧根本不是第一人称 —— 第三人称 / GUI / 掉落物 / 展示框 / LOD 都在 `renderModel`
     * 的第一人称块之外，压根不会填它。与 [deployedArmAnchors] 一样**每帧开头清空**。
     *
     * ⚠ **只有 [updateSubWeaponIdleView] 会写它，而且每帧只写一次**（在 `renderModel` 的第一人称块里）。
     * 消费者（[computeViewTransform] / [computeEditFocusOffset]）**只读、不推进**。
     * 不能把推进写进 [computeViewTransform]：它每帧被调**两次**（定位一次、`zoomPivot` 一次），
     * 3 刻的淡入会被走成 1.5 刻 —— "速率跟左手一样"当场失守（§11.11.10）。
     */
    private var idleViewAnchor: Matrix4f? = null

    /**
     * 每只手的副武器 `idle_view` 淡入淡出状态（[updateSubWeaponIdleView]）。
     *
     * 与 [armAnchorSourceKey] 那套**同形同速**（都走 [ARM_ANCHOR_FADE_TICKS]，都用同一个
     * `smoothstep`），区别只在它插值的是**相机锚点**而不是手臂锚点；两者各管一摊、互不影响。
     *
     * ⚠ 按 [InteractionHand] 分开存（同 [scopeViewSmoothing]）：**同一把枪两手各拿一支**时
     * 命中的是**同一个渲染器实例**，一帧里 `renderModel` 会跑两遍 —— 用单份状态两只手会互相踩。
     * ⚠ 跨帧字段放在渲染器上还算安全的那条论证与 [armAnchorSourceKey] 完全相同：这条路径
     * 只在第一人称（本地玩家自己）才会走到。
     */
    private val idleViewFades = mutableMapOf<InteractionHand, IdleViewFade>()

    /** [idleViewFades] 的一份：来源标识、进度，以及"上一帧真正画出去的那个基准"。 */
    private data class IdleViewFade(
        /** `"<挂点骨骼>@<附件模型路径>"`；`null` = 主武器自己的 `idle_view` */
        var key: String? = null,
        /** 0 → 1；1 = 已经到位 */
        var fade: Float = 1f,
        /** 切换那一刻**屏幕上原本那个**基准（上一帧画出去的），插值的起点 */
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

            // 本帧的 hip 位形基准（部署副武器时换成副武器自己的 `idle_view`）。
            // ⚠ 位置不能挪：**必须早于** `updateEditFocus`（改装聚焦偏移是相对**同一个基准**算的，
            // 基准换了它也得跟着换，否则相机偏 |副武器 idle_view − 主武器 idle_view|）与
            // `applyFirstPersonPositioningTransform`（它正是读 [idleViewAnchor] 的那个消费者）。
            // ⚠ 也**只能每帧调一次**，理由见 [updateSubWeaponIdleView]。
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
        val attachmentRender = resolveMuzzleAttachmentRender(stack)
        val attachmentMuzzleTransform = attachmentRender?.let {
            resolveMuzzleAttachmentMuzzleTransform(stack, model, it)
        }
        val muzzleFlashScale = resolveMuzzleAttachmentMuzzleFlashScale(stack)

        val canStencil = transformType.firstPerson()
//                && !OculusCompat.isRenderingShadowPass()
                && bufferSource is MultiBufferSource.BufferSource
        val stencilScope = if (canStencil) findStencilScope(stack, model) else null
        var gunCulled = false
        var builtinScopeActive = false
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
                    bufferSource as MultiBufferSource.BufferSource,
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

        // **部署副武器期间的手臂接管**（§11.11.7.4）：只有副武器被切出来时手臂才挂到它自己
        // 那两根骨骼上。
        //
        // ⚠ **采样点必须尽可能晚**，和 `renderAttachments` 里那份换弹锚点对齐：挂点变换取自
        // 「本帧最终的主武器骨骼」，而这一帧里改写骨骼的步骤不止一处 —— `applyPose`、`applyCameraShake`
        // （瞄准时把 `root` 的平移按 (0.6, 0.5, 0.18)、欧拉角按 (0.45, 0.8, 0.8) 压缩）、以及脚本回调
        // `applyCustomAnimationsByScript`。在它们之前采样，手臂跟上的是**没被压缩过的**后坐，而枪身画的是
        // 压缩过的：实测 `ak_12.fire_sub_weapon` 瞄准射击时手会离枪 **≈0.27 方块**（27 厘米；随美术当前 clip 浮动），
        // 且误差随后坐曲线回落（0.27→0.23→0.08→0.02），看上去就是"左手随着动画飘"；
        // 腰射时压缩系数为 1，误差恰好 0.0000。见 `build/verify/VerifyArmShake.java`。
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
                // ⚠ 必须在 `renderAttachments` **之后**算：副武器换弹那份锚点是在里面填的，
                // 而它的优先级高于常驻接管（见 [resolveArmAnchorsForDraw]）。
                resolveArmAnchorsForDraw(model, transformType),
                // 同目录下的 `<贴图名>_e.png`，没有就返回 null（绝大多数枪都是这样），枪照旧只画一遍。
                // 按**最终选中的那张贴图**推，所以 LOD 贴图会自动去找 `gun_lod/` 里的 `_e`，
                // 不需要为两套贴图各写一份配置（见 [GunEmissiveTextures]）。
                GunEmissiveTextures.get(texture)
            )
        } finally {
            // 与上面 [resolveDeployedSubWeaponArmAnchors] 同一条约定：可见性是写在**共享**模型实例上的，
            // 中间抛异常也必须还原 —— 否则这几根骨骼会一直留在 hidden，直到下一次资源重载。
            if (savedOcularBones != null) model.builtinScopeRenderer.restoreOcularBones(savedOcularBones)
            model.builtinScopeRenderer.restoreDivisionBones(savedDivisionBones)
        }
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
            //
            // ⚠⚠ **姿态必须先和绑定姿势做加法混合再套**（[GeoGunModel.getBindPose] + `BLENDER`），
            // 口径与主武器那一行**逐字一致**。原因：动画文件里的平移量是**相对绑定姿势的偏移**，
            // 写 `0` 的意思是"停在这根骨骼自己的静止位置"；而 `BoneTreeInstance.applyPose` 是
            // **直接写** `BoneState.x/y/z`（不做混合），拿原始姿态直接套，`0` 就变成了"跑到父节点原点"。
            // 绑定为 0 的骨骼（`root`、以及绝大多数枪的骨）看不出差别，绑定不为 0 的立刻错位：
            // 下挂筒的 `projectile` 绑定 `(0, -1.1743, -6.5436)`（正是枪管轴线），clip 里 `[0,0,0]`
            // 会把**炮弹连同手**（`lefthand` 是它的子骨骼）抬到模型原点 ——
            // 实测偏上 0.0734、偏后 0.4090 方块，就是"炮弹错位、手臂乱飞"的成因。
            // 反过来，混合之后副武器与手持形态对同一份 clip 的解读完全相同，
            // 美术在手持形态上调好的动作可以原样搬过来。
            // 没被 key 的骨骼经混合后仍是绑定姿势；`resetPose()` 在 finally 里还原，漏了会串到别的枪上。
            //
            // ⚠⚠ **副武器换弹时这里要把姿态里的 `root` 通道摘掉**（[withoutBone]）：
            // "整把武器在手里怎么动"已经由主武器 `root` 的 `D` 承担了，而附件渲染用的挂点变换
            // 是**已姿态**的（上面那行 `model.getGlobalTransform(boneName)`），会跟着主武器一起走 ——
            // 所以附件这边只需要"相对整枪"的那些通道；再套一次 `root` 就会被推离枪身（§11.11.7.2）。
            val subWeaponAnimation = if (slot.type == AttachmentType.SUBWEAPON) {
                FirstPersonRenderHandler.getActiveAnimationInstance(hand) as? GeoGunAnimationInstance
            } else {
                null
            }
            val clipPose = subWeaponAnimation?.subWeaponReloadPose()
            val subWeaponPose = if (clipPose != null && subWeaponFollow?.modelPath == modelPath) {
                withoutBone(clipPose, attachmentModel.baseModel.getIndex(ATTACHMENT_ROOT_BONE))
            } else {
                clipPose
            }

            // 吊坠摆动：**只有本地玩家自己的第一人称**才推进物理。
            //
            // 判据用 [localFirstPersonHand] 而不是 `transformType`：这个入口拿不到 `transformType`，
            // 而那个字段正好就是"这一帧画的是本地玩家自己的手"的既有标记（见它的注释）。
            // 第三人称 / 掉落物 / 展示框 / GUI 都拿不到它，于是吊坠静静地垂着；
            // 阴影 pass 也不推进，免得一帧被推进两次。
            val charm = slot.type == AttachmentType.CHARM &&
                    localFirstPersonHand != null
//                    && !OculusCompat.isRenderingShadowPass()

            poseStack.pushPose()
            mulPoseWithNormal(poseStack, Matrix4f(mountTransform))
            var charmSnapshot: CharmSnapshot? = null
            try {
                if (subWeaponPose != null) {
                    attachmentModel.applyPose(BLENDER.blend(attachmentModel.getBindPose(), subWeaponPose))
                }
                // 手臂锚点要在**姿态还在实例上**的时候取（下面 `finally` 里就 `resetPose()` 了）。
                // 取到之后由 `GeoGunModel.renderHands` 用它代替主武器的同名骨骼 ——
                // 它**优先于**常驻接管（[resolveArmAnchorsForDraw] 里的优先级说明）。
                if (subWeaponPose != null) {
                    subWeaponHandAnchors = resolveSubWeaponHandAnchors(attachmentModel, mountTransform)
                    subWeaponAnchorKey = subWeaponAnimation?.subWeaponReloadClipName
                }
                // 摆动姿态必须在这之前写进骨骼：它改的是 `string` / `charm` 两根骨骼的
                // `x/y/z + rotation`，`renderToBuffer` 只是照着画。
                // ⚠ 挂点变换已经乘在 `poseStack` 上了，所以这里交出去的正是"模型局部 → 视图空间"。
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
                attachmentModel.renderToBuffer(
                    poseStack, bufferSource, texture, packedLight, packedOverlay,
                    null, resolveAmmoReadout(stack, definition.effectiveAmmoBar(), definition.effectiveTextShow())
                )
            } finally {
                // 附件模型实例是全局共享的，写进去的摆动姿态必须还原
                if (charmSnapshot != null) CharmRuntime.revert(attachmentModel, charmSnapshot)
                if (subWeaponPose != null) attachmentModel.resetPose()
            }
            poseStack.popPose()
        }
    }

    open fun renderMagazine(stack: ItemStack, model: GeoGunModel) {
        model.showMagazineBone(resolveMagazineBone(stack))
    }

    /**
     * 按剩余弹量画弹链上的子弹（`bullet_1`……）：模型里有几发 `bullet_*` 骨骼，打到只剩几发就藏掉几发，
     * 所以枪只需要在模型里把弹链子弹按 `bullet_1` 起编号，**不用**自己声明"打到几发以下开始藏"。
     *
     * 换弹动画的时间轴只管一件事：`HIDE_BULLET_CHAIN` 那个动作点之后整条弹链换新、重新画满
     * （`HideBulletChain` 置 false）。它与 [GunData.reloading] 相与，是因为那个状态也可能停在
     * false（换弹中途被打断）而"弹链是新换的"只在换弹过程中成立 —— 非换弹时一律按弹量算，
     * 状态卡住也不会让空弹链看着是满的。
     */
    open fun renderBulletChain(stack: ItemStack, model: GeoGunModel) {
        val data = from(stack)
        val freshBelt = data.reloading() && !data.hideBulletChain.get()
        model.showBulletChainBones(data.ammo.get(), freshBelt)
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

    /**
     * 本帧要不要给这把枪的**自带瞄具**开镜筒窗口。
     *
     * 三道闸，任何一道不过就返回 null（绝大多数枪在第一道就出去了，一次 `RenderSystem` 调用都不会多）：
     *
     * 1. 枪的 assets json 里声明了 `BuiltinScope`；
     * 2. **没有装镜配件** —— 装了就让配件赢。绝不两个瞄具同时出现。igla 根本没配镜槽，
     *    这一条纯属防御，但少了它，将来谁给某把枪既配了镜槽又加了 `ocular`，画面就会同时出现两个窗口；
     * 3. geo 里真有 `ocular` 骨骼（[BuiltinGunScopeRenderer.available]），且开镜进度过了
     *    [SCOPE_STENCIL_START_PROGRESS] —— 与配件瞄准镜同一个门槛，开镜起手时才张开。
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

    /**
     * 护木的"原厂 / 导轨"二选一：导轨上挂着东西就换 [CUSTOM_HAND_GUARD_BONE]、藏 [OEM_HAND_GUARD_BONE]。
     *
     * 占用导轨的来源有**两个**，任一成立都要换：[AttachmentType.GRIP] 握把，以及下挂副武器
     * （`AttachmentDefinition.subWeapon != null`，判据与渲染副武器本体时用的 [findSubWeapon] 同一个
     * —— 副武器的身份来自配件数据里的 `SubWeapon` 定义，不看它住在哪个槽位）。副武器挂在同一段
     * 导轨上，原厂护木会盖住它的身管与导轨座，所以它和握把一样要求换成带导轨的那一支。
     *
     * 两者共用枪 json 里同一个 `Attachments.GripHandGuard` 开关
     * （`assets/.../sbw/guns/<id>.json`，见 [com.atsuishio.superbwarfare.resource.gun.pojo.AttachmentInfo]）：
     * 那个开关问的是"这把枪有没有带导轨的护木可选"，与装的是哪一种导轨件无关。
     *
     * 模型里没有 `custom_hand_guard` 骨骼就直接返回 —— 没做护木替换的枪一字不变
     * （当前有这根骨骼的 5 把：aa_12 / ak_47 / mp_5 / qbz_95 / rpk，其中 mp_5 与 rpk 还没有副武器挂点）。
     */
    open fun renderGripHandGuard(stack: ItemStack, model: GeoGunModel) {
        val customBone = model.getBone(CUSTOM_HAND_GUARD_BONE) ?: return
        val gun = from(stack)
        val railOccupied = gun.attachment.has(AttachmentType.GRIP) || findSubWeapon(gun) != null
        val showCustom = railOccupied && GunResource.compute(stack).attachmentInfo.gripHandGuard
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
            Matrix4f(mountTransform).mul(resolveMuzzleAttachmentLocalTransform(stack))
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
        attachmentModel.renderToBuffer(
            poseStack, bufferSource, texture, packedLight, packedOverlay,
            null, resolveAmmoReadout(stack, definition.effectiveAmmoBar(), definition.effectiveTextShow())
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
     * 设挂点在主武器 `root` 空间里的变换为 `M`（bind pose 下算，稳定、不受姿态影响），
     * 副武器动画的 `root` 通道为 `A`。那么真正该动的是这个量：
     *
     * ```
     * D = M · A · M⁻¹        ← 把"附件空间里的整体运动"换算成"主武器空间里的整体运动"
     * ```
     *
     * 主武器的 `root` 由 `G` 变成 `D·G`：整枪（含其它配件、枪口焰，以及**手臂** —— 手是跟着
     * `lefthand_pos` 那根骨骼画出来的）一起跟着动。
     *
     * 副武器那一侧**不需要任何补偿**：它渲染时的挂点变换取自**已姿态**的主武器骨骼
     * （[renderRegisteredAttachments] 里的 `model.getGlobalTransform(boneName)`），
     * 主武器 `root` 一动，挂点跟着动，附件自然焊在轨道上。只要把附件的 `root` 通道**摘掉**
     * （[withoutBone]），附件内部就只剩"炮弹/炮管/扳机相对整枪怎么动"这一部分，
     * 与手持形态逐帧一致：
     *
     * ```
     * 附件渲染 = W(D·G 算出来的挂点世界变换) · (A 去掉 root 后的通道)   ← 与手持形态同构
     * ```
     *
     * ## ⚠ 单位（这一条曾经错了）
     *
     * 姿态（`BoneTransform`）的平移是 **Bedrock 单位**（`BoneState.applyCurrentSelfTransform` 里除 16），
     * 而 `getLocalTransform()` / `getGlobalTransform()` / `getBindGlobalTransform()` 给的是**方块**。
     * 混着乘矩阵，平移就差 16 倍 —— `D` 里"绕挂点转"那一项（量级 = 挂点距离 × 转角，
     * 实测约 3.46 单位 ≈ 0.22 方块）会被压到几乎看不见，看上去就像"挂点距离没被排除掉"。
     * 现在两头都显式换算到方块空间（[localMatrixOfPose] / [poseTransformOf]），算完再写回姿态。
     *
     * ## 边界
     *
     * - 只在"副武器被切出来 + 正在换弹"时生效（[GeoGunAnimationInstance.subWeaponReloadPose] 非空）；
     * - `A` 是单位阵时 `D` 也是单位阵 → **完全无副作用**，绝大部分帧走的就是这条；
     * - 挂点骨骼 / `root` 骨骼 / 附件模型任一解析不到 → 返回 `null`，退回原有渲染；
     * - 主武器 `root` 不满足"枢轴在原点 + 无绑定旋转"时也返回 `null`（见 [poseTransformOf]）；
     * - 返回的 [SubWeaponFollowPose.gunRoot] 由调用方 merge 到主武器姿态上，附件侧的 `root`
     *   则由 [renderRegisteredAttachments] 摘掉（**不是**乘 `D⁻¹` —— 那种手写抵消只在
     *   `A` 与 `M` 可交换时才精确，而且单位一错就彻底失效）。
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

        // ⚠ 用 **bind pose** 的挂点变换：它只取决于骨骼静态父子关系，不受主武器姿态影响，
        // 因此可以缓存、也不会和"我们刚给 root 加的 D"互相纠缠（用 posed 版本会自反馈）。
        val mountBind = model.getBindGlobalTransform(boneName) ?: return null
        val gunRootBind = model.getBindGlobalTransform(GUN_ROOT_BONE) ?: return null

        // M = 挂点在主武器 root 空间里的变换（方块）
        val mount = Matrix4f(gunRootBind).invert().mul(mountBind)

        // A = 附件动画 `root` 通道。⚠ 先换算到**方块**：姿态的平移是 Bedrock 单位，
        // 和上面两个 `…GlobalTransform` 混着乘，`D` 的挂点项会小 16 倍（见函数说明）。
        val rootLocal = localMatrixOfPose(attachmentModel.baseModel.bone(rootIndex), attachmentRoot)

        // D = M · A · M⁻¹：把附件空间里的整体运动换算成主武器空间里的整体运动
        val worldOffset = Matrix4f(mount).mul(rootLocal).mul(Matrix4f(mount).invert())

        // 炮弹：**不做任何修正**。炮弹是附件 `root` 的子节点，`A` 里的 `projectile` 通道
        // 与手持形态逐字节相同，摘掉 `root` 之后它就是"相对整枪怎么动"，本来就对。
        // （曾经试过把它的位移从 `root` 空间换基到父骨骼空间，方向反了、更差，已删。）

        // 主武器 root 的修正：G → D · G。这里**只算**、不写进模型实例 ——
        // 真正的"主武器跟着动"由 `renderModel` 把 [SubWeaponFollowPose.gunRoot] 叠加到渲染姿态上完成；
        // 在这里改实例只会让本帧后续所有从实例读出来的骨骼都带上这份偏移。
        val newGunRootLocal = Matrix4f(worldOffset).mul(gunRoot)
        val newGunRoot = poseTransformOf(gunRootIndex, gunRootBone, newGunRootLocal) ?: return null

        return SubWeaponFollowPose(modelPath, singleBonePose(newGunRoot))
    }

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
    )

    /**
     * 姿态变换（`BoneTransform`）→ 模型空间的局部矩阵（**方块**）。
     *
     * ⚠ 单位：姿态的平移是 **Bedrock 单位**，`…GlobalTransform` 给的是**方块**，
     * 两者混着做矩阵乘法平移就差 16 倍。组合顺序与 `BoneState.applyCurrentSelfTransform`
     * 逐字一致（含"绕枢轴旋转"的夹逼），所以结果和渲染时用的矩阵是同一个空间。
     */
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
     * [localMatrixOfPose] 的逆：模型空间局部矩阵（**方块**）→ 姿态变换（Bedrock 单位）。
     *
     * ⚠ 只在"**枢轴在原点 + 无绑定旋转 + 无折叠父变换**"时无损：姿态是**绝对**的
     * （`BoneTreeInstance.applyPose` 直接写 `BoneState` 的字段），一根带绑定旋转的骨骼
     * 没法用姿态值表达任意局部变换。不满足就返回 `null`，调用方退回原有渲染。
     * 主武器/附件的 `root` 都满足这个前提（pivot `[0,0,0]`、无 `rotation`）。
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
     * 从姿态里**摘掉**某一根骨骼，其余原样保留。
     *
     * 用在副武器换弹时：附件的 `root` 通道已经由主武器 `root` 的 `D` 承担了，
     * 附件这边再套一次就会被推离枪身；摘掉之后 `applyPose` 不会碰它，
     * 它就停在绑定姿势（`root` 的绑定是单位阵），相当于只播"相对整枪"的那些通道。
     */
    private fun withoutBone(pose: Pose, boneIndex: Int): Pose {
        if (boneIndex < 0) return pose
        val builder = ArrayPoseBuilder()
        for (transform in pose.getBoneTransforms()) {
            if (transform.boneIndex() != boneIndex) builder.addBoneTransform(transform)
        }
        return builder.toPose()
    }

    /**
     * 从姿态里取出**指定骨骼下标**的那一条变换。
     *
     * `Pose` 只有 `getBoneTransforms()` 这一个读接口（没有按下标取的方法），所以这里遍历一次；
     * 一帧里最多两次调用、骨骼数量级是几十，可以忽略。
     */
    private fun Pose.findTransform(boneIndex: Int): BoneTransform? =
        getBoneTransforms().firstOrNull { it.boneIndex() == boneIndex }

    /** 构造一个"只含某一根骨骼"的姿态，供 [MERGE_BLENDER] 覆盖到别的姿态上 */
    private fun singleBonePose(transform: BoneTransform): Pose {
        val builder = ArrayPoseBuilder()
        builder.addBoneTransform(transform)
        return builder.toPose()
    }

    /**
     * 副武器换弹时，手臂该挂到哪儿：取**附件模型自己**的 `lefthand_pos` / `righthand_pos`
     * 在**主武器模型空间**里的变换（挂点 × 附件骨骼），交给 `GeoGunModel.renderHands` 用它
     * 代替主武器同名骨骼。
     *
     * ## 为什么必须换
     *
     * 真手臂一直是按**主武器模型**的 `lefthand_pos` 画的（见 `GeoGunModel.renderHands`），
     * 而美术的换弹动画是对着手持形态的骨架做的 —— "手离开护木去抓炮弹、再跟着炮弹进膛"
     * 这一整套动作写在**附件模型**的 `lefthand`/`lefthand_pos` 上。附件那两根骨骼的枢轴
     * 与手持模型完全一样（`lefthand [-6.01875, 18, 0]` → `lefthand_pos [-0.01875, 7, 0]`，
     * 父级也都是 `projectile`），所以这里不需要任何换算：把附件那一侧解算出来的全局变换
     * 乘上挂点，就是"手在主武器空间里该在的位置"，正是 `renderHands` 需要的东西。
     * 设计文档里"两套骨架差了约 40 个单位、只能重导动画"的结论是误判，已改。
     *
     * ## 时机
     *
     * **必须在附件实例还带着换弹姿态的时候取**（调用点在 `applyPose` 与 `resetPose` 之间），
     * 否则拿到的是绑定姿势。附件里没有的骨骼（目前 `righthand_pos` 只有手持模型有）
     * 会被跳过，调用方退回主武器的那一根，所以这个方案对右手是**自动就绪**的：
     * 哪天把 `righthand_pos` 补进附件模型，右手立刻跟着走。
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
     * **部署期间的手臂接管**（§11.11.7.4）：副武器被 G 键切出来之后，手臂就挂在
     * **副武器自己**的 `lefthand_pos`/`righthand_pos` 上，直到把它切回去。
     *
     * ## 为什么需要它
     *
     * 真手臂由 `GeoGunModel.renderHands` 画在锚点骨骼上。在此之前，锚点**平时**取自主武器，
     * 只有"部署中的副武器正在换弹"时才换成副武器的（[resolveSubWeaponHandAnchors]）——
     * 于是换弹开始/结束的那一帧手会硬切。实测（`build/verify/VerifyArmOverlay.java`，宿主 ak_12）
     * 这一次切换是 **0.4919 方块**，就是用户报的"突变"。
     *
     * 而副武器动画的 `idle` 与 `reload` **首尾完全重合**（实测差 `0.0000`）：美术本来就是把
     * "手停在发射器上"当作换弹的起点与终点的。所以只要**部署期间一直接管**，那一次切换就根本
     * 不存在了；切出/切回时那一次换源则由 [resolveArmAnchorsForDraw] 的淡入淡出抹平。
     *
     * ## 作用范围**只有部署期间**（这是刻意的，别再放开）
     *
     * 曾经试过"**装上就接管**（含非部署状态）"，实机效果不好：非部署时主武器的动画在**中途**也可能
     * 挪动左手（不是每支 clip 都像 `ak_12.fire` 那样干脆不 key `lefthand`），常驻接管会把这些动作
     * 全部压掉。所以这里的条件**只有一条**：`ActiveGun.isDeployed` —— 没被切出来的副武器
     * 一个像素都不影响手臂。**也不再按"配件"泛化**（握把接管那套已废弃，见 §11.11.7.4-F）。
     *
     * ## 什么时候**不**接管
     *
     * 主武器自己换弹/近战/改装时手必须去抓弹匣、挥刺刀、或者被枪身一起甩进检视姿势
     * （见 [GunAnimationState.takesHandAway]，实测这三类要挪 **1.55 / 1.25 / 0.93 方块**），
     * 这些状态下让位给主武器 —— 手该待哪儿只有主武器自己知道。
     * 其余状态（`IDLE` / `FIRE` / `fire_sub_weapon` / `CHANGE_FIRE_MODE` …）主武器都没有把手挪去别处
     * （`ak_12.fire` 甚至压根没 key `lefthand`），部署期间继续接管才不会有跳变。
     *
     * ## 姿态从哪来
     *
     * 副武器**自己的数据**写了 clip 名（`sbw/guns/<副武器 id>.json` 的 `Animation.Idle`），
     * 动画本体在**附件动画表**里（`animations/bedrock/attachment/`）—— 与换弹那条链路
     * （`GeoGunAnimationInstance.updateSubWeaponReload`）**同一套两步解析**，只是不要求在换弹。
     * 两处的解析都在动画实例里（[GeoGunAnimationInstance.subWeaponIdlePose]），
     * 这里只负责把姿态与挂点乘成手臂锚点。
     *
     * ## 实现上的两个约束
     *
     * - **挂点变换取自"本帧最终"的主武器骨骼**，与配件渲染那条路径同源。所以调用点必须排在
     *   这一帧所有会改写骨骼的步骤**之后**（`applyPose` → `applyCameraShake` → 脚本回调），
     *   具体位置与理由见 `renderModel` 里的调用点注释 ——
     *   踩过的坑：在 `applyCameraShake` **之前**采样，瞄准射击时手会离枪 **≈0.27 方块**；
     * - 附件模型实例是**全局共享**的（同一种配件装在多把枪上共用一份），所以这里
     *   `applyPose` → 取全局变换 → `finally` 里 `resetPose()` 必须配平，否则姿态会串到别的枪上。
     *   这一段是纯 CPU 计算、中间不渲染，所以放在附件渲染窗口之外也是安全的。
     *
     * 空表 = 没有接管（调用方退回主武器自己的骨骼，逐字走改动前的老路径）。
     */
    private fun resolveDeployedSubWeaponArmAnchors(stack: ItemStack, model: GeoGunModel, hand: InteractionHand) {
        val data = from(stack)

        // ⚠ 唯一的准入条件。没部署 → 直接返回，手臂完全按改动前的行为走。
        if (!ActiveGun.isDeployed(data, true)) return

        val animation = FirstPersonRenderHandler.getActiveAnimationInstance(hand) as? GeoGunAnimationInstance
        // 主武器自己换弹/近战 → 让位。状态还没解析出来（等同没有主武器动画）时照旧接管。
        if (animation?.currentGunState?.takesHandAway == true) return

        val (slot, definition) = findSubWeapon(data) ?: return
        val modelPath = definition.model ?: return
        val boneName = AttachmentSlots.mountBoneOf(slot, definition) ?: return
        val pose = animation?.subWeaponIdlePose() ?: return
        val attachmentModel = AttachmentModelReloadListener.getModel(modelPath) ?: return
        val mountTransform = model.getGlobalTransform(boneName) ?: return

        try {
            // 与主武器那一行**逐字同一套口径**：动画文件里的平移是"相对绑定姿势的偏移"，
            // 不先垫绑定就会把绑定不为 0 的骨骼送到父节点原点（§11.11.7.3-C）。
            attachmentModel.applyPose(BLENDER.blend(attachmentModel.getBindPose(), pose))
            deployedArmAnchors = resolveSubWeaponHandAnchors(attachmentModel, mountTransform)
            deployedArmKey = "idle:${animation.subWeaponIdleClipName ?: ""}"
        } finally {
            attachmentModel.resetPose()
        }
    }

    /**
     * 本帧真正要交给 `GeoGunModel.renderHands` 的手臂锚点 —— 两个可能来源合并之后、再抹平突变。
     *
     * 优先级：**副武器换弹锚点 > 部署接管 > 主武器自己的骨骼（返回空表）**。
     *
     * 换弹锚点赢是因为它更具体（"这一帧手正在抓炮弹"），而且它的两端与部署接管重合，
     * 交接本来就不该看见跳变。
     *
     * ## 突变是怎么被抹平的
     *
     * 来源标识（[armAnchorSourceKey]）一变，就把**上一帧真正画出去的那两个矩阵**记下来，
     * 用 [ARM_ANCHOR_FADE_TICKS] 帧把它们插值到这一帧的目标上：平移 `lerp`、旋转 `nlerp`。
     * 于是"**切出副武器**"、"**切回主武器**"、"主武器换弹抢回手臂"这几处都不再是硬跳。
     * 进度到 1 就直接用目标矩阵，不留残差（否则手臂会永远差一点点）。
     *
     * ⚠ 插值只保留**平移 + 旋转**，矩阵里若有缩放会在这一小段里被丢掉 ——
     * 现在的挂点链路（`mount × 附件骨骼`）里没有缩放，记在这里备查。
     *
     * ⚠ 没有来源、且淡出已经走完时**返回空表**：没部署副武器的枪（绝大多数）于是让
     * `renderHands` 用回主武器自己的骨骼，与改动前逐字一致。
     * ⚠⚠ 这里的判据是 [armAnchorFade]，**不是** [armAnchorSourceKey]：来源刚变成 `null` 的那一帧
     * 是"淡出开始"而不是"淡出结束"，只看来源标识会把上一帧的矩阵冻住一帧、下一帧直接硬跳到骨骼上
     * —— 那正是这次要修的那种跳变，只是换了个位置发生。
     * ⚠⚠ 同理，**换来源时一律从进度 0 开始**，不能"没有上一帧记录就跳过淡入"：非部署时这里
     * 根本不画（交空表），记录永远是空的，跳过就等于每次切出副武器都瞬移。
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

        // 来源换了：从"上一帧真正画出去的那个矩阵"开始插值。
        // ⚠ 这里**一律从 0 开始**，哪怕 [armAnchorShown] 是空的（没有上一帧可参考）：
        // 那条路上起点会退回主武器自己的骨骼（见下面 `from`），也就是屏幕上原本的位置，
        // 所以淡入照样成立。判断"没有记录就跳过淡入"是错的 —— **非部署时这条路径根本不画**
        // （直接交空表），记录永远是空的，那样写等于每次切出副武器都瞬移，而"平滑移过去"
        // 正是这一步的全部意义。
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
            // 淡出结束：这一帧屏幕上又是主武器自己的骨骼了，把记录清掉 ——
            // 留着会让"换了一把枪再切出副武器"的淡入从**上一把枪**的手位起步。
            armAnchorShown.clear()
            return emptyMap()
        }

        val shown = EnumMap<HumanoidArm, Matrix4f>(HumanoidArm::class.java)
        for ((arm, boneName) in SUB_WEAPON_HAND_BONES) {
            // 配件没有的骨骼（目前 `righthand_pos` 只有手持模型有）退回主武器自己那一根 ——
            // 与"锚点里缺一根就用模型的骨骼"这条既有回退语义一致，只是这里显式算出来，
            // 好让它也能参与插值（否则切换那一帧右手会漏过去）。
            val gunBone = model.getGlobalTransform(boneName)?.let { Matrix4f(it) }
            val goal = target[arm] ?: gunBone ?: continue
            // 起点：上一帧真正画出去的那个矩阵；没有就退回**主武器自己的骨骼** ——
            // 那正是"还没接管过"（这一帧之前交的是空表）时 `renderHands` 画的位置，
            // 于是"切出副武器"的淡入从手原来的地方起步，而不是从别的什么地方飞过来。
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
     * 两个锚点之间的插值：平移线性、旋转球面线性（`nlerp`）、**缩放线性**，用 `smoothstep` 缓入缓出。
     *
     * ⚠⚠ **缩放必须一起插**。锚点矩阵是「挂点 × 附件骨骼」，而附件骨骼是可以带 `scale` 通道的 ——
     * 副武器的 `lefthand` 现在就 key 了 `[1, 1.5, 1]`（把手持手臂拉长 50%），于是锚点矩阵带着
     * `(1, 1.5, 1)` 的缩放。第一版只重建 `translationRotate(...)`，缩放被**丢掉**，淡入淡出的
     * 那 3 刻里手臂会画回原长 —— 而换弹的**开始与结束**各会换一次锚点来源（`idle` ↔ `reload`），
     * 也就是每次换弹闪两下，正是"换弹时手臂闪现 / 长度跳变"。
     *
     * 更糟的是它**破坏了这套机制赖以成立的前提**："idle 与 reload 两端重合，所以换弹进出的淡入
     * 淡出什么也不做"。加了缩放之后两端仍然完全重合（实测矩阵级 `max diff = 0.000000`），
     * 于是插值本来应该原样输出目标矩阵 —— 丢缩放之后它反而成了唯一的跳变源。
     *
     * 分解用 `getUnnormalizedRotation`（按列归一化，正好对应 `getScale` 的每轴长度）：
     * 这条链上带非均匀缩放的骨骼是线性部分的**最后一个**因子（`R_挂点 · R_lefthand · S`），
     * 是干净的 TRS，往返误差 ~1e-7（实测 `build/verify/VerifyArmScale.java`）。
     * 真出现被旋转夹住的非均匀缩放（剪切）时，中间这几帧会是近似值 ——
     * 两端（`progress >= 1`）仍然逐字用目标矩阵，不留残差。
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
        return level.gameTime + mc.timer.gameTimeDeltaTicks.toDouble()
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
        // 基准取自 [idleViewAnchor]（`renderModel` 每帧算一次）：部署副武器期间它换成**副武器自己**
        // 的 `idle_view`，切出 / 切回时在 [ARM_ANCHOR_FADE_TICKS] 内插值；为空就逐字退回模型自己的
        // `idle_view`（没装副武器 / 副武器没做这支骨骼 / 不是第一人称）。
        // ⚠ 这里**只读不推进**：本函数每帧被调两次（定位一次、`zoomPivot` 一次），写在这里会走成 1.5 刻。
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
     * 取的顺序：附件模型自己的 `iron_view` × 挂点骨骼。
     * hip 位形（`idle_view`）走的是它自己的那条链 —— [resolveSubWeaponIdleTransform]，
     * 两支骨骼各管一段（不瞄准时看 hip、瞄准时看这里），互不干涉。
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
     * 副武器**自己**的持枪位形（hip 位形）：附件模型的 `idle_view` × 挂点骨骼。
     *
     * 与 [resolveSubWeaponAimTransform] 是**一对**（一个管 hip 位形、一个管瞄准位形），骨架逐字相同：
     * 同样的约定骨骼（`SubWeaponInfo.IDLE_VIEW_BONE` = `idle_view`，**没有配置字段**）、
     * 同样的缺失回退（附件模型里没有这支骨骼就返回 `null`，调用方退回主武器自己的位形）、
     * 同样的挂点口径（[GeoGunModel.getBindGlobalTransform]，理由见 [resolveSubWeaponAimTransform]）。
     *
     * 附件那一侧取实例自己的 [BedrockAttachmentModel.getGlobalTransform]：附件实例的 pose 由
     * `renderRegisteredAttachments` 用完 `resetPose`，采样时它就是绑定姿态；而 `idle_view` 挂在
     * 附件模型里**没有任何 clip key** 的 `positioning` 下（`sub_weapon_gp_25` 的 idle 只 key
     * `lefthand`、reload 只 key `root`/`camera`/`lefthand`/… ），所以这支骨骼是**静态**的。
     *
     * ⚠ 别和 [GeoGunAnimationInstance.subWeaponIdlePose] 混了：那是副武器 **idle 动画**的姿势
     * （clip，t = 0），这里要的是**模型里的骨骼**。
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
     * [updateSubWeaponIdleView] 用的**来源标识**：能唯一确定"这个基准是从哪来的"。
     *
     * 取 `"<挂点骨骼>@<附件模型路径>"` 而不是只取"部署中 / 没部署"：**换掉副武器**（改装界面换一个
     * 下挂件）时基准也会变，那一下同样该淡过去。取不到副武器时返回 `null`（= 主武器自己的位形）。
     */
    private fun subWeaponIdleViewKey(stack: ItemStack): String? {
        val (slot, definition) = findSubWeapon(from(stack)) ?: return null
        val modelPath = definition.model ?: return null
        val boneName = AttachmentSlots.mountBoneOf(slot, definition) ?: return null
        return "$boneName@$modelPath"
    }

    /**
     * 推进并解析本帧的 hip 位形基准（[idleViewAnchor]）。
     *
     * 来源只有两个：**主武器自己的** `idle_view`（没部署，或副武器模型里没有这支骨骼）与
     * **正在部署的那把副武器**的（[resolveSubWeaponIdleTransform]）。两者之间的切换用
     * [ARM_ANCHOR_FADE_TICKS] 与 [fadeAnchor] 同一个 `smoothstep` 抹平 —— 于是"切主副武器的 hip 位形"
     * 与"切左手"**共用同一个常量、同一段曲线**，速率是构造出来的而不是抄来的。
     *
     * ⚠ **每帧只调一次**（`renderModel` 的第一人称块里）。写进 [computeViewTransform] 会走两遍
     * （那个函数每帧被调两次），3 刻的淡入变成 1.5 刻。
     * ⚠ 收尾判据是 [IdleViewFade.fade] 而**不是** [IdleViewFade.key]，理由与 [resolveArmAnchorsForDraw]
     * 里那条逐字相同：来源刚变成 `null` 的那一帧是"淡出**开始**"，只看 key 会把上一帧的基准冻一帧再硬跳。
     */
    private fun updateSubWeaponIdleView(stack: ItemStack, model: GeoGunModel, hand: InteractionHand) {
        val subWeaponIdle = if (playerDeployedSubWeapon(stack)) {
            resolveSubWeaponIdleTransform(stack, model)
        } else {
            null
        }
        val mainIdle = model.getGlobalTransform(IDLE_VIEW_BONE)

        if (subWeaponIdle == null && mainIdle == null) {
            // 连主武器的 `idle_view` 都没有（这种枪不存在，但不能让它变成 NPE）：交回 `null`，
            // 让 `computeViewTransform` 的 `?: return null` 照旧生效。
            idleViewFades.remove(hand)
            return
        }

        val state = idleViewFades.getOrPut(hand) { IdleViewFade() }
        val key = if (subWeaponIdle != null) subWeaponIdleViewKey(stack) else null
        val target = subWeaponIdle ?: mainIdle!!

        if (key != state.key) {
            // 来源换了：从"上一帧真正画出去的那个基准"起步；还没有记录（第一次切出副武器）就从
            // **主武器自己的位形**起步 —— 那正是这一帧之前屏幕上画的位置，于是淡入从原地开始。
            state.key = key
            state.from = state.shown?.let { Matrix4f(it) } ?: mainIdle?.let { Matrix4f(it) }
            state.fade = 0f
        } else {
            val delta = Minecraft.getInstance().deltaFrameTime.coerceIn(0f, MAX_FRAME_DELTA_TICKS)
            state.fade = (state.fade + delta / ARM_ANCHOR_FADE_TICKS).coerceAtMost(1f)
        }

        if (key == null && state.fade >= 1f) {
            // 淡出结束：这一帧屏幕上又是主武器自己的 `idle_view` 了 —— 交 `null` 走老路径，并把记录清掉
            // （留着会让"换一把枪再部署副武器"的淡入从**上一把枪**的视点起步）。
            state.shown = null
            return
        }

        val from = state.from
        val shown = if (from == null || state.fade >= 1f) {
            // 到位就直接用目标矩阵，不留残差（否则视点会永远差那么一点点）
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
     * 返回改装聚焦的目标偏移（相对**本帧的 hip 位形基准**，模型空间）：聚焦点为当前编辑配件定位点的
     * 绝对坐标往 Z 轴负方向偏移 [EDIT_FOCUS_Z_OFFSET] 单位，避免视角卡进模型。
     * 未选中配件时返回浮动预览的鼠标平移偏移；未处于改装状态或对应骨骼不存在时返回 null。
     *
     * ⚠ **基准必须与 [computeViewTransform] 加回去的那个完全一致**（都用 [idleViewAnchor]）：
     * 那个函数是 `相机 = 基准 + 偏移`，只有偏移是相对**同一个**基准算的，这个和才恒等于配件定位点
     * （`基准 + (配件点 − 基准)`）。固定减 `IDLE_VIEW_BONE` 的话，部署副武器时相机会偏
     * `|副武器 idle_view − 主武器 idle_view|`。
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

        /**
         * 副武器换弹时接管手臂的两根骨骼（**附件**模型里的，不是主武器的同名骨骼）。
         *
         * 目前只有一个型号的附件模型里存在 `righthand_pos` 时右手才会跟着走 —— 见
         * [resolveSubWeaponHandAnchors]：取不到就退回主武器那一根，不需要改这里。
         */
        private val SUB_WEAPON_HAND_BONES = listOf(
            HumanoidArm.LEFT to "lefthand_pos",
            HumanoidArm.RIGHT to "righthand_pos",
        )

        /**
         * 手臂锚点换来源时的淡入淡出时长，单位 **tick**（`Minecraft.deltaFrameTime` 的刻度，
         * 20 tick = 1 秒）。3 tick ≈ 0.15 秒：够盖住 0.49 方块那一下硬切，又短到不会被看成"手在飘"。
         *
         * 见 [resolveArmAnchorsForDraw]。注意两端重合的切换（副武器 idle ↔ 换弹）实际什么都不做，
         * 真正会用到它的是**切出/切回副武器**那两次。
         */
        private const val ARM_ANCHOR_FADE_TICKS = 3f

        /**
         * 单帧步进的上限（tick）。卡顿或调试暂停会让 `deltaFrameTime` 偶尔跳到很大，
         * 不夹住的话淡入淡出会"一帧走完"，看起来还是硬切。
         * 0.8 与同文件 [`scriptFrameDeltaSeconds`] 的口径一致。
         */
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
