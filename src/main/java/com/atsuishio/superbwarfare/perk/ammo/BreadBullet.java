package com.atsuishio.superbwarfare.perk.ammo;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;

public class BreadBullet extends AmmoPerk {

    public BreadBullet() {
        super(new AmmoPerk.Builder("bread_bullet", Perk.Type.AMMO)
//                .bypassArmorRate(1.0f).damageRate(0.5f).speedRate(0.6f).rgb(0xde, 0xab, 0x82).mobEffect(() -> MobEffects.MOVEMENT_SLOWDOWN)
        );
    }

    @Override
    public int getEffectAmplifier(PerkInstance instance) {
        return 1;
    }

    @Override
    public void modifyProjectile(GunData data, PerkInstance instance, Entity entity) {
        super.modifyProjectile(data, instance, entity);
        if (!(entity instanceof ProjectileEntity projectile)) return;
        projectile.knockback(instance.level() * 0.3f);
        projectile.forceKnockback();
    }
}
