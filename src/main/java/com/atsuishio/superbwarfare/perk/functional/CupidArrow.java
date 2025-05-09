package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.Nullable;

public class CupidArrow extends Perk {

    public CupidArrow() {
        super("cupid_arrow", Perk.Type.FUNCTIONAL);
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
    }

    @Override
    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, @Nullable LivingEntity target, DamageSource source) {
        return 0;
    }

    @Override
    public boolean shouldCancelHurtEvent(float damage, GunData data, PerkInstance instance, LivingEntity target, DamageSource source) {
        return true;
    }

    @Override
    public double getDisplayDamage(double damage, GunData data, PerkInstance instance) {
        return 0;
    }

    @Override
    public double getExtraDisplayDamage(double damage, GunData data, PerkInstance instance) {
        return 0;
    }
}
