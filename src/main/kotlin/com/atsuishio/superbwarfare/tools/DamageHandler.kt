package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.entity.mixin.DamageAccess
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier.ModifyResult
import com.atsuishio.superbwarfare.tools.DamageHandler.doDamage
import com.atsuishio.superbwarfare.tools.FormatTool.format2D
import net.minecraft.ChatFormatting
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.DamageTypeTags
import net.minecraft.tags.EntityTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.neoforged.neoforge.common.CommonHooks
import net.neoforged.neoforge.common.damagesource.DamageContainer

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

                val damageAccess = DamageAccess.of(entity)
                val container = damageAccess.`superbwarfare$getDamageContainers`() ?: return false
                container.push(DamageContainer(source, damage))

                if (CommonHooks.onEntityIncomingDamage(entity, container.peek())) {
                    return false
                } else {
                    if (entity.isSleeping && !entity.level().isClientSide) {
                        entity.stopSleeping()
                    }

                    entity.noActionTime = 0
                    damage = container.peek().newDamage
                    val f = damage
                    val flag = false

                    if (source.`is`(DamageTypeTags.IS_FREEZING) && entity.type.`is`(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                        damage *= 5f
                    }

                    if (source.`is`(DamageTypeTags.DAMAGES_HELMET) && !entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty) {
                        damageAccess.`superbWarfare$hurtHelmet`(source, damage)
                        damage *= 0.75f
                    }

                    container.peek().newDamage = damage
                    entity.walkAnimation.setSpeed(1.5f)

                    var flag1 = true
                    if (entity.invulnerableTime > 10f && !source.`is`(DamageTypeTags.BYPASSES_COOLDOWN)) {
                        if (damage <= entity.lastHurt) {
                            container.pop()
                            return false
                        }

                        damageAccess.`superbWarfare$actuallyHurt`(source, damage - entity.lastHurt)
                        entity.lastHurt = damage
                        flag1 = false
                    } else {
                        entity.lastHurt = damage
                        entity.invulnerableTime = container.peek().postAttackInvulnerabilityTicks
                        damageAccess.`superbWarfare$actuallyHurt`(source, damage)
                        entity.hurtDuration = 10
                        entity.hurtTime = entity.hurtDuration
                    }

                    damage = container.peek().newDamage

                    if (sourceEntity != null) {
                        if (sourceEntity is LivingEntity) {
                            if (!source.`is`(DamageTypeTags.NO_ANGER) &&
                                (!source.`is`(DamageTypes.WIND_CHARGE) || !entity.type.`is`(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE))
                            ) {
                                entity.lastHurtByMob = sourceEntity
                            }
                        }

                        if (sourceEntity is Player) {
                            entity.lastHurtByPlayerTime = 100
                            entity.setLastHurtByPlayer(sourceEntity)
                        } else if (sourceEntity is TamableAnimal && sourceEntity.isTame) {
                            entity.lastHurtByPlayerTime = 100
                            val owner = sourceEntity.owner
                            entity.setLastHurtByPlayer(owner as? Player)
                        }
                    }

                    if (flag1) {
                        entity.level().broadcastDamageEvent(entity, source)

                        if (!source.`is`(DamageTypeTags.NO_IMPACT)) {
                            entity.hurtMarked = true
                        }

                        if (!source.`is`(DamageTypeTags.NO_KNOCKBACK)) {
                            var d0 = 0.0
                            var d1 = 0.0
                            val directEntity = source.directEntity
                            if (directEntity is Projectile) {
                                val pair = directEntity.calculateHorizontalHurtKnockbackDirection(entity, source)
                                d0 = -pair.leftDouble()
                                d1 = -pair.rightDouble()
                            } else if (source.sourcePosition != null) {
                                d0 = source.sourcePosition!!.x() - entity.x
                                d1 = source.sourcePosition!!.z() - entity.z
                            }

                            entity.knockback(0.4000000059604645, d0, d1)
                            if (!flag) {
                                entity.indicateDamage(d0, d1)
                            }
                        }
                    }

                    if (entity.isDeadOrDying) {
                        if (!damageAccess.`superbWarfare$checkTotemDeathProtection`(source)) {
                            if (flag1) {
                                entity.makeSound(damageAccess.`superbWarfare$getDeathSound`())
                            }

                            entity.die(source)
                        }
                    } else if (flag1) {
                        damageAccess.`superbWarfare$playHurtSound`(source)
                    }

                    entity.lastDamageSource = source
                    entity.lastDamageStamp = entity.level().gameTime

                    for (instance in entity.activeEffects) {
                        instance.onMobHurt(entity, source, damage)
                    }

                    if (entity is ServerPlayer) {
                        CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(entity, source, f, damage, flag)
                    }

                    if (sourceEntity is ServerPlayer) {
                        CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(sourceEntity, entity, source, f, damage, flag)
                    }

                    container.pop()
                    return true
                }
            }
        }
        return false
    }

    fun getDamageInfo(vehicle: VehicleEntity, source: DamageSource, amount: Float): MutableComponent {
        val detailedDamageResult = vehicle.getDamageModifier().matchResult(source, amount)
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

        return Component.literal("[").append(vehicle.displayName ?: Component.empty())
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
