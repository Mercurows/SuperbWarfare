package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public class GutshotStraight extends Perk {

    public GutshotStraight() {
        super("gutshot_straight", Perk.Type.DAMAGE);
    }

    @Override
    public float getModifiedDamage(float damage, GunData data, PerkInstance instance, Entity target, DamageSource source) {
        if (source != null && DamageTypeTool.isGunFireDamage(source) && source.getDirectEntity() instanceof ProjectileEntity projectile && projectile.isZoom()) {
            return damage * (1.15f + 0.05f * instance.level());
        }
        return super.getModifiedDamage(damage, data, instance, target, source);
    }
}
