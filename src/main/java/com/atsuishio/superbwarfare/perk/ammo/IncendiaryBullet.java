package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class IncendiaryBullet extends AmmoPerk {

    public IncendiaryBullet() {
        super(new AmmoPerk.Builder("incendiary_bullet", Perk.Type.AMMO).bypassArmorRate(-0.4f).damageRate(0.7f).speedRate(0.75f).slug().rgb(230, 131, 65)
                .mobEffect(() -> ModMobEffects.BURN));
    }

    @Override
    public DefaultGunData computeProperties(GunData gunData, DefaultGunData rawData) {
        rawData.velocity = gunData.isShotgun() ? 4.5f : rawData.velocity;
        return super.computeProperties(gunData, rawData);
    }

    @Override
    public int getEffectDuration(PerkInstance instance) {
        return 60 + 20 * instance.level();
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        super.modifyProjectile(data, instance, entity);
        if (!(entity instanceof ProjectileEntity projectile)) return;
        projectile.fireBullet(instance.level(), data.isShotgun());
    }
}
