package com.atsuishio.superbwarfare.perk.damage;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.GunGrenadeEntity;
import com.atsuishio.superbwarfare.entity.projectile.JavelinMissileEntity;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketEntity;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class MonsterHunter extends Perk {

    public MonsterHunter() {
        super("monster_hunter", Perk.Type.DAMAGE);
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        float multiplier = 0.1f + 0.1f * instance.level();
        if (entity instanceof ProjectileEntity projectile) {
            projectile.setMonsterMultiplier(multiplier);
        } else if (entity instanceof JavelinMissileEntity projectile) {
            projectile.setMonsterMultiplier(multiplier);
        } else if (entity instanceof GunGrenadeEntity projectile) {
            projectile.setMonsterMultiplier(multiplier);
        } else if (entity instanceof RpgRocketEntity projectile) {
            projectile.setMonsterMultiplier(multiplier);
        }
    }
}
