package com.atsuishio.superbwarfare.client.animation.gun

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.magazineLevel
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.resource.gun.GunAnimation
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.resource.model.GunModelReloadListener
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
    hand: InteractionHand
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
    private var editExitRunner: AnimationRunner? = null
    private var currentState: GunAnimationState? = null
    private var fireSerial = 0
    private var consumedFireSerial = 0
    private var fireModeSwitchSerial = 0
    private var consumedFireModeSwitchSerial = 0
    private var lastFireModeName: String? = null
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

        if (animation.melee != null && ClientEventHandler.gunMelee > 0) return GunAnimationState.MELEE
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

    fun triggerFire(stack: ItemStack) {
        val animation = GunResource.compute(stack).animation ?: return
        val fireName = animation.fire ?: return
        if (!animations.containsKey(fireName)) return

        if (this.stack.item != stack.item) {
            updateItem(stack)
        }

        fireSerial++
        pendingShellEjects += 0
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
        return GunResource.compute(stack).drumLevels.list.contains(GunData.from(stack).magazineLevel())
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
            GunAnimationState.ITERATIVE -> animation.iterative
            GunAnimationState.ITERATIVE_2 -> animation.iterative
            GunAnimationState.FINISH -> animation.finish
            GunAnimationState.MELEE -> animation.melee
            GunAnimationState.FIRE -> animation.fire
            GunAnimationState.RUN -> animation.run
        }
    }

    private fun GunAnimationState.isReload(): Boolean {
        return this == GunAnimationState.RELOAD ||
                this == GunAnimationState.RELOAD_NORMAL ||
                this == GunAnimationState.RELOAD_EMPTY ||
                this == GunAnimationState.PREPARE ||
                this == GunAnimationState.ITERATIVE ||
                this == GunAnimationState.ITERATIVE_2 ||
                this == GunAnimationState.FINISH
    }

    private fun reloadTicks(state: GunAnimationState, data: GunData): Int {
        val rawTicks = when (state) {
            GunAnimationState.RELOAD_NORMAL -> data.get(GunProp.NORMAL_RELOAD_TIME)
            GunAnimationState.RELOAD_EMPTY -> data.get(GunProp.EMPTY_RELOAD_TIME)
            GunAnimationState.RELOAD ->
                if (data.reload.empty()) data.get(GunProp.EMPTY_RELOAD_TIME)
                else data.get(GunProp.NORMAL_RELOAD_TIME)

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
        if (state.isReload()) {
            setAnimationSpeed(playState, reloadPlaybackSpeed(state, animation))
        }
        val newRunner = AnimationRunner(animation, AnimationContext(animation.specifiedEndTimeS))
        newRunner.state = playState
        runner = newRunner
        currentState = state
        cachedPose = newRunner.evaluate()
    }

    private fun playFire() {
        val animation = GunResource.compute(stack).animation ?: return
        val fireName = animation.fire ?: return
        val fireAnimation = animations[fireName] ?: return

        val newRunner = AnimationRunner(fireAnimation, AnimationContext(fireAnimation.specifiedEndTimeS))
        newRunner.state = AnimationPlayType.PLAY_ONCE_STOP.state()
        fireRunner = newRunner
        cachedPose = newRunner.evaluate()
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

        cachedPose = combineFireModeSwitch(
            combineLayers(
                exitRunner.evaluate(),
                fireModeRunner?.evaluate() ?: DummyPose.INSTANCE,
                holdOpenRunner?.evaluate() ?: DummyPose.INSTANCE,
                closeStrikeRunner?.evaluate() ?: DummyPose.INSTANCE
            ),
            fireModeSwitchRunner?.evaluate() ?: DummyPose.INSTANCE,
            fireRunner?.evaluate() ?: DummyPose.INSTANCE
        )
    }

    private fun updateMechanicalRunners(
        data: GunData,
        animation: GunAnimation?
    ): Pair<Boolean, Boolean> {
        val shouldHoldOpen = data.holdOpen.get()
                && fireRunner == null
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
                cachedPose = combineFireModeSwitch(
                    combineLayers(
                        editExitRunner!!.evaluate(),
                        fireModeRunner?.evaluate() ?: DummyPose.INSTANCE,
                        holdOpenRunner?.evaluate() ?: DummyPose.INSTANCE,
                        closeStrikeRunner?.evaluate() ?: DummyPose.INSTANCE
                    ),
                    fireModeSwitchRunner?.evaluate() ?: DummyPose.INSTANCE,
                    fireRunner?.evaluate() ?: DummyPose.INSTANCE
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

        if (runner == null || currentState != target) {
            play(target)
        } else {
            runner?.tick()
        }
        // Keep the reload animation aligned if perks change the reload prop mid-reload.
        if (currentState != null && currentState!!.isReload()) {
            val runnerAnimation = runner?.animation as? BedrockAnimation
            if (runnerAnimation != null) {
                setAnimationSpeed(runner?.state, reloadPlaybackSpeed(currentState!!, runnerAnimation))
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

        if (fireRunner?.state is StopState) {
            fireRunner = null
        }

        cachedPose = combineFireModeSwitch(
            combineLayers(
                runner?.evaluate() ?: DummyPose.INSTANCE,
                fireModeRunner?.evaluate() ?: DummyPose.INSTANCE,
                holdOpenRunner?.evaluate() ?: DummyPose.INSTANCE,
                closeStrikeRunner?.evaluate() ?: DummyPose.INSTANCE
            ),
            fireModeSwitchRunner?.evaluate() ?: DummyPose.INSTANCE,
            fireRunner?.evaluate() ?: DummyPose.INSTANCE
        )
    }

    private fun collectParticleEvents(animationRunner: AnimationRunner?) {
        val particles = animationRunner?.clip<ParticleEffectData>(BedrockAnimation.PARTICLE_CHANNEL_NAME) ?: return
        for (keyframe in particles) {
            keyframe?.value?.let { pendingParticles += it }
        }
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
        currentState = null
        fireSerial = 0
        consumedFireSerial = 0
        pendingShellEjects.clear()
        pendingParticles.clear()
        cachedPose = DummyPose.INSTANCE
    }

    override fun shouldRenderHand(): Boolean {
        return true
    }

    companion object {
        private const val EDIT_EXIT_SPEED = 1.5f

        private val BLENDER: EulerAdditiveBlender =
            SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }

        private val MERGE_BLENDER = NoAllocMergeBlender()
    }
}
