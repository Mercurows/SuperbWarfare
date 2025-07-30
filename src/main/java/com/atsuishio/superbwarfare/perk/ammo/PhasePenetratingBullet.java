package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class PhasePenetratingBullet extends AmmoPerk {

    public PhasePenetratingBullet() {
        super(new Builder("phase_penetrating_bullet", Type.AMMO).damageRate(0.7f).speedRate(1.5f).rgb(255, 255, 255));
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        super.modifyProjectile(data, instance, entity);
        if (!(entity instanceof ProjectileEntity projectile)) return;
        projectile.setPenetrating(true);
    }
}
