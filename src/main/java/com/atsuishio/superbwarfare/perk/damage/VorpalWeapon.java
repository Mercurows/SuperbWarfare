package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class VorpalWeapon extends Perk {

    public VorpalWeapon() {
        super("vorpal_weapon", Perk.Type.DAMAGE);
    }

    @Override
    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, @Nullable LivingEntity target, DamageSource source) {
        if (DamageTypeTool.isGunDamage(source) || source.is(ModDamageTypes.PROJECTILE_BOOM)) {
            if (target != null && target.getHealth() >= 100.0f) {
                return (float) (damage + target.getHealth() * 0.00002f * Math.pow(instance.level(), 2));
            }
        }
        return super.getModifiedDamage(damage, data, instance, target, source);
    }
}
