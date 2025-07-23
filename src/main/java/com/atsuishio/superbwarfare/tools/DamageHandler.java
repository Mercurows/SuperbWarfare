package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.entity.mixin.DamageAccess;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

public class DamageHandler {

    public static boolean doDamage(Entity entity, DamageSource source, float damage) {
        if (entity.hurt(source, damage)) {
            return true;
        } else if (entity instanceof LivingEntity living) {
            if (living.isInvulnerableTo(source)) {
                return false;
            } else if (living.level().isClientSide) {
                return false;
            } else if (living.isDeadOrDying()) {
                return false;
            } else if (source.is(DamageTypeTags.IS_FIRE) && living.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                return false;
            } else {
                if (living.isSleeping() && !living.level().isClientSide) {
                    living.stopSleeping();
                }
                living.setNoActionTime(0);

                DamageAccess access = DamageAccess.of(living);

                boolean flag = false;

                living.walkAnimation.setSpeed(1.5F);

                boolean flag1 = true;
                if (living.invulnerableTime > 10.0F && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
                    if (damage <= living.lastHurt) {
                        return false;
                    }

                    access.superbWarfare$actuallyHurt(source, damage - living.lastHurt);
                    living.lastHurt = damage;
                    flag1 = false;
                } else {
                    living.lastHurt = damage;
                    living.invulnerableTime = 20;
                    access.superbWarfare$actuallyHurt(source, damage);
                    living.hurtDuration = 10;
                    living.hurtTime = living.hurtDuration;
                }

                if (source.is(DamageTypeTags.DAMAGES_HELMET) && !living.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                    access.superbWarfare$hurtHelmet(source, damage);
                    damage *= 0.75F;
                }

                Entity entity1 = source.getEntity();
                if (entity1 != null) {
                    if (entity1 instanceof LivingEntity livingEntity) {
                        if (!source.is(DamageTypeTags.NO_ANGER)) {
                            living.setLastHurtByMob(livingEntity);
                        }
                    }

                    if (entity1 instanceof Player p) {
                        living.lastHurtByPlayerTime = 100;
                        living.setLastHurtByPlayer(p);
                    } else if (entity1 instanceof TamableAnimal tamableEntity) {
                        if (tamableEntity.isTame()) {
                            living.lastHurtByPlayerTime = 100;
                            if (tamableEntity.getOwner() instanceof Player player) {
                                living.setLastHurtByPlayer(player);
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

                    if (entity1 != null && !source.is(DamageTypeTags.IS_EXPLOSION)) {
                        double d0 = entity1.getX() - living.getX();

                        double d1;
                        for (d1 = entity1.getZ() - living.getZ(); d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
                            d0 = (Math.random() - Math.random()) * 0.01D;
                        }

                        living.knockback(0.4F, d0, d1);
                        if (!flag) {
                            living.indicateDamage(d0, d1);
                        }
                    }
                }

                if (living.isDeadOrDying()) {
                    if (!access.superbWarfare$checkTotemDeathProtection(source)) {
                        SoundEvent soundevent = access.superbWarfare$getDeathSound();
                        if (flag1 && soundevent != null) {
                            living.playSound(soundevent, access.superbWarfare$getSoundVolume(), living.getVoicePitch());
                        }
                        living.die(source);
                    }
                } else if (flag1) {
                    access.superbWarfare$playHurtSound(source);
                }

                living.lastDamageSource = source;
                living.lastDamageStamp = living.level().getGameTime();

                if (living instanceof ServerPlayer) {
                    CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer) living, source, damage, damage, flag);
                }

                if (entity1 instanceof ServerPlayer) {
                    CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer) entity1, living, source, damage, damage, flag);
                }

                return true;
            }
        }
        return false;
    }
}
