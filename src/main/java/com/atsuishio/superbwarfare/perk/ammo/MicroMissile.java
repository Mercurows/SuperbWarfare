package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class MicroMissile extends AmmoPerk {

    public MicroMissile() {
        super(new AmmoPerk.Builder("micro_missile", Perk.Type.AMMO).speedRate(1.2f));
        appendModification(GunProp.EXPLOSION_DAMAGE, (data, damage) -> damage * (0.8 + data.perk.getLevel(this) * 0.1));
        appendModification(GunProp.EXPLOSION_RADIUS, radius -> radius * 0.5);
        appendModification(GunProp.GRAVITY, g -> Double.valueOf(0));
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        entity.setNoGravity(true);
    }
}
