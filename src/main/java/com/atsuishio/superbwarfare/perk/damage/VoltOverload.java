package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class VoltOverload extends Perk {

    public VoltOverload() {
        super("volt_overload", Perk.Type.DAMAGE);
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        if (entity instanceof TaserBulletEntity taserBulletEntity) {
            taserBulletEntity.setVolt(instance.level());
        }
    }
}
