package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.entity.mixin.DamageAccess
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier.ModifyResult
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.DamageHandler.doDamage
import com.atsuishio.superbwarfare.tools.FormatTool.format2D
import net.minecraft.ChatFormatting
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.player.Player

fun Entity?.forceHurt(source: DamageSource, damage: Float): Boolean {
    return if (this == null) false
    else doDamage(this, source, damage)
}

object DamageHandler {
    @JvmStatic
    fun doDamage(entity: Entity, source: DamageSource, damage: Float): Boolean {
        var damage = damage
        if (entity.hurt(source, damage)) {
            return true
        } else if (entity is LivingEntity) {
            if (!MiscConfig.FORCE_DAMAGE_MODE.get()) {
                return false
            }
            if (entity.isInvulnerableTo(source)) {
                return false
            } else if (entity.level().isClientSide) {
                return false
            } else if (entity.isDeadOrDying) {
                return false
            } else if (source.`is`(DamageTypeTags.IS_FIRE) && entity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                return false
            } else if (entity is Player && (entity.isCreative || entity.isSpectator)) {
                return false
            } else {
                val sourceEntity = source.entity
                if (sourceEntity != null && entity.isAlliedTo(sourceEntity) && entity.team?.isAllowFriendlyFire == false) {
                    return false
                }

                if (entity.isSleeping && !entity.level().isClientSide) {
                    entity.stopSleeping()
                }
                entity.setNoActionTime(0)

                val access = DamageAccess.of(entity)

                val flag = false

                entity.walkAnimation.setSpeed(1.5f)

                var flag1 = true
                if (entity.invulnerableTime > 10 && !source.`is`(DamageTypeTags.BYPASSES_COOLDOWN)) {
                    if (damage <= entity.lastHurt) {
                        return false
                    }

                    access.`superbWarfare$actuallyHurt`(source, damage - entity.lastHurt)
                    entity.lastHurt = damage
                    flag1 = false
                } else {
                    entity.lastHurt = damage
                    entity.invulnerableTime = 20
                    access.`superbWarfare$actuallyHurt`(source, damage)
                    entity.hurtDuration = 10
                    entity.hurtTime = entity.hurtDuration
                }

                if (source.`is`(DamageTypeTags.DAMAGES_HELMET) && !entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty) {
                    access.`superbWarfare$hurtHelmet`(source, damage)
                    damage *= 0.75f
                }

                if (sourceEntity != null) {
                    if (sourceEntity is LivingEntity) {
                        if (!source.`is`(DamageTypeTags.NO_ANGER)) {
                            entity.setLastHurtByMob(sourceEntity)
                        }
                    }

                    if (sourceEntity is Player) {
                        entity.lastHurtByPlayerTime = 100
                        entity.setLastHurtByPlayer(sourceEntity)
                    } else if (sourceEntity is TamableAnimal) {
                        if (sourceEntity.isTame) {
                            entity.lastHurtByPlayerTime = 100
                            val owner = sourceEntity.owner
                            if (owner is Player) {
                                entity.setLastHurtByPlayer(owner)
                            } else {
                                entity.setLastHurtByPlayer(null)
                            }
                        }
                    }
                }

                if (flag1) {
                    entity.level().broadcastDamageEvent(entity, source)

                    if (!source.`is`(DamageTypeTags.NO_IMPACT)) {
                        entity.hurtMarked = true
                    }

                    if (sourceEntity != null && !source.`is`(DamageTypeTags.IS_EXPLOSION)) {
                        var d0 = sourceEntity.x - entity.x

                        var d1: Double
                        d1 = sourceEntity.z - entity.z
                        while (d0 * d0 + d1 * d1 < 1.0E-4) {
                            d0 = (Math.random() - Math.random()) * 0.01
                            d1 = (Math.random() - Math.random()) * 0.01
                        }

                        if (!source.`is`(ModTags.DamageTypes.NO_HURT_EFFECT)) {
                            entity.knockback(0.4, d0, d1)
                        }
                        if (!flag) {
                            entity.indicateDamage(d0, d1)
                        }
                    }
                }

                if (entity.isDeadOrDying) {
                    if (!access.`superbWarfare$checkTotemDeathProtection`(source)) {
                        val soundEvent = access.`superbWarfare$getDeathSound`()
                        if (flag1 && soundEvent != null) {
                            entity.playSound(
                                soundEvent,
                                access.`superbWarfare$getSoundVolume`(),
                                entity.voicePitch
                            )
                        }
                        entity.die(source)
                    }
                } else if (flag1) {
                    access.`superbWarfare$playHurtSound`(source)
                }

                entity.lastDamageSource = source
                entity.lastDamageStamp = entity.level().gameTime

                if (entity is ServerPlayer) {
                    CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(entity, source, damage, damage, flag)
                }

                if (sourceEntity is ServerPlayer) {
                    CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(sourceEntity, entity, source, damage, damage, flag)
                }

                return true
            }
        }
        return false
    }

    fun getDamageInfo(vehicle: VehicleEntity, source: DamageSource, amount: Float): MutableComponent {
        val detailedDamageResult = vehicle.getDamageModifier().matchResult(vehicle, source, amount)
        val finalDamage =
            if (detailedDamageResult.isEmpty()) amount else detailedDamageResult[detailedDamageResult.size - 1].damage

        val details = Component.empty()
            .append(
                Component.translatable(
                    "des.superbwarfare.vehicle_damage_analyzer.info.raw",
                    format2D(amount.toDouble()) + "\n"
                ).withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.UNDERLINE)
            )
            .append(Component.empty().withStyle(ChatFormatting.RESET))
            .append(integrateInfo(detailedDamageResult))
            .append(
                Component.translatable(
                    "des.superbwarfare.vehicle_damage_analyzer.info.final",
                    format2D(finalDamage.toDouble())
                ).withStyle(ChatFormatting.GREEN)
            )

        return Component.literal("[").append(vehicle.displayName)
            .append(Component.literal("] ").withStyle(ChatFormatting.WHITE))
            .append(
                Component.translatable(
                    "des.superbwarfare.vehicle_damage_analyzer.info.raw",
                    format2D(amount.toDouble())
                ).withStyle(ChatFormatting.YELLOW)
            )
            .append(Component.literal(" => ").withStyle(ChatFormatting.WHITE))
            .append(
                Component.translatable(
                    "des.superbwarfare.vehicle_damage_analyzer.info.final",
                    format2D(finalDamage.toDouble())
                ).withStyle(ChatFormatting.GREEN)
            )
            .withStyle {
                it.withHoverEvent(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        details
                    )
                )
            }
    }

    private fun integrateInfo(results: MutableList<ModifyResult>): MutableComponent {
        var info = Component.empty()
        for (result in results) {
            info = info.append(result.getDamageInfo()).append(Component.literal("\n").withStyle(ChatFormatting.RESET))
        }
        return info
    }
}
