package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.sendPacketTo
import com.atsuishio.superbwarfare.world.phys.ExtendedEntityRayTraceResult
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import kotlin.math.min

open class SplitType88GrenadeEntity : BounceProjectile, BasicGeoProjectileEntity {
    private var fuse = ExplosionConfig.SPLIT_TYPE_88_GRENADE_FUSE.get()

    constructor(type: EntityType<out SplitType88GrenadeEntity>, level: Level) : super(type, level)

    constructor(type: EntityType<out SplitType88GrenadeEntity>, x: Double, y: Double, z: Double, level: Level) :
            super(type, x, y, z, level)

    constructor(owner: Entity?, level: Level) : super(ModEntities.SPLIT_TYPE_88_GRENADE.get(), owner, level)

    init {
        this.damageValue = 1f
        this.headShotValue = 10f
        this.explosionDamageValue = ExplosionConfig.SPLIT_TYPE_88_GRENADE_EXPLOSION_DAMAGE.get().toFloat()
        this.explosionRadiusValue = ExplosionConfig.SPLIT_TYPE_88_GRENADE_EXPLOSION_RADIUS.get().toFloat()
    }

    override fun getDefaultItem(): Item {
        return ModItems.TYPE_88_CLUSTER_GRENADES.get()
    }

    override fun canPassThroughFluid() = true

    override fun setExplosionDamage(value: Float) {
        this.explosionDamageValue = ExplosionConfig.SPLIT_TYPE_88_GRENADE_EXPLOSION_DAMAGE.get().toFloat()
    }

    override fun setExplosionRadius(value: Float) {
        this.explosionRadiusValue = ExplosionConfig.SPLIT_TYPE_88_GRENADE_EXPLOSION_RADIUS.get().toFloat()
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putInt("Fuse", this.fuse)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("Fuse")) {
            this.fuse = compound.getInt("Fuse")
        }
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
                result.location.x,
                result.location.y,
                result.location.z,
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
        --this.fuse

        if (this.fuse <= 0) {
            this.causeExplode(position())
            this.discard()
        }

        val level = this.level()
        if (level is ServerLevel) {
            ParticleTool.sendParticle(
                level, ParticleTypes.SMOKE, this.xo, this.yo, this.zo,
                1, 0.0, 0.0, 0.0, 0.01, true
            )
        }
    }

    override fun isFastMoving(): Boolean {
        return false
    }
}
