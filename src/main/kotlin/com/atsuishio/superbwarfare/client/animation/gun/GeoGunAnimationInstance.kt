package com.atsuishio.superbwarfare.client.animation.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.client.gun.GunChargeHandler
import com.atsuishio.superbwarfare.client.gun.MeleeClientHandler
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.*
import com.atsuishio.superbwarfare.data.gun.melee.MeleeAction
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.DeployedWeaponHandler
import com.atsuishio.superbwarfare.item.attachment.SubWeaponItem
import com.atsuishio.superbwarfare.resource.gun.GunAnimation
import com.atsuishio.superbwarfare.resource.gun.GunAnimationNames
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.resource.model.AttachmentModelReloadListener
import com.atsuishio.superbwarfare.resource.model.GunModelReloadListener
import com.atsuishio.superbwarfare.tools.localPlayer
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData
import com.maydaymemory.mae.basic.*
import com.maydaymemory.mae.blend.*
import com.maydaymemory.mae.control.runner.*
import com.maydaymemory.mae.util.MathUtil
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*

open class GeoGunAnimationInstance(
    private var stack: ItemStack,
    entity: Entity?,
    private val hand: InteractionHand
) : IFPAnimationInstance {
    private val animations = hashMapOf<String, BedrockAnimation>()
    private var runner: AnimationRunner? = null
    private var fireRunner: AnimationRunner? = null
    private var fireModeRunner: AnimationRunner? = null
    private var fireModeAnimationName: String? = null
    private var fireModeSwitchRunner: AnimationRunner? = null
    private var holdOpenRunner: AnimationRunner? = null
    private var holdOpenAnimationName: String? = null
    private var closeStrikeRunner: AnimationRunner? = null
    private var closeStrikeAnimationName: String? = null

    // 枪管旋转的循环 runner
    private var spinRunner: AnimationRunner? = null

    // 已经解析好的 `hold` 片段
    private var spinAnimation: BedrockAnimation? = null

    /** 当前转到几成（0..1），缓入缓出就缓在这里。 */
    private var spinPower = 0f

    // 循环开火 runner
    private var fireLoopRunner: AnimationRunner? = null

    /** 已经解析好的循环开火片段*/
    private var fireLoopAnimation: BedrockAnimation? = null

    /**
     * 循环开火层现在亮到几成（0..1）
     */
    private var fireLoopPower = 0f

    // 自定义奔跑层（`Animation.Run`）
    private val customSprintLayer = WeightedLoopLayer()

    // 刺刀冲锋层（`Animation.BayonetCharge`），冲锋窗口内顶掉奔跑层
    private val bayonetChargeLayer = WeightedLoopLayer()

    private var deployIdleRunner: AnimationRunner? = null
    private var deployIdleAnimation: BedrockAnimation? = null
    private var deployIdlePower = 0f

    // 常驻循环层（`Animation.Loop`）runner
    private var loopRunner: AnimationRunner? = null

    /** 已经解析好的常驻循环片段 */
    private var loopAnimation: BedrockAnimation? = null

    // 蓄力层 runner
    private var chargeRunner: AnimationRunner? = null

    /** 已经解析好的蓄力片段 */
    private var chargeAnimation: BedrockAnimation? = null

    /**
     * 蓄力层现在亮到几成（0..1）
     */
    private var chargePower = 0f
    private var chargeWasActive = false

    /** 淡入 / 淡出时长（tick） */
    private var fireLoopFadeInTicks = 0f
    private var fireLoopFadeOutTicks = 0f
    private var editExitRunner: AnimationRunner? = null
    private var currentState: GunAnimationState? = null
    private var fireSerial = 0
    private var consumedFireSerial = 0

    /**
     * 本 tick 主武器所处的动画状态（还没解析出状态时为 `null`，等同"没有主武器动画"）
     */
    val currentGunState: GunAnimationState?
        get() = currentState

    /**
     * 本次开火已经解析好的 clip 名（由 [triggerFire] 写入，[playFire] 读取）。
     */
    private var pendingFireAnimation: String? = null

    /**
     * 当前这一支开火动画是不是**副武器专属**的那一支
     */
    private var subWeaponFire = false

    /**
     * 副武器**自己**的换弹动画 runner
     */
    private var subWeaponReloadRunner: AnimationRunner? = null

    /** 当前这支副武器换弹动画的 clip 名（用来判断"换了一支动画"要不要重建 runner） */
    private var subWeaponReloadAnimationName: String? = null

    /** 已经打过日志的"解析不到副武器换弹动画"（键含副武器 id/原因/clip 名，同一条失败只打一次） */
    private var loggedSubWeaponReloadMiss: String? = null

    /**
     * 已消费的近战挥击序号
     */
    private var consumedMeleeSerial = 0

    /** 提前衔接下一段时，上一段挥击片段作为淡出层继续走完的 runner */
    private var meleeFadeRunner: AnimationRunner? = null

    /** 淡出层剩余时长（tick） */
    private var meleeFadeTicks = 0f

    /** 淡出层总时长（tick），用来算权重 */
    private var meleeFadeDuration = 0f

    /** 这次淡出是"离开近战"（而不是挥击接挥击）：相机晃动要跟着衰减 */
    private var meleeCameraFading = false
    private var fireModeSwitchSerial = 0
    private var consumedFireModeSwitchSerial = 0
    private var lastFireModeName: String? = null

    private var loggedMeleeMiss: String? = null
    private var loggedFireMiss: String? = null
    private var loggedOverrideFireMiss: String? = null

    private val pendingShellEjects = ArrayList<Int>()
    private val pendingParticles = ArrayList<ParticleEffectData>()
    private var cachedPose: Pose = DummyPose.INSTANCE
    private val cameraRotation = Quaternionf()
    private val fadedCameraRotation = Quaternionf()

    init {
        loadAnimations()
    }

    private fun loadAnimations() {
        animations.clear()
        val location = GunResource.compute(stack).getModel().animation ?: return
        GunModelReloadListener.getAnimation(location)?.forEach { animation ->
            animations[animation.name] = animation
        }
    }

    private fun resolveState(): GunAnimationState? {
        // 没有本地玩家（还没进世界）就没有主武器动画
        if (localPlayer == null) return null
        val animation = GunResource.compute(stack).animation ?: return null
        val data = GunData.from(stack)

        if (animation.edit != null && ClientEventHandler.isEditing) return GunAnimationState.EDIT
        if (animation.bolt != null && data.bolt.actionTimer.get() > 0) return GunAnimationState.BOLT

        if (data.reloading()) {
            when {
                data.reload.stage() == 1 && animation.prepareLoad != null && data.reload.prepareLoadTimer.get() > 0 -> return GunAnimationState.PREPARE_LOAD
                data.reload.stage() == 1 && animation.prepare != null -> return GunAnimationState.PREPARE
                data.reload.stage() == 2 && animation.iterative != null -> {
                    return if (data.loadIndex.get() == 1) {
                        GunAnimationState.ITERATIVE_2
                    } else {
                        GunAnimationState.ITERATIVE
                    }
                }

                data.reload.stage() == 3 && animation.finish != null -> return GunAnimationState.FINISH
            }
            if (animation.reload != null) return GunAnimationState.RELOAD
            if (data.reload.normal() && normalReloadName(animation) != null) return GunAnimationState.RELOAD_NORMAL
            if (data.reload.empty() && emptyReloadName(animation) != null) return GunAnimationState.RELOAD_EMPTY
        }

        if (animation.melee != null && ClientEventHandler.isGunMeleeActive(stack)) return GunAnimationState.MELEE

        return if (animation.idle != null) GunAnimationState.IDLE else null
    }

    /**
     * 这一帧要不要把**自定义奔跑动画**（`Animation.Run`）亮起来。
     *
     * 判据就是它以前当基础状态时的判据（见 [GunAnimationState] 的旧 `RUN`）：
     * 在跑、在地面上、没被"禁止冲刺"压住、也没在掏枪。另外它现在是一条**层**，
     * 得有个基础状态垫着 —— 所以还要求解析出来的是 `Idle`；换弹/近战/改装那些
     * 本来就把 `Run` 挤掉的状态由 [WeightedLoopLayer.tick] 的 `yieldNow` 在
     * [ACTION_HANDOVER_TICKS] 里让位，这里只负责"奔跑本身结束"那几种缓出。
     */
    private fun wantsCustomSprint(target: GunAnimationState?, animation: GunAnimation?): Boolean {
        val player = localPlayer ?: return false
        if (animation?.run == null) return false
        if (target != GunAnimationState.IDLE) return false
        if (isDeployedGun()) return false
        if (ClientEventHandler.drawTime >= 0.01) return false

        // 冲锋窗口内让位给冲锋层，两条层同一套速率交叉淡入淡出
        if (wantsBayonetCharge(target, animation)) return false

        return player.isSprinting
                && player.onGround()
                && ClientEventHandler.noSprintTicks == 0f
    }

    /**
     * 这一帧要不要把**刺刀冲锋层**（`Animation.BayonetCharge`）亮起来。
     *
     * 判据和奔跑层同源，触发源换成冲锋窗口 —— 玩家在冲锋时不一定真按着冲刺键，
     * 但那 3 秒内一律当作在冲刺，播冲锋那支循环片段。
     */
    private fun wantsBayonetCharge(target: GunAnimationState?, animation: GunAnimation?): Boolean {
        val player = localPlayer ?: return false
        if (animation?.bayonetCharge == null) return false
        if (target != GunAnimationState.IDLE) return false
        if (isDeployedGun()) return false
        if (ClientEventHandler.drawTime >= 0.01) return false
        // 右键是开镜的枪可以在窗口里开镜（刺雷的右键就是冲锋，到不了这里）
        if (ClientEventHandler.zoomTime > 0.01) return false
        if (!GunChargeHandler.isCharging()) return false
        return player.onGround()
    }

    /**
     * 触发一次开火动画。
     *
     * @param candidates 本次开火使用的动画**候选链**（`SubWeaponInfo.Animation`）；留空 = 主武器
     *   自己开火，走宿主枪的 `GunAnimation.Fire`。
     * @param reportMissing 候选全部落空时是否按**数据写错**报 error。默认候选（没写 `Animation` 时
     *   那条 `fire_sub_weapon`）落空是正常情况 —— 绝大多数枪就没做这支 clip，
     *   所以调用方按"这份候选是不是数据里显式写的"传（见 `SubWeaponInfo.hasExplicitFireAnimation`）。
     * @param overrideAnimation **显式指定**本发用的 clip（`GunProp.SHOOT_ANIMATION`），留空走原有解析。
     *   它是"这一发放哪支 clip"的直接答案，所以**优先于候选链与 `Fire` 兜底** —— 充能射击就是靠它
     *   换成 `*_charge` 那支的。名字同样是全名/短名两可（见 [GunAnimationNames]），
     *   拼出来不存在时退回原有解析并记一条日志（静默退回会让"动画没换"变成查不出来的问题）。
     */
    @JvmOverloads
    fun triggerFire(
        stack: ItemStack,
        candidates: List<String> = emptyList(),
        reportMissing: Boolean = false,
        overrideAnimation: String? = null,
    ) {
        val hostStack = this.stack

        val fireName = resolveOverrideFireName(hostStack, overrideAnimation)
            ?: resolveFireName(hostStack, candidates, reportMissing)
            ?: return
        if (!animations.containsKey(fireName)) return

        // 这一发是不是副武器打的
        val subWeaponShot = stack.item is SubWeaponItem
        if (!subWeaponShot && this.stack.item != stack.item) {
            updateItem(stack)
        }

        val gunFire = hostFireName(GunResource.compute(hostStack).animation, hostStack)
        pendingFireAnimation = fireName
        subWeaponFire = candidates.isNotEmpty() && fireName != gunFire

        fireSerial++
        // 副武器开火不抛壳
        if (isFirstPerson() && !subWeaponShot) {
            pendingShellEjects += 0
        }
    }

    /**
     * 当前正在播的开火动画是不是副武器专属的那一支
     */
    fun isSubWeaponFire(): Boolean = subWeaponFire && fireRunner != null

    /**
     * 副武器的换弹姿态
     */
    fun subWeaponReloadPose(): Pose? = subWeaponReloadRunner?.evaluate()

    /**
     * 上面那份姿态是哪一支 clip 给的
     */
    val subWeaponReloadClipName: String?
        get() = subWeaponReloadRunner?.let { subWeaponReloadAnimationName }

    /**
     * 推进副武器的换弹动画
     */
    private fun updateSubWeaponReload() {
        val player = localPlayer
        val gun = player?.let { GunData.from(it.mainHandItem) }
        val subStack = gun?.let { ActiveGun.stackOf(it, true) }?.takeIf { it.item !== stack.item }
        if (subStack == null) {
            clearSubWeaponReloadRunner()
            return
        }

        val subData = GunData.from(subStack)
        if (!subData.reloading()) {
            clearSubWeaponReloadRunner()
            return
        }

        // clip 名来自**副武器自己的资源**（与普通枪完全同一套：`GunResource` 按物品注册 id 解析）
        val subResource = GunResource.from(subStack)
        val clipName = subResource.compute().animation?.reloadClip(
            emptyReload = subData.reload.empty(),
            drumLevel = subData.isDrumLevel(),
        )
        if (clipName == null) {
            logSubWeaponReloadMissOnce(subResource.id, "noClip", subResource.id)
            clearSubWeaponReloadRunner()
            return
        }

        // 动画本体来自**附件模型**的动画表：`animations/bedrock/attachment/<id>.animation.json`
        // 里的 clip 是绑在附件模型骨骼上的，不能拿枪模型动画表里的同名 clip 顶替。
        val clip = AttachmentModelReloadListener.findAnimation(clipName)
        if (clip == null) {
            // ⚠ 走到这里最常见的两种情况，见下面那段"为什么以前是静默的"
            logSubWeaponReloadMissOnce(subResource.id, "unbound", clipName)
            clearSubWeaponReloadRunner()
            return
        }

        // 这一支终于解析到了：把"缺动画"的记账清掉，下次再缺（数据包改回来了）还能再报一次
        loggedSubWeaponReloadMiss = null

        if (subWeaponReloadRunner == null || subWeaponReloadAnimationName != clip.name) {
            val runner = AnimationRunner(clip, AnimationContext(clip.specifiedEndTimeS))
            runner.state = AnimationPlayType.PLAY_ONCE_HOLD.state()
            subWeaponReloadRunner = runner
            subWeaponReloadAnimationName = clip.name
        }

        // 与主武器换弹同一套：把 clip 的长度对齐到**数据里的换弹时长**
        val totalTicks = subData.reload.totalTicks.get().takeIf { it > 0 }
            ?: subData.get(GunProp.EMPTY_RELOAD_TIME)
        val targetSeconds = totalTicks.coerceAtLeast(1) / 20.0f
        if (clip.specifiedEndTimeS > 0f) {
            setAnimationSpeed(subWeaponReloadRunner?.state, clip.specifiedEndTimeS / targetSeconds)
        }
    }

    private fun clearSubWeaponReloadRunner() {
        subWeaponReloadRunner = null
        subWeaponReloadAnimationName = null
    }

    /**
     * 部署中的副武器的 **`Idle` clip**
     */
    private var subWeaponIdleClip: BedrockAnimation? = null

    /** 上面那支 idle clip 的名字，只用于渲染侧的"锚点来源变没变"标识 */
    private var subWeaponIdleAnimationName: String? = null

    /**
     * 部署中的副武器的 `Idle` 姿态
     */
    fun subWeaponIdlePose(): Pose? = subWeaponIdleClip?.evaluate(0f)

    /** [subWeaponIdlePose] 是哪一支 clip 给的（没部署时为 `null`） */
    val subWeaponIdleClipName: String?
        get() = subWeaponIdleClip?.let { subWeaponIdleAnimationName }

    /**
     * 解析部署中的副武器的 `Idle` clip（每 tick 一次，与 [updateSubWeaponReload] 同一处调用）。
     *
     * 前两步与 [updateSubWeaponReload] 逐字相同（当前操控的是副武器 → 问它自己的资源要 clip 名），
     * 只是这里连"在不在换弹"都不问：部署了就解析。
     */
    private fun updateSubWeaponIdle() {
        val player = localPlayer
        val gun = player?.let { GunData.from(it.mainHandItem) }
        val subStack = gun?.let { ActiveGun.stackOf(it, true) }?.takeIf { it.item !== stack.item }
        if (subStack == null) {
            subWeaponIdleClip = null
            subWeaponIdleAnimationName = null
            return
        }

        val clipName = GunResource.from(subStack).compute().animation?.idle
        val clip = clipName?.let { AttachmentModelReloadListener.findAnimation(it) }
        subWeaponIdleClip = clip
        subWeaponIdleAnimationName = clip?.name
    }

    /**
     * "这一支副武器换弹动画解析不到"只报一次
     */
    private fun logSubWeaponReloadMissOnce(subId: String, reason: String, detail: String) {
        val key = "$subId|$reason|$detail"
        if (loggedSubWeaponReloadMiss == key) return
        loggedSubWeaponReloadMiss = key
        if (!DisplayConfig.MELEE_DEBUG_LOG.get()) return

        when (reason) {
            "noClip" -> Mod.LOGGER.info(
                "[SubWeapon] '{}' reload animation is not wired: its gun resource (sbw/guns/{}.json) " +
                        "has no Animation.Reload / ReloadNormal / ReloadEmpty, so there is no clip to play. " +
                        "Add one pointing at a clip in {}.",
                subId, subId, AttachmentModelReloadListener.animPath,
            )

            else -> {
                // 从 clip 名反推"它应该在哪个文件里"：`animation.<fileId>.<clip>` → `<fileId>.animation.json`
                val fileId = detail.removePrefix("animation.").substringBefore('.')
                Mod.LOGGER.info(
                    "[SubWeapon] reload clip '{}' of '{}' is not bound: no clip with that name in {} " +
                            "(expected it to come from {}/{}.animation.json). " +
                            "⚠ That folder is bound to the ATTACHMENT model by file name, so the clip must " +
                            "animate bones that exist in the attachment model (root / gun / tube / trigger / " +
                            "projectile), NOT the gun model's (righthand / camera / head / undefined) — " +
                            "channels for missing bones are dropped silently, which looks exactly like 'no animation'.",
                    detail, subId, AttachmentModelReloadListener.animPath,
                    AttachmentModelReloadListener.animPath, fileId,
                )
            }
        }
    }

    fun consumePendingShellEjects(): List<Int> {
        if (pendingShellEjects.isEmpty()) return emptyList()

        val result = ArrayList(pendingShellEjects)
        pendingShellEjects.clear()
        return result
    }

    fun consumePendingParticles(): List<ParticleEffectData> {
        if (pendingParticles.isEmpty()) return emptyList()

        val result = ArrayList(pendingParticles)
        pendingParticles.clear()
        return result
    }

    private fun isDrumLevel(): Boolean {
        return GunData.from(stack).isDrumLevel()
    }

    private fun handGuardName(base: String?, handGuard: String?): String? {
        if (handGuard == null || !animations.containsKey(handGuard) || !GunData.from(stack).showsHandGuard()) return base
        return handGuard
    }

    private fun normalReloadName(animation: GunAnimation): String? {
        if (isDeployedGun()) {
            val deployName = animation.reloadNormalDeploy
            if (deployName != null && animations.containsKey(deployName)) return deployName
        }
        if (isDrumLevel()) {
            val drumName = animation.reloadNormalDrum
            if (drumName != null && animations.containsKey(drumName)) return drumName
        }
        return handGuardName(animation.reloadNormal, animation.reloadNormalHandGuard)
    }

    private fun emptyReloadName(animation: GunAnimation): String? {
        if (isDeployedGun()) {
            val deployName = animation.reloadEmptyDeploy
            if (deployName != null && animations.containsKey(deployName)) return deployName
        }
        if (isDrumLevel()) {
            val drumName = animation.reloadEmptyDrum
            if (drumName != null && animations.containsKey(drumName)) return drumName
        }
        return handGuardName(animation.reloadEmpty, animation.reloadEmptyHandGuard)
    }

    private fun hostFireName(animation: GunAnimation?, stack: ItemStack): String? {
        if (animation == null) return null
        // 两个都装上时护木优先：它换的是枪管那一段
        val handGuard = handGuardName(animation.fire, animation.fireHandGuard)
        if (handGuard != animation.fire) return handGuard
        val stock = animation.fireStock
        if (stock != null && animations.containsKey(stock) && GunData.from(stack).attachment.has(AttachmentType.STOCK)) {
            return stock
        }
        return animation.fire
    }

    private fun isDeployedGun(): Boolean {
        val player = localPlayer ?: return false
        return DeployedWeaponHandler.isDeployed(player)
    }

    /**
     * `GunAnimation.Melee` 可以写成字符串（单段，旧数据）或列表（连招各段各一支 clip）。
     *
     * 下标来自 `MeleeAction.Animation ?: melee[idx % size]`，**idx 由动作锁在挥击开始时锁存**
     * （动画状态机只在状态切换那一帧解析 clip 名，中途改下标会让动画和判定对不上）。
     */
    private fun meleeName(animation: GunAnimation): String? {
        val names = animation.melee?.list ?: return null
        if (names.isEmpty()) return null
        return names[ClientEventHandler.currentMeleeIndex(stack).mod(names.size)]
    }

    /** 动作锁当前锁存的那一段动作；解析失败时返回 null */
    private fun currentMeleeAction(): MeleeAction? {
        val data = GunData.from(stack)
        if (!data.hasMeleeAttack()) return null
        return try {
            val actions = data.meleeActions()
            actions[ClientEventHandler.currentMeleeIndex(stack).mod(actions.size)]
        } catch (_: Exception) {
            null
        }
    }

    /** 本段动作自己声明的动画候选链（`MeleeAction.Animation`），优先于 `GunAnimation.Melee` */
    private fun meleeActionAnimations(): List<String> =
        currentMeleeAction()?.animationCandidates().orEmpty()

    /**
     * 解析 MELEE 状态实际使用的 clip 名
     */
    private fun resolveMeleeName(): String? {
        val resource = GunResource.from(stack)
        val animation = resource.compute().animation ?: return null
        val names = animation.melee?.list.orEmpty()

        // 右键蓄势：抬起保持播 `HoldAnimation`，松开那一下播 `ReleaseAnimation`
        val variant = if (ClientEventHandler.isGunMeleeHoldActive(stack)) {
            currentMeleeAction()?.holdAnimationCandidates()
        } else if (ClientEventHandler.currentMeleeHeavyRelease(stack)) {
            currentMeleeAction()?.releaseAnimationCandidates()
        } else {
            null
        }
        if (!variant.isNullOrEmpty()) {
            val resolved = GunAnimationNames.resolveFirst(variant, resource.id) { animations.containsKey(it) }
            if (resolved != null) {
                loggedMeleeMiss = null
                return resolved
            }
        }

        // 冲锋撞人那一下换 clip（和 `FireStock` 换 `Fire` 同一套思路）：只换动画，
        // 本段时长/判定/效果还是动作表里那一段
        val chargeHit = animation.bayonetChargeHit
        if (chargeHit != null && ClientEventHandler.currentMeleeFromBayonetCharge(stack)) {
            val resolved = GunAnimationNames.resolve(chargeHit, resource.id)
            if (animations.containsKey(resolved)) {
                loggedMeleeMiss = null
                return resolved
            }
            if (animations.isNotEmpty()) {
                logMeleeMissOnce(
                    "charge-hit=$resolved",
                    "Bayonet charge hit animation '{}' not found in the animation file of {}",
                    resolved,
                    stack.item
                )
            }
        }

        val candidates = meleeActionAnimations()
        if (candidates.isNotEmpty()) {
            val resolved = GunAnimationNames.resolveFirst(candidates, resource.id) { animations.containsKey(it) }
            if (resolved != null) {
                loggedMeleeMiss = null
                return resolved
            }

            // 资源还没加载完时（animations 为空）不打日志，下一帧还会再解析一次
            if (animations.isNotEmpty()) {
                logMeleeMissOnce(
                    "candidates=$candidates",
                    "Melee action animation candidates {} not found in the animation file of {}; " +
                            "falling back to GunAnimation.Melee[0]",
                    candidates.map { GunAnimationNames.resolve(it, resource.id) },
                    stack.item
                )
            }
        }

        val fallback = meleeName(animation)
        if (fallback == null) {
            logMeleeMissOnce(
                "no-melee-clip",
                "No melee animation declared (GunAnimation.Melee) for {}", stack.item
            )
            return null
        }
        if (animations.containsKey(fallback)) {
            loggedMeleeMiss = null
            return fallback
        }

        // 资源还没加载好 / 名字写错：回退到列表第一支，并记一条 error
        val first = names.firstOrNull()
        logMeleeMissOnce(
            "fallback=$fallback",
            "Melee animation '{}' not found in the animation file of {}; falling back to '{}'",
            fallback, stack.item, first
        )
        return first?.takeIf { animations.containsKey(it) }
    }

    /**
     * 同一条解析失败只打一次日志
     */
    private fun logMeleeMissOnce(key: String, message: String, vararg args: Any?) {
        if (loggedMeleeMiss == key) return
        loggedMeleeMiss = key
        Mod.LOGGER.error(message, *args)
    }

    private fun animationName(state: GunAnimationState): String? =
        animationName(GunResource.compute(stack).animation, state)

    /**
     * 与 [animationName] 同一张表，但让调用方把已经取到的 [GunAnimation] 递进来。
     *
     * 蓄力层不在 [resolveState] 的状态机里（它是一条层，见 [updateChargeRunner]），
     * 只借这张表查 clip 名 —— 拆成重载是为了不把 `Charge` 那一支抄第二遍。
     */
    private fun animationName(animation: GunAnimation?, state: GunAnimationState): String? {
        if (animation == null) return null
        return when (state) {
            GunAnimationState.IDLE -> handGuardName(animation.idle, animation.idleHandGuard)
            GunAnimationState.EDIT -> handGuardName(animation.edit, animation.editHandGuard)
            GunAnimationState.BOLT -> animation.bolt
            GunAnimationState.RELOAD -> handGuardName(animation.reload, animation.reloadHandGuard)
            GunAnimationState.RELOAD_NORMAL -> normalReloadName(animation)
            GunAnimationState.RELOAD_EMPTY -> emptyReloadName(animation)
            GunAnimationState.PREPARE -> animation.prepare
            GunAnimationState.PREPARE_LOAD -> animation.prepareLoad
            GunAnimationState.ITERATIVE -> animation.iterative
            GunAnimationState.ITERATIVE_2 -> animation.iterative
            GunAnimationState.FINISH -> animation.finish
            GunAnimationState.MELEE -> resolveMeleeName()
            GunAnimationState.FIRE -> hostFireName(animation, stack)
            GunAnimationState.CHARGE -> animation.charge
        }
    }

    private fun reloadTicks(state: GunAnimationState, data: GunData): Int {
        val rawTicks = when (state) {
            GunAnimationState.RELOAD_NORMAL -> data.get(GunProp.NORMAL_RELOAD_TIME)
            GunAnimationState.RELOAD_EMPTY -> data.get(GunProp.EMPTY_RELOAD_TIME)
            GunAnimationState.RELOAD ->
                if (data.reload.empty()) data.get(GunProp.EMPTY_RELOAD_TIME)
                else data.get(GunProp.NORMAL_RELOAD_TIME)

            GunAnimationState.PREPARE_LOAD -> data.get(GunProp.PREPARE_LOAD_TIME)
            GunAnimationState.PREPARE -> data.get(GunProp.PREPARE_TIME)
            GunAnimationState.ITERATIVE, GunAnimationState.ITERATIVE_2 -> data.get(GunProp.ITERATIVE_TIME)
            GunAnimationState.FINISH -> data.get(GunProp.FINISH_TIME)
            else -> 0
        }
        if (rawTicks <= 0) return 0

        // GunEventHandler starts at NORMAL/EMPTY + 1 when a barrel bullet exists,
        // and at EMPTY + 2 without one, so the final gameplay window is one tick shorter
        // when the weapon has a barrel bullet.
        val correctedTicks = rawTicks - if (data.item.hasBulletInBarrel(data)) 1 else 0
        return correctedTicks.coerceAtLeast(1)
    }

    private fun reloadPlaybackSpeed(state: GunAnimationState, animation: BedrockAnimation): Float {
        // MAE advances states by real time; scale it so the animation matches the gameplay reload window.
        val targetSeconds = reloadTicks(state, GunData.from(stack)) / 20.0f
        return if (animation.specifiedEndTimeS > 0f && targetSeconds > 0f) {
            animation.specifiedEndTimeS / targetSeconds
        } else {
            1f
        }
    }

    /**
     * 是否新开了一段挥击（每段只返回一次 `true`）
     */
    private fun consumeMeleeSwing(): Boolean {
        val serial = MeleeClientHandler.swingSerial
        if (serial <= consumedMeleeSerial) return false
        consumedMeleeSerial = serial
        return true
    }

    /** 提前衔接时把上一段片段留作淡出层 */
    private fun startMeleeFade(ticks: Float = MELEE_CHAIN_FADE_TICKS, cameraFade: Boolean = false) {
        val old = runner ?: return
        meleeFadeRunner = old
        meleeFadeDuration = ticks
        meleeFadeTicks = ticks
        meleeCameraFading = cameraFade
    }

    private fun tickMeleeFade() {
        val fade = meleeFadeRunner ?: return
        fade.tick()
        meleeFadeTicks -= Minecraft.getInstance().deltaFrameTime
            .coerceIn(0f, SPIN_MAX_FRAME_DELTA_TICKS)
        if (meleeFadeTicks <= 0f) clearMeleeFade()
    }

    private fun clearMeleeFade() {
        handOverMeleeCamera()
        meleeFadeRunner = null
        meleeFadeTicks = 0f
        meleeFadeDuration = 0f
    }

    /** 镜头交还给下一段：`cameraRotation` 还是上一帧的满幅值，得先定格在当前衰减结果上 */
    private fun handOverMeleeCamera() {
        if (!meleeCameraFading) return
        cameraRotation.set(getCameraRotation())
        meleeCameraFading = false
    }

    /** 淡出层（旧片段）的剩余权重 */
    private fun meleeFadeWeight(): Float {
        if (meleeFadeRunner == null || meleeFadeDuration <= 0f) return 0f
        return smoothstep((meleeFadeTicks / meleeFadeDuration).coerceIn(0f, 1f))
    }

    /** 相机晃动的剩余权重（1 = 不衰减）。`Idle` 没有 `camera` 骨骼，姿势混合补不到这一块 */
    private fun meleeCameraFadeWeight(): Float =
        if (meleeCameraFading) meleeFadeWeight() else 1f

    private fun applyMeleeFade(lowerPose: Pose): Pose {
        val fade = meleeFadeRunner ?: return lowerPose
        val weight = meleeFadeWeight()
        if (weight <= 0f) return lowerPose

        return CROSSFADE_BLENDER.combine(lowerPose, fade.evaluate()) { lower, upper ->
            when {
                upper.boneIndex() < 0 -> lower
                lower.boneIndex() < 0 -> upper
                else -> SimpleInterpolatorBlender.leanerLerpTransforms(
                    lower, upper, weight, maxOf(lower.boneIndex(), upper.boneIndex()), TRANSFORM_FACTORY
                )
            }
        }
    }

    private fun meleePlaybackSpeed(animation: BedrockAnimation): Float {
        // 抬刀保持按片段原速播完，然后停在最后一帧
        if (ClientEventHandler.isGunMeleeHoldActive(stack)) return 1f

        val duration = ClientEventHandler.currentMeleeDuration(stack)
            .takeIf { it > 0 }
            ?: GunData.from(stack).get(GunProp.MELEE_DURATION)
        val targetSeconds = duration.coerceAtLeast(1) / 20.0f
        return if (animation.specifiedEndTimeS > 0f) {
            animation.specifiedEndTimeS / targetSeconds
        } else {
            1f
        }
    }

    private fun play(state: GunAnimationState, keepProgress: Boolean = false, keepMeleeFade: Boolean = false) {
        // 淡出层主要服务挥击接挥击，离开近战时（收刀回待机）也跟着淡一次
        if (state != GunAnimationState.MELEE && !keepMeleeFade) clearMeleeFade()
        val name = animationName(state) ?: return
        val animation = animations[name] ?: return
        val playState = state.playType.state()
        if (state.isReload) {
            setAnimationSpeed(playState, reloadPlaybackSpeed(state, animation))
        } else if (state == GunAnimationState.MELEE) {
            setAnimationSpeed(playState, meleePlaybackSpeed(animation))
        }
        val newRunner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
        newRunner.state = playState
        // 只是换了变体（装上/卸下护木这类）：把播放头按比例接过去，不然整段会从头再播一遍
        if (keepProgress) {
            val old = runner
            val oldMax = old?.maxProgress ?: 0L
            if (old != null && oldMax > 0L) {
                newRunner.progress = (old.progress.toDouble() / oldMax * newRunner.maxProgress).toLong()
            }
        }
        runner = newRunner
        currentState = state
        cachedPose = newRunner.evaluate()
    }

    private fun playsCurrentClip(state: GunAnimationState): Boolean {
        val clip = animationName(state)?.let(animations::get) ?: return true
        return runner?.animation === clip
    }

    private fun playFire() {
        val fireName = pendingFireAnimation ?: return
        val fireAnimation = animations[fireName] ?: return

        val newRunner = AnimationRunner(fireAnimation, AnimationContext(fireAnimation.specifiedEndTimeS))
        newRunner.state = AnimationPlayType.PLAY_ONCE_STOP.state()
        fireRunner = newRunner
        cachedPose = newRunner.evaluate()
    }

    /**
     * 解析显式指定的开火 clip（`GunProp.SHOOT_ANIMATION`），没写或解析不出来时返回 `null`
     */
    private fun resolveOverrideFireName(stack: ItemStack, overrideAnimation: String?): String? {
        if (overrideAnimation.isNullOrBlank()) return null

        val name = GunAnimationNames.resolve(overrideAnimation, GunResource.from(stack).id)
        if (animations.containsKey(name)) {
            loggedOverrideFireMiss = null
            return name
        }

        // 资源还没加载完时（animations 为空）不打日志，下一帧还会再解析一次
        if (animations.isNotEmpty() && loggedOverrideFireMiss != overrideAnimation) {
            loggedOverrideFireMiss = overrideAnimation
            Mod.LOGGER.error(
                "ShootAnimation '{}' (resolved to '{}') not found in the animation file of {}; " +
                        "falling back to the normal fire animation",
                overrideAnimation,
                name,
                stack.item
            )
        }
        return null
    }

    /**
     * 解析本次开火实际使用的 clip 名
     */
    private fun resolveFireName(stack: ItemStack, candidates: List<String>, reportMissing: Boolean): String? {
        val resource = GunResource.from(stack)
        val animation = resource.compute().animation

        if (candidates.isNotEmpty()) {
            val resolved = GunAnimationNames.resolveFirst(candidates, resource.id) { animations.containsKey(it) }
            if (resolved != null) {
                loggedFireMiss = null
                return resolved
            }

            // 资源还没加载完时（animations 为空）不打日志，下一帧还会再解析一次
            if (animations.isNotEmpty()) {
                logFireMissOnce(
                    "candidates=$candidates",
                    reportMissing,
                    "Fire animation candidates {} not found in the animation file of {}; " +
                            "falling back to GunAnimation.Fire",
                    candidates.map { GunAnimationNames.resolve(it, resource.id) },
                    stack.item
                )
            }
        }

        return hostFireName(animation, stack)
    }

    /**
     * 同一条开火动画解析失败只打一次日志（与 [logMeleeMissOnce] 同一套去重）
     */
    private fun logFireMissOnce(key: String, fatal: Boolean, message: String, vararg args: Any?) {
        if (loggedFireMiss == key) return
        loggedFireMiss = key
        if (fatal) {
            Mod.LOGGER.error(message, *args)
        } else {
            Mod.LOGGER.debug(message, *args)
        }
    }

    private fun currentFireModeAnimation(): BedrockAnimation? {
        val animation = GunResource.compute(stack).animation ?: return null
        val modeName = GunData.from(stack).selectedFireModeInfo().name
        val suffix = modeName.lowercase(Locale.ROOT)
        return animation.fireModes.asSequence()
            .mapNotNull(animations::get)
            .firstOrNull { it.name.endsWith(".fire_mode_$suffix") }
    }

    private fun syncFireMode(): Boolean {
        val modeName = GunData.from(stack).selectedFireModeInfo().name
        if (lastFireModeName != null && lastFireModeName != modeName) {
            fireModeSwitchSerial++
        }
        lastFireModeName = modeName
        return updateFireModeRunner()
    }

    private fun updateFireModeRunner(): Boolean {
        val animation = currentFireModeAnimation() ?: run {
            fireModeRunner = null
            fireModeAnimationName = null
            return false
        }
        if (fireModeRunner != null && fireModeAnimationName == animation.name) return false

        fireModeAnimationName = animation.name
        val newRunner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
        newRunner.state = AnimationPlayType.LOOP.state()
        fireModeRunner = newRunner
        return true
    }

    private fun playFireModeSwitch() {
        val animation = GunResource.compute(stack).animation ?: return
        val switchName = animation.changeFireMode ?: return
        val switchAnimation = animations[switchName] ?: return

        val newRunner = AnimationRunner(switchAnimation, AnimationContext(switchAnimation.specifiedEndTimeS))
        newRunner.state = AnimationPlayType.PLAY_ONCE_STOP.state()
        fireModeSwitchRunner = newRunner
    }

    private fun consumeFireModeSwitch(editing: Boolean): Boolean {
        if (fireModeSwitchSerial <= consumedFireModeSwitchSerial) return false
        if (!editing) {
            playFireModeSwitch()
        }
        consumedFireModeSwitchSerial = fireModeSwitchSerial
        return !editing
    }

    private fun tickFireModeRunners(fireModeStarted: Boolean, switchStarted: Boolean = false) {
        if (fireModeRunner != null && !fireModeStarted) {
            fireModeRunner?.tick()
        }
        if (fireModeSwitchRunner != null && !switchStarted) {
            fireModeSwitchRunner?.tick()
        }
        if (fireModeSwitchRunner?.state is StopState) {
            fireModeSwitchRunner = null
        }
    }

    private fun clearFireModeLayers() {
        fireModeRunner = null
        fireModeAnimationName = null
        fireModeSwitchRunner = null
        fireModeSwitchSerial = 0
        consumedFireModeSwitchSerial = 0
        lastFireModeName = null
    }

    private fun startEditExit() {
        val animation = GunResource.compute(stack).animation ?: return
        val editName = animation.edit ?: return
        val editAnimation = animations[editName] ?: return

        val newRunner = AnimationRunner(editAnimation, AnimationContext(editAnimation.specifiedEndTimeS))
        newRunner.progress = newRunner.maxProgress
        val reverseState = PlayingState({ System.nanoTime() }, { StopState() })
        reverseState.speed = -EDIT_EXIT_SPEED
        newRunner.state = reverseState
        editExitRunner = newRunner
    }

    private fun tickEditExit() {
        val exitRunner = editExitRunner ?: return

        exitRunner.tick()
        if (exitRunner.state is StopState) {
            editExitRunner = null
            runner = null
            currentState = null
            return
        }

        val fireModeStarted = syncFireMode()
        val data = GunData.from(stack)
        val animation = GunResource.compute(stack).animation
        val (holdOpenStarted, closeStrikeStarted) = updateMechanicalRunners(data, animation)
        tickMechanicalRunners(holdOpenStarted, closeStrikeStarted)
        tickSpinRunner(updateSpinRunner(data, animation))
        // 编辑收尾期间火焰照旧按扳机淡出（这时候玩家手上就是平常拿着枪，没有"在编辑"这回事）
        tickFireLoopRunner(updateFireLoopRunner(animation))
        val switchStarted = consumeFireModeSwitch(false)
        tickFireModeRunners(fireModeStarted, switchStarted)

        collectParticleEvents(fireRunner)
        collectParticleEvents(fireModeRunner)
        collectParticleEvents(fireModeSwitchRunner)
        collectSoundEvents(fireRunner)
        collectSoundEvents(fireModeRunner)
        collectSoundEvents(fireModeSwitchRunner)
        collectSoundEvents(holdOpenRunner)
        collectSoundEvents(closeStrikeRunner)
        if (fireRunner?.state is StopState) {
            fireRunner = null
        }

        cachedPose = combineHoldOpen(
            combineFireModeSwitch(
                combineLayers(
                    overLoop(exitRunner.evaluate()),
                    fireModeRunner?.evaluate() ?: DummyPose.INSTANCE,
                    closeStrikeRunner?.evaluate() ?: DummyPose.INSTANCE,
                    spinRunner?.evaluate() ?: DummyPose.INSTANCE
                ),
                fireModeSwitchRunner?.evaluate() ?: DummyPose.INSTANCE,
                fireRunner?.evaluate() ?: DummyPose.INSTANCE
            ),
            holdOpenRunner?.evaluate() ?: DummyPose.INSTANCE
        )
    }

    private fun updateMechanicalRunners(
        data: GunData,
        animation: GunAnimation?
    ): Pair<Boolean, Boolean> {
        // The bolt must be held open as soon as the magazine runs dry, even while the
        // fire animation is still playing, so this is not gated on fireRunner.
        val shouldHoldOpen = data.holdOpen.get()
        val holdOpenStarted = updateHoldOpen(if (shouldHoldOpen) animation?.holdOpen else null)
        val shouldCloseStrike = data.closeStrike.get()
        val closeStrikeStarted = updateCloseStrike(if (shouldCloseStrike) animation?.closeStrike else null)
        return holdOpenStarted to closeStrikeStarted
    }

    private fun tickMechanicalRunners(holdOpenStarted: Boolean, closeStrikeStarted: Boolean) {
        if (holdOpenRunner != null && !holdOpenStarted) {
            holdOpenRunner?.tick()
        }
        if (closeStrikeRunner != null && !closeStrikeStarted) {
            closeStrikeRunner?.tick()
        }
    }

    /**
     * 推进枪管旋转层
     */
    private fun updateSpinRunner(data: GunData, animation: GunAnimation?): Boolean {
        val hold = if (animation == null) spinAnimation else animation.hold?.let(animations::get)
        if (hold == null) {
            spinPower = 0f
            applySpinSpeed(0f)
            return false
        }

        val runner = spinRunner
        if (runner == null || spinAnimation !== hold) {
            spinAnimation = hold
            val newRunner = AnimationRunner(hold, AnimationContext(hold.specifiedEndTimeS))
            newRunner.state = AnimationPlayType.LOOP.state()
            spinRunner = newRunner
            return true
        }

        val target = if (shouldSpin()) 1f else 0f
        val durationTicks = spinDurationTicks(data).toFloat()
        val step = if (durationTicks <= 0f) 1f
        else Minecraft.getInstance().deltaFrameTime.coerceIn(0f, SPIN_MAX_FRAME_DELTA_TICKS) / durationTicks
        spinPower = approach(spinPower, target, step)

        // 射速每帧重算：切模式、换 perk 立刻体现在转速上
        applySpinSpeed(holdSpinSpeed(hold, data) * smoothstep(spinPower))
        return false
    }

    private fun tickSpinRunner(started: Boolean) {
        if (started) return
        spinRunner?.tick()
    }

    private fun clearSpinRunner() {
        spinRunner = null
        spinAnimation = null
        spinPower = 0f
    }

    /**
     * 推进循环开火层
     */
    private fun updateFireLoopRunner(animation: GunAnimation?): Boolean {
        val loop = if (animation == null) fireLoopAnimation else animation.fireLoop?.let(animations::get)
        val runner = fireLoopRunner

        if (loop == null || (runner == null && !shouldFireLoop())) {
            clearFireLoopRunner()
            return false
        }

        // 时长每帧重算：`/reload` 改了秒数立刻生效，不用等下一次重建 runner
        if (animation != null) {
            fireLoopFadeInTicks = secondsToTicks(animation.fireLoopFadeIn)
            fireLoopFadeOutTicks = secondsToTicks(animation.fireLoopFadeOut)
        }

        if (runner == null || fireLoopAnimation !== loop) {
            fireLoopAnimation = loop
            val newRunner = AnimationRunner(loop, AnimationContext(loop.specifiedEndTimeS))
            newRunner.state = AnimationPlayType.LOOP.state()
            fireLoopRunner = newRunner
            // 换了一支片段就从 0 重新淡入：接着上一支的权重会闪一下
            fireLoopPower = 0f
            return true
        }

        val target = if (shouldFireLoop()) 1f else 0f
        val fadeTicks = if (target > fireLoopPower) fireLoopFadeInTicks else fireLoopFadeOutTicks
        val step = if (fadeTicks <= 0f) 1f
        else Minecraft.getInstance().deltaFrameTime.coerceIn(0f, SPIN_MAX_FRAME_DELTA_TICKS) / fadeTicks
        fireLoopPower = approach(fireLoopPower, target, step)

        // 淡出到底、扳机也松了 → 整层摘掉（此刻权重已经是 0，摘掉看不出任何变化）
        if (fireLoopPower <= 0f && target <= 0f) {
            clearFireLoopRunner()
        }
        return false
    }

    private fun tickFireLoopRunner(started: Boolean) {
        if (started) return
        fireLoopRunner?.tick()
    }

    private fun clearFireLoopRunner() {
        fireLoopRunner = null
        fireLoopAnimation = null
        fireLoopPower = 0f
        fireLoopFadeInTicks = 0f
        fireLoopFadeOutTicks = 0f
    }

    /**
     * 推进蓄力层（`GunAnimation.Charge`）。
     *
     * **这是一条层，不是基础状态** —— 与 [GunAnimationState.FIRE] 同理：`ql_1031.charge` 只
     * key 了 `root`，而 `righthand` / `lefthand` 两个手部锚点的姿势是基础状态的 `Idle` 给的
     * （那支只有一帧，`scale = [1, 1.2, 1]`，见 `GunAnimation.idle` 的注释与
     * `VerifyArmScale`）。把它当基础状态播，这 1 秒里双臂锚点会掉回绑定姿势，枪会被"甩出去"。
     *
     * 三种结局：
     * 1. 按下蓄力 → 正向播放（`PLAY_ONCE_HOLD`，播到末帧停住）；
     * 2. 没到发射标准就松手 → 相位冻在原地，权重（[chargePower]）在
     *    [CHARGE_CANCEL_FADE_TICKS] 里退到 0，也就是**从当前姿势缓出**回基础状态；
     * 3. 打出去了 → 整层立刻摘掉，让位给开火动画。
     *
     * 第 2 条曾经是"2 倍速倒放回原点"，改成了缓出：倒放等于把刚做过的动作再倒着演一遍，
     * 而缓出只让姿势**散掉**，松手的感觉是"松劲儿"而不是"倒带"。
     *
     * @param firedThisFrame 这一帧有没有真的打出去。调用方必须在 [playFire] **消费 `fireSerial`
     *   之前**读出来传进来（松开蓄力那一帧 `chargeActive` 已经归零，"打出去了"只剩这一个信号）。
     * @return 是否**新建**了 runner（新建的这一帧不能再 tick，否则同一帧走两步）
     */
    private fun updateChargeRunner(animation: GunAnimation?, firedThisFrame: Boolean): Boolean {
        val clip = animationName(animation, GunAnimationState.CHARGE)?.let(animations::get)
        val active = clip != null && isCharging()
        val runner = chargeRunner
        val wasActive = chargeWasActive
        chargeWasActive = active

        // 没配片段，或者从来没起过层 → 什么都不用做
        if (clip == null || (runner == null && !active)) {
            clearChargeRunner()
            return false
        }

        // 打出去了 → 立刻摘掉，让位给开火动画。
        // 判在 [active] **之前**：常见的自动开火/松手开火那一帧 `chargeActive` 确实已经归零，
        // 但服务端驱动的开火（`ShootClientMessage` 那一类）打进来时 `chargeActive` 还立着，
        // 排在后面就会漏掉，蓄力动画会一直挂在枪上。
        if (firedThisFrame) {
            clearChargeRunner()
            return false
        }

        if (active) {
            // 底下垫着奔跑层的时候淡入（疾跑中拉弓，姿势不会当场跳过去），平时按下就是满权重
            chargePower = if (!wasActive) {
                if (customSprintLayer.visible) 0f else 1f
            } else {
                approach(
                    chargePower, 1f,
                    Minecraft.getInstance().deltaFrameTime.coerceIn(0f, SPIN_MAX_FRAME_DELTA_TICKS) /
                            ACTION_HANDOVER_TICKS
                )
            }
            // 头一帧建层；缓出到一半又按下去时（`!wasActive`）也是重建 —— 从第 0 帧重来才和
            // 游戏里的 `chargeProgress` 同步，接着缓出的相位往前播会让动画和蓄力进度对不上
            if (runner == null || chargeAnimation !== clip || !wasActive) {
                chargeAnimation = clip
                val newRunner = AnimationRunner(clip, AnimationContext(clip.specifiedEndTimeS))
                val playState = GunAnimationState.CHARGE.playType.state()
                setAnimationSpeed(playState, chargePlaybackSpeed(clip, GunData.from(stack)))
                newRunner.state = playState
                chargeRunner = newRunner
                return true
            }
            return false
        }

        // 松手那一帧先不动权重，下一帧才开始退
        if (wasActive) {
            return false
        }

        chargePower = approach(
            chargePower, 0f,
            Minecraft.getInstance().deltaFrameTime.coerceIn(0f, SPIN_MAX_FRAME_DELTA_TICKS) /
                    CHARGE_CANCEL_FADE_TICKS
        )
        if (chargePower <= 0f) clearChargeRunner()
        return false
    }

    private fun tickChargeRunner(started: Boolean) {
        if (started) return
        // 缓出期间冻住相位：只让权重退，不让枪继续做蓄力动作
        if (!chargeWasActive) return
        chargeRunner?.tick()
    }

    private fun clearChargeRunner() {
        chargeRunner = null
        chargeAnimation = null
        chargePower = 0f
        chargeWasActive = false
    }

    /**
     * 这把枪此刻是不是"正在蓄力"。
     *
     * 先认**本地玩家手里的这把枪**：[ClientEventHandler.chargeActive] 是全局字段，
     * 副手那把枪、别人手里的同一把枪都会读到它，不认枪的话它们会跟着一起抖
     * （同 [shouldFireLoop] / [shouldSpin]）。
     */
    private fun isCharging(): Boolean {
        val player = localPlayer ?: return false
        if (player.mainHandItem.item !== stack.item) return false
        if (!ClientEventHandler.chargeActive) return false
        return GunData.from(stack).selectedFireModeInfo().isChargeMode()
    }

    /**
     * 蓄力动画的播放倍率：让片段自己的时长对上蓄力模式的 `Duration`。
     *
     * 算法同 [reloadPlaybackSpeed] —— MAE 按真实时间推进，倍率就是"片段时长 / 数据时长"。
     * `ql_1031` 两者都是 1 秒（`Duration = 20` tick，`animation_length = 1`），所以是 `1.0`；
     * 以后改 `Duration` 或换一支片段都不用回来手调这个常数。
     */
    private fun chargePlaybackSpeed(animation: BedrockAnimation, data: GunData): Float {
        val seconds = (data.selectedFireModeInfo().chargeConfig()?.effectiveDuration ?: return 1f) /
                TICKS_PER_SECOND
        return if (animation.specifiedEndTimeS > 0f && seconds > 0f) {
            animation.specifiedEndTimeS / seconds
        } else {
            1f
        }
    }

    /** 数据里的**秒** → tick。缓动要的步长单位是 tick（`deltaFrameTime` 就是 tick，20/s）。 */
    private fun secondsToTicks(seconds: Float): Float =
        if (seconds <= 0f) 0f else seconds * TICKS_PER_SECOND

    /**
     * 把循环开火层按 [fireLoopPower]**交叉淡入/淡出**到 [lowerPose] 上
     */
    private fun applyFireLoop(lowerPose: Pose): Pose {
        val runner = fireLoopRunner ?: return lowerPose
        val weight = smoothstep(fireLoopPower)
        if (weight <= 0f) return lowerPose

        val loopPose = runner.evaluate()
        return CROSSFADE_BLENDER.combine(lowerPose, loopPose) { lower, loop ->
            if (loop.boneIndex() < 0) lower
            else SimpleInterpolatorBlender.leanerLerpTransforms(
                lower, loop, weight, maxOf(lower.boneIndex(), loop.boneIndex()), TRANSFORM_FACTORY
            )
        }
    }

    /**
     * 是否循环播放开火
     */
    private fun shouldFireLoop(): Boolean {
        val player = localPlayer ?: return false
        if (player.mainHandItem.item !== stack.item) return false
        return ClientEventHandler.holdingFireKey
    }

    /**
     * 这把枪现在该不该转
     */
    private fun shouldSpin(): Boolean {
        val player = localPlayer ?: return false
        if (player.mainHandItem.item !== stack.item) return false
        return ClientEventHandler.isBarrelSpinTriggered(stack)
    }

    /**
     * 缓入缓出用的时长（tick）：就是这把枪的蓄力时长——蓄力武器看蓄力配置，其余看 `ShootDelay`。
     * 与 `handleShootDelay` 里 `holdingFireKeyTicks` 的上限同源，所以转速升满、蓄力满、第一发子弹出膛
     * 是同一个瞬间。
     */
    private fun spinDurationTicks(data: GunData): Int = barrelSpinUpTicks(data)

    /**
     * 满速时的播放倍率
     */
    private fun holdSpinSpeed(hold: BedrockAnimation, data: GunData): Float {
        val endTimeS = hold.specifiedEndTimeS
        if (endTimeS <= 0f) return 1f
        return ClientEventHandler.effectiveRpm(data).toFloat() / (360f / endTimeS)
    }

    private fun applySpinSpeed(speed: Float) {
        setAnimationSpeed(spinRunner?.state, speed)
    }

    // ── 权重循环层（自定义奔跑 / 刺刀冲锋）──────────────────────────────────────

    // 两条权重层取同一份缓动参数
    private fun sprintEaseDelta(): Float = ClientEventHandler.sprintDelta(Minecraft.getInstance().deltaFrameTime)

    // 循环片段的播放倍率：按实际水平移速线性走，标准疾跑速度 = 片段作者写的 1.0 倍速
    private fun loopPlaybackSpeed(): Float {
        val speed = localPlayer?.deltaMovement?.horizontalDistance() ?: 0.0
        return (speed / SPRINT_SPEED_REF).toFloat().coerceIn(LOOP_MIN_SPEED, LOOP_MAX_SPEED)
    }

    // 和 handleWeaponMove 的 customWeight 同源
    private fun sprintEaseWeight(): Double =
        sprintEaseHandling().let { it.item.getHandlingWeight(it) }.coerceIn(1.0, 50.0)

    private fun sprintEaseSwitchSpeed(): Double =
        sprintEaseHandling().get(GunProp.SPRINT_TRANSITION_SPEED)

    // 淡出片段按枪型分，跟时长同源（和 handleWeaponMove 的 sprintHandgun 一样取主武器）
    private fun sprintEaseHandgun(): Boolean =
        sprintEaseHandling().get(GunProp.GUN_TYPE) == GunType.HANDGUN

    private fun sprintEaseHandling(): GunData =
        localPlayer?.let { ActiveGun.handlingData(it) } ?: GunData.from(stack)

    // 循环层：权重取程序性奔跑那对样张曲线（相位 → 权重），时长也是同一套
    private class WeightedLoopLayer {
        private var runner: AnimationRunner? = null
        private var clip: BedrockAnimation? = null

        // 权重曲线上的相位；[entering] = 现在走的是进曲线还是出曲线
        private var progress = 0f
        private var entering = true
        private var power = 0f

        // 让位：动作片段接管基础状态时在 ACTION_HANDOVER_TICKS 里退干净，不走进出曲线
        private var yielding = false
        private var yieldFrom = 0f
        private var yieldTicks = 0f

        // 这一帧片段时间走没走
        private var ticking = false

        // 这一帧真走了时间的 runner，没走的那几帧不给事件（`clip()` 会重报上一窗口的键）
        val activeRunner: AnimationRunner?
            get() = if (ticking) runner else null

        val visible: Boolean
            get() = runner != null && power > 0f

        // 推进这一层：换片段不动权重，退到看不见就收层
        fun tick(
            clip: BedrockAnimation?,
            want: Boolean,
            yieldNow: Boolean,
            handgun: Boolean,
            deltaTicks: Float,
            weight: Double,
            switchSpeed: Double,
            speed: Float,
        ) {
            // 上一帧走没走必须在清零之前读
            val wasTicking = ticking
            ticking = false
            if (clip == null) {
                clear()
                return
            }

            if (yieldNow) {
                if (!yielding) {
                    yielding = true
                    yieldFrom = power
                    yieldTicks = 0f
                }
                yieldTicks += deltaTicks
                val x = (yieldTicks / ACTION_HANDOVER_TICKS).coerceIn(0f, 1f)
                power = yieldFrom * (1f - x * x * (3f - 2f * x))
                if (power <= CUSTOM_SPRINT_MIN_POWER) {
                    clear()
                }
                return
            }

            // 刚让位完又回来（或掉头）时，相位要按当前权重接回去，不然权重会跳
            val resume = yielding
            yielding = false
            if (rebuild(clip, want)) return

            val curve =
                if (want) GunSprintAnimation.IN_ROTATION else GunSprintAnimation.outRotation(handgun)
            if (want != entering || resume) {
                progress = curve.phaseFor(power)
                entering = want
            }

            val duration = ClientEventHandler.sprintEaseDuration(want, weight, switchSpeed)
            val step = if (deltaTicks <= 0f || duration <= 0.0) 1f else (deltaTicks / duration).toFloat()
            progress = (progress + step).coerceAtMost(1f)
            power = curve.atMonotone(progress, power)

            // 退到看不见就收层
            if (!want && power <= CUSTOM_SPRINT_MIN_POWER) {
                clear()
                return
            }

            val resuming = want && !wasTicking

            // 离场把片段冻住，摆动不跟着权重一起收
            ticking = want
            if (want) {
                runner?.let {
                    setAnimationSpeed(it.state, speed)
                    // MAE 的状态时钟是真实时间：冻住那几帧照走，接着跑前先拨到当下，不然进度一步跳完
                    if (resuming) it.state?.onEnter(it.animationContext)
                    it.tick()
                }
            }
        }

        // 资源重载 / 换片段：权重留在原地，只换 runner。返回是否刚建过
        private fun rebuild(clip: BedrockAnimation, want: Boolean): Boolean {
            if (runner == null && !want) return false
            if (runner != null && this.clip === clip) return false

            this.clip = clip
            val newRunner = AnimationRunner(clip, AnimationContext(clip.specifiedEndTimeS))
            newRunner.state = AnimationPlayType.LOOP.state()
            runner = newRunner
            return true
        }

        fun clear() {
            runner = null
            clip = null
            progress = 0f
            entering = true
            power = 0f
            yielding = false
            yieldTicks = 0f
            ticking = false
        }

        // 把这一层按权重混到 lowerPose 上
        fun apply(lowerPose: Pose): Pose {
            val runner = runner ?: return lowerPose
            val weight = power
            if (weight <= 0f) return lowerPose

            val layerPose = runner.evaluate()
            return CROSSFADE_BLENDER.combine(lowerPose, layerPose) { lower, upper ->
                if (upper.boneIndex() < 0) lower
                else TRANSFORM_FACTORY.createBoneTransform(
                    maxOf(lower.boneIndex(), upper.boneIndex()),
                    lower.translation().lerp(upper.translation(), weight, Vector3f()),
                    MathUtil.nlerpShortestPath(
                        lower.rotation().asQuaternion(), upper.rotation().asQuaternion(), weight
                    ),
                    lower.scale().lerp(upper.scale(), weight, Vector3f())
                )
            }
        }
    }

    private fun updateDeployIdle(animation: GunAnimation?, active: Boolean): Boolean {
        val clip = animation?.idleDeploy?.let(animations::get)
        if (clip == null) {
            clearDeployIdle()
            return false
        }

        val runner = deployIdleRunner
        if (runner == null) {
            if (!active) {
                clearDeployIdle()
                return false
            }
            deployIdleAnimation = clip
            val newRunner = AnimationRunner(clip, AnimationContext(clip.specifiedEndTimeS))
            newRunner.state = AnimationPlayType.LOOP.state()
            deployIdleRunner = newRunner
            return true
        }

        if (deployIdleAnimation !== clip) {
            deployIdleAnimation = clip
            val newRunner = AnimationRunner(clip, AnimationContext(clip.specifiedEndTimeS))
            newRunner.state = AnimationPlayType.LOOP.state()
            deployIdleRunner = newRunner
            return true
        }

        deployIdlePower = if (active) {
            ClientEventHandler.bipodViewTime.toFloat().coerceIn(0f, 1f)
        } else {
            approach(deployIdlePower, 0f, ClientEventHandler.bipodViewStep().toFloat())
        }

        if (!active && deployIdlePower <= CUSTOM_SPRINT_MIN_POWER) {
            clearDeployIdle()
        }
        return false
    }

    private fun tickDeployIdle(started: Boolean) {
        if (started) return
        deployIdleRunner?.tick()
    }

    private fun clearDeployIdle() {
        deployIdleRunner = null
        deployIdleAnimation = null
        deployIdlePower = 0f
    }

    private fun applyDeployIdle(lowerPose: Pose): Pose {
        val runner = deployIdleRunner ?: return lowerPose
        val weight = smoothstep(deployIdlePower.coerceIn(0f, 1f))
        if (weight <= 0f) return lowerPose

        val deployPose = runner.evaluate()
        return CROSSFADE_BLENDER.combine(lowerPose, deployPose) { lower, deploy ->
            if (deploy.boneIndex() < 0) lower
            else TRANSFORM_FACTORY.createBoneTransform(
                maxOf(lower.boneIndex(), deploy.boneIndex()),
                lower.translation().lerp(deploy.translation(), weight, Vector3f()),
                MathUtil.nlerpShortestPath(
                    lower.rotation().asQuaternion(), deploy.rotation().asQuaternion(), weight
                ),
                lower.scale().lerp(deploy.scale(), weight, Vector3f())
            )
        }
    }

    private fun wantsDeployIdle(target: GunAnimationState?, animation: GunAnimation?): Boolean {
        if (animation?.idleDeploy == null) return false
        if (!isDeployedGun()) return false
        return target == GunAnimationState.IDLE
    }

    // ── 常驻循环层（`Animation.Loop`）──────────────────────────────────────────

    private fun updateLoopRunner(animation: GunAnimation?): Boolean {
        val clip = animation?.loop?.let(animations::get)
        if (clip == null) {
            loopRunner = null
            loopAnimation = null
            return false
        }

        if (loopRunner != null && loopAnimation === clip) return false

        loopAnimation = clip
        val newRunner = AnimationRunner(clip, AnimationContext(clip.specifiedEndTimeS))
        newRunner.state = AnimationPlayType.LOOP.state()
        loopRunner = newRunner
        return true
    }

    private fun tickLoopRunner(animation: GunAnimation?) {
        if (updateLoopRunner(animation)) return
        loopRunner?.tick()
    }

    private fun clearLoopRunner() {
        loopRunner = null
        loopAnimation = null
    }

    // 两边都 key 的骨骼听上层的，只有常驻层 key 的听常驻层
    private fun overLoop(upperPose: Pose): Pose {
        val loop = loopRunner ?: return upperPose
        if (upperPose == DummyPose.INSTANCE) return loop.evaluate()
        return MERGE_BLENDER.blend(listOf(loop.evaluate(), upperPose))
    }

    /**
     * 把蓄力层按 [chargePower]**交叉淡出**到 [lowerPose] 上（[applyFireLoop] 的蓄力版）。
     *
     * 权重恒为 1 的时候就是个 nlerp 到 1，等价于直接盖住；只有"没到发射标准就松手"那段
     * 权重才会从 1 退到 0，把当前姿势溶回基础状态。
     */
    private fun applyCharge(lowerPose: Pose): Pose {
        val runner = chargeRunner ?: return lowerPose
        val weight = smoothstep(chargePower)
        if (weight <= 0f) return lowerPose

        val chargePose = runner.evaluate()
        return CROSSFADE_BLENDER.combine(lowerPose, chargePose) { lower, charge ->
            if (charge.boneIndex() < 0) lower
            else SimpleInterpolatorBlender.leanerLerpTransforms(
                lower, charge, weight, maxOf(lower.boneIndex(), charge.boneIndex()), TRANSFORM_FACTORY
            )
        }
    }

    /** smoothstep：起步和到顶都是缓的（缓入缓出）。 */
    private fun smoothstep(power: Float): Float = power * power * (3f - 2f * power)

    private fun approach(current: Float, target: Float, step: Float): Float {
        return if (current < target) (current + step).coerceAtMost(target)
        else (current - step).coerceAtLeast(target)
    }

    private fun combineLayers(vararg layers: Pose): Pose {
        var result: Pose? = null
        for (layer in layers) {
            if (layer == DummyPose.INSTANCE) continue
            result = if (result == null) layer else BLENDER.blend(result, layer)
        }
        return result ?: DummyPose.INSTANCE
    }

    private fun combineFireModeSwitch(
        lowerPose: Pose,
        switchPose: Pose,
        upperPose: Pose
    ): Pose {
        val pose = if (switchPose == DummyPose.INSTANCE) {
            lowerPose
        } else {
            MERGE_BLENDER.blend(listOf(lowerPose, switchPose))
        }
        return combineLayers(pose, upperPose)
    }

    private fun combineHoldOpen(pose: Pose, holdOpenPose: Pose): Pose {
        if (holdOpenPose == DummyPose.INSTANCE) return pose
        if (pose == DummyPose.INSTANCE) return holdOpenPose
        return MERGE_BLENDER.blend(listOf(pose, holdOpenPose))
    }

    private fun updateHoldOpen(name: String?): Boolean {
        val animation = name?.let(animations::get)
        if (animation == null) {
            holdOpenRunner = null
            holdOpenAnimationName = null
            return false
        }
        if (holdOpenRunner != null && holdOpenAnimationName == name) return false

        holdOpenAnimationName = name
        val newRunner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
        newRunner.state = AnimationPlayType.LOOP.state()
        holdOpenRunner = newRunner
        return true
    }

    private fun updateCloseStrike(name: String?): Boolean {
        val animation = name?.let(animations::get)
        if (animation == null) {
            closeStrikeRunner = null
            closeStrikeAnimationName = null
            return false
        }
        if (closeStrikeRunner != null && closeStrikeAnimationName == name) return false

        closeStrikeAnimationName = name
        val newRunner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
        newRunner.state = AnimationPlayType.LOOP.state()
        closeStrikeRunner = newRunner
        return true
    }

    override fun currentItem(): ItemStack = stack

    override fun getPose(): Pose = cachedPose

    override fun getCachedPose(): Pose = cachedPose

    override fun tick(partialTicks: Float) {
        val target = resolveState()
        val animation = GunResource.compute(stack).animation

        // 常驻层排在状态分支之前：换弹/开火/近战都停不下它
        tickLoopRunner(animation)

        // 编辑/改装里不该有蓄力（`chargeActive` 本来就要求 `!isEditing`）。这一句是防"蓄力收到
        // 一半就进编辑"：那条路走的是下面的提前 return，蓄力层会没人摘，一直挂着抖。
        if (ClientEventHandler.isEditing) {
            clearChargeRunner()
        }

        if (editExitRunner != null && ClientEventHandler.isEditing) {
            editExitRunner = null
            runner = null
            currentState = null
            play(GunAnimationState.EDIT)
        }

        if (editExitRunner != null) {
            tickEditExit()
            if (editExitRunner != null) return
        }

        if (target == null) {
            runner = null
            editExitRunner = null
            holdOpenRunner = null
            holdOpenAnimationName = null
            closeStrikeRunner = null
            closeStrikeAnimationName = null
            clearFireModeLayers()
            // 循环开火层也一并摘掉：它没有"停在那个角度"这种需要保留的相位（对比枪管旋转），
            // 留着只会让下次亮起来时从一个旧权重接着走
            clearFireLoopRunner()
            clearChargeRunner()
            clearMeleeFade()
            customSprintLayer.clear()
            bayonetChargeLayer.clear()
            clearDeployIdle()
            currentState = null
            pendingParticles.clear()
            cachedPose = DummyPose.INSTANCE
            return
        }

        if (currentState == GunAnimationState.EDIT
            && target != GunAnimationState.EDIT
            && !ClientEventHandler.isEditing
        ) {
            startEditExit()
            if (editExitRunner != null) {
                cachedPose = combineHoldOpen(
                    applyFireLoop(
                        combineFireModeSwitch(
                            combineLayers(
                                overLoop(editExitRunner!!.evaluate()),
                                fireModeRunner?.evaluate() ?: DummyPose.INSTANCE,
                                closeStrikeRunner?.evaluate() ?: DummyPose.INSTANCE,
                                spinRunner?.evaluate() ?: DummyPose.INSTANCE
                            ),
                            fireModeSwitchRunner?.evaluate() ?: DummyPose.INSTANCE,
                            fireRunner?.evaluate() ?: DummyPose.INSTANCE
                        )
                    ),
                    holdOpenRunner?.evaluate() ?: DummyPose.INSTANCE
                )
                return
            }
        }

        val editing = ClientEventHandler.isEditing || target == GunAnimationState.EDIT
        if (editing) {
            fireRunner = null
            fireModeSwitchRunner = null
            if (fireSerial > consumedFireSerial) {
                consumedFireSerial = fireSerial
            }
            if (fireModeSwitchSerial > consumedFireModeSwitchSerial) {
                consumedFireModeSwitchSerial = fireModeSwitchSerial
            }
        }

        val fireModeStarted = syncFireMode()
        val data = GunData.from(stack)

        // 蓄力层收尾要判"这一发打出去没有"。**必须在下面 [playFire] 消费 `fireSerial` 之前读**：
        // 松开蓄力那一帧 `chargeActive` 已经归零，"是打出去了还是没到发射标准"只剩这一个信号。
        val firedThisFrame = fireSerial > consumedFireSerial

        val (holdOpenStarted, closeStrikeStarted) = updateMechanicalRunners(data, animation)

        // 是否是新的一次挥击。**必须先于下面的 runner 判定消费掉**：
        // 第一段挥击走的是 `runner == null` 分支（那里本来就会 play），要是留到这里再判，
        // 序号没被消费就会在下一帧被当成"又挥了一次"，把动画从头重播一遍。
        val newMeleeSwing = consumeMeleeSwing()
        // 新的一段近战开始了：镜头晃动交还给新片段，不再跟着上一次的离场淡出
        if (newMeleeSwing) handOverMeleeCamera()

        if (runner == null || currentState != target) {
            // 近战收尾（挥完或蓄势松开没收刀）回到待机：上一段留作淡出层，别硬切
            val leavingMelee = currentState == GunAnimationState.MELEE && target == GunAnimationState.IDLE
            if (leavingMelee) startMeleeFade(MELEE_EXIT_FADE_TICKS, cameraFade = true)
            play(target, keepMeleeFade = leavingMelee)
        } else if (newMeleeSwing && currentState == GunAnimationState.MELEE) {
            // 连招/连挥：状态没变但确实开了新的一段，重播一次（不重建状态，只换 runner）
            startMeleeFade()
            play(GunAnimationState.MELEE)
        } else if (currentState != GunAnimationState.MELEE && !playsCurrentClip(target)) {
            // 状态没变但该播的 clip 换了（装上/卸下护木这类变体），换 runner 但接着旧进度走
            play(target, keepProgress = true)
        } else {
            runner?.tick()
        }
        // Keep the reload animation aligned if perks change the reload prop mid-reload.
        if (currentState != null && currentState!!.isReload) {
            val runnerAnimation = runner?.animation as? BedrockAnimation
            if (runnerAnimation != null) {
                setAnimationSpeed(runner?.state, reloadPlaybackSpeed(currentState!!, runnerAnimation))
            }
        }
        // Melee duration can be changed by properties such as ammo type or perks.
        if (currentState == GunAnimationState.MELEE) {
            val runnerAnimation = runner?.animation as? BedrockAnimation
            if (runnerAnimation != null) {
                setAnimationSpeed(runner?.state, meleePlaybackSpeed(runnerAnimation))
            }
        }

        if (!editing && fireSerial > consumedFireSerial) {
            playFire()
            consumedFireSerial = fireSerial
        } else if (!editing) {
            fireRunner?.tick()
        }
        val fireModeSwitchStarted = consumeFireModeSwitch(editing)
        tickFireModeRunners(fireModeStarted, fireModeSwitchStarted)
        tickMechanicalRunners(holdOpenStarted, closeStrikeStarted)
        tickSpinRunner(updateSpinRunner(data, animation))
        tickFireLoopRunner(updateFireLoopRunner(animation))
        tickChargeRunner(updateChargeRunner(animation, firedThisFrame))
        tickMeleeFade()
        val sprintDelta = sprintEaseDelta()
        val sprintWeight = sprintEaseWeight()
        val sprintSwitchSpeed = sprintEaseSwitchSpeed()
        // 基础状态交给动作片段（换弹/近战/改装）时两条层在几 tick 内让位
        val actionTookOver = target != GunAnimationState.IDLE
        val loopSpeed = loopPlaybackSpeed()
        val handgun = sprintEaseHandgun()
        customSprintLayer.tick(
            animation?.run?.let(animations::get),
            wantsCustomSprint(target, animation),
            actionTookOver,
            handgun,
            sprintDelta,
            sprintWeight,
            sprintSwitchSpeed,
            loopSpeed,
        )
        bayonetChargeLayer.tick(
            animation?.bayonetCharge?.let(animations::get),
            wantsBayonetCharge(target, animation),
            actionTookOver,
            handgun,
            sprintDelta,
            sprintWeight,
            sprintSwitchSpeed,
            loopSpeed,
        )
        tickDeployIdle(updateDeployIdle(animation, wantsDeployIdle(target, animation)))

        if (hand == InteractionHand.MAIN_HAND) {
            updateSubWeaponReload()
            subWeaponReloadRunner?.tick()
            updateSubWeaponIdle()
        }

        collectParticleEvents(runner)
        collectParticleEvents(fireRunner)
        collectParticleEvents(fireModeRunner)
        collectParticleEvents(fireModeSwitchRunner)

        collectSoundEvents(runner)
        collectSoundEvents(fireRunner)
        collectSoundEvents(fireModeRunner)
        collectSoundEvents(fireModeSwitchRunner)
        collectSoundEvents(holdOpenRunner)
        collectSoundEvents(closeStrikeRunner)
        collectSoundEvents(subWeaponReloadRunner)
        // 蓄力层也是一条独立的时间轴，`sound_effects` 得跟着一起收，不然枪的蓄力动画里
        // 写的时间轴音效永远不响
        collectSoundEvents(chargeRunner)

        if (fireLoopPower > 0f) {
            collectParticleEvents(fireLoopRunner)
            collectSoundEvents(fireLoopRunner)
        }

        // 权重层只在亮着的时候收事件（它们跟循环开火层一样是 loop 片段）
        if (customSprintLayer.visible) {
            collectParticleEvents(customSprintLayer.activeRunner)
            collectSoundEvents(customSprintLayer.activeRunner)
        }
        if (bayonetChargeLayer.visible) {
            collectParticleEvents(bayonetChargeLayer.activeRunner)
            collectSoundEvents(bayonetChargeLayer.activeRunner)
        }

        if (deployIdleRunner != null && deployIdlePower > 0f) {
            collectParticleEvents(deployIdleRunner)
            collectSoundEvents(deployIdleRunner)
        }

        collectParticleEvents(loopRunner)
        collectSoundEvents(loopRunner)

        if (fireRunner?.state is StopState) {
            fireRunner = null
        }

        cachedPose = combineHoldOpen(
            applyFireLoop(
                combineFireModeSwitch(
                    combineLayers(
                        // 蓄力层压在基础状态上、垫在开火层下面：它只 key `root`（枪身），
                        // 手部锚点仍由基础状态的 `Idle` 给，两者合起来才是"枪在手里抖"。
                        // 它是按权重交叉淡出出来的，所以基础状态单独先合一次。
                        // 自定义奔跑层在最底下（`Idle` 之上、其余层之下）：蓄力/开火那类"正在做动作"
                        // 的层要压得住它，而它自己得盖住 `Idle` 的枪身。冲锋层压在奔跑层上 ——
                        // 两者本来互斥，重叠的那几帧正好是交叉淡入淡出。
                        applyCharge(
                            applyDeployIdle(
                                bayonetChargeLayer.apply(
                                    customSprintLayer.apply(
                                        overLoop(applyMeleeFade(runner?.evaluate() ?: DummyPose.INSTANCE))
                                    )
                                )
                            )
                        ),
                        fireModeRunner?.evaluate() ?: DummyPose.INSTANCE,
                        closeStrikeRunner?.evaluate() ?: DummyPose.INSTANCE,
                        spinRunner?.evaluate() ?: DummyPose.INSTANCE
                    ),
                    fireModeSwitchRunner?.evaluate() ?: DummyPose.INSTANCE,
                    fireRunner?.evaluate() ?: DummyPose.INSTANCE
                )
            ),
            holdOpenRunner?.evaluate() ?: DummyPose.INSTANCE
        )
    }

    private fun collectParticleEvents(animationRunner: AnimationRunner?) {
        if (!isFirstPerson()) return

        val particles = animationRunner?.clip<ParticleEffectData>(BedrockAnimation.PARTICLE_CHANNEL_NAME) ?: return
        for (keyframe in particles) {
            keyframe?.value?.let { pendingParticles += it }
        }
    }

    private fun isFirstPerson(): Boolean {
        return Minecraft.getInstance().options.cameraType.isFirstPerson
    }

    private fun collectSoundEvents(animationRunner: AnimationRunner?) {
        val sounds = animationRunner?.clip<ResourceLocation>(BedrockAnimation.SOUND_CHANNEL_NAME) ?: return
        val player = localPlayer ?: return
        for (keyframe in sounds) {
            val soundLocation = keyframe.value ?: continue
            val soundEvent = SoundEvent.createVariableRangeEvent(soundLocation)
            player.level().playSound(
                player,
                player.blockPosition(),
                soundEvent,
                SoundSource.PLAYERS,
                1.0f,
                1.0f
            )
        }
    }

    override fun getCameraRotation(): Quaternionf {
        val weight = meleeCameraFadeWeight()
        if (weight >= 1f) return cameraRotation
        return fadedCameraRotation.identity().slerp(cameraRotation, weight)
    }

    override fun setCameraRotation(rotation: Quaternionf) {
        cameraRotation.set(rotation)
    }

    override fun updateItem(stack: ItemStack) {
        val itemChanged = this.stack.item != stack.item
        this.stack = stack
        if (itemChanged) {
            editExitRunner = null
            clearFireModeLayers()
            holdOpenRunner = null
            holdOpenAnimationName = null
            closeStrikeRunner = null
            closeStrikeAnimationName = null
            clearSpinRunner()
            clearFireLoopRunner()
            clearChargeRunner()
            clearMeleeFade()
            customSprintLayer.clear()
            bayonetChargeLayer.clear()
            clearLoopRunner()
            pendingParticles.clear()
            loadAnimations()
        }
    }

    override fun triggerDraw() {
        if (runner == null) {
            play(resolveState() ?: GunAnimationState.IDLE)
        }
    }

    override fun triggerPutAway() {
        runner = null
        editExitRunner = null
        fireRunner = null
        clearFireModeLayers()
        holdOpenRunner = null
        holdOpenAnimationName = null
        closeStrikeRunner = null
        closeStrikeAnimationName = null
        clearSpinRunner()
        clearFireLoopRunner()
        clearChargeRunner()
        clearMeleeFade()
        customSprintLayer.clear()
        bayonetChargeLayer.clear()
        clearLoopRunner()
        currentState = null
        fireSerial = 0
        consumedFireSerial = 0
        pendingFireAnimation = null
        subWeaponFire = false
        pendingShellEjects.clear()
        pendingParticles.clear()
        cachedPose = DummyPose.INSTANCE
    }

    override fun shouldRenderHand(): Boolean {
        return true
    }

    companion object {
        private const val EDIT_EXIT_SPEED = 1.5f

        /**
         * 蓄力没到发射标准就松手时，姿势缓出回基础状态的时长（tick）。
         *
         * 0.2 秒：够看出是"松劲儿"而不是硬切，又短到不会和紧跟着的下一次蓄力撞上
         * （同 `takesHandAway` 那套副武器手臂交接用的 3 tick 量级）。
         */
        private const val CHARGE_CANCEL_FADE_TICKS = 4f

        // 动作片段（或蓄力层）接管那一刻的一小段交接（tick）：奔跑层让位、蓄力层淡入都用它。
        // 不能是 0（硬切会弹），也不能长（波塞克的蓄力总共才 10 tick）
        private const val ACTION_HANDOVER_TICKS = 3f

        // 挥击提前衔接时，上一段片段的淡出时长（tick）
        private const val MELEE_CHAIN_FADE_TICKS = 3f

        /** 近战结束/蓄势没收刀回待机的淡出时长：抬起的姿势要落回去，比连招衔接长一些 */
        private const val MELEE_EXIT_FADE_TICKS = 6f

        /**
         * 缓入缓出每帧最多吃掉多少 tick。`deltaFrameTime` 单位是 tick（20/s，60FPS 一帧约 0.33），
         * 卡顿或断点续跑时可能蹦得很大——上面的常数按秒写就会一帧走完。上限照抄
         * `GeoGunRenderer.scriptFrameDeltaSeconds`，掉帧的时候是"跳帧"而不是"瞬移"。
         */
        private const val SPIN_MAX_FRAME_DELTA_TICKS = 0.8f

        @JvmStatic
        fun barrelSpinUpTicks(data: GunData): Int =
            (data.selectedFireModeInfo().chargeConfig()?.effectiveDuration
                ?: data.get(GunProp.SHOOT_DELAY)).coerceAtLeast(1)

        /** 一秒的 tick 数：数据里的秒（`FireLoopFadeIn` 这类）换算成缓动要的 tick */
        private const val TICKS_PER_SECOND = 20f

        /**
         * 自定义奔跑层退到这个权重以下就收掉。
         *
         * 权重是渐近的 lerp，永远到不了 0；这条片段满权重也就 ±10° / 0.19 格，千分之一
         * （0.01° / 0.0002 格）已经看不见了。
         */
        private const val CUSTOM_SPRINT_MIN_POWER = 0.001f

        // 标准疾跑的水平速度（格/tick）：自定义循环片段按它换算播放倍率
        private const val SPRINT_SPEED_REF = 0.14

        // 自定义循环层播放倍率的上下限：站着不动也继续摆，被加速时也不至于糊成一片
        private const val LOOP_MIN_SPEED = 0.25f
        private const val LOOP_MAX_SPEED = 2f

        private fun setAnimationSpeed(state: IAnimationState?, speed: Float) {
            when (state) {
                is PlayingState -> state.speed = speed
                is LoopingState -> state.speed = speed
                else -> {}
            }
        }


        private val TRANSFORM_FACTORY: BoneTransformFactory = ZYXBoneTransformFactory()

        private val BLENDER: EulerAdditiveBlender =
            SimpleEulerAdditiveBlender(TRANSFORM_FACTORY) { ArrayPoseBuilder() }

        private val MERGE_BLENDER = NoAllocMergeBlender()

        /**
         * 循环开火层那一次逐骨骼插值
         */
        private val CROSSFADE_BLENDER = BiPoseCombiner { ArrayPoseBuilder() }
    }
}
