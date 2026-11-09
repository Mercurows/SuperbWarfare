package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.entity.ai.goal.DecoyGrenadeGoal
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import java.util.*

open class DecoyGrenadeEntity(type: EntityType<out DecoyGrenadeEntity>, level: Level) : BounceProjectile(type, level),
    BasicGeoProjectileEntity {
    var activated = false
        private set
    private var activeAge = 0
    private var age = 0
    var settled by SETTLED
    private var settledAge = 0
    private var detonated = false
    private var nextShot = 0
    private val hitTargets = mutableSetOf<UUID>()
    private val luredMobs = mutableMapOf<Mob, DecoyGrenadeGoal>()

    init {
        lifeValue = ProjectileConfig.DECOY_GRENADE_LIFETIME.get()
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(SETTLED, false)
    }

    override fun getDefaultItem() = ModItems.DECOY_GRENADE.get()

    override fun canPassThroughFluid() = true

    override fun isFastMoving() = false

    override fun getCustomGravity() = 0.05f

    override fun onHitEntity(result: EntityHitResult) {
        if (settled || result.entity === owner) return
        if (!level().isClientSide && hitTargets.add(result.entity.uuid)) {
            val damage = ProjectileConfig.DECOY_GRENADE_HIT_DAMAGE.get().toFloat()
            if (damage > 0f) result.entity.forceHurt(ModDamageTypes.causeProjectileHitDamage(level().registryAccess(), this, owner), damage)
        }
        bounce(Direction.getNearest(deltaMovement.x, deltaMovement.y, deltaMovement.z).opposite)
    }

    override fun afterHitBlock(result: BlockHitResult) {
        if (!settled) bounce(result.direction)
    }

    override fun projectileMove(level: Level) {
        if (settled) {
            deltaMovement = Vec3.ZERO
            return
        }
        super.projectileMove(level)
        if (!level.isClientSide && onGround()) {
            val stopSpeed = ProjectileConfig.DECOY_GRENADE_STOP_SPEED.get()
            if (deltaMovement.lengthSqr() <= stopSpeed * stopSpeed) {
                settled = true
                deltaMovement = Vec3.ZERO
            }
        }
    }

    override fun tick() {
        if (!level().isClientSide && ++age > lifeValue) {
            discard()
            return
        }
        val wasSettled = settled
        super.tick()
        if (level().isClientSide || isRemoved) return
        if (!ProjectileConfig.DECOY_GRENADE_ATTRACT_MOBS.get()) clearLures()
        if (!settled || !wasSettled) return
        if (!activated) {
            if (++settledAge < ProjectileConfig.DECOY_GRENADE_ACTIVATION_DELAY.get()) return
            activated = true
        }
        if (activeAge >= ProjectileConfig.DECOY_GRENADE_DURATION.get()) {
            detonate()
            return
        }
        activeAge++
        if (--nextShot <= 0) {
            level().playSound(null, x, y, z, GUNSHOTS[random.nextInt(GUNSHOTS.size)], SoundSource.HOSTILE, 2f, 1f)
            nextShot = 4 + random.nextInt(9)
        }
        if (!ProjectileConfig.DECOY_GRENADE_ATTRACT_MOBS.get() || activeAge % 10 != 1) return
        val radius = ProjectileConfig.DECOY_GRENADE_RADIUS.get().toDouble()
        val iterator = luredMobs.iterator()
        while (iterator.hasNext()) {
            val (mob, goal) = iterator.next()
            if (!goal.canUse()) {
                mob.goalSelector.removeGoal(goal)
                iterator.remove()
            }
        }
        for (mob in level().getEntitiesOfClass(Mob::class.java, boundingBox.inflate(radius))) {
            if (!mob.isAlive || mob.isNoAi || mob.isSpectator || mob.distanceToSqr(this) > radius * radius || mob in luredMobs) continue
            val goal = DecoyGrenadeGoal(mob, this)
            luredMobs[mob] = goal
            mob.goalSelector.addGoal(-1, goal)
        }
    }

    private fun detonate() {
        if (detonated || isRemoved || level().isClientSide) return
        detonated = true
        activated = false
        clearLures()
        CustomExplosion.Builder(this).attacker(owner)
            .damage(ProjectileConfig.DECOY_GRENADE_EXPLOSION_DAMAGE.get().toFloat())
            .radius(ProjectileConfig.DECOY_GRENADE_EXPLOSION_RADIUS.get().toFloat())
            .keepBlock().explode()
        discard()
    }

    private fun clearLures() {
        luredMobs.forEach { (mob, goal) -> mob.goalSelector.removeGoal(goal) }
        luredMobs.clear()
    }

    override fun remove(reason: RemovalReason) {
        clearLures()
        super.remove(reason)
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putBoolean("Activated", activated)
        compound.putInt("ActiveAge", activeAge)
        compound.putInt("DecoyAge", age)
        compound.putBoolean("Settled", settled)
        compound.putInt("SettledAge", settledAge)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        activated = compound.getBoolean("Activated")
        activeAge = compound.getInt("ActiveAge").coerceAtLeast(0)
        age = compound.getInt("DecoyAge").coerceAtLeast(0)
        settled = compound.getBoolean("Settled") || activated
        settledAge = compound.getInt("SettledAge").coerceAtLeast(0)
        if (settled) deltaMovement = Vec3.ZERO
    }

    companion object {
        @JvmField
        val SETTLED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(DecoyGrenadeEntity::class.java, EntityDataSerializers.BOOLEAN)

        private val GUNSHOTS = listOf("ak_47", "glock_17", "m_4").map {
            SoundEvent.createVariableRangeEvent(loc("${it}_fire_3p"))
        }
    }
}
