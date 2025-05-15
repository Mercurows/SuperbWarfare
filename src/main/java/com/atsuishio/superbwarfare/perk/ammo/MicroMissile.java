package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ExplosiveProjectile;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class MicroMissile extends AmmoPerk {

    public MicroMissile() {
        super(new AmmoPerk.Builder("micro_missile", Perk.Type.AMMO).speedRate(1.2f));
    }

    // TODO 正确设计和实现伤害修改
    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        float radius = (float) (data.explosionRadius() * 0.5f);
        float damage = (float) data.explosionDamage() * (1.1f + instance.level() * 0.1f);
        entity.setNoGravity(true);
        if (entity instanceof ExplosiveProjectile projectile) {
            projectile.setExplosionRadius(radius);
            projectile.setExplosionDamage(damage);
        }
    }

    @Override
    public double getDisplayDamage(double damage, GunData data, PerkInstance instance) {
        return damage * (1.1f + instance.level() * 0.1f);
    }
}
