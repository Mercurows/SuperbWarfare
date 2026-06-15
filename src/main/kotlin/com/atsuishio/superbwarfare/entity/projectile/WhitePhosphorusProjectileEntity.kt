package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.max

open class WhitePhosphorusProjectileEntity : FastThrowableProjectile {
    constructor(type: EntityType<out WhitePhosphorusProjectileEntity>, world: Level) : super(type, world)

    constructor(entity: Entity?, level: Level) : super(ModEntities.WHITE_PHOSPHORUS_PROJECTILE.get(), entity, level)

    init {
        this.explosionRadiusValue = 5.0f
    }

    override fun getDefaultItem(): Item {
        return ModItems.WP_HEAD.get()
    }

    override fun performDamage(
        entity: Entity,
        damage: Float,
        isHeadshot: Boolean
    ) {
        entity.invulnerableTime = 0

        val headShotModifier = if (isHeadshot) this.getHeadShot() else 1f
        if (damage > 0) {
            entity.forceHurt(
                ModDamageTypes.causeBurnDamage(this.level().registryAccess(), this.owner),
                damage * headShotModifier
            )
            entity.invulnerableTime = 0
        }
    }

    override fun afterHitEntity(result: EntityHitResult) {
        val entity = result.entity
        if (entity is LivingEntity && !entity.level().isClientSide()) {
            entity.addEffect(MobEffectInstance(ModMobEffects.PHOSPHORUS_FIRE.get(), 200, 4), owner)
        }
        super.afterHitEntity(result)
    }

    override fun afterHitBlock(result: BlockHitResult) {
        val owner = this.owner
        if (owner != null) {
            causeWPEffect(result.getLocation(), owner)
        }
        this.discard()
    }

    open fun causeWPEffect(pos: Vec3, shooter: Entity) {
        if (this.level() is ServerLevel) {
            val entities = SeekTool.Builder(shooter)
                .withinRange(pos, explosionRadiusValue.toDouble())
                .notItsVehicle()
                .baseFilter()
                .noVehicle()
                .build()

            entities.asSequence()
                .filter { it is LivingEntity && !(it is Player && it.isCreative) }
                .forEach {
                    val dis = pos.distanceTo(it.position())
                    if (!checkNoClip(it, pos)) return@forEach

                    val owner = this.owner
                    it.forceHurt(ModDamageTypes.causeBurnDamage(this.level().registryAccess(), owner), 1f)
                    it.invulnerableTime = 0

                    (it as LivingEntity).addEffect(
                        MobEffectInstance(
                            ModMobEffects.PHOSPHORUS_FIRE.get(),
                            (300 - 30 * dis).toInt(),
                            max(explosionRadiusValue - dis, 0.0).toInt()
                        ), this.owner
                    )

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
        }
    }

    override fun tick() {
        super.tick()
        this.deltaMovement = this.deltaMovement.add(0.0, -0.02, 0.0)
        this.move(MoverType.SELF, this.deltaMovement)

        if (level().isClientSide()) {
            level().addAlwaysVisibleParticle(ParticleTypes.END_ROD, true, this.xo, this.yo, this.zo, 0.0, 0.0, 0.0)
            level().addAlwaysVisibleParticle(ParticleTypes.CLOUD, true, this.xo, this.yo, this.zo, 0.0, 0.0, 0.0)
        }
        if (this.tickCount > 200 || this.isInWater) {
            this.discard()
        }
    }
}
