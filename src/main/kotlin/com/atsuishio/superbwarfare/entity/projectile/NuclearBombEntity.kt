package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.projectile.NuclearBombEntity.Companion.STUN_CONE_DEGREES
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.FlashbangMessage
import com.atsuishio.superbwarfare.network.message.receive.NuclearExplosionMessage
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.SoundTool
import com.atsuishio.superbwarfare.tools.forceApplyEffect
import com.atsuishio.superbwarfare.tools.sendPacketTo
import kotlinx.serialization.Serializable
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.Level
import net.minecraft.world.phys.*
import net.minecraftforge.event.ForgeEventFactory
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt

open class NuclearBombEntity(type: EntityType<out NuclearBombEntity>, level: Level, animated: Boolean = true) :
    FastThrowableProjectile(type, level), BasicGeoProjectileEntity {
    private val anim: BasicProjectileAnimationInstance<*>? =
        if (animated && level.isClientSide) BasicProjectileAnimationInstance(
            this, loop = true, loopAnimation = "animation.projectile.flight"
        ) else null

    init {
        this.explosionRadiusValue = NuclearExplosionParameters.DEFAULT_RADIUS
        this.explosionDamageValue = 200000f
        this.explosionPenetrationRatioValue = ExplosionConfig.NUCLEAR_BOMB_EXPLOSION_PENETRATION_RATIO.get().toFloat()
    }

    /** Null follows the projectile's explosion radius; height and duration are independent. */
    var effectRadius: Float? = null
    var mushroomHeight: Float = NuclearExplosionParameters.DEFAULT_HEIGHT
    var effectDurationTicks: Int = NuclearExplosionParameters.DEFAULT_DURATION_TICKS

    open fun getNuclearExplosionParameters() = NuclearExplosionParameters(
        radius = effectRadius ?: getExplosionRadius(),
        mushroomHeight = mushroomHeight,
        durationTicks = effectDurationTicks
    ).sanitized()

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        readNuclearEffectData(compound)
    }

    fun readNuclearEffectData(compound: CompoundTag) {
        val effect = compound.getCompound("NuclearEffect")
        effectRadius = if (effect.contains("Radius", Tag.TAG_ANY_NUMERIC.toInt())) effect.getFloat("Radius") else null
        mushroomHeight = if (effect.contains("Height", Tag.TAG_ANY_NUMERIC.toInt())) effect.getFloat("Height")
            else NuclearExplosionParameters.DEFAULT_HEIGHT
        effectDurationTicks = if (effect.contains("Duration", Tag.TAG_ANY_NUMERIC.toInt())) effect.getInt("Duration")
            else NuclearExplosionParameters.DEFAULT_DURATION_TICKS
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        val effect = CompoundTag()
        effectRadius?.let { effect.putFloat("Radius", it) }
        effect.putFloat("Height", mushroomHeight)
        effect.putInt("Duration", effectDurationTicks)
        compound.put("NuclearEffect", effect)
    }

    override fun getDefaultItem() = ModItems.SMELLY_BOY.get()

    override fun getAnimationInstance() = this.anim

    override fun canPassThroughFluid() = true

    override fun isFastMoving() = false

    override fun afterHitBlock(result: BlockHitResult) {
        causeExplode(result.location)
    }

    override fun afterHitEntity(result: EntityHitResult) {
        causeExplode(result.location)
    }

    override fun afterShieldBlock(result: EntityHitResult) {
        causeExplode(result.location)
    }

    override fun causeExplode(vec3: Vec3, splitNormal: Vec3?) {
        val level = level() as? ServerLevel ?: return
        if (exploded) return
        exploded = true
        val blastRadius = getExplosionRadius().let {
            if (it.isFinite() && it > 0f) it.coerceIn(1f, 4096f) else NuclearExplosionParameters.DEFAULT_RADIUS
        }
        val effect = getNuclearExplosionParameters()
        val explosion = CustomExplosion(
            level, this, ModDamageTypes.causeCustomExplosionDamage(level.registryAccess(), this, owner), null,
            getExplosionDamage(), vec3.x, vec3.y, vec3.z, blastRadius,
            if (hasExplosionDestroy()) Explosion.BlockInteraction.DESTROY else Explosion.BlockInteraction.KEEP
        ).setPenetrationRatio(ExplosionConfig.NUCLEAR_BOMB_EXPLOSION_PENETRATION_RATIO.get().toFloat())
            .setDropBlocks(false)
            .setBlockDestructionDuration((effect.durationTicks / 5).coerceIn(1, 80))
        if (ForgeEventFactory.onExplosionStart(level, explosion)) {
            discard()
            return
        }
        NuclearExplosionMessage.send(level, vec3, effect)

        SoundTool.playDistantSound(level, ModSounds.NUKE_EXPLOSION_CLOSE.get(), vec3, 32f, 1f, null)
        SoundTool.playDistantSound(level, ModSounds.NUKE_EXPLOSION_FAR.get(), vec3, 64f, 1f, null)
        SoundTool.playDistantSound(level, ModSounds.NUKE_EXPLOSION_VERY_FAR.get(), vec3, 192f, 1f, null)

        ShakeClientMessage.sendToNearbyPlayers(level, vec3.x, vec3.y, vec3.z, 2048.0, 45.0, 16.0)
        explosion.explode()
        explosion.finalizeExplosion(false)

        // 爆闪：正对爆心且中间没有方块遮挡的玩家和生物被闪光眩晕，越近越久
        applyFlashStun(level, vec3, blastRadius)
        applyRadiation(level, vec3, blastRadius)
        applyThermalBurn(level, vec3, blastRadius, THERMAL_WINDOW_TICKS)

        this.discard()
    }

    /**
     * 核爆瞬间的闪光眩晕（本模组的 STUN 效果，等同闪光弹的致盲）：伤害半径内、
     * 视线与爆心方向夹角小于 [STUN_CONE_DEGREES]、且眼睛高度到爆心没有方块遮挡的
     * 玩家和生物会被眩晕，越靠近爆心时间越长。
     */
    private fun applyFlashStun(level: ServerLevel, center: Vec3, blastRadius: Float) {
        val minCos = cos(Math.toRadians(STUN_CONE_DEGREES))
        val area = AABB(center, center).inflate(blastRadius.toDouble())

        for (target in level.getEntitiesOfClass(LivingEntity::class.java, area)) {
            if (!target.isAlive || target.isSpectator) continue

            val eyePos = target.eyePosition
            val direction = center.subtract(eyePos)
            val distance = direction.length()
            if (distance > blastRadius) continue

            // 视线方向与爆心方向的夹角必须小于 STUN_CONE_DEGREES
            if (distance > 1.0e-3 && target.lookAngle.dot(direction.scale(1.0 / distance)) < minCos) continue

            // 眼睛高度到爆心之间不能有方块遮挡
            val obstruction = level.clip(
                ClipContext(eyePos, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target)
            )
            if (obstruction.type != HitResult.Type.MISS) continue

            val exposure = (1.0 - distance / blastRadius).coerceIn(0.0, 1.0)
            val duration = (STUN_MIN_TICKS + (STUN_MAX_TICKS - STUN_MIN_TICKS) * exposure).toInt()

            target.forceApplyEffect(
                MobEffectInstance(ModMobEffects.STUN.get(), duration, 0, false, false, true),
                owner
            )
            if (target is ServerPlayer) {
                // 复用闪光弹的爆闪/残影/耳鸣表现，强度按距离衰减
                sendPacketTo(target, FlashbangMessage(exposure.toFloat(), (0.7 * exposure).toFloat()))
            }
        }
    }

    private fun applyRadiation(level: ServerLevel, center: Vec3, blastRadius: Float) {
        val area = AABB(center, center).inflate(blastRadius.toDouble())
        for (target in level.getEntitiesOfClass(LivingEntity::class.java, area)) {
            if (!target.isAlive || target.isSpectator) continue
            val distance = target.position().distanceTo(center)
            if (distance > blastRadius) continue

            val exposure = (1.0 - distance / blastRadius).coerceIn(0.0, 1.0)
            val amplifier = (RADIATION_MAX_AMPLIFIER * exposure).roundToInt()
            val duration = (RADIATION_MIN_EFFECT_TICKS +
                    (RADIATION_MAX_EFFECT_TICKS - RADIATION_MIN_EFFECT_TICKS) * exposure).roundToInt()
            target.forceApplyEffect(
                MobEffectInstance(ModMobEffects.RADIATION.get(), duration, amplifier, false, true, true),
                owner
            )
        }
    }

    private fun applyThermalBurn(level: ServerLevel, center: Vec3, blastRadius: Float, remainingTicks: Int) {
        if (remainingTicks <= 0) return

        val attacker = owner
        val windowFactor = remainingTicks.toFloat() / THERMAL_WINDOW_TICKS
        val area = AABB(center, center).inflate(blastRadius.toDouble())

        for (target in level.getEntitiesOfClass(LivingEntity::class.java, area)) {
            if (!target.isAlive || target.isSpectator) continue

            val distance = sqrt(target.eyePosition.distanceToSqr(center))
            if (distance > blastRadius) continue

            val intensity = (1.0 - distance / blastRadius).toFloat().coerceIn(0f, 1f) * windowFactor
            if (intensity <= 0f) continue

            val amplifier = (intensity * (THERMAL_MAX_AMPLIFIER + 1)).toInt()
                .coerceIn(0, THERMAL_MAX_AMPLIFIER)
            val duration = (THERMAL_MAX_EFFECT_TICKS * intensity).toInt()
                .coerceIn(THERMAL_MIN_EFFECT_TICKS, THERMAL_MAX_EFFECT_TICKS)

            target.forceApplyEffect(
                MobEffectInstance(ModMobEffects.BURN.get(), duration, amplifier, false, true, true),
                attacker
            )
        }

        val next = remainingTicks - THERMAL_INTERVAL_TICKS
        if (next > 0) {
            Mod.queueServerWork(THERMAL_INTERVAL_TICKS) {
                applyThermalBurn(level, center, blastRadius, next)
            }
        }
    }

    /** Per-detonation visuals. Distances are blocks; duration is game ticks (20 ticks = 1 second). */
    @Serializable
    data class NuclearExplosionParameters(
        /** Nominal mature cloud radius. Dust and pressure fronts keep their relative proportions. */
        val radius: Float = DEFAULT_RADIUS,
        /** Nominal height from the ground to the top of the mature cloud. */
        val mushroomHeight: Float = DEFAULT_HEIGHT,
        val durationTicks: Int = DEFAULT_DURATION_TICKS,
        val renderMushroomCloud: Boolean = true
    ) {
        fun sanitized() = copy(
            radius = if (radius.isFinite() && radius > 0f) radius.coerceIn(1f, 4096f) else DEFAULT_RADIUS,
            mushroomHeight = if (mushroomHeight.isFinite() && mushroomHeight > 0f)
                mushroomHeight.coerceIn(4f, 4096f) else DEFAULT_HEIGHT,
            durationTicks = durationTicks.coerceIn(20, 12000)
        )

        val radiusScale get() = radius / DEFAULT_RADIUS
        val heightScale get() = mushroomHeight / DEFAULT_HEIGHT
        val timeScale get() = DEFAULT_DURATION_TICKS.toFloat() / durationTicks
        val visibilityRange get() = maxOf(1024.0, radius * 5.0 + 256.0, mushroomHeight + 256.0)
        fun isAlive(elapsedTicks: Long) = elapsedTicks in 0 until durationTicks.toLong()
        fun ageSeconds(elapsedTicks: Float) = (elapsedTicks * timeScale / 20f).coerceAtLeast(0f)

        companion object {
            const val DEFAULT_RADIUS = 100f
            const val DEFAULT_HEIGHT = 140f
            const val DEFAULT_DURATION_TICKS = 1000
        }
    }

    companion object {
        /** 闪光眩晕：视线与爆心方向夹角小于该角度（度）时才会被闪到 */
        const val STUN_CONE_DEGREES = 70.0

        /** 闪光眩晕的最长持续时间（tick）：贴脸 30 秒 */
        const val STUN_MAX_TICKS = 600

        /** 闪光眩晕的最短持续时间（tick） */
        const val STUN_MIN_TICKS = 20

        /** 燃烧窗口时长（tick）：爆后 30 秒 */
        const val THERMAL_WINDOW_TICKS = 600

        /** 燃烧窗口内的刷新间隔（tick） */
        const val THERMAL_INTERVAL_TICKS = 20

        /** 燃烧的最高等级对应的放大器（5 级 → 4） */
        const val THERMAL_MAX_AMPLIFIER = 4

        /** 燃烧效果的最长持续时间（tick）：30 秒 */
        const val THERMAL_MAX_EFFECT_TICKS = 600

        /** 燃烧效果的最短持续时间（tick） */
        const val THERMAL_MIN_EFFECT_TICKS = 20

        private const val RADIATION_MAX_AMPLIFIER = 19
        private const val RADIATION_MAX_EFFECT_TICKS = 3600
        private const val RADIATION_MIN_EFFECT_TICKS = 100
    }
}
