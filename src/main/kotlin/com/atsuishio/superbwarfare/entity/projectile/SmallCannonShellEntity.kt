package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeProjectileHitDamage
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.Item
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BellBlock
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3

open class SmallCannonShellEntity(type: EntityType<out SmallCannonShellEntity>, level: Level) :
    FastThrowableProjectile(type, level) {
    private var aa = false

    init {
        this.noCulling = true
        this.damageValue = 40f
        this.explosionDamageValue = 80f
        this.explosionRadiusValue = 5f
    }

    override fun getDefaultItem(): Item {
        return ModItems.SMALL_SHELL_AP.get()
    }

    override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)
        val entity = result.entity
        val owner = this.owner
        if (owner != null && owner.vehicle != null && entity == owner.vehicle) return
        if (this.level() is ServerLevel) {
            entity.forceHurt(causeProjectileHitDamage(this.level().registryAccess(), this, owner), damageValue)

            if (entity is LivingEntity) {
                entity.invulnerableTime = 0
            }

            if (this.tickCount > 0) {
                causeExplode(result.getLocation(), true)
            }
            this.discard()
        }
    }

    public override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)
        val resultPos = result.blockPos
        val state = this.level().getBlockState(resultPos)

        if (this.level() is ServerLevel) {
            val hardness = this.level().getBlockState(resultPos).block.defaultDestroyTime()
            if (hardness != -1f) {
                if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                    val destroy = Math.random() < (1.0 - (hardness / 50.0)).coerceIn(0.1, 1.0)
                    if (destroy) {
                        this.level().destroyBlock(resultPos, true)
                    }
                }
            }
        }

        val block = state.block
        if (block is BellBlock) {
            block.attemptToRing(this.level(), resultPos, result.direction)
        }
        if (this.level() is ServerLevel) {
            causeExplode(result.getLocation(), false)
        }
        this.discard()
    }

    private fun causeExplode(vec3: Vec3, hitEntity: Boolean) {
        CustomExplosion.Builder(this)
            .attacker(this.owner)
            .damage(explosionDamageValue)
            .radius(explosionRadiusValue)
            .position(vec3)
            .withParticleType(explosionParticleType(explosionRadiusValue))
            .destroyBlock { if (hitEntity) Explosion.BlockInteraction.KEEP else (if (ExplosionConfig.EXPLOSION_DESTROY.get()) Explosion.BlockInteraction.DESTROY else Explosion.BlockInteraction.KEEP) }
            .damageMultiplier(1.25f)
            .explode()
    }

    override fun tick() {
        super.tick()
        if (aa) {
            crushProjectile(deltaMovement)
        }
        if (owner != null && distanceToSqr(owner!!) > 1048576) {
            if (level() is ServerLevel) {
                causeExplode(position())
            }
            this.discard()
        }
    }

    fun crushProjectile(velocity: Vec3) {
        if (this.level() is ServerLevel) {
            val frontBox = boundingBox.inflate(0.5).expandTowards(velocity)

            val target = level().getEntities(
                EntityTypeTest.forClass(Projectile::class.java),
                frontBox,
            ) { it !== this }
                .filter { it !is SmallCannonShellEntity && (it.bbWidth >= 0.3 || it.bbHeight >= 0.3) }
                .minByOrNull { it.position().distanceTo(this.position()) }

            if (target != null) {
                causeExplode(target.position(), false)
                if (target is DestroyableProjectile) {
                    val owner = this.owner
                    if (owner is LivingEntity) {
                        if (owner is ServerPlayer) {
                            owner.level().playSound(
                                null,
                                owner.blockPosition(),
                                ModSounds.INDICATION.get(),
                                SoundSource.VOICE,
                                1f,
                                1f
                            )
                            sendPacketTo(owner, ClientIndicatorMessage(0, 5))
                        }
                    }
                    target.forceHurt(
                        causeProjectileHitDamage(this.level().registryAccess(), this, owner),
                        damageValue
                    )
                } else {
                    target.discard()
                }

                this.discard()
            }
        }
    }

    fun antiAir(antiAir: Boolean) {
        this.aa = antiAir
    }

    override fun isFastMoving(): Boolean {
        return false
    }
}
