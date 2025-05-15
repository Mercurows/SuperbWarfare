package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class APBullet extends AmmoPerk {

    public APBullet() {
        super(new AmmoPerk.Builder("ap_bullet", Perk.Type.AMMO).bypassArmorRate(0.4f).damageRate(0.9f).speedRate(1.2f).slug(true).rgb(230, 70, 35));
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        super.modifyProjectile(data, instance, entity);
        if (!(entity instanceof ProjectileEntity projectile)) return;
        projectile.bypassArmorRate((float) Math.max(data.bypassArmor() + this.bypassArmorRate + 0.05f * (instance.level() - 1), 0));
    }
}
