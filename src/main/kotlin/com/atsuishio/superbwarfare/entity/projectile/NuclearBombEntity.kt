package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.network.message.receive.NuclearExplosionMessage
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage
import com.atsuishio.superbwarfare.tools.CustomExplosion
import kotlinx.serialization.Serializable
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.ForgeEventFactory

open class NuclearBombEntity(type: EntityType<out NuclearBombEntity>, level: Level, animated: Boolean = true) :
    FastThrowableProjectile(type, level), BasicGeoProjectileEntity {
    private val anim: BasicProjectileAnimationInstance<*>? =
        if (animated && level.isClientSide) BasicProjectileAnimationInstance(
            this, loop = true, loopAnimation = "animation.projectile.flight"
        ) else null

    init {
        this.explosionRadiusValue = NuclearExplosionParameters.DEFAULT_RADIUS
        this.explosionDamageValue = 1000f
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

    override fun getDefaultItem() = BuiltInRegistries.ITEM.get(BuiltInRegistries.ENTITY_TYPE.getKey(this.type))

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
        ).setDropBlocks(false).setBlockDestructionRadius(blastRadius)
            .setBlockDestructionDuration((effect.durationTicks / 5).coerceIn(1, 80))
        if (ForgeEventFactory.onExplosionStart(level, explosion)) {
            discard()
            return
        }
        NuclearExplosionMessage.send(level, vec3, effect)
        ShakeClientMessage.sendToNearbyPlayers(level, vec3.x, vec3.y, vec3.z, 384.0, 35.0, 8.0)
        explosion.explode()
        explosion.finalizeExplosion(false)
        this.discard()
    }

    /** Per-detonation visuals. Distances are blocks; duration is game ticks (20 ticks = 1 second). */
    @Serializable
    data class NuclearExplosionParameters(
        /** Nominal mature cloud radius. Dust and pressure fronts keep their relative proportions. */
        val radius: Float = DEFAULT_RADIUS,
        /** Nominal height from the ground to the top of the mature cloud. */
        val mushroomHeight: Float = DEFAULT_HEIGHT,
        val durationTicks: Int = DEFAULT_DURATION_TICKS
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
            const val DEFAULT_RADIUS = 60f
            const val DEFAULT_HEIGHT = 140f
            const val DEFAULT_DURATION_TICKS = 1000
        }
    }
}
