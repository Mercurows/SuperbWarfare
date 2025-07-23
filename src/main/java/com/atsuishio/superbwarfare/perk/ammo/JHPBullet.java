package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

public class JHPBullet extends AmmoPerk {

    public JHPBullet() {
        super(new AmmoPerk.Builder("jhp_bullet", Perk.Type.AMMO).bypassArmorRate(-0.2f).damageRate(1.1f).speedRate(0.95f).slug(true).rgb(230, 131, 65));
    }

    @Override
    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, @Nullable Entity target, DamageSource source) {
        if (target != null) {
            double armor = target instanceof LivingEntity living ? living.getAttributeValue(Attributes.ARMOR) : 0;
            return damage * (1.0f + 0.15f * instance.level()) * ((float) (400 / (Math.pow(armor, 2) + 400)) + 0.2f);
        }
        return super.getModifiedDamage(damage, data, instance, null, source);
    }
}
