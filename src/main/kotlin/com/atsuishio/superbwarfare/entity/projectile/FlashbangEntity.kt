package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.FlashbangMessage
import com.atsuishio.superbwarfare.tools.forceApplyEffect
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import kotlin.math.roundToInt

class FlashbangEntity(type: EntityType<out FlashbangEntity>, level: Level) :
    BounceProjectile(type, level), BasicGeoProjectileEntity {
    private var fuse = 40

    override fun getDefaultItem() = ModItems.FLASHBANG.get()
    override fun canPassThroughFluid() = true
    override fun isFastMoving() = false
    override fun getCustomGravity() = 0.05f

    // Bypass the damage path entirely, including shield and hit-effect callbacks.
    override fun onHitEntity(result: EntityHitResult) {
        if (result.entity === owner) return
        bounce(Direction.getNearest(deltaMovement.x, deltaMovement.y, deltaMovement.z).opposite)
    }

    override fun afterHitBlock(result: BlockHitResult) = bounce(result.direction)

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putInt("FlashFuse", fuse)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("FlashFuse")) fuse = compound.getInt("FlashFuse").coerceIn(1, 40)
    }

    override fun tick() {
        super.tick()
        val level = level() as? ServerLevel ?: return
        if (!isRemoved && --fuse <= 0) {
            val origin = position().add(0.0, 0.15, 0.0)
            level.playSound(null, x, y, z, ModSounds.FLASHBANG_BURST.get(), SoundSource.PLAYERS, 2f, 1f)
            // No bright FLASH particle: the accessibility setting controls the entire visual flash.
            level.sendParticles(ParticleTypes.SMOKE, origin.x, origin.y, origin.z, 8, 0.1, 0.1, 0.1, 0.03)
            for (target in level.getEntitiesOfClass(LivingEntity::class.java, boundingBox.inflate(16.0))) {
                if (!target.isAlive || target.isSpectator || (target !is ServerPlayer && target !is Mob)) continue
                val direction = origin.subtract(target.eyePosition)
                val distance = direction.length()
                if (distance >= 16.0) continue
                val obstruction = level.clip(ClipContext(
                    target.eyePosition, origin, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target
                ))
                if (obstruction.type != HitResult.Type.MISS) continue
                val exposure = ((16.0 - distance) / 13.0).coerceIn(0.0, 1.0)
                if (target is ServerPlayer) {
                    val facing = if (distance < 0.01) 1.0 else target.lookAngle.dot(direction.scale(1.0 / distance))
                    val visual = exposure * (0.25 + 0.75 * ((facing + 0.25) / 0.75).coerceIn(0.0, 1.0))
                    sendPacketTo(target, FlashbangMessage(visual.toFloat(), exposure.toFloat()))
                }
                if (target is Mob || ProjectileConfig.FLASHBANG_STUN_PLAYERS.get()) {
                    target.forceApplyEffect(MobEffectInstance(
                        ModMobEffects.STUN.get(), (20 + 80 * exposure).roundToInt(), 0, false, false, true
                    ), owner)
                }
            }
            discard()
        }
    }
}
