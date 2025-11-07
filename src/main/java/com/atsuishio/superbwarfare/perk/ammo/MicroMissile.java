package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class MicroMissile extends AmmoPerk {

    public MicroMissile() {
        super(new AmmoPerk.Builder("micro_missile", Perk.Type.AMMO).speedRate(1.2f));
    }

    @Override
    public DefaultGunData computeProperties(GunData gunData, DefaultGunData rawData) {
        rawData.explosionDamage *= 0.8 + gunData.perk.getLevel(this) * 0.1;
        rawData.explosionRadius *= 0.5;
        rawData.gravity = 0;
        return super.computeProperties(gunData, rawData);
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        entity.setNoGravity(true);
    }
}
