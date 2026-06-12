package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModDamageTypes.causeShockDamage
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.AbstractArrow
import net.minecraft.world.entity.projectile.ItemSupplier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BellBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.network.NetworkHooks

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier::class)
open class TaserBulletEntity(type: EntityType<out TaserBulletEntity>, level: Level) : AbstractArrow(type, level),
    BasicGeoProjectileEntity {

    private var initialPos: Vec3? = null
    var damage = 1f
    var volt: Int = 0
    var wireLength: Int = 0
    private var stopped = false

    init {
        this.noCulling = true
    }

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> {
        return NetworkHooks.getEntitySpawningPacket(this)
    }

    override fun playerTouch(pEntity: Player) {
    }

    override fun getPickupItem(): ItemStack {
        return ItemStack.EMPTY
    }

    override fun onHitEntity(result: EntityHitResult) {
        val entity = result.entity
        val owner = this.owner
        if (owner != null && owner.vehicle != null && entity == owner.vehicle) return
        if (owner is ServerPlayer) {
            owner.level()
                .playSound(null, owner.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1f, 1f)
            sendPacketTo(owner, ClientIndicatorMessage(0, 5))
        }
        if (entity is LivingEntity) {
            entity.invulnerableTime = 0
            entity.forceHurt(causeShockDamage(this.level().registryAccess(), owner), this.damage)
            if (entity is Player && entity.isCreative) {
                return
            }

            val level = entity.level()
            if (level is ServerLevel) {
                if (entity is Creeper) {
                    entity.thunderHit(level, LightningBolt(EntityType.LIGHTNING_BOLT, level))
                } else {
                    entity.addEffect(
                        MobEffectInstance(ModMobEffects.SHOCK.get(), 100 + volt * 30, volt),
                        owner
                    )
                }
            }
        }
        this.discard()
    }

    public override fun onHitBlock(blockHitResult: BlockHitResult) {
        super.onHitBlock(blockHitResult)
        val resultPos = blockHitResult.blockPos
        val state = this.level().getBlockState(resultPos)
        val block = state.block
        if (block is BellBlock) {
            block.attemptToRing(this.level(), resultPos, blockHitResult.direction)
        }
    }

    override fun tick() {
        super.tick()

        if (this.tickCount == 1) {
            initialPos = this.position()
        }

        if (initialPos != null && this.position().distanceTo(initialPos!!) > 10 + 4 * wireLength && !stopped) {
            stopped = true
            this.deltaMovement = Vec3(0.0, 0.0, 0.0)
        }

        if (this.tickCount > 200) {
            this.discard()
        }
    }
}

