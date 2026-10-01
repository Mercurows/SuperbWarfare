package com.atsuishio.superbwarfare.client.animation.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.client.gun.MeleeClientHandler
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.isDrumLevel
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.item.attachment.SubWeaponItem
import com.atsuishio.superbwarfare.resource.gun.GunAnimation
import com.atsuishio.superbwarfare.resource.gun.GunAnimationNames
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.resource.model.AttachmentModelReloadListener
import com.atsuishio.superbwarfare.resource.model.GunModelReloadListener
import com.atsuishio.superbwarfare.tools.ActiveGun
import com.atsuishio.superbwarfare.tools.deltaFrameTime
import com.atsuishio.superbwarfare.tools.localPlayer
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.DummyPose
import com.maydaymemory.mae.basic.Pose
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.NoAllocMergeBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.maydaymemory.mae.control.runner.*
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import org.joml.Quaternionf
import java.util.*

open class GeoGunAnimationInstance(
    private var stack: ItemStack,
    entity: Entity,
    /**
     * 这一份实例是**哪只手**的（`FirstPersonRenderHandler` 两手各持一份 `HandRenderState`）。
     *
     * 目前只有一个用处：**副武器那套状态只有主手实例该管**（[updateSubWeaponReload] /
     * [updateSubWeaponIdle] / 它们的 `sound_effects` 关键帧）。理由不是"两手会画出两份"，而是
     * **解析口径**：`updateSubWeaponReload` 里的副武器是从 `player.mainHandItem` 解析的
     * （`ActiveGun` 的不变量 ① —— 副武器是**主手**那把枪身上的东西），**与实例自己的 `stack` 无关**。
     * 所以"这份实例是哪只手的"和"它该不该管副武器"是同一件事，用 `hand` 表达最直接。
     *
     * ⚠ **今天这道门禁是"保险"，不是"修 bug"**：库的 `IFPGeoItemRenderer.canRenderInHand`
     * 默认返回 `hand == MAIN_HAND`，本模组没有覆盖它，于是 `FirstPersonRenderHandler.tickHandAnimation`
     * 在 `canActuallyRenderInHand` 那一关就把副手实例挡掉了（`ani.tick()` 根本不会被调到）。
     * 留着它的原因是**这个默认值正是库文档里写明供覆盖的扩展点**（"物品本身决定能否在某只手呈现"，
     * 用于双手/单手骨架变体）——一旦有人让副手也 tick，副手实例就会照着**同一份**主手副武器数据
     * 再建一套 runner：姿态上无害（那套 pose 没人读），**但它会把同一支换弹动画的 `sound_effects`
     * 再收一遍，音效响两遍**。不变式写在代码里，比寄望于库的默认值稳。
     */
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

    /**
     * 枪管旋转那一层的循环 runner。**一支常驻**：起转、停转都只改播放速度，从不重建，所以
     * "哪一帧出岔子就把动画推回第一帧"这种毛病从根上没有了——那一层没有别的状态。
     */
    private var spinRunner: AnimationRunner? = null

    /**
     * 已经解析好的 `hold` 片段。动画资源偶尔有一帧取不到（正在重新加载、还没加载完）时继续用这一支，
     * 免得那一帧把 runner 重建掉、枪管卡在第一帧。
     */
    private var spinAnimation: BedrockAnimation? = null

    /** 当前转到几成（0..1），缓入缓出就缓在这里。 */
    private var spinPower = 0f
    private var editExitRunner: AnimationRunner? = null
    private var currentState: GunAnimationState? = null
    private var fireSerial = 0
    private var consumedFireSerial = 0

    /**
     * 本 tick 主武器所处的动画状态（还没解析出状态时为 `null`，等同"没有主武器动画"）。
     *
     * 渲染侧要问它一件事：这个状态下主武器会不会**把左手拿开**（[GunAnimationState.takesHandAway]）
     * —— 部署副武器期间的手臂接管据此决定要不要让位
     * （`GeoGunRenderer.resolveDeployedSubWeaponArmAnchors`）。
     * 状态本身由 [tick] 里的状态机推进，一 tick 只变一次，渲染用的 partialTick 不参与。
     */
    val currentGunState: GunAnimationState?
        get() = currentState

    /**
     * 本次开火已经解析好的 clip 名（由 [triggerFire] 写入，[playFire] 读取）。
     *
     * 开火是**分层播放**的（叠在状态机之上），所以它不能像近战那样在状态切换那一帧现解析：
     * 触发与播放之间隔着一次 tick，而候选链的解析需要"这一次是谁在开火"这个信息
     * ——副武器开火与主武器开火用的是两条不同的候选链。
     */
    private var pendingFireAnimation: String? = null

    /**
     * 当前这一支开火动画是不是**副武器专属**的那一支（`fire_sub_weapon` 这类）。
     *
     * 枪口效应据此换骨骼：副武器开火时动画关键帧里的枪口烟挂在副武器模型自己的 `flare` 上，
     * 而不是主武器的（见 `GeoGunRenderer.resolveSubWeaponFlareTransform`）。
     * 候选全落空、播的还是 `GunAnimation.Fire` 时它是 `false` —— 那一发视觉上就是主武器在开火。
     */
    private var subWeaponFire = false

    /**
     * 副武器**自己**的换弹动画 runner（四期，§9.8.7）。
     *
     * 副武器是一把独立的枪（有自己的 `GunData` / `GunResource`），所以它的换弹动画写在
     * **它自己的枪械资源**里（`sbw/guns/<id>.json` 的 `Animation.Reload*`，口径见
     * [GunAnimation.reloadClip]），由**它自己的附件模型**播。这一支 runner 只负责
     * "把那个 clip 推到第几帧了"，姿态由 `GeoGunRenderer.renderRegisteredAttachments`
     * 取 [subWeaponReloadPose] 应用到附件模型上。
     *
     * 为什么状态挂在这里而不是附件模型上：附件模型是**全局共享**的（同一种配件装在多把枪上
     * 共用一份实例），往它身上挂状态会串台；这个动画实例才是"某一把具体的枪"。
     *
     * ⚠ **解析要过两关，任一关不过就全程空转**（[updateSubWeaponReload] 里有详细说明）：
     * ① 数据里写了 clip 名（`sbw/guns/<id>.json`）；② **附件动画表**里有这支 clip
     * （`animations/bedrock/attachment/<附件 id>.animation.json`，且骨骼必须是附件模型的）。
     * 任一条不成立 → runner 保持 `null` → 渲染侧不应用姿态（副武器静止挂在枪上）。
     * 这是**有意的静默回退**，不是 bug；两条失败路径都会打一条能定位的日志
     * （见 [logSubWeaponReloadMissOnce]，`melee_debug_log` 打开时可见）。
     */
    private var subWeaponReloadRunner: AnimationRunner? = null

    /** 当前这支副武器换弹动画的 clip 名（用来判断"换了一支动画"要不要重建 runner） */
    private var subWeaponReloadAnimationName: String? = null

    /** 已经打过日志的"解析不到副武器换弹动画"（键含副武器 id/原因/clip 名，同一条失败只打一次） */
    private var loggedSubWeaponReloadMiss: String? = null

    /**
     * 已消费的近战挥击序号。
     *
     * 近战是 `PLAY_ONCE_HOLD`：播完停在最后一帧。按住 V 连续挥击时 `currentState` 与
     * `resolveState()` 的目标都是 `MELEE`，`currentState != target` 不成立 → runner 只会被 `tick()`，
     * 于是**只有第一段会播**。这里比照开火那套 `fireSerial`，按序号重播。
     */
    private var consumedMeleeSerial = 0
    private var fireModeSwitchSerial = 0
    private var consumedFireModeSwitchSerial = 0
    private var lastFireModeName: String? = null

    /**
     * 上一次已经打过日志的近战动画解析失败（见 [logMeleeMissOnce]）。
     *
     * runner 为空时 [resolveMeleeName] 每个 tick 都会被调一次，不去重的话"clip 名写错"会刷满日志。
     */
    private var loggedMeleeMiss: String? = null

    /** 上一次已经打过日志的开火动画解析失败（见 [logFireMissOnce]） */
    private var loggedFireMiss: String? = null

    /**
     * 上一次已经打过日志的 [GunProp.SHOOT_ANIMATION] 解析失败（见 [resolveOverrideFireName]）。
     *
     * 与 [loggedFireMiss] **分开存**：两者在同一次开火里都会跑，共用一个槽位时
     * 后者成功会把前者的去重标记清掉（`resolveFireName` 命中就 `loggedFireMiss = null`），
     * 于是"显式动画名写错"又变成每发一条。
     */
    private var loggedOverrideFireMiss: String? = null
    private val pendingShellEjects = ArrayList<Int>()
    private val pendingParticles = ArrayList<ParticleEffectData>()
    private var cachedPose: Pose = DummyPose.INSTANCE
    private val cameraRotation = Quaternionf()

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
        val player = localPlayer ?: return null
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
        if (animation.run != null
            && player.isSprinting
            && player.onGround()
            && ClientEventHandler.noSprintTicks == 0f
            && ClientEventHandler.drawTime < 0.01
        ) {
            return GunAnimationState.RUN
        }

        return if (animation.idle != null) GunAnimationState.IDLE else null
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
        // ⚠ 候选链与 `GunAnimation.Fire` 一律按**宿主枪**的资源解析：这个动画实例属于宿主枪的
        // 模型，`animations` 那张表也是从宿主枪的动画文件加载的。副武器的合成栈没有自己的
        // 枪械动画文件，拿它去解析只会把表清空（见下面的 `updateItem`）。
        val hostStack = this.stack

        val fireName = resolveOverrideFireName(hostStack, overrideAnimation)
            ?: resolveFireName(hostStack, candidates, reportMissing)
            ?: return
        if (!animations.containsKey(fireName)) return

        // 这一发是不是**副武器**打的。抛壳只认它，与"播了哪支 clip"无关。
        val subWeaponShot = stack.item is SubWeaponItem

        // 主武器开火：兜底把实例同步到手上这把枪（换枪后渲染路径还没跑到的那个窗口）。
        //
        // ⚠ 副武器开火**绝不能**同步：`updateItem` 会重绑实例、按副武器的资源重载 `animations`，
        // 把宿主枪的动画表**清空** —— 紧接着的 `playFire` 就一张 clip 都找不到，
        // 表现是"打榴弹时枪的动画卡住一帧/不播"。
        if (!subWeaponShot && this.stack.item != stack.item) {
            updateItem(stack)
        }

        val gunFire = GunResource.compute(hostStack).animation?.fire
        pendingFireAnimation = fireName
        subWeaponFire = candidates.isNotEmpty() && fireName != gunFire

        fireSerial++
        // 副武器开火**不抛壳**：弹壳模型与 `shell` 骨骼都是主武器自己的 `ShellEject` 配置，
        // 打出去的却是副武器的弹药 —— 照旧抛壳就成了"榴弹发射时步枪抛壳"。
        //
        // 判据是 [subWeaponShot]（**谁打的**）而不是 [subWeaponFire]（**播了哪支 clip**）：
        // 宿主枪没做 `fire_sub_weapon` 时 `fireName` 会退回它自己的 `Fire`，两者相等、
        // `subWeaponFire` 为假 —— 但那一发**仍然是副武器打的**，照样不该抛壳。
        if (isFirstPerson() && !subWeaponShot) {
            pendingShellEjects += 0
        }
    }

    /**
     * 当前正在播的开火动画是不是副武器专属的那一支（`fire_sub_weapon` 这类）。
     *
     * 只在**确实换了动画**时为真：候选链全部落空、播的还是 `GunAnimation.Fire` 时它是 `false`
     * （那一发在视觉上就是主武器在开火，枪口效应该留在主武器的枪口上）。
     */
    fun isSubWeaponFire(): Boolean = subWeaponFire && fireRunner != null

    /**
     * 副武器**自己**的换弹姿态；没有在换弹、或那支 clip 还没做出来时返回 `null`。
     *
     * 渲染侧（`GeoGunRenderer.renderRegisteredAttachments` 的 `SUBWEAPON` 槽位）在画副武器模型前
     * `BedrockAttachmentModel.applyPose(pose)`、画完 `resetPose()`。
     *
     * **宿主枪在此期间照常播它自己的 `idle`/`run`**（`cachedPose` 不受这里影响）——
     * 两个模型是各自独立的姿势树、骨骼命名空间不重叠，所以"融合"是天然发生的，不需要混合器（§9.8.7）。
     */
    fun subWeaponReloadPose(): Pose? = subWeaponReloadRunner?.evaluate()

    /**
     * 上面那份姿态是哪一支 clip 给的（没在换弹时为 `null`）。
     *
     * 渲染侧只用它当"手臂锚点来源变没变"的标识：换一支 clip（正常/空仓/鼓式）要把锚点
     * 淡过去一小段，而不是硬切（`GeoGunRenderer.resolveArmAnchorsForDraw`）。
     */
    val subWeaponReloadClipName: String?
        get() = subWeaponReloadRunner?.let { subWeaponReloadAnimationName }

    /**
     * 推进副武器的换弹动画（四期，§9.8.7）。
     *
     * 触发条件是"**部署中的副武器正在换弹**"：`ActiveGun` 解析出当前操控的是副武器、
     * 且它 `reloading()`。换弹时长取**副武器数据**的剩余总 tick，据此拉伸播放速度
     * （与主武器的换弹动画同一套口径）。
     *
     * 部署解除、换弹结束、或者下面两步任一步解析不到时，runner 被清掉、返回 `null` ——
     * 渲染侧于是不应用姿态。解析分两步，**两步都可能单独失败**：
     *
     * 1. **clip 名**：问副武器自己的 `GunResource`（`sbw/guns/<id>.json` 的 `Animation.Reload*`）。
     *    没写 → 没有 clip 可播（[GunAnimation.reloadClip]）。
     * 2. **动画本体**：问 [AttachmentModelReloadListener] 的动画表，也就是
     *    `animations/bedrock/**attachment**/` 那个目录 —— 它**按文件名 id** 与附件模型配对
     *    （`sub_weapon_gp_25.animation.json` ↔ `sub_weapon_gp_25.geo.json`）。
     *
     * ⚠ **第 2 步是本机制最容易配错的地方**（五期实测踩到）：
     * - 文件放进 `animations/bedrock/**gun**/` 是无效的 —— 那边是主武器动画表，
     *   副武器换弹只读附件表；
     * - 就算放对了目录，**clip 也必须驱动附件模型自己的骨骼**（`root` / `gun` / `tube` /
     *   `trigger` / `projectile` …）。照抄枪模型的 `righthand` / `camera` / `head` /
     *   `undefined` 会被静默丢弃 —— 症状是"runner 建起来了但模型纹丝不动"，与"没做动画"外观一致。
     *
     * 两条失败路径都会经 [logSubWeaponReloadMissOnce] 打一条带定位信息的日志
     * （只在 `melee_debug_log` 打开时输出）。
     */
    private fun updateSubWeaponReload() {
        val player = localPlayer
        // ⚠ 必须显式传 `client = true`：单人游戏里客户端与服务端共享静态缓存表，
        // 用错一侧的实例会让状态在两边互相覆盖（`SubWeaponRuntime` 的不变式 ③）。
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
     * 部署中的副武器的 **`Idle` clip**（`sbw/guns/<副武器 id>.json` 的 `Animation.Idle`，
     * 由**它自己的**附件模型播）；没部署 / 数据里没写 / 动画表里没有时为 `null`。
     *
     * 它**不驱动模型**，只用来回答"手臂该抓在哪儿"（§11.11.7.4）：`GeoGunRenderer` 取它在 `t=0`
     * 的姿态、按附件骨骼算出手臂锚点，于是"G 键把副武器切出来之后，左手一直在发射器上"。
     * 这也是换弹开始/结束那一下不再跳变的原因 —— 换弹 clip 的两端与这支 idle **逐位重合**（实测 0.0000）。
     *
     * ⚠ 解析口径与 [updateSubWeaponReload] 的**第二步完全相同**（同一个 [AttachmentModelReloadListener]
     * 动画表、同样只认附件模型自己的骨骼），区别只有：①问的是 `Animation.Idle` 而不是 `Reload*`；
     * ②**不要求**在换弹。所以"数据里写了名字但动画文件里没有"这种配错照样是静默回退。
     *
     * ⚠ 千万不要把它混进 [subWeaponReloadPose]：那一个非空意味着"副武器正在做换弹动作"，
     * `GeoGunRenderer.resolveSubWeaponFollowPose` 会据此**把整把主武器反推着动起来**
     * （摘 `root` 那套，§11.11.7.2）。idle 是常驻状态，混进去会让枪一直跟着 `root` 抖。
     */
    private var subWeaponIdleClip: BedrockAnimation? = null

    /** 上面那支 idle clip 的名字，只用于渲染侧的"锚点来源变没变"标识 */
    private var subWeaponIdleAnimationName: String? = null

    /**
     * 部署中的副武器的 `Idle` 姿态（`t=0`；没部署时为 `null`）。
     *
     * 取固定的一帧而不是跑一个 runner：这支 clip 回答的是"手抓在哪儿"这个**静态**问题，
     * 让它动起来对锚点没有意义（运动员是主武器与副武器的换弹 clip）。要让它动也很简单 ——
     * 换成 runner 即可，但那时得先想清楚"动起来的手"与"换弹 clip 端点重合"这条性质还在不在。
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
     * "这一支副武器换弹动画解析不到"只报一次。
     *
     * ⚠ **这里必须给出足够定位的信息，而且必须真的打得出来。**
     *
     * 以前它是一句 `Mod.LOGGER.debug`（默认根本不输出），于是"动画没生效"在客户端**完全无声**：
     * 表现只是"副武器换弹时纹丝不动"，看不出是数据没写、文件放错目录、还是骨骼名对不上。
     * 现在拆成两条**互不覆盖**的记账（`noClip` / `unbound`），并把三个答案一起打出来：
     *
     * | 问题 | 日志里的答案 |
     * |---|---|
     * | 数据里写了 clip 名吗 | `noClip` = 没写（`sbw/guns/<id>.json` 缺 `Animation.Reload*`） |
     * | clip 名对应的文件在不在 | `unbound` 那条会打印 clip 名与它要求的**文件 id** |
     * | 文件该放哪个目录 | 两条都打印 [AttachmentModelReloadListener.animPath] |
     *
     * **分档是"正常状态"那一档**：没做这支动画本来就是允许的（副武器静止挂在枪上，
     * 换弹的音效与进度仍由数据/服务端负责），所以用 `info` 且只挂在 `melee_debug_log` 上，
     * 不是 `error`（与 `fire_sub_weapon` 候选落空的分档一致）。
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

    private fun normalReloadName(animation: GunAnimation): String? {
        if (isDrumLevel()) {
            val drumName = animation.reloadNormalDrum
            if (drumName != null && animations.containsKey(drumName)) return drumName
        }
        return animation.reloadNormal
    }

    private fun emptyReloadName(animation: GunAnimation): String? {
        if (isDrumLevel()) {
            val drumName = animation.reloadEmptyDrum
            if (drumName != null && animations.containsKey(drumName)) return drumName
        }
        return animation.reloadEmpty
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

    /** 本段动作自己声明的动画候选链（`MeleeAction.Animation`），优先于 `GunAnimation.Melee` */
    private fun meleeActionAnimations(): List<String> {
        val data = GunData.from(stack)
        if (!data.hasMeleeAttack()) return emptyList()
        return try {
            val actions = data.meleeActions()
            actions[ClientEventHandler.currentMeleeIndex(stack).mod(actions.size)].animationCandidates()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 解析 MELEE 状态实际使用的 clip 名。
     *
     * 动作表里是**候选链**，短名会被拼成 `animation.<宿主枪 id>.<短名>`（见 [GunAnimationNames]）：
     * 动作表所在的枪械数据会被多把枪共用（配件/弹种覆盖），写不了某把枪的完整 clip 名，
     * 拼出来的候选正好能表达"这把枪有专属动画就用专属的，没有就退回通用的那一支"。
     *
     * 候选全部落空时**打 error 日志并回退到 `GunAnimation.Melee`**，而不是静默失败
     * ——静默失败会让"动画没播"变成一个查不出来的问题。
     *
     * 注意：runner 为空时本方法**每个 tick 都会被调一次**（`tick()` 里 `runner == null` 就会重播），
     * 所以失败日志按"这一次的解析结果"去重，不会刷屏。
     */
    private fun resolveMeleeName(): String? {
        val resource = GunResource.from(stack)
        val animation = resource.compute().animation ?: return null
        val names = animation.melee?.list.orEmpty()

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
     * 同一条解析失败只打一次日志。
     *
     * [key] 描述"这次失败的是什么"，连续相同的失败被吞掉；成功解析一次后 key 会被清掉，
     * 之后再失败仍然会打日志。
     */
    private fun logMeleeMissOnce(key: String, message: String, vararg args: Any?) {
        if (loggedMeleeMiss == key) return
        loggedMeleeMiss = key
        Mod.LOGGER.error(message, *args)
    }

    private fun animationName(state: GunAnimationState): String? {
        val animation = GunResource.compute(stack).animation ?: return null
        return when (state) {
            GunAnimationState.IDLE -> animation.idle
            GunAnimationState.EDIT -> animation.edit
            GunAnimationState.BOLT -> animation.bolt
            GunAnimationState.RELOAD -> animation.reload
            GunAnimationState.RELOAD_NORMAL -> normalReloadName(animation)
            GunAnimationState.RELOAD_EMPTY -> emptyReloadName(animation)
            GunAnimationState.PREPARE -> animation.prepare
            GunAnimationState.PREPARE_LOAD -> animation.prepareLoad
            GunAnimationState.ITERATIVE -> animation.iterative
            GunAnimationState.ITERATIVE_2 -> animation.iterative
            GunAnimationState.FINISH -> animation.finish
            GunAnimationState.MELEE -> resolveMeleeName()
            GunAnimationState.FIRE -> animation.fire
            GunAnimationState.RUN -> animation.run
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
     * 近战动画的播放速度：按**本段动作**的时长拉伸，而不是全局 `MeleeDuration`。
     *
     * `playbackSpeed = clip.specifiedEndTimeMs / (action.Duration / 20f)`
     */
    /**
     * 是否新开了一段挥击（每段只返回一次 `true`）。
     *
     * 与开火的 `fireSerial > consumedFireSerial` 同一个套路：近战状态一直是 `MELEE` 的时候，
     * 只有这个序号能告诉动画侧"该从头播了"。
     */
    private fun consumeMeleeSwing(): Boolean {
        val serial = MeleeClientHandler.swingSerial
        if (serial <= consumedMeleeSerial) return false
        consumedMeleeSerial = serial
        return true
    }

    private fun meleePlaybackSpeed(animation: BedrockAnimation): Float {
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

    private fun setAnimationSpeed(state: IAnimationState?, speed: Float) {
        when (state) {
            is PlayingState -> state.speed = speed
            is LoopingState -> state.speed = speed
            else -> {}
        }
    }

    private fun play(state: GunAnimationState) {
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
        runner = newRunner
        currentState = state
        cachedPose = newRunner.evaluate()
    }

    private fun playFire() {
        val fireName = pendingFireAnimation ?: return
        val fireAnimation = animations[fireName] ?: return

        val newRunner = AnimationRunner(fireAnimation, AnimationContext(fireAnimation.specifiedEndTimeS))
        newRunner.state = AnimationPlayType.PLAY_ONCE_STOP.state()
        fireRunner = newRunner
        cachedPose = newRunner.evaluate()    }

    /**
     * 解析**显式指定**的开火 clip（`GunProp.SHOOT_ANIMATION`），没写或解析不出来时返回 `null`。
     *
     * 名字口径与候选链完全一致（[GunAnimationNames]：`animation.` 开头当全名，否则按**宿主枪 id**
     * 拼短名），但这里**只认这一支**——没有候选可言，解析出来不存在就交给调用方退回原有解析。
     *
     * 解析失败是"数据或动画文件写错了"（写这个名字的目的就是要换动画，结果没换成），
     * 所以报 error，并按解析结果去重（每发都解析，不去重会刷屏）。
     *
     * @param stack 宿主枪 —— 与 [resolveFireName] 一样，短名要按**宿主的 id** 拼
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
     * 解析本次开火实际使用的 clip 名。
     *
     * 候选链写在**副武器定义**里（`SubWeaponInfo.Animation`），而那份定义是给多把枪共用的，
     * 写不了某一把枪的完整 clip 名 —— 所以短名在这里按**宿主枪 id** 拼
     * （`fire_sub_weapon` → `animation.ak_12.fire_sub_weapon`），规则与近战的
     * `MeleeAction.Animation` 完全同一套，见 [GunAnimationNames]：
     *
     * - 候选里**第一个存在**的 clip 胜出（做了 `fire_sub_weapon` 的枪用它）；
     * - 候选全部落空 → 退回 `GunAnimation.Fire`（"这把枪没做副武器开火动画"）。
     *
     * 全部落空会记一条日志 —— 静默失败会让"动画没播"变成查不出来的问题。级别分两种：
     * **显式写的候选**落空是数据/动画文件写错，报 error；**默认候选**落空只是"这把枪没做这支 clip"，
     * 退回 `Fire` 本来就是设计的一部分，只打 debug。两个级别都按"这一次的解析结果"去重
     * （与 [resolveMeleeName] 同一套），不会因为每发都解析而刷屏。
     *
     * @param reportMissing 见 [triggerFire]
     * @param stack 用哪把枪的资源拼短名 —— **宿主枪**（动画实例绑定的那把）
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

        return animation?.fire
    }

    /**
     * 同一条开火动画解析失败只打一次日志（与 [logMeleeMissOnce] 同一套去重）。
     *
     * @param fatal 显式写在数据里的候选落空 = 数据或动画文件写错了，报 error；
     *   默认候选落空只是"这把枪没做这支 clip"（退回 `Fire` 本来就是设计的一部分），留一条 debug ——
     *   这里报 error 会让一份正常存档的日志看起来像坏了。
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
                    exitRunner.evaluate(),
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
     * 推进枪管旋转层，返回本帧是否新建了 runner——与 [updateHoldOpen] 一样，新建的那一帧由
     * [tickSpinRunner] 跳过 tick（runner 进状态时已经记过时间戳了）。
     *
     * 这一层只有一支循环动画（[GunAnimation.hold]），全部状态就是"现在转到几成"（[spinPower]）：
     *
     * - **缓入**：按住开火键之后，[spinPower] 用掉这把枪的**蓄力时长**（[spinDurationTicks]，就是
     *   `handleShootDelay` 里 `holdingFireKeyTicks` 的上限）从 0 升到 1，所以第一发子弹出膛（蓄力满）
     *   的那一刻枪管刚好到满速。转速再走一道 smoothstep，起步和到顶都是缓的。
     * - **缓出**：松开扳机之后，按同样的时长平滑退回 0。过热、空仓、换弹都**不**影响这一层：
     *   那些是子弹的事，枪管跟着扳机走（见 [shouldSpin]）。
     *   退到 0 只退转速，**相位停在那个角度不动**：枪管是一圈对称的六根，停在哪一相位看着都是装好的，
     *   而"归位"得把相位倒回去，那是肉眼可见的一顿。所以这一层在速度为 0 时照样每帧求值——不叠这一层
     *   就等于把枪管摁回绑定姿态，那才是真的跳。
     * - **满速**：见 [holdSpinSpeed]，默认 1200RPM 是 1×，600 是 0.5×，1800 是 1.5×。
     *
     * 射速被开火模式/perk 改掉、蓄力被打断、单帧抖动，全都只体现为转速平滑地跟上或退下来：
     * 没有状态机、没有按帧重建，也就没有"被某一帧推回起点"的可能。
     */
    private fun updateSpinRunner(data: GunData, animation: GunAnimation?): Boolean {
        // 动画资源这一帧拿不到就沿用上一次解析的结果；实在没有就把相位冻住、让这一层留在原地。
        // **不能在这里清掉 runner**：清掉等于这一层从合成里消失，枪管会当帧弹回绑定姿态（肉眼可见的
        // 一顿），下一次拿到资源又从第一帧开始转——这本身就是一种「时有时无」。
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
        applySpinSpeed(holdSpinSpeed(hold, data) * easeSpin(spinPower))
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
     * 这把枪现在该不该转：**只看扳机**——开火键按着（加特林开镜也算），见
     * [ClientEventHandler.isBarrelSpinTriggered]，与那声旋转音效共用同一个判据。
     *
     * 这里**故意不查 `canShoot`**：过热、背包弹药打空、换弹都只该停子弹，不该停枪管。之前把两者
     * 绑在一起，连射到过热（热量到 100 上锁、降到 80 以下才解锁）时枪管跟着停转、退热后又自己
     * 转起来，看着就是「旋转时有时无」。
     *
     * 只认本地玩家手里正拿着的那把枪。**判据必须比物品类型，不能比 ItemStack 对象身份**：
     * 每发子弹出膛都会改一次手上这把枪的 NBT（弹药、热量），服务端随之把手持槽同步下来，而
     * 客户端收到同步是把整个 ItemStack **换成新对象**（见 `GunData.DATA_CACHE` 与 `rebind` 的注释，
     * `GunResource.RESOURCE_CACHE` 也是为同一件事按物品 id 建键的）。可 instance 里的 `stack` 要等
     * 下一次客户端 tick 才由 `updateItem` 刷新，中间那几帧身份就对不上——`tick()` 却是**每帧**跑一次
     * （`FirstPersonRenderHandler` 挂在 RenderTickEvent 上），于是按住扫射时那些帧会把转速推一下、
     * 下一帧又自己缓回来。
     *
     * 比类型不影响原来的意思：副手、展示框、掉落物、别人手里的枪都拿不到本地玩家主手的这个物品，
     * 只有"双手各一把加特林"这种边角情况会让副手那把也跟着转，而主手是双手武器时副手本来就不渲染。
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
    private fun spinDurationTicks(data: GunData): Int {
        return (data.selectedFireModeInfo().chargeConfig()?.effectiveDuration
                ?: data.get(GunProp.SHOOT_DELAY)).coerceAtLeast(1)
    }

    /**
     * 满速时的播放倍率。六根枪管均分一圈，"每秒转多少度"在数值上就等于 RPM
     * （1200RPM = 20 发/s × 每发 60° = 1200°/s），所以按射速转 = 每两发之间正好走 1/6 圈。
     * 动画里枪管在 [BedrockAnimation.specifiedEndTimeS] 秒（= json 的 `animation_length`，
     * minigun 的 hold 是 0.3s，即 1200°/s）里转满一圈，两者相除就是倍率：
     * 1200RPM 是 1×，600 是 0.5×，1800 是 1.5×。
     */
    private fun holdSpinSpeed(hold: BedrockAnimation, data: GunData): Float {
        val endTimeS = hold.specifiedEndTimeS
        if (endTimeS <= 0f) return 1f
        return ClientEventHandler.effectiveRpm(data).toFloat() / (360f / endTimeS)
    }

    private fun applySpinSpeed(speed: Float) {
        setAnimationSpeed(spinRunner?.state, speed)
    }

    /** smoothstep：起步和到顶都是缓的（缓入缓出）。 */
    private fun easeSpin(power: Float): Float = power * power * (3f - 2f * power)

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
        // NoAllocMergeBlender keeps the last occurrence of a bone, so switchPose
        // must come after lowerPose to override shared bones while it plays.
        val pose = if (switchPose == DummyPose.INSTANCE) {
            lowerPose
        } else {
            MERGE_BLENDER.blend(listOf(lowerPose, switchPose))
        }
        return combineLayers(pose, upperPose)
    }

    private fun combineHoldOpen(pose: Pose, holdOpenPose: Pose): Pose {
        // hold_open drives the same bolt/slide bones that the fire animation cycles,
        // and both are authored around the closed position, so adding them would send
        // the bolt twice as far back. Merge instead: the hold-open pose wins over the
        // fire animation, keeping the bolt back the moment the magazine runs dry.
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
                    combineFireModeSwitch(
                        combineLayers(
                            editExitRunner!!.evaluate(),
                            fireModeRunner?.evaluate() ?: DummyPose.INSTANCE,
                            closeStrikeRunner?.evaluate() ?: DummyPose.INSTANCE,
                            spinRunner?.evaluate() ?: DummyPose.INSTANCE
                        ),
                        fireModeSwitchRunner?.evaluate() ?: DummyPose.INSTANCE,
                        fireRunner?.evaluate() ?: DummyPose.INSTANCE
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
        val animation = GunResource.compute(stack).animation
        val (holdOpenStarted, closeStrikeStarted) = updateMechanicalRunners(data, animation)

        // 是否是新的一次挥击。**必须先于下面的 runner 判定消费掉**：
        // 第一段挥击走的是 `runner == null` 分支（那里本来就会 play），要是留到这里再判，
        // 序号没被消费就会在下一帧被当成"又挥了一次"，把动画从头重播一遍。
        val newMeleeSwing = consumeMeleeSwing()

        if (runner == null || currentState != target) {
            play(target)
        } else if (newMeleeSwing && currentState == GunAnimationState.MELEE) {
            // 连招/连挥：状态没变但确实开了新的一段，重播一次（不重建状态，只换 runner）
            play(GunAnimationState.MELEE)
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

        // 副武器自己的换弹动画（四期，§9.8.7）：它播在**附件模型**上，不进下面这份 `cachedPose`
        // —— 宿主枪照常播自己的 idle/run，两套骨骼天然不冲突。
        //
        // ⚠ 整块**只有主手实例跑**（见构造参数 [hand] 的说明）：副武器是主手那把枪身上的东西，
        // 而两份实例（两手的）解析出来的是同一个 `player.mainHandItem` —— 不挡的话副手实例会在
        // 同一份数据上再建一套 runner，把同一支换弹动画的音效关键帧再收一遍。
        if (hand == InteractionHand.MAIN_HAND) {
            updateSubWeaponReload()
            subWeaponReloadRunner?.tick()
            // 常驻的 idle 锚点（§11.11.7.4）：只解析 clip，不跑 runner —— 渲染侧每帧取它的 `t=0`
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
        // 副武器换弹动画的**时间轴音效**（五期）：`animation.sub_weapon_gp_25.reload` 里那几条
        // `sound_effects` 与主武器完全同一套机制（同一个 `BedrockAnimation.SOUND_CHANNEL_NAME`，
        // 同一支 [collectSoundEvents]），只是先前没人来收 —— 附件动画播放链路当时只取姿态。
        // 位置就在 [subWeaponReloadRunner] 的 `tick()` 之后，与上面那一排同款。
        collectSoundEvents(subWeaponReloadRunner)

        if (fireRunner?.state is StopState) {
            fireRunner = null
        }

        cachedPose = combineHoldOpen(
            combineFireModeSwitch(
                combineLayers(
                    runner?.evaluate() ?: DummyPose.INSTANCE,
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

    override fun getCameraRotation(): Quaternionf = cameraRotation

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
         * 缓入缓出每帧最多吃掉多少 tick。`deltaFrameTime` 单位是 tick（20/s，60FPS 一帧约 0.33），
         * 卡顿或断点续跑时可能蹦得很大——上面的常数按秒写就会一帧走完。上限照抄
         * `GeoGunRenderer.scriptFrameDeltaSeconds`，掉帧的时候是"跳帧"而不是"瞬移"。
         */
        private const val SPIN_MAX_FRAME_DELTA_TICKS = 0.8f

        private val BLENDER: EulerAdditiveBlender =
            SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }

        private val MERGE_BLENDER = NoAllocMergeBlender()
    }
}
