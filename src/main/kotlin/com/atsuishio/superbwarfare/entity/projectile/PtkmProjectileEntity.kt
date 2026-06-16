package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeCustomExplosionDamage
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.RangeTool.calculateFiringSolution
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

open class PtkmProjectileEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    private var shootTime = 3
    private var target: Entity? = null

    init {
        this.damageValue = ExplosionConfig.PTKM_1R_PROJECTILE_HIT_DAMAGE.get().toFloat()
        this.explosionDamageValue = ExplosionConfig.PTKM_1R_PROJECTILE_EXPLOSION_DAMAGE.get().toFloat()
        this.explosionRadiusValue = ExplosionConfig.PTKM_1R_PROJECTILE_EXPLOSION_RADIUS.get().toFloat()
    }

    constructor(type: EntityType<out PtkmProjectileEntity>, level: Level) : super(type, level)

    constructor(entity: LivingEntity?, level: Level) : super(ModEntities.PTKM_PROJECTILE.get(), entity, level)

    override fun getDefaultItem(): Item {
        return ModItems.PTKM_1R.get()
    }

    override fun isPickable(): Boolean {
        return !this.isRemoved
    }

    override fun performDamage(
        entity: Entity,
        damage: Float,
        isHeadshot: Boolean
    ) {
        entity.invulnerableTime = 0

        val headShotModifier = if (isHeadshot) this.getHeadShot() else 1f
        if (damage > 0) {
            val finalDamage = damage * headShotModifier * if (target != null && tickCount > shootTime) 1f else 0.04f

            entity.forceHurt(
                if (isHeadshot)
                    ModDamageTypes.causeProjectileHitHeadshotDamage(this.level().registryAccess(), this, this.owner)
                else
                    ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.owner),
                finalDamage
            )
            entity.invulnerableTime = 0
        }
    }

    override fun afterHitEntity(result: EntityHitResult) {
        if (this.level() is ServerLevel) {
            explode(result.getLocation())
            this.discard()
        }
    }

    override fun afterHitBlock(result: BlockHitResult) {
        if (this.level() is ServerLevel) {
            explode(result.getLocation())
            this.discard()
        }
    }

    override fun tick() {
        super.tick()

        largeTrail()

        if (target != null) {
            if (tickCount == shootTime) {
                val targetVel = target!!.deltaMovement
                val targetVec =
                    calculateFiringSolution(position(), target!!.boundingBox.center, targetVel, 15.0, 0.05)
                this.deltaMovement = targetVec.scale(15.0)

                val level = this.level()
                if (level is ServerLevel) {
                    level.playSound(
                        null,
                        BlockPos.containing(position()),
                        ModSounds.EXPLOSION_AIR.get(),
                        SoundSource.BLOCKS,
                        8f,
                        1f
                    )
                    ParticleTool.spawnSmallExplosionParticles(level, position())
                    ParticleTool.sendParticle(
                        level, ParticleTypes.LARGE_SMOKE, position().x, position().y, position().z,
                        40, 0.5, 0.25, 0.5, 0.01, true
                    )
                    ParticleTool.sendParticle(
                        level, ParticleTypes.CAMPFIRE_COSY_SMOKE, position().x, position().y, position().z,
                        30, 0.5, 0.25, 0.5, 0.005, true
                    )
                    spawnDirectionalParticles(this, 55, 3.25, level, ParticleTypes.CAMPFIRE_COSY_SMOKE)
                    spawnDirectionalParticles(this, 50, 3.0, level, ParticleTypes.CAMPFIRE_COSY_SMOKE)
                    spawnDirectionalParticles(this, 45, 2.75, level, ParticleTypes.CAMPFIRE_COSY_SMOKE)
                    spawnDirectionalParticles(this, 40, 2.5, level, ParticleTypes.CAMPFIRE_COSY_SMOKE)

                    var count = 8

                    var i = 0f
                    while (i < this.distanceTo(target!!)) {
                        ParticleTool.sendParticle(
                            level, ParticleTypes.CLOUD,
                            position().x + i * deltaMovement.normalize().x,
                            position().y + i * deltaMovement.normalize().y,
                            position().z + i * deltaMovement.normalize().z,
                            (--count).coerceIn(2, 8), 0.25, 0.25, 0.25, 0.0025, true
                        )
                        ParticleTool.sendParticle(
                            level, ParticleTypes.FLAME,
                            position().x + i * deltaMovement.normalize().x,
                            position().y + i * deltaMovement.normalize().y,
                            position().z + i * deltaMovement.normalize().z,
                            (--count).coerceIn(2, 8), 0.25, 0.25, 0.25, 0.0025, true
                        )
                        i += .5f
                    }

                    var j = 0f
                    while (j < 16) {
                        ParticleTool.sendParticle(
                            level, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            position().x + j * deltaMovement.scale(-1.0).normalize().x,
                            position().y + j * deltaMovement.scale(-1.0).normalize().y,
                            position().z + j * deltaMovement.scale(-1.0).normalize().z,
                            (--count).coerceIn(2, 8), 0.25, 0.25, 0.25, 0.0025, true
                        )
                        j += .5f
                    }
                }
            }
        } else {
            if (tickCount > 100) {
                explode(position())
            }
        }
    }

    fun explode(pos: Vec3) {
        CustomExplosion.Builder(this)
            .damageSource(causeCustomExplosionDamage(level().registryAccess(), this, this.owner))
            .damage(explosionDamageValue)
            .radius(explosionRadiusValue)
            .position(pos)
            .particlePosition(pos)
            .beast(this.isBeast())
            .explode()
    }

    fun setShootTime(time: Int) {
        this.shootTime = time
    }

    fun setTarget(entity: Entity?) {
        this.target = entity
    }

    override fun largeTrail() {
        if (level().isClientSide && tickCount > 0) {
            val l = deltaMovement.length()
            var i = 0.0
            while (i < l) {
                val startPos = Vec3(this.xo, this.yo, this.zo)
                val pos = startPos.add(deltaMovement.normalize().scale(-i))
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0)
                i++
            }
        }
    }

    override fun isFastMoving(): Boolean {
        return false
    }

    companion object {
        fun spawnDirectionalParticles(
            projectile: Entity,
            count: Int,
            radius: Double,
            level: ServerLevel,
            particle: SimpleParticleType
        ) {
            val deltaMovement = projectile.deltaMovement

            val direction = deltaMovement.normalize()
            val position = projectile.position()

            // 构建垂直正交基
            val randomPerp: Vec3 = getRandomPerpendicular(direction)
            val u = randomPerp.normalize()
            val v = direction.cross(u).normalize()

            spawnCircularParticles(level, position, u, v, count, radius, particle)
        }

        private fun getRandomPerpendicular(dir: Vec3): Vec3 {
            val candidate1 = Vec3(dir.y, -dir.x, 0.0) // 在XY平面垂直
            if (candidate1.lengthSqr() > 1e-4) return candidate1
            return Vec3(0.0, dir.z, -dir.y) // 备用垂直向量
        }

        private fun spawnCircularParticles(
            level: ServerLevel,
            center: Vec3,
            u: Vec3,
            v: Vec3,
            count: Int,
            radius: Double,
            particle: SimpleParticleType
        ) {
            for (i in 0..<count) {
                val theta = 2 * Math.PI * i / count
                val xOffset = radius * (cos(theta) * u.x + sin(theta) * v.x)
                val yOffset = radius * (cos(theta) * u.y + sin(theta) * v.y)
                val zOffset = radius * (cos(theta) * u.z + sin(theta) * v.z)

                val pos = center.add(xOffset, yOffset, zOffset)
                spawnParticle(level, pos, particle)
            }
        }

        private fun spawnParticle(level: ServerLevel, pos: Vec3, particle: SimpleParticleType) {
            ParticleTool.sendParticle(
                level, particle, pos.x, pos.y, pos.z,
                1, 0.02, 0.02, 0.02, 0.0001, true
            )
        }
    }
}
