package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class JHPBullet extends AmmoPerk {

    public JHPBullet() {
        super(new AmmoPerk.Builder("jhp_bullet", Perk.Type.AMMO).bypassArmorRate(-0.2f).damageRate(1.1f).speedRate(0.95f).slug(true).rgb(230, 131, 65));
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        super.modifyProjectile(data, instance, entity);
        if (!(entity instanceof ProjectileEntity projectile)) return;
        projectile.jhpBullet(instance.level());
    }
}
