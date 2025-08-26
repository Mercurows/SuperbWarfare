package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.mixin.CupidLove;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;

public class CupidArrow extends AmmoPerk {

    public CupidArrow() {
        super(new AmmoPerk.Builder("cupid_arrow", Perk.Type.AMMO).damageRate(0.0f).slug().rgb(255, 185, 215));
    }

    @Override
    public void onHit(LivingEntity attacker, GunData data, PerkInstance instance, Entity target) {
        super.onHit(attacker, data, instance, target);

        int perkLevel = instance.level();
        var list = target.level().getEntities((Entity) null, target.getBoundingBox().inflate(perkLevel * 0.25), e -> e instanceof AgeableMob);

        for (var entity : list) {
            if (entity instanceof Animal animal && animal.canFallInLove() && attacker instanceof Player player) {
                animal.setInLove(player);
            }
            if (entity instanceof Villager villager && !villager.isBaby()) {
                CupidLove cupidLove = CupidLove.getInstance(villager);
                cupidLove.superbwarfare$setCupidLove(true);

                if (villager.canBreed()) {
                    villager.getBrain().setActiveActivityIfPossible(Activity.IDLE);
                    villager.getBrain().addActivity(Activity.IDLE, ImmutableList.of(Pair.of(1, new VillagerMakeLove())));
                }
            }

            if (perkLevel >= 10) {
                if (entity instanceof AgeableMob ageableMob && ageableMob.isBaby()) {
                    ageableMob.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-ageableMob.getAge()) * (int) (Math.max(1, perkLevel - 10) / 5d), true);
                }
            }

            if (entity.level() instanceof ServerLevel serverLevel) {
                double d0 = serverLevel.random.nextGaussian() * 0.02D;
                double d1 = serverLevel.random.nextGaussian() * 0.02D;
                double d2 = serverLevel.random.nextGaussian() * 0.02D;
                ParticleTool.sendParticle(serverLevel, ParticleTypes.HEART, entity.getRandomX(1.0D), entity.getRandomY() + 0.5D, entity.getRandomZ(1.0D),
                        5, d0, d1, d2, 0.1, false);
            }
        }
    }
}
