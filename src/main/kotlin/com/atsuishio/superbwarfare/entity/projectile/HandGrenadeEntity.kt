package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.sendPacketTo
import com.atsuishio.superbwarfare.world.phys.ExtendedEntityRayTraceResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.min

open class HandGrenadeEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    constructor(type: EntityType<out HandGrenadeEntity>, level: Level) : super(type, level)

    constructor(type: EntityType<out HandGrenadeEntity>, x: Double, y: Double, z: Double, level: Level) :
            super(type, x, y, z, level)

    constructor(entity: LivingEntity?, level: Level) : super(ModEntities.HAND_GRENADE.get(), entity, level)

    init {
        this.damageValue = 1f
        this.explosionDamageValue = ExplosionConfig.M67_GRENADE_EXPLOSION_DAMAGE.get().toFloat()
        this.explosionRadiusValue = ExplosionConfig.M67_GRENADE_EXPLOSION_RADIUS.get().toFloat()
        this.headShotValue = 10f
    }

    override fun getDefaultItem(): Item {
        return ModItems.HAND_GRENADE.get()
    }

    override fun afterHitBlock(result: BlockHitResult) {
        val resultPos = result.blockPos
        val state = this.level().getBlockState(resultPos)
        val block = state.block
        val event = block.getSoundType(state, this.level(), resultPos, this).breakSound
        val speed = this.deltaMovement.length()
        if (speed > 0.3) {
            val volume = min(4f, speed.toFloat() / 4f + 0.5f)
            this.level().playSound(
                null,
                result.getLocation().x,
                result.getLocation().y,
                result.getLocation().z,
                event,
                SoundSource.AMBIENT,
                volume,
                1f
            )
        }
        this.bounce(result.direction)
    }

    override fun afterHitEntity(result: EntityHitResult) {
        if (result !is ExtendedEntityRayTraceResult) return
        val entity = result.entity
        val owner = this.owner
        if (entity == owner || entity == this.vehicle) return
        val speedE = this.deltaMovement.length()
        if (speedE > 0.1) {
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
        this.bounce(
            Direction.getNearest(
                this.deltaMovement.x(),
                this.deltaMovement.y(),
                this.deltaMovement.z()
            ).opposite
        )
        this.deltaMovement = this.deltaMovement.multiply(0.25, 1.0, 0.25)
    }

    override fun tick() {
        super.tick()
        val level = this.level()
        if (level is ServerLevel) {
            ParticleTool.sendParticle(
                level, ParticleTypes.SMOKE, this.xo, this.yo, this.zo,
                1, 0.0, 0.0, 0.0, 0.01, true
            )
        }
        if (isInFluidType) {
            deltaMovement = deltaMovement.scale(0.75)
        }

        // 接近静止且下方有固体方块时，彻底停止移动，避免因重力反复微弹跳
        if (!level.isClientSide && this.deltaMovement.length() < 0.08) {
            val groundPos = BlockPos.containing(this.position().subtract(0.0, 0.15, 0.0))
            if (level.getBlockState(groundPos).isSolid) {
                this.deltaMovement = Vec3.ZERO
            }
        }
    }

    override fun isFastMoving(): Boolean {
        return false
    }
}
