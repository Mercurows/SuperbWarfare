package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class LongerWire extends AmmoPerk {

    public LongerWire() {
        super("longer_wire", Perk.Type.AMMO);
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        if (entity instanceof TaserBulletEntity taserBulletEntity) {
            taserBulletEntity.setWireLength(instance.level());
        }
    }
}
