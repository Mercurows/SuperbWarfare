package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class HEBullet extends AmmoPerk {

    public HEBullet() {
        super(new AmmoPerk.Builder("he_bullet", Perk.Type.AMMO).bypassArmorRate(-0.3f).damageRate(0.5f).speedRate(0.85f).slug(true).rgb(240, 20, 10));
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        super.modifyProjectile(data, instance, entity);
        if (!(entity instanceof ProjectileEntity projectile)) return;
        projectile.heBullet(instance.level());
    }

    @Override
    public double getExtraDisplayDamage(double damage, GunData data, PerkInstance instance) {
        return 0.8 * damage * (1 + 0.1 * instance.level());
    }
}
