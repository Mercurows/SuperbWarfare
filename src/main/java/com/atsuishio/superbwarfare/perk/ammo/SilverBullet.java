package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class SilverBullet extends AmmoPerk {

    public SilverBullet() {
        super(new AmmoPerk.Builder("silver_bullet", Perk.Type.AMMO).bypassArmorRate(0.05f).damageRate(0.8f).speedRate(1.1f).rgb(87, 166, 219));
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        super.modifyProjectile(data, instance, entity);
        if (!(entity instanceof ProjectileEntity projectile)) return;
        projectile.undeadMultiple(1.0f + 0.5f * instance.level());
    }
}
