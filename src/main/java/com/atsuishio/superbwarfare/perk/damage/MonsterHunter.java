package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;

public class MonsterHunter extends Perk {

    public MonsterHunter() {
        super("monster_hunter", Perk.Type.DAMAGE);
    }

    @Override
    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (target instanceof Monster) {
            return damage * (1.1f + 0.1f * instance.level());
        }
        return super.getModifiedDamage(damage, data, instance, target, source);
    }
}
