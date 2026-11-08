package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.FlashbangMessage
import com.atsuishio.superbwarfare.tools.forceApplyEffect
import com.atsuishio.superbwarfare.tools.forceHurt
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
import java.util.*
import kotlin.math.roundToInt

class M84StunGrenadeEntity(type: EntityType<out M84StunGrenadeEntity>, level: Level) :
    BounceProjectile(type, level), BasicGeoProjectileEntity {
    private var fuse = 40
    private val hitTargets = mutableSetOf<UUID>()

    override fun getDefaultItem() = ModItems.M84_STUN_GRENADE.get()
    override fun canPassThroughFluid() = true
    override fun isFastMoving() = false
    override fun getCustomGravity() = 0.05f

    override fun onHitEntity(result: EntityHitResult) {
        val target = result.entity
        if (target === owner) return
        val damage = ProjectileConfig.FLASHBANG_HIT_DAMAGE.get().toFloat()
        if (!level().isClientSide && damage > 0f && hitTargets.add(target.uuid)) {
            target.forceHurt(ModDamageTypes.causeProjectileHitDamage(level().registryAccess(), this, owner), damage)
        }
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
            val radius = ProjectileConfig.FLASHBANG_RADIUS.get().toDouble()
            val coreRadius = ProjectileConfig.FLASHBANG_CORE_RADIUS.get()
            val explosionDamage = ProjectileConfig.FLASHBANG_EXPLOSION_DAMAGE.get().toFloat()
            level.playSound(null, x, y, z, ModSounds.FLASHBANG_BURST.get(), SoundSource.PLAYERS, 2f, 1f)
            // No bright FLASH particle: the accessibility setting controls the entire visual flash.
            level.sendParticles(ParticleTypes.SMOKE, origin.x, origin.y, origin.z, 8, 0.1, 0.1, 0.1, 0.03)
            for (target in level.getEntitiesOfClass(LivingEntity::class.java, boundingBox.inflate(maxOf(radius, coreRadius)))) {
                if (!target.isAlive || target.isSpectator || (target !is ServerPlayer && target !is Mob)) continue
                val direction = origin.subtract(target.eyePosition)
                val distance = direction.length()
                if (distance >= maxOf(radius, coreRadius)) continue
                // Preserve the previous distance falloff, scaled to the configured radius.
                val exposure = ((radius - distance) / (radius * 13.0 / 16.0)).coerceIn(0.0, 1.0)
                val obstruction = level.clip(ClipContext(
                    target.eyePosition, origin, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target
                ))
                val visible = obstruction.type == HitResult.Type.MISS
                if (visible && explosionDamage > 0f && distance < coreRadius) {
                    target.forceHurt(ModDamageTypes.causeProjectileExplosionDamage(level.registryAccess(), this, owner),
                        explosionDamage)
                }
                if (distance >= radius) continue
                if (target is ServerPlayer) {
                    val facing = if (distance < 0.01) 1.0 else target.lookAngle.dot(direction.scale(1.0 / distance))
                    val visual = if (visible) {
                        exposure * (0.25 + 0.75 * ((facing + 0.25) / 0.75).coerceIn(0.0, 1.0))
                    } else 0.0
                    // Walls and looking away do not protect hearing; send audio even with no visual exposure.
                    sendPacketTo(target, FlashbangMessage(visual.toFloat(), exposure.toFloat()))
                }
                if (visible && (target is Mob || ProjectileConfig.FLASHBANG_STUN_PLAYERS.get())) {
                    target.forceApplyEffect(MobEffectInstance(
                        ModMobEffects.STUN.get(), (20 + 80 * exposure).roundToInt(), 0, false, false, true
                    ), owner)
                }
            }
            discard()
        }
    }
}
