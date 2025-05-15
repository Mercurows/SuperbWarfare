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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.Nullable;

public class CupidArrow extends AmmoPerk {

    public CupidArrow() {
        super(new AmmoPerk.Builder("cupid_arrow", Perk.Type.AMMO).bypassArmorRate(0.0f).damageRate(1.0f).speedRate(1.0f).slug(true).rgb(255, 185, 215));
    }

    @Override
    public void onHit(float damage, GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        Player attacker = null;
        if (source.getEntity() instanceof Player player) {
            attacker = player;
        }
        if (source.getDirectEntity() instanceof Projectile p && p.getOwner() instanceof Player player) {
            attacker = player;
        }

        if (target instanceof Animal animal && animal.canFallInLove()) {
            animal.setInLove(attacker);
        }
        if (target instanceof Villager villager && !villager.isBaby()) {
            CupidLove cupidLove = CupidLove.getInstance(villager);
            cupidLove.superbwarfare$setCupidLove(true);

            if (villager.canBreed()) {
                villager.getBrain().setActiveActivityIfPossible(Activity.IDLE);
                villager.getBrain().addActivity(Activity.IDLE, ImmutableList.of(Pair.of(1, new VillagerMakeLove())));
            }
        }

        if (target.level() instanceof ServerLevel serverLevel) {
            double d0 = serverLevel.random.nextGaussian() * 0.02D;
            double d1 = serverLevel.random.nextGaussian() * 0.02D;
            double d2 = serverLevel.random.nextGaussian() * 0.02D;
            ParticleTool.sendParticle(serverLevel, ParticleTypes.HEART, target.getRandomX(1.0D), target.getRandomY() + 0.5D, target.getRandomZ(1.0D),
                    5, d0, d1, d2, 0.1, false);
        }
    }

    @Override
    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, @Nullable LivingEntity target, DamageSource source) {
        return 0;
    }

    @Override
    public boolean shouldCancelHurtEvent(float damage, GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        return true;
    }
}
