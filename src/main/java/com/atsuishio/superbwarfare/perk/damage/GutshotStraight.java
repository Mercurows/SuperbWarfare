package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;

public class GutshotStraight extends Perk {

    public GutshotStraight() {
        super("gutshot_straight", Perk.Type.DAMAGE);
        appendModification(GunProp.DAMAGE, (data, value, target, source) -> {
            if (source != null && DamageTypeTool.isGunFireDamage(source) && source.getDirectEntity() instanceof ProjectileEntity projectile && projectile.isZoom()) {
                return value * (1.15 + 0.05 * data.perk.getLevel(this));
            }
            return value;
        });
    }
}
