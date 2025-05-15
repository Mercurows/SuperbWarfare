package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class IncendiaryBullet extends AmmoPerk {

    public IncendiaryBullet() {
        super(new AmmoPerk.Builder("incendiary_bullet", Perk.Type.AMMO).bypassArmorRate(-0.4f).damageRate(0.7f).speedRate(0.75f).slug(false).rgb(230, 131, 65));
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        super.modifyProjectile(data, instance, entity);
        if (!(entity instanceof ProjectileEntity projectile)) return;
        projectile.fireBullet(instance.level(), data.stack.is(ModTags.Items.SHOTGUN));
    }

    @Override
    public double getModifiedVelocity(GunData data, PerkInstance instance) {
        return data.stack.is(ModTags.Items.SHOTGUN) ? 4.5f : super.getModifiedVelocity(data, instance);
    }
}
