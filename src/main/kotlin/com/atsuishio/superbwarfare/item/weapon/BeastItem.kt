package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.mixin.BeastEntityKiller
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModRarities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.CustomDamageProperty
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.network.message.receive.LivingGunKillMessage
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.sendPacket
import com.atsuishio.superbwarfare.tools.sendPacketToAll
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tiers
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.AABB
import java.util.function.Consumer
import javax.annotation.ParametersAreNonnullByDefault

open class BeastItem : SwordItem(
    Tiers.NETHERITE, CustomDamageProperty(false)
        .stacksTo(1)
        .rarity(ModRarities.LEGENDARY)
        .setNoRepair()
) {
    override fun isDamageable(stack: ItemStack): Boolean {
        return false
    }

    override fun hurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        beastKill(attacker, target)
        return true
    }

    override fun getSweepHitBox(stack: ItemStack, player: Player, target: Entity): AABB {
        return super.getSweepHitBox(stack, player, target).inflate(3.0)
    }

    override fun canBeHurtBy(stack: ItemStack, source: DamageSource): Boolean {
        return false
    }

    override fun isEnchantable(stack: ItemStack): Boolean {
        return false
    }

    @ParametersAreNonnullByDefault
    override fun onEntitySwing(stack: ItemStack, entity: LivingEntity, hand: InteractionHand): Boolean {
        val target = TraceTool.findMeleeEntity(entity, 51.4)
        if (target != null) {
            beastKill(entity, target)
        }
        return super.onEntitySwing(stack, entity, hand)
    }

    override fun onLeftClickEntity(stack: ItemStack, player: Player, entity: Entity): Boolean {
        beastKill(player, entity)
        return super.onLeftClickEntity(stack, player, entity)
    }

    override fun canDisableShield(
        stack: ItemStack,
        shield: ItemStack,
        entity: LivingEntity,
        attacker: LivingEntity
    ): Boolean {
        return true
    }

    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.beast").withColor(0xa56855))
    }

    companion object {
        @JvmStatic
        fun beastKill(attacker: Entity?, target: Entity) {
            if (target.level().isClientSide) return

            if (target is TargetEntity) {
                target.hurt(
                    ModDamageTypes.causeBeastDamage(target.level().registryAccess(), attacker, attacker),
                    114514F
                )
                return
            }

            if (target is DPSGeneratorEntity) {
                target.hurt(
                    ModDamageTypes.causeBeastDamage(target.level().registryAccess(), attacker, attacker),
                    114514F
                )
                target.beastCharge()
                return
            }

            if (attacker is ServerPlayer) {
                attacker.sendPacket(ClientIndicatorMessage(0, 5))
                val holder = Holder.direct(ModSounds.INDICATION.get())
                attacker.connection.send(
                    ClientboundSoundPacket(
                        holder,
                        SoundSource.PLAYERS,
                        attacker.x,
                        attacker.y,
                        attacker.z,
                        1f,
                        1f,
                        attacker.level().random.nextLong()
                    )
                )

                val box = target.boundingBox
                (attacker.level() as ServerLevel).sendParticles<SimpleParticleType?>(
                    ParticleTypes.DAMAGE_INDICATOR,
                    target.x, target.y + .5, target.z,
                    1000,
                    box.xsize / 2.5, box.ysize / 3, box.zsize / 2.5,
                    0.0
                )

                if (MiscConfig.SEND_KILL_FEEDBACK.get()) {
                    sendPacketToAll(
                        LivingGunKillMessage(
                            attacker.id,
                            target.id,
                            false,
                            ModDamageTypes.BEAST
                        )
                    )
                }
            }

            if (target is ServerPlayer) {
                target.health = 0f
                target.level().players().forEach { p: Player? ->
                    p!!.sendSystemMessage(
                        Component.translatable(
                            "death.attack.beast_gun",
                            target.getDisplayName(),
                            if (attacker != null) attacker.displayName else ""
                        )
                    )
                }
            } else {
                if (target is LivingEntity) {
                    BeastEntityKiller.getInstance(target).`sbw$kill`()
                    target.health = 0f
                }
                target.level().broadcastEntityEvent(target, 60.toByte())

                target.removalReason = Entity.RemovalReason.KILLED
                target.getPassengers().forEach(Consumer { obj: Entity? -> obj!!.stopRiding() })
                target.stopRiding()

                target.levelCallback.onRemove(Entity.RemovalReason.KILLED)

                target.gameEvent(GameEvent.ENTITY_DIE)
            }

            target.level().playSound(
                target,
                BlockPos(target.x.toInt(), target.y.toInt(), target.z.toInt()),
                ModSounds.OUCH.get(),
                SoundSource.PLAYERS,
                2f,
                1f
            )
        }
    }
}