package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModDamageTypes.causeBurnDamage
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
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
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.max

open class WhitePhosphorusProjectileEntity : FastThrowableProjectile {
    constructor(type: EntityType<out WhitePhosphorusProjectileEntity>, world: Level) : super(type, world)

    constructor(entity: Entity?, level: Level) : super(ModEntities.WHITE_PHOSPHORUS_PROJECTILE.get(), entity, level) {
        this.noCulling = true
    }

    override fun getDefaultItem(): Item {
        return Items.AIR
    }

    override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)
        val entity = result.entity
        val owner = this.owner
        if (owner is ServerPlayer) {
            owner.level()
                .playSound(null, owner.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1f, 1f)
            sendPacketTo(owner, ClientIndicatorMessage(0, 5))
        }
        if (entity is LivingEntity) {
            entity.forceHurt(causeBurnDamage(entity.level().registryAccess(), owner), 1f)
            entity.invulnerableTime = 0
            if (entity is Player && entity.isCreative) {
                return
            }
            if (!entity.level().isClientSide()) {
                entity.addEffect(MobEffectInstance(ModMobEffects.PHOSPHORUS_FIRE, 200, 4), owner)
            }
        }

        this.discard()
    }

    override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)
        val owner = this.owner
        if (owner != null) {
            findNearEntity(result.getLocation(), owner)
        }
        this.discard()
    }

    fun findNearEntity(pos: Vec3, shooter: Entity) {
        if (this.level() is ServerLevel) {
            val entities = SeekTool.Builder(shooter)
                .withinRange(pos, 5.0)
                .notItsVehicle()
                .baseFilter()
                .noVehicle()
                .build()

            for (e in entities) {
                val dis = pos.distanceTo(e.position())

                if (e is LivingEntity && checkNoClip(e, pos)) {
                    if (e is Player && e.isCreative) {
                        return
                    }

                    val owner = this.owner

                    e.forceHurt(causeBurnDamage(this.level().registryAccess(), owner), 1f)
                    e.invulnerableTime = 0

                    if (!e.level().isClientSide()) {
                        e.addEffect(
                            MobEffectInstance(
                                ModMobEffects.PHOSPHORUS_FIRE,
                                (200 - 30 * dis).toInt(),
                                max(4 - dis, 0.0).toInt()
                            ), owner
                        )
                    }

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
