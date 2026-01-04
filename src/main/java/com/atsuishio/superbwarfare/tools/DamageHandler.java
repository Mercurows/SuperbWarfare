package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.entity.mixin.DamageAccess;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

import java.util.List;

public class DamageHandler {

    public static boolean doDamage(Entity entity, DamageSource source, float damage) {
        if (entity.hurt(source, damage)) {
            return true;
        } else if (entity instanceof LivingEntity living) {
            if (!MiscConfig.FORCE_DAMAGE.get()) {
                return false;
            }
            if (living.isInvulnerableTo(source)) {
                return false;
            } else if (living.level().isClientSide) {
                return false;
            } else if (living.isDeadOrDying()) {
                return false;
            } else if (source.is(DamageTypeTags.IS_FIRE) && living.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                return false;
            } else if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
                return false;
            } else {
                DamageAccess damageAccess = DamageAccess.of(living);

                damageAccess.superbwarfare$getDamageContainers().push(new DamageContainer(source, damage));
                if (CommonHooks.onEntityIncomingDamage(living, damageAccess.superbwarfare$getDamageContainers().peek())) {
                    return false;
                } else {
                    if (living.isSleeping() && !living.level().isClientSide) {
                        living.stopSleeping();
                    }

                    living.setNoActionTime(0);
                    damage = damageAccess.superbwarfare$getDamageContainers().peek().getNewDamage();
                    float f = damage;
                    boolean flag = false;

                    if (source.is(DamageTypeTags.IS_FREEZING) && living.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                        damage *= 5F;
                    }

                    if (source.is(DamageTypeTags.DAMAGES_HELMET) && !living.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                        damageAccess.superbWarfare$hurtHelmet(source, damage);
                        damage *= 0.75F;
                    }

                    damageAccess.superbwarfare$getDamageContainers().peek().setNewDamage(damage);
                    living.walkAnimation.setSpeed(1.5F);
                    boolean flag1 = true;
                    if ((float) living.invulnerableTime > 10F && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
                        if (damage <= living.lastHurt) {
                            damageAccess.superbwarfare$getDamageContainers().pop();
                            return false;
                        }

                        damageAccess.superbWarfare$actuallyHurt(source, damage - living.lastHurt);
                        living.lastHurt = damage;
                        flag1 = false;
                    } else {
                        living.lastHurt = damage;
                        living.invulnerableTime = damageAccess.superbwarfare$getDamageContainers().peek().getPostAttackInvulnerabilityTicks();
                        damageAccess.superbWarfare$actuallyHurt(source, damage);
                        living.hurtDuration = 10;
                        living.hurtTime = living.hurtDuration;
                    }

                    damage = damageAccess.superbwarfare$getDamageContainers().peek().getNewDamage();
                    entity = source.getEntity();
                    if (entity != null) {
                        if (entity instanceof LivingEntity livingentity1) {
                            if (!source.is(DamageTypeTags.NO_ANGER) && (!source.is(DamageTypes.WIND_CHARGE) || !living.getType().is(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE))) {
                                living.setLastHurtByMob(livingentity1);
                            }
                        }

                        if (entity instanceof Player player1) {
                            living.lastHurtByPlayerTime = 100;
                            living.setLastHurtByPlayer(player1);
                        } else if (entity instanceof TamableAnimal tamableAnimal) {
                            if (tamableAnimal.isTame()) {
                                living.lastHurtByPlayerTime = 100;
                                LivingEntity var12 = tamableAnimal.getOwner();
                                if (var12 instanceof Player) {
                                    living.setLastHurtByPlayer((Player) var12);
                                } else {
                                    living.setLastHurtByPlayer(null);
                                }
                            }
                        }
                    }

                    if (flag1) {
                        living.level().broadcastDamageEvent(living, source);

                        if (!source.is(DamageTypeTags.NO_IMPACT)) {
                            living.hurtMarked = true;
                        }

                        if (!source.is(DamageTypeTags.NO_KNOCKBACK)) {
                            double d0 = 0;
                            double d1 = 0;
                            Entity var14 = source.getDirectEntity();
                            if (var14 instanceof Projectile projectile) {
                                DoubleDoubleImmutablePair doubledoubleimmutablepair = projectile.calculateHorizontalHurtKnockbackDirection(living, source);
                                d0 = -doubledoubleimmutablepair.leftDouble();
                                d1 = -doubledoubleimmutablepair.rightDouble();
                            } else if (source.getSourcePosition() != null) {
                                d0 = source.getSourcePosition().x() - living.getX();
                                d1 = source.getSourcePosition().z() - living.getZ();
                            }

                            living.knockback(0.4000000059604645, d0, d1);
                            if (!flag) {
                                living.indicateDamage(d0, d1);
                            }
                        }
                    }

                    if (living.isDeadOrDying()) {
                        if (!damageAccess.superbWarfare$checkTotemDeathProtection(source)) {
                            if (flag1) {
                                living.makeSound(damageAccess.superbWarfare$getDeathSound());
                            }

                            living.die(source);
                        }
                    } else if (flag1) {
                        damageAccess.superbWarfare$playHurtSound(source);
                    }

                    living.lastDamageSource = source;
                    living.lastDamageStamp = living.level().getGameTime();

                    for (MobEffectInstance mobeffectinstance : living.getActiveEffects()) {
                        mobeffectinstance.onMobHurt(living, source, damage);
                    }

                    if (living instanceof ServerPlayer) {
                        CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer) living, source, f, damage, flag);
                    }

                    if (entity instanceof ServerPlayer) {
                        CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer) entity, living, source, f, damage, flag);
                    }

                    damageAccess.superbwarfare$getDamageContainers().pop();
                    return true;
                }
            }
        }
        return false;
    }

    public static MutableComponent getDamageInfo(VehicleEntity vehicle, DamageSource source, float damage) {
        var detailedDamageResult = vehicle.getDamageModifier().matchResult(source, damage);
        float finalDamage = detailedDamageResult.isEmpty() ? damage : detailedDamageResult.getLast().damage();

        var details = Component.empty()
                .append(Component.translatable("des.superbwarfare.vehicle_damage_analyzer.info.raw", FormatTool.format2D(damage) + "\n").withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.UNDERLINE))
                .append(Component.empty().withStyle(ChatFormatting.RESET))
                .append(integrateInfo(detailedDamageResult))
                .append(Component.translatable("des.superbwarfare.vehicle_damage_analyzer.info.final", FormatTool.format2D(finalDamage)).withStyle(ChatFormatting.GREEN));

        return Component.literal("[").append(vehicle.getDisplayName()).append(Component.literal("] ").withStyle(ChatFormatting.WHITE))
                .append(Component.translatable("des.superbwarfare.vehicle_damage_analyzer.info.raw", FormatTool.format2D(damage)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" => ").withStyle(ChatFormatting.WHITE))
                .append(Component.translatable("des.superbwarfare.vehicle_damage_analyzer.info.final", FormatTool.format2D(finalDamage)).withStyle(ChatFormatting.GREEN))
                .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, details)));
    }

    private static MutableComponent integrateInfo(List<DamageModifier.ModifyResult> results) {
        var info = Component.empty();
        for (var result : results) {
            info = info.append(result.getDamageInfo()).append(Component.literal("\n").withStyle(ChatFormatting.RESET));
        }
        return info;
    }
}
